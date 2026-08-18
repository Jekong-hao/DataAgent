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

import com.alibaba.cloud.ai.dataagent.entity.Datasource;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dataagent.service.datasource.handler.DatasourceTypeHandler;
import org.springframework.stereotype.Component;

@Component
public class TrinoDatasourceTypeHandler implements DatasourceTypeHandler {

	@Override
	public String typeName() {
		return BizDataSourceTypeEnum.TRINO.getTypeName();
	}

	@Override
	public String buildConnectionUrl(Datasource datasource) {
		if (!hasRequiredConnectionFields(datasource)) {
			return datasource.getConnectionUrl();
		}
		String[] catalogAndSchema = splitCatalogAndSchema(datasource.getDatabaseName());
		return String.format("jdbc:trino://%s:%d/%s", datasource.getHost(), datasource.getPort(),
				catalogAndSchema[1] == null ? catalogAndSchema[0] : catalogAndSchema[0] + "/" + catalogAndSchema[1]);
	}

	@Override
	public String extractSchemaName(Datasource datasource) {
		String[] catalogAndSchema = splitCatalogAndSchema(datasource.getDatabaseName());
		if (catalogAndSchema[1] != null) {
			return catalogAndSchema[1];
		}
		String connectionUrl = datasource.getConnectionUrl();
		if (connectionUrl == null || !connectionUrl.startsWith("jdbc:trino:")) {
			return null;
		}
		String path = connectionUrl.substring("jdbc:trino:".length()).replaceFirst("^[^/]*//[^/]+/?", "");
		String[] pathParts = path.split("[/?]", 3);
		return pathParts.length > 1 ? pathParts[1] : null;
	}

	private String[] splitCatalogAndSchema(String databaseName) {
		if (databaseName == null || databaseName.isBlank()) {
			throw new IllegalArgumentException("Trino catalog cannot be blank");
		}
		String normalized = databaseName.trim();
		String[] parts = normalized.contains("|") ? normalized.split("\\|", 2) : normalized.split("/", 2);
		String catalog = parts[0].trim();
		if (catalog.isEmpty()) {
			throw new IllegalArgumentException("Trino catalog cannot be blank");
		}
		String schema = parts.length == 2 && !parts[1].isBlank() ? parts[1].trim() : null;
		return new String[] { catalog, schema };
	}

}
