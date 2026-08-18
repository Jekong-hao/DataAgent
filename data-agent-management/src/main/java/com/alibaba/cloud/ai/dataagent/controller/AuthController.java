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

import com.alibaba.cloud.ai.dataagent.dto.AuthLoginRequest;
import com.alibaba.cloud.ai.dataagent.properties.AuthProperties;
import com.alibaba.cloud.ai.dataagent.service.auth.AuthService;
import com.alibaba.cloud.ai.dataagent.vo.ApiResponse;
import com.alibaba.cloud.ai.dataagent.vo.AuthLoginVo;
import com.alibaba.cloud.ai.dataagent.vo.AuthUserVo;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	private final AuthProperties authProperties;

	public AuthController(AuthService authService, AuthProperties authProperties) {
		this.authService = authService;
		this.authProperties = authProperties;
	}

	@GetMapping("/status")
	public ApiResponse<Boolean> status() {
		return ApiResponse.success("获取认证状态成功", authProperties.getLdap().isEnabled());
	}

	@PostMapping("/login")
	public ApiResponse<AuthLoginVo> login(@Valid @RequestBody AuthLoginRequest request) {
		try {
			return ApiResponse.success("登录成功", authService.login(request.getLoginName(), request.getPassword()));
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, exception.getMessage(), exception);
		}
	}

	@GetMapping("/me")
	public ApiResponse<AuthUserVo> currentUser(@RequestHeader(AuthService.TOKEN_HEADER) String token) {
		AuthUserVo user = authService.currentUser(token);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
		}
		return ApiResponse.success("获取当前用户成功", user);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(@RequestHeader(value = AuthService.TOKEN_HEADER, required = false) String token) {
		authService.logout(token);
		return ApiResponse.success("已退出登录");
	}

}
