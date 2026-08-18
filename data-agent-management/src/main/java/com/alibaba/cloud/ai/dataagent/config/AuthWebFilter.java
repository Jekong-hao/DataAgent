/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.config;

import com.alibaba.cloud.ai.dataagent.service.auth.AuthService;
import com.alibaba.cloud.ai.dataagent.vo.AuthUserVo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.ai.alibaba.data-agent.auth.ldap.enabled", havingValue = "true")
public class AuthWebFilter implements WebFilter {

	public static final String CURRENT_USER_ATTRIBUTE = AuthWebFilter.class.getName() + ".currentUser";

	private final AuthService authService;

	public AuthWebFilter(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		if (!path.startsWith("/api/") || path.equals("/api/auth/login") || path.equals("/api/auth/logout")
				|| path.equals("/api/auth/status")
				|| exchange.getRequest().getMethod().name().equals("OPTIONS")) {
			return chain.filter(exchange);
		}
		String token = exchange.getRequest().getHeaders().getFirst(AuthService.TOKEN_HEADER);
		if (token == null || token.isBlank()) {
			token = exchange.getRequest().getQueryParams().getFirst(AuthService.TOKEN_HEADER);
		}
		AuthUserVo user = authService.currentUser(token);
		if (user == null) {
			return unauthorized(exchange);
		}
		exchange.getAttributes().put(CURRENT_USER_ATTRIBUTE, user);
		return chain.filter(exchange);
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		byte[] body = "{\"success\":false,\"message\":\"登录已失效\"}".getBytes(StandardCharsets.UTF_8);
		return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
	}

}
