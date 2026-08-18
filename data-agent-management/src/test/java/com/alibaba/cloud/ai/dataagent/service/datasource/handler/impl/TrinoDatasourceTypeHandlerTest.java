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
package com.alibaba.cloud.ai.dataagent.service.datasource.handler.impl;

import com.alibaba.cloud.ai.dataagent.bo.DbConfigBO;
import com.alibaba.cloud.ai.dataagent.entity.Datasource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrinoDatasourceTypeHandlerTest {

	private final TrinoDatasourceTypeHandler handler = new TrinoDatasourceTypeHandler();

	@Test
	void buildsUrlAndSchemaFromCatalogAndSchema() {
		Datasource datasource = datasource("hive|default");

		DbConfigBO config = handler.toDbConfig(datasource);

		assertEquals("jdbc:trino://trino.example.com:8080/hive/default", config.getUrl());
		assertEquals("default", config.getSchema());
	}

	@Test
	void supportsLegacyCatalogAndSchemaSeparator() {
		Datasource datasource = datasource("iceberg/analytics");

		assertEquals("jdbc:trino://trino.example.com:8080/iceberg/analytics", handler.buildConnectionUrl(datasource));
		assertEquals("analytics", handler.extractSchemaName(datasource));
	}

	@Test
	void extractsSchemaFromCustomJdbcUrl() {
		Datasource datasource = datasource("hive");
		datasource.setConnectionUrl("jdbc:trino://trino.example.com:8443/hive/default?SSL=true");

		assertEquals("default", handler.extractSchemaName(datasource));
		assertEquals(datasource.getConnectionUrl(), handler.resolveConnectionUrl(datasource));
	}

	@Test
	void rejectsBlankCatalog() {
		Datasource datasource = datasource(" ");

		assertThrows(IllegalArgumentException.class, () -> handler.buildConnectionUrl(datasource));
	}

	private Datasource datasource(String databaseName) {
		Datasource datasource = new Datasource();
		datasource.setHost("trino.example.com");
		datasource.setPort(8080);
		datasource.setDatabaseName(databaseName);
		datasource.setUsername("dataagent");
		return datasource;
	}

}
