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

import com.alibaba.cloud.ai.dataagent.bo.DbConfigBO;
import com.alibaba.cloud.ai.dataagent.connector.pool.AbstractDBConnectionPool;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum.DATASOURCE_CONNECTION_FAILURE_08001;
import static com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum.INSUFFICIENT_PRIVILEGE_42501;
import static com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum.OTHERS;
import static com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum.PASSWORD_ERROR_28000;
import static com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum.SUCCESS;

/**
 * Trino JDBC connection pool implementation.
 */
@Slf4j
@Service("trinoJdbcConnectionPool")
public class TrinoJdbcConnectionPool extends AbstractDBConnectionPool {

	private static final String DRIVER = "io.trino.jdbc.TrinoDriver";

	@Override
	public String getDriver() {
		return DRIVER;
	}

	@Override
	public ErrorCodeEnum errorMapping(String sqlState) {
		if (sqlState == null) {
			return OTHERS;
		}

		ErrorCodeEnum ret = ErrorCodeEnum.fromCode(sqlState);
		if (ret != OTHERS) {
			return ret;
		}

		switch (sqlState) {
			case "08001":
			case "08S01":
				return DATASOURCE_CONNECTION_FAILURE_08001;
			case "28000":
				return PASSWORD_ERROR_28000;
			case "42501":
				return INSUFFICIENT_PRIVILEGE_42501;
			default:
				return OTHERS;
		}
	}

	@Override
	public boolean supportedDataSourceType(String type) {
		return BizDataSourceTypeEnum.TRINO.getTypeName().equalsIgnoreCase(type);
	}

	@Override
	public String getConnectionPoolType() {
		return "Trino_JDBC_Pool";
	}

	@Override
	public DataSource createdDataSource(String url, String username, String password) throws Exception {
		log.info("Creating Trino DataSource with custom configuration");
		Map<String, String> props = new TrinoDruidProperties(getDriver(), url, username, password).toMap();
		return DruidDataSourceFactory.createDataSource(props);
	}

	@Override
	public ErrorCodeEnum ping(DbConfigBO config) {
		log.info("Trino ping method called, url: {}", config.getUrl());
		try (Connection connection = getConnection(config); Statement stmt = connection.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT 1");
			if (rs.next()) {
				rs.close();
				return SUCCESS;
			}
			rs.close();
			return DATASOURCE_CONNECTION_FAILURE_08001;
		}
		catch (SQLException e) {
			log.error("Trino connection test failed, url:{}, state:{}, message:{}", config.getUrl(), e.getSQLState(),
					e.getMessage());
			return errorMapping(e.getSQLState());
		}
		catch (Exception e) {
			log.error("Trino connection test failed with unexpected error, url:{}, message:{}", config.getUrl(),
					e.getMessage());
			return DATASOURCE_CONNECTION_FAILURE_08001;
		}
	}

	private static final class TrinoDruidProperties {

		private final String driver;

		private final String url;

		private final String username;

		private final String password;

		private TrinoDruidProperties(String driver, String url, String username, String password) {
			this.driver = driver;
			this.url = url;
			this.username = username;
			this.password = password;
		}

		private Map<String, String> toMap() {
			Map<String, String> props = new HashMap<>();
			props.put(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, this.driver);
			props.put(DruidDataSourceFactory.PROP_URL, this.url);
			props.put(DruidDataSourceFactory.PROP_USERNAME, this.username);
			if (StringUtils.hasText(this.password)) {
				props.put(DruidDataSourceFactory.PROP_PASSWORD, this.password);
			}
			props.put(DruidDataSourceFactory.PROP_FILTERS, "stat");
			props.put(DruidDataSourceFactory.PROP_INITIALSIZE, "5");
			props.put(DruidDataSourceFactory.PROP_MINIDLE, "5");
			props.put(DruidDataSourceFactory.PROP_MAXACTIVE, "20");
			props.put(DruidDataSourceFactory.PROP_MAXWAIT, "60000");
			props.put(DruidDataSourceFactory.PROP_TIMEBETWEENEVICTIONRUNSMILLIS, "60000");
			props.put(DruidDataSourceFactory.PROP_MINEVICTABLEIDLETIMEMILLIS, "300000");
			props.put(DruidDataSourceFactory.PROP_VALIDATIONQUERY, "SELECT 1");
			props.put(DruidDataSourceFactory.PROP_TESTWHILEIDLE, "true");
			props.put(DruidDataSourceFactory.PROP_TESTONBORROW, "false");
			props.put(DruidDataSourceFactory.PROP_TESTONRETURN, "false");
			return props;
		}

	}

}
