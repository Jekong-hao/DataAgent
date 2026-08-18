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
package com.alibaba.cloud.ai.dataagent.config;

import com.alibaba.cloud.ai.dataagent.properties.PgVectorStoreProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 1.1.0 的 PGVector 自动配置只会使用主 JdbcTemplate。业务库是 MySQL 时，
 * 需要在这里显式创建 PGVector 专用连接，避免 PostgreSQL DDL 被发往业务库。
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "pgvector")
@EnableConfigurationProperties(PgVectorStoreProperties.class)
public class PgVectorStoreConfiguration {

	@Bean(destroyMethod = "close")
	public PgVectorDatabase pgVectorDatabase(PgVectorStoreProperties properties) {
		PgVectorStoreProperties.DatasourceProperties datasource = properties.getDatasource();
		HikariDataSource dataSource = DataSourceBuilder.create()
			.type(HikariDataSource.class)
			.driverClassName(datasource.getDriverClassName())
			.url(datasource.getUrl())
			.username(datasource.getUsername())
			.password(datasource.getPassword())
			.build();
		return new PgVectorDatabase(dataSource);
	}

	@Bean
	public PgVectorStore vectorStore(@Qualifier("pgVectorDatabase") PgVectorDatabase pgVectorDatabase,
			EmbeddingModel embeddingModel,
			PgVectorStoreProperties properties) {
		return PgVectorStore.builder(pgVectorDatabase.jdbcTemplate(), embeddingModel)
			.schemaName(properties.getSchemaName())
			.vectorTableName(properties.getTableName())
			.dimensions(properties.getDimensions())
			.distanceType(properties.getDistanceType())
			.indexType(properties.getIndexType())
			.removeExistingVectorStoreTable(properties.isRemoveExistingVectorStoreTable())
			.initializeSchema(properties.isInitializeSchema())
			.build();
	}

	public static final class PgVectorDatabase implements AutoCloseable {

		private final HikariDataSource dataSource;

		private PgVectorDatabase(HikariDataSource dataSource) {
			this.dataSource = dataSource;
		}

		private JdbcTemplate jdbcTemplate() {
			return new JdbcTemplate(dataSource);
		}

		@Override
		public void close() {
			dataSource.close();
		}

	}

}
