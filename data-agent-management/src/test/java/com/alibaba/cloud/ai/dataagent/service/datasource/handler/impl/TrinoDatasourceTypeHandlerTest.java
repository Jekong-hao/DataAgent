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
package com.alibaba.cloud.ai.dataagent.service.datasource.handler.impl;

import com.alibaba.cloud.ai.dataagent.bo.DbConfigBO;
import com.alibaba.cloud.ai.dataagent.entity.Datasource;
import com.alibaba.cloud.ai.dataagent.enums.DbAccessTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrinoDatasourceTypeHandlerTest {

	private TrinoDatasourceTypeHandler handler;

	@BeforeEach
	void setUp() {
		handler = new TrinoDatasourceTypeHandler();
	}

	@Test
	void typeName_returnsTrino() {
		assertEquals("trino", handler.typeName());
	}

	@Test
	void connectionType_returnsJdbc() {
		assertEquals(DbAccessTypeEnum.JDBC.getCode(), handler.connectionType());
	}

	@Test
	void dialectType_returnsTrino() {
		assertEquals("trino", handler.dialectType());
	}

	@Test
	void supports_matchesCaseInsensitive() {
		assertTrue(handler.supports("trino"));
		assertTrue(handler.supports("TRINO"));
		assertTrue(handler.supports("Trino"));
	}

	@Test
	void supports_rejectsOtherTypes() {
		assertFalse(handler.supports("hive"));
		assertFalse(handler.supports("mysql"));
	}

	@Test
	void buildConnectionUrl_constructsValidUrlWithDefaultHiveCatalog() {
		Datasource ds = Datasource.builder().host("172.20.11.214").port(9090).databaseName("default").build();
		assertEquals("jdbc:trino://172.20.11.214:9090/hive/default", handler.buildConnectionUrl(ds));
	}

	@Test
	void buildConnectionUrl_fallsBackToConnectionUrlWhenFieldsMissing() {
		Datasource ds = Datasource.builder().connectionUrl("jdbc:trino://host:9090/hive/default").build();
		assertEquals("jdbc:trino://host:9090/hive/default", handler.buildConnectionUrl(ds));
	}

	@Test
	void resolveConnectionUrl_prefersExistingUrl() {
		Datasource ds = Datasource.builder()
			.host("172.20.11.214")
			.port(9090)
			.databaseName("default")
			.connectionUrl("jdbc:trino://explicit:9090/hive/ods")
			.build();
		assertEquals("jdbc:trino://explicit:9090/hive/ods", handler.resolveConnectionUrl(ds));
	}

	@Test
	void toDbConfig_populatesAllFields() {
		Datasource ds = Datasource.builder()
			.host("172.20.11.214")
			.port(9090)
			.databaseName("default")
			.username("hadoop")
			.password("")
			.build();
		DbConfigBO config = handler.toDbConfig(ds);
		assertNotNull(config.getUrl());
		assertEquals("jdbc:trino://172.20.11.214:9090/hive/default", config.getUrl());
		assertEquals("hadoop", config.getUsername());
		assertEquals(DbAccessTypeEnum.JDBC.getCode(), config.getConnectionType());
		assertEquals("trino", config.getDialectType());
		assertEquals("default", config.getSchema());
	}

}
