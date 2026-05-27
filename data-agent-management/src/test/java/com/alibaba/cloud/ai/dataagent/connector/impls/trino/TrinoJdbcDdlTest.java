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
package com.alibaba.cloud.ai.dataagent.connector.impls.trino;

import com.alibaba.cloud.ai.dataagent.bo.schema.ColumnInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.DatabaseInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.SchemaInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.TableInfoBO;
import com.alibaba.cloud.ai.dataagent.connector.SqlExecutor;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class TrinoJdbcDdlTest {

	private TrinoJdbcDdl trinoJdbcDdl;

	private Connection connection;

	@BeforeEach
	void setUp() {
		trinoJdbcDdl = new TrinoJdbcDdl();
		connection = mock(Connection.class);
	}

	@Test
	void getDataSourceType_returnsTrino() {
		assertEquals(BizDataSourceTypeEnum.TRINO, trinoJdbcDdl.getDataSourceType());
	}

	@Test
	void showDatabases_usesShowCatalogs() throws SQLException {
		String[][] resultArr = { { "Catalog" }, { "hive" }, { "system" } };
		try (MockedStatic<SqlExecutor> ms = mockStatic(SqlExecutor.class)) {
			ms.when(() -> SqlExecutor.executeSqlAndReturnArr(connection, "SHOW CATALOGS")).thenReturn(resultArr);

			List<DatabaseInfoBO> databases = trinoJdbcDdl.showDatabases(connection);

			assertEquals(2, databases.size());
			assertEquals("hive", databases.get(0).getName());
			assertEquals("system", databases.get(1).getName());
		}
	}

	@Test
	void showSchemas_usesShowSchemas() throws SQLException {
		String[][] resultArr = { { "Schema" }, { "default" } };
		try (MockedStatic<SqlExecutor> ms = mockStatic(SqlExecutor.class)) {
			ms.when(() -> SqlExecutor.executeSqlAndReturnArr(connection, "SHOW SCHEMAS")).thenReturn(resultArr);

			List<SchemaInfoBO> schemas = trinoJdbcDdl.showSchemas(connection);

			assertEquals(1, schemas.size());
			assertEquals("default", schemas.get(0).getName());
		}
	}

	@Test
	void showTables_usesSchemaAndPattern() throws SQLException {
		String[][] resultArr = { { "Table" }, { "orders" } };
		try (MockedStatic<SqlExecutor> ms = mockStatic(SqlExecutor.class)) {
			ms.when(() -> SqlExecutor.executeSqlAndReturnArr(connection, "SHOW TABLES FROM default LIKE 'order*'"))
				.thenReturn(resultArr);

			List<TableInfoBO> tables = trinoJdbcDdl.showTables(connection, "default", "order*");

			assertEquals(1, tables.size());
			assertEquals("orders", tables.get(0).getName());
		}
	}

	@Test
	void showColumns_usesDescribe() throws SQLException {
		String[][] resultArr = { { "Column", "Type", "Extra", "Comment" }, { "id", "bigint", "", "主键" } };
		try (MockedStatic<SqlExecutor> ms = mockStatic(SqlExecutor.class)) {
			ms.when(() -> SqlExecutor.executeSqlAndReturnArr(connection, "DESCRIBE default.orders"))
				.thenReturn(resultArr);

			List<ColumnInfoBO> columns = trinoJdbcDdl.showColumns(connection, "default", "orders");

			assertEquals(1, columns.size());
			assertEquals("id", columns.get(0).getName());
			assertEquals("number", columns.get(0).getType());
			assertEquals("主键", columns.get(0).getDescription());
		}
	}

	@Test
	void sampleColumn_usesDoubleQuotedColumn() throws SQLException {
		String[][] resultArr = { { "name" }, { "alice" }, { "bob" }, { "alice" } };
		try (MockedStatic<SqlExecutor> ms = mockStatic(SqlExecutor.class)) {
			ms.when(() -> SqlExecutor.executeSqlAndReturnArr(any(Connection.class), any(), anyString()))
				.thenReturn(resultArr);

			List<String> samples = trinoJdbcDdl.sampleColumn(connection, "default", "users", "name");

			assertEquals(2, samples.size());
		}
	}

}
