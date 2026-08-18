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

import com.alibaba.cloud.ai.dataagent.connector.pool.AbstractDBConnectionPool;
import com.alibaba.cloud.ai.dataagent.bo.DbConfigBO;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Service("trinoJdbcConnectionPool")
public class TrinoJdbcConnectionPool extends AbstractDBConnectionPool {

	@Override
	public String getDriver() {
		return "io.trino.jdbc.TrinoDriver";
	}

	@Override
	public ErrorCodeEnum errorMapping(String sqlState) {
		ErrorCodeEnum errorCode = ErrorCodeEnum.fromCode(sqlState);
		return errorCode == null ? ErrorCodeEnum.OTHERS : errorCode;
	}

	@Override
	public boolean supportedDataSourceType(String type) {
		return BizDataSourceTypeEnum.TRINO.getTypeName().equalsIgnoreCase(type);
	}

	@Override
	public String getConnectionPoolType() {
		return BizDataSourceTypeEnum.TRINO.getTypeName();
	}

	@Override
	public DataSource createdDataSource(String url, String username, String password) throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, getDriver());
		properties.put(DruidDataSourceFactory.PROP_URL, url);
		properties.put(DruidDataSourceFactory.PROP_USERNAME, username);
		properties.put(DruidDataSourceFactory.PROP_PASSWORD, password);
		properties.put(DruidDataSourceFactory.PROP_INITIALSIZE, "0");
		properties.put(DruidDataSourceFactory.PROP_MINIDLE, "0");
		properties.put(DruidDataSourceFactory.PROP_MAXACTIVE, "20");
		properties.put(DruidDataSourceFactory.PROP_MAXWAIT, "10000");
		properties.put(DruidDataSourceFactory.PROP_TIMEBETWEENEVICTIONRUNSMILLIS, "60000");
		// Druid Wall does not provide a Trino dialect and can reject valid Trino SQL.
		properties.put(DruidDataSourceFactory.PROP_FILTERS, "stat");

		DruidDataSource dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(properties);
		dataSource.setInitialSize(0);
		dataSource.setMinIdle(0);
		dataSource.setBreakAfterAcquireFailure(true);
		dataSource.setConnectionErrorRetryAttempts(2);
		dataSource.setTestWhileIdle(false);
		return dataSource;
	}

	@Override
	public ErrorCodeEnum ping(DbConfigBO config) {
		try (Connection connection = DriverManager.getConnection(config.getUrl(), config.getUsername(),
				config.getPassword()); Statement statement = connection.createStatement()) {
			statement.execute("SELECT 1");
			return ErrorCodeEnum.SUCCESS;
		}
		catch (Exception exception) {
			return errorMapping(exception instanceof java.sql.SQLException sqlException ? sqlException.getSQLState() : null);
		}
	}

}
