package com.example.backend.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.example.backend.common.enums.SimpleResponseMessage;
import com.example.backend.dish.entity.Dish;
import com.example.backend.dish.entity.Option;
import com.example.backend.dish.repository.DishRepository;
import com.example.backend.dish.repository.OptionRepository;
import com.example.backend.order.entity.Order;
import com.example.backend.order.entity.OrderItem;
import com.example.backend.order.entity.OrderItemOption;
import com.example.backend.order.enums.OrderStatus;
import com.example.backend.order.repository.OrderItemOptionRepository;
import com.example.backend.order.repository.OrderItemRepository;
import com.example.backend.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.exception.JDQRException;
import com.example.backend.common.redis.repository.RedisHashRepository;
import com.example.backend.common.util.GenerateLink;
import com.example.backend.order.dto.CartRequest.ProductInfo;
import com.example.backend.notification.service.NotificationService;
import com.example.backend.table.entity.Table;
import com.example.backend.table.repository.TableRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

	private final TableRepository tableRepository;
	private final GenerateLink generateLink;
	private final NotificationService notificationService;
	private final RedisHashRepository redisHashRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderItemOptionRepository orderItemOptionRepository;
	private final DishRepository dishRepository;
	private final OptionRepository optionRepository;

	/**
	 * tableName으로 qrCode를 찾아서, 해당 코드에 token을 더한 주소를 반환
	 * @param tableId
	 * @return
	 */
	@Override
	public String redirectUrl(String tableId,String uuid) {
		//1. table을 찾는다
		Table table = tableRepository.findById(tableId)
			.orElseThrow(() -> new JDQRException(ErrorCode.FUCKED_UP_QR));

		log.warn("table : {}",table);

		// 현재 url이 유효하지 않다면 예외를 반환한다
		String targetUrl = GenerateLink.AUTH_PREFIX + "/"+tableId+"/"+uuid;
		if(!targetUrl.equals(table.getQrCode())){
			throw new JDQRException(ErrorCode.FUCKED_UP_QR);
		}

		//2. table의 링크에 token을 생성한다
		String authLink = generateLink.createAuthLink(table.getQrCode(),table.getId());

		return authLink;
	}

	/**
	 * 장바구니에 물품을 담는 메서드
	 * @param tableId
	 * @param productInfo
	 */
	@Override
	public void addItem(String tableId, ProductInfo productInfo) {

		// Redis 장바구니에 물품 담기

		// //우선 productInfo에서 id를 꺼내서 그런 메뉴가 있는지부터 확인
		// int dishId = productInfo.id();
		//
		// dishRepository.findById(dishId); 이런식으로 확인해서 없으면 Exception

//		log.warn("productInfo : {}",productInfo);
//		for(ProductOption o: productInfo.options()){
//			log.warn("option : {}",o);
//		}

		// 테이블 장바구니에 물품을 담는다
		List<ProductInfo> cachedCartData = redisHashRepository.getCartDatas(tableId);
		if(cachedCartData != null){
			cachedCartData.add(productInfo);
			redisHashRepository.saveCartDatas(tableId, cachedCartData);
		}
		else{
			cachedCartData = new ArrayList<>();
			cachedCartData.add(productInfo);
			redisHashRepository.saveCartDatas(tableId, cachedCartData);
		}

		// 전송할 데이터를 가지고온다
		List<ProductInfo> sendDatas = redisHashRepository.getCartDatas(tableId);
		log.warn("sendDatas  :{}",sendDatas);

		notificationService.sentToClient(tableId,sendDatas);
	}

	/**
	 * 유저가 담은 상품의 정보를 db에 저장한다
	 * orders, order_items, order_item_options 테이블 전체를 포함
	 *
	 * @param tableId : 유저가 주문하는 테이블의 id
	 * @return : 성공/실패 여부를 담은 문자열
	 */
	@Override
	@Transactional
	public SimpleResponseMessage saveWholeOrder(String tableId) {

		List<ProductInfo> cartDatas = redisHashRepository.getCartDatas(tableId);

		// redis에 아직 데이터가 없을 경우
		if (cartDatas == null || cartDatas.isEmpty()) {
			return SimpleResponseMessage.ORDER_ITEM_EMPTY;
		}

		// 1. orders table에 데이터를 추가한다
		Order order = saveOrder(cartDatas);

		// 2. order_items에 데이터를 추가한다
		List<OrderItem> orderItems = saveOrderItems(order, cartDatas);

		// 3. order_item_options에 데이터를 추가한다
		saveOrderItemOptions(orderItems, cartDatas);

		return SimpleResponseMessage.ORDER_SUCCESS;
	}

	/**
	 * 유저들이 담은 메뉴들의 정보를 바탕으로, order_item_options table에 데이터를 저장한다
	 *
	 * @param orderItems : order_items table에 저장된 주문 정보
	 * @param cartDatas : 유저들이 담은 메뉴들의 정보
	 */
	private void saveOrderItemOptions(List<OrderItem> orderItems, List<ProductInfo> cartDatas) {

		// validation 체크
		// 두 리스트의 길이가 다르다면 에러 반환
		if (orderItems.size() != cartDatas.size()) {
			throw new JDQRException(ErrorCode.VALIDATION_ERROR_INTERNAL);
		}

		// orderItems와 cartDatas의 같은 위치에 있는 객체는 같은 주문을 나타냄
		List<OrderItemOption> orderItemOptions = new ArrayList<>();

		for (int i = 0; i < orderItems.size(); i++) {
			OrderItem orderItem = orderItems.get(i);
			ProductInfo productInfo = cartDatas.get(i);

			List<Integer> optionIds = productInfo.optionIds();

			orderItemOptions.addAll(getOrderItemOptions(orderItem, optionIds));
		}

		// DB에 저장
		orderItemOptionRepository.saveAll(orderItemOptions);
	}

	// orderItem과 해당 orderItem에 포함된 optionId 리스트를 이용해서, orderItemOption 리스트를 만드는 메서드
	private List<OrderItemOption> getOrderItemOptions(OrderItem orderItem, List<Integer> optionIds) {
		List<Option> options = optionRepository.findAllById(optionIds);

		return options.stream()
			.map(option -> OrderItemOption.builder()
				.orderItem(orderItem)
				.option(option)
				.build())
			.toList();
	}

	/**
	 * 유저들이 담은 메뉴들의 정보를 바탕으로, order_items table에 데이터를 저장한다
	 *
	 * @param order : orders table에 저장된 주문 정보
	 * @param cartDatas : 유저들이 담은 메뉴들의 정보
	 * @return : 저장된 entity list
	 */
	private List<OrderItem> saveOrderItems(Order order, List<ProductInfo> cartDatas) {

		List<OrderItem> orderItems = cartDatas.stream()
			.map(cartData -> productInfoToOrderItem(order, cartData))
			.toList();

		return orderItemRepository.saveAll(orderItems);
	}

	private OrderItem productInfoToOrderItem(Order order, ProductInfo productInfo) {
		Integer dishId = productInfo.dishId();
		Dish dish = dishRepository.findById(dishId)
			.orElseThrow(() -> new JDQRException(ErrorCode.DISH_NOT_FOUND));
		String userId = productInfo.userId();

		return OrderItem.builder()
			.order(order)
			.dish(dish)
			.userId(userId)
			.orderPrice(productInfo.price())
			.quantity(productInfo.quantity())
			.orderedAt(productInfo.orderedAt())
			.build();
	}

	/**
	 * 유저들이 담은 메뉴들의 정보를 바탕으로, orders table에 데이터를 저장한다
	 *
	 * @param cartDatas : 유저들이 담은 메뉴들의 정보
	 * @return : 저장된 entity
	 */
	private Order saveOrder(List<ProductInfo> cartDatas) {
		Order order = Order.builder()
			.orderStatus(OrderStatus.PENDING)
			.menuCnt(cartDatas.size())
			.build();

		return orderRepository.save(order);
	}

}
