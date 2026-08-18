/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.dataagent.properties;

import com.alibaba.cloud.ai.dataagent.constant.Constant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = Constant.PROJECT_PROPERTIES_PREFIX + ".auth")
public class AuthProperties {

	private LdapProperties ldap = new LdapProperties();

	private int tokenTtlHours = 24;

	private List<String> admins = new ArrayList<>();

	@Getter
	@Setter
	public static class LdapProperties {

		private boolean enabled;

		private String url = "";

		private String baseDn = "";

		private String factory = "com.sun.jndi.ldap.LdapCtxFactory";

		private String filter = "account";

		private SecurityProperties security = new SecurityProperties();

	}

	@Getter
	@Setter
	public static class SecurityProperties {

		private String authentication = "simple";

		private String principal = "";

		private String credentials = "";

	}
}
