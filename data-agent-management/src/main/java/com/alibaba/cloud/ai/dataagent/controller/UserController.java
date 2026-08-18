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
package com.alibaba.cloud.ai.dataagent.controller;

import com.alibaba.cloud.ai.dataagent.config.AuthWebFilter;
import com.alibaba.cloud.ai.dataagent.service.auth.AuthService;
import com.alibaba.cloud.ai.dataagent.vo.ApiResponse;
import com.alibaba.cloud.ai.dataagent.vo.AuthUserVo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final AuthService authService;

	public UserController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping
	public ApiResponse<List<AuthUserVo>> list(ServerWebExchange exchange) {
		AuthUserVo currentUser = exchange.getAttribute(AuthWebFilter.CURRENT_USER_ATTRIBUTE);
		if (currentUser == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "LDAP 登录未启用");
		}
		return ApiResponse.success("获取用户列表成功", authService.listUsers(currentUser));
	}

}
