// JDQRChannelInterceptor.java
package com.example.backend.common.interceptor;

import static com.example.backend.common.enums.OnlineUser.*;

import com.example.backend.common.enums.OnlineUser;
import com.example.backend.common.exception.ErrorCode;
import com.example.backend.common.exception.JDQRException;
import com.example.backend.common.util.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class JDQRChannelInterceptor implements ChannelInterceptor {

	private final TokenProvider tokenProvider;
	private static final String TOKEN_PREFIX = "Bearer ";
	private final RedisTemplate<String,Object> redisTemplate;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		// CONNECT 프레임에 대해서만 처리
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String accessToken = accessor.getFirstNativeHeader("Authorization");
			log.warn("accessToken : {}",accessToken);
			if (!ObjectUtils.isEmpty(accessToken) && accessToken.startsWith(TOKEN_PREFIX)) {
				accessToken = accessToken.substring(TOKEN_PREFIX.length());

				if(accessToken.equals("dummyTableToken")){
					accessor.getSessionAttributes().put("tableId","6721aa9b0d22a923091eef73");
					// 인원 수 증가
					incrementOnlineUserCount("6721aa9b0d22a923091eef73");
					return message;
				}

			} else {
				log.info("Authorization 헤더가 없습니다.");
				throw new IllegalArgumentException("Authorization 헤더가 없습니다.");
			}

			// 토큰 검증 로직 적용
			try {
				if (tokenProvider.validateToken(accessToken)) {
					String tableId = tokenProvider.extractSubject(accessToken);
					// 세션 속성에 사용자 ID 저장
					accessor.getSessionAttributes().put("tableId", tableId);
					// 인원 수 증가
					incrementOnlineUserCount("6721aa9b0d22a923091eef73");
				} else {
					throw new JDQRException(ErrorCode.TOKEN_IS_NOT_VALID);
				}
			} catch (Exception e) {
				log.error("토큰 검증 실패: {}", e.getMessage());
				throw new IllegalArgumentException("토큰 검증 실패");
			}
		}

		return message;
	}
	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		// DISCONNECT 프레임에 대해서만 처리
		if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
			decrementOnlineUserCount("6721aa9b0d22a923091eef73");  // 인원수 감소
		}
	}

	private void incrementOnlineUserCount(String tableId) {
		log.warn("인원수 증가 !!!!");
		redisTemplate.opsForHash().increment(ONLINE_USER.getExplain(), tableId, 1);
	}

	private void decrementOnlineUserCount(String tableId) {
		log.warn("인원수 감소 !!!!");
		Integer currentCount = (Integer) redisTemplate.opsForHash().get(ONLINE_USER.getExplain(), tableId);

		// currentCount가 null이거나 1 이상일 때만 감소시킴
		if (currentCount != null && currentCount > 0) {
			redisTemplate.opsForHash().increment(ONLINE_USER.getExplain(), tableId, -1);
		}
	}

}
