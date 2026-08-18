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
package com.alibaba.cloud.ai.dataagent.connector.impls.trino;

import com.alibaba.cloud.ai.dataagent.bo.schema.ColumnInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.DatabaseInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.ResultSetBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.SchemaInfoBO;
import com.alibaba.cloud.ai.dataagent.bo.schema.TableInfoBO;
import com.alibaba.cloud.ai.dataagent.connector.SqlExecutor;
import com.alibaba.cloud.ai.dataagent.connector.ddl.AbstractJdbcDdl;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alibaba.cloud.ai.dataagent.util.ColumnTypeUtil.wrapType;

@Service
public class TrinoJdbcDdl extends AbstractJdbcDdl {

	@Override
	public List<DatabaseInfoBO> showDatabases(Connection connection) {
		try (ResultSet resultSet = connection.getMetaData().getCatalogs()) {
			List<DatabaseInfoBO> databases = new ArrayList<>();
			while (resultSet.next()) {
				databases.add(DatabaseInfoBO.builder().name(resultSet.getString("TABLE_CAT")).build());
			}
			return databases;
		}
		catch (SQLException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public List<SchemaInfoBO> showSchemas(Connection connection) {
		try (ResultSet resultSet = connection.getMetaData().getSchemas()) {
			List<SchemaInfoBO> schemas = new ArrayList<>();
			while (resultSet.next()) {
				schemas.add(SchemaInfoBO.builder().name(resultSet.getString("TABLE_SCHEM")).build());
			}
			return schemas;
		}
		catch (SQLException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public List<TableInfoBO> showTables(Connection connection, String schema, String tablePattern) {
		try (ResultSet resultSet = connection.getMetaData().getTables(connection.getCatalog(), schema, tablePattern,
				new String[] { "TABLE", "VIEW" })) {
			List<TableInfoBO> tables = new ArrayList<>();
			while (resultSet.next()) {
				tables.add(TableInfoBO.builder()
					.name(resultSet.getString("TABLE_NAME"))
					.description(resultSet.getString("REMARKS"))
					.build());
			}
			return tables;
		}
		catch (SQLException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public List<TableInfoBO> fetchTables(Connection connection, String schema, List<String> tables) {
		if (tables == null || tables.isEmpty()) {
			return Collections.emptyList();
		}
		List<TableInfoBO> result = new ArrayList<>();
		for (String table : tables) {
			result.addAll(showTables(connection, schema, table));
		}
		return result;
	}

	@Override
	public List<ColumnInfoBO> showColumns(Connection connection, String schema, String table) {
		try (ResultSet resultSet = connection.getMetaData().getColumns(connection.getCatalog(), schema, table, null)) {
			List<ColumnInfoBO> columns = new ArrayList<>();
			while (resultSet.next()) {
				columns.add(ColumnInfoBO.builder()
					.name(resultSet.getString("COLUMN_NAME"))
					.description(resultSet.getString("REMARKS"))
					.type(wrapType(resultSet.getString("TYPE_NAME")))
					.primary(false)
					.notnull(resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls)
					.build());
			}
			return columns;
		}
		catch (SQLException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public List<ForeignKeyInfoBO> showForeignKeys(Connection connection, String schema, List<String> tables) {
		return Collections.emptyList();
	}

	@Override
	public List<String> sampleColumn(Connection connection, String schema, String table, String column) {
		String qualifiedTable = qualify(schema, table);
		String sql = "SELECT DISTINCT " + quote(column) + " FROM " + qualifiedTable + " WHERE " + quote(column)
				+ " IS NOT NULL LIMIT 99";
		try {
			String[][] rows = SqlExecutor.executeSqlAndReturnArr(connection, sql);
			List<String> values = new ArrayList<>();
			for (int row = 1; row < rows.length; row++) {
				if (rows[row].length > 0 && rows[row][0] != null) {
					values.add(rows[row][0]);
				}
			}
			return values;
		}
		catch (SQLException exception) {
			return Collections.emptyList();
		}
	}

	@Override
	public ResultSetBO scanTable(Connection connection, String schema, String table) {
		try {
			return SqlExecutor.executeSqlAndReturnObject(connection, schema, "SELECT * FROM " + qualify(schema, table) + " LIMIT 20");
		}
		catch (SQLException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public BizDataSourceTypeEnum getDataSourceType() {
		return BizDataSourceTypeEnum.TRINO;
	}

	private String qualify(String schema, String table) {
		return schema == null || schema.isBlank() ? quote(table) : quote(schema) + "." + quote(table);
	}

	private String quote(String identifier) {
		return '"' + identifier.replace("\"", "\"\"") + '"';
	}

}
