package com.example.backend.dish.service;

import static com.example.backend.dish.dto.DishResponse.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.exception.JDQRException;
import com.example.backend.common.redis.repository.RedisHashRepository;
import com.example.backend.dish.dto.DishResponse;
import com.example.backend.dish.dto.DishResponse.DishSimpleInfo;
import com.example.backend.dish.dto.DishResponse.DishSummaryInfo;
import com.example.backend.dish.dto.OptionDto;
import com.example.backend.dish.entity.Choice;
import com.example.backend.dish.entity.Dish;
import com.example.backend.dish.entity.DishCategory;
import com.example.backend.dish.entity.DishOption;
import com.example.backend.dish.entity.DishTag;
import com.example.backend.dish.entity.Option;
import com.example.backend.dish.entity.Tag;
import com.example.backend.dish.repository.DishCategoryRepository;
import com.example.backend.dish.repository.DishOptionRepository;
import com.example.backend.dish.repository.DishRepository;
import com.example.backend.dish.repository.DishTagRepository;
import com.example.backend.etc.entity.Restaurant;
import com.example.backend.etc.repository.RestaurantRepository;
import com.example.backend.table.entity.Table;
import com.example.backend.table.repository.TableRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DishServiceImpl implements DishService {

	private final TableRepository tableRepository;
	private final RestaurantRepository restaurantRepository;
	private final DishRepository dishRepository;
	private final RedisHashRepository redisHashRepository;
	private final DishTagRepository dishTagRepository;
	private final DishOptionRepository dishOptionRepository;

	/**
	 * 음식점의 메뉴판을 조회하는 메서드
	 * @param tableId
	 * @return
	 */
	@Override
	public DishSummaryResultDto getAllDishes(String tableId) {

		//1. 해당 테이블이 존재하는지 조회한다
		Table table = tableRepository.findById(tableId)
			.orElseThrow(() -> new JDQRException(ErrorCode.TABLE_NOT_FOUND));

		//2. 식당을 조회한다
		Restaurant restaurant = restaurantRepository.findById(table.getRestaurantId())
			.orElseThrow(() -> new JDQRException(ErrorCode.RESTAURANT_NOT_FOUND));

		//3. 식당의 메뉴를 조회한다
		List<Dish> dishes = dishRepository.findDishesByRestaurant(restaurant);

		//4. 현재 메뉴판을 보고있는 사람 수를 찾는다
		Integer currentUserCnt = redisHashRepository.getCurrentUserCnt(tableId);

		Map<Integer,String> idToNameMap = new HashMap<>();
		for(Dish dish : dishes) {
			DishCategory dishCategory = dish.getDishCategory();
			idToNameMap.put(dishCategory.getId(), dishCategory.getName());
		}

		Map<Integer,List<DishSimpleInfo>> idToSimpleInfoMap = new HashMap<>();
		for(Dish dish : dishes){

			DishCategory dishCategory = dish.getDishCategory();
			// dishCategory정보 추출
			int dishCategoryId = dishCategory.getId();

			// itmes 항목 채우기
			// 우선, 메뉴의 태그를 가져와야한다
			List<DishTag> dishTags = dishTagRepository.findTagsByDish(dish);
			List<String> tags = dishTags.stream().map(DishTag::getTag)
				.map(Tag::getName).toList();

			DishSimpleInfo dishSimpleInfo = DishSimpleInfo.of(dish,tags);

			// 카테고리 별 SimpleInfo정보를 추가한다
			if(!idToSimpleInfoMap.containsKey(dishCategoryId)){
				List<DishSimpleInfo> dishSimpleInfoList = new ArrayList<>();
				dishSimpleInfoList.add(dishSimpleInfo);
				idToSimpleInfoMap.put(dishCategoryId,dishSimpleInfoList);
			}
			else idToSimpleInfoMap.get(dishCategoryId).add(dishSimpleInfo);
		}

		// 정보를 생성한다
		List<DishSummaryInfo> dishSummaryInfoList = new ArrayList<>();
		for(Map.Entry<Integer,List<DishSimpleInfo>> entry : idToSimpleInfoMap.entrySet()){

			int dishCategoryId = entry.getKey();
			String dishCategoryName = idToNameMap.get(dishCategoryId);
			List<DishSimpleInfo> dishSimpleInfoList = entry.getValue();

			DishSummaryInfo dishSummaryInfo = DishSummaryInfo.builder()
				.dishCategoryId(dishCategoryId)
				.dishCategoryName(dishCategoryName)
				.items(dishSimpleInfoList)
				.build();
			dishSummaryInfoList.add(dishSummaryInfo);
		}
		List<String> dishCategories = idToNameMap.values().stream().map(String::toString).toList();

		// 반환 DTO
		DishSummaryResultDto dishSummaryResultDto = DishSummaryResultDto.builder()
			.tableId(tableId)
			.tableName(table.getName())
			.peopleCount(currentUserCnt)
			.dishCategories(dishCategories)
			.dishes(dishSummaryInfoList)
			.build();

		return dishSummaryResultDto;
	}

	/**
	 * 음식점의 상세메뉴를 조회하는 메서드
	 * @param dishId
	 * @param tableId
	 * @return
	 */
	@Override
	public DishDetailInfo getDish(Integer dishId, String tableId) {

		//1. 테이블을 조회한다
		Table table = tableRepository.findById(tableId)
			.orElseThrow(() -> new JDQRException(ErrorCode.TABLE_NOT_FOUND));

		//2. 메뉴를 찾아온다
		Dish dish = dishRepository.findById(dishId)
			.orElseThrow(() -> new JDQRException(ErrorCode.DISH_NOT_FOUND));

		//3. 메뉴의 옵션들을 가지고온다
		List<DishOption> dishOptions = dishOptionRepository.findByDishId(dishId);

		List<OptionDto> optionDtos = new ArrayList<>();
		for(DishOption dishOption : dishOptions){

			Option option = dishOption.getOption();
			List<Choice> choices = option.getChoices();
			OptionDto optionDto = OptionDto.of(option, choices);

			optionDtos.add(optionDto);
		}

		// 4. 메뉴의 태그들을 가지고온다
		List<DishTag> dishTags = dishTagRepository.findTagsByDish(dish);
		List<String> tags = dishTags.stream().map(DishTag::getTag)
			.map(Tag::getName).toList();

		// 5. 반환 DTO
		DishDetailInfo dishDetailInfo = DishDetailInfo.of(dish,optionDtos,tags);

		return dishDetailInfo;

	}

}
