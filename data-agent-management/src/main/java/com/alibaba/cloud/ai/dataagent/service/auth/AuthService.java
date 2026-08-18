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
package com.alibaba.cloud.ai.dataagent.service.auth;

import com.alibaba.cloud.ai.dataagent.properties.AuthProperties;
import com.alibaba.cloud.ai.dataagent.vo.AuthLoginVo;
import com.alibaba.cloud.ai.dataagent.vo.AuthUserVo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;

@Service
public class AuthService {

	public static final String TOKEN_HEADER = "data_agent_access_token";

	private final JdbcTemplate jdbcTemplate;

	private final AuthProperties properties;

	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(JdbcTemplate jdbcTemplate, AuthProperties properties) {
		this.jdbcTemplate = jdbcTemplate;
		this.properties = properties;
	}

	public AuthLoginVo login(String loginName, String password) {
		if (!properties.getLdap().isEnabled()) {
			throw new IllegalStateException("LDAP 登录未启用");
		}
		verifyLdapCredentials(loginName, password);
		Long userId = findOrCreateUser(loginName);
		String token = newToken();
		Timestamp expireTime = Timestamp.from(Instant.now().plus(properties.getTokenTtlHours(), ChronoUnit.HOURS));
		jdbcTemplate.update("DELETE FROM data_agent_user_token WHERE user_id = ?", userId);
		jdbcTemplate.update("INSERT INTO data_agent_user_token (user_id, login_name, token, expire_time) VALUES (?, ?, ?, ?)",
				userId, loginName, token, expireTime);
		return AuthLoginVo.builder().token(token).user(toUser(userId, loginName)).build();
	}

	public AuthUserVo currentUser(String token) {
		List<AuthUserVo> users = jdbcTemplate.query(
				"SELECT u.id, u.login_name FROM data_agent_user u JOIN data_agent_user_token t ON u.id = t.user_id "
						+ "WHERE t.token = ? AND t.expire_time > CURRENT_TIMESTAMP AND u.is_deleted = 0",
				(rs, rowNum) -> toUser(rs.getLong("id"), rs.getString("login_name")), token);
		return users.isEmpty() ? null : users.get(0);
	}

	public void logout(String token) {
		if (token != null && !token.isBlank()) {
			jdbcTemplate.update("DELETE FROM data_agent_user_token WHERE token = ?", token);
		}
	}

	public List<AuthUserVo> listUsers(AuthUserVo operator) {
		if (!operator.isAdmin()) {
			return List.of(operator);
		}
		return jdbcTemplate.query("SELECT id, login_name FROM data_agent_user WHERE is_deleted = 0 ORDER BY create_time DESC",
				(rs, rowNum) -> toUser(rs.getLong("id"), rs.getString("login_name")));
	}

	private Long findOrCreateUser(String loginName) {
		List<Long> ids = jdbcTemplate.query("SELECT id FROM data_agent_user WHERE login_name = ? AND is_deleted = 0",
				(rs, rowNum) -> rs.getLong(1), loginName);
		if (!ids.isEmpty()) {
			return ids.get(0);
		}
		jdbcTemplate.update("INSERT INTO data_agent_user (login_name) VALUES (?)", loginName);
		return jdbcTemplate.queryForObject("SELECT id FROM data_agent_user WHERE login_name = ?", Long.class, loginName);
	}

	private AuthUserVo toUser(long id, String loginName) {
		return AuthUserVo.builder().id(id).loginName(loginName).admin(properties.getAdmins().contains(loginName)).build();
	}

	private String newToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private void verifyLdapCredentials(String loginName, String password) {
		AuthProperties.LdapProperties ldap = properties.getLdap();
		DirContext adminContext = null;
		DirContext userContext = null;
		try {
			adminContext = bind(ldap.getSecurity().getPrincipal(), ldap.getSecurity().getCredentials());
			String userDn = findUserDn(adminContext, ldap, loginName);
			userContext = bind(userDn, password);
			// A successful bind is the LDAP credential verification.
		}
		catch (Exception exception) {
			throw new IllegalArgumentException("用户名或密码错误", exception);
		}
		finally {
			close(userContext);
			close(adminContext);
		}
	}

	private void close(DirContext context) {
		if (context == null) {
			return;
		}
		try {
			context.close();
		}
		catch (Exception ignored) {
			// Nothing useful can be done if LDAP context cleanup fails.
		}
	}

	private DirContext bind(String principal, String credentials) throws Exception {
		AuthProperties.LdapProperties ldap = properties.getLdap();
		Hashtable<String, String> environment = new Hashtable<>();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, ldap.getFactory());
		environment.put(Context.PROVIDER_URL, ldap.getUrl());
		environment.put(Context.SECURITY_AUTHENTICATION, ldap.getSecurity().getAuthentication());
		environment.put(Context.SECURITY_PRINCIPAL, principal);
		environment.put(Context.SECURITY_CREDENTIALS, credentials);
		return new InitialDirContext(environment);
	}

	private String findUserDn(DirContext context, AuthProperties.LdapProperties ldap, String loginName) throws Exception {
		SearchControls controls = new SearchControls();
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		NamingEnumeration<SearchResult> results = context.search(ldap.getBaseDn(), "(" + ldap.getFilter() + "={0})",
				new Object[] { loginName }, controls);
		if (!results.hasMore()) {
			throw new IllegalArgumentException("用户不存在");
		}
		return results.next().getNameInNamespace();
	}

}
