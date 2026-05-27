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

import com.alibaba.druid.pool.DruidDataSource;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Keeps the DataAgent business datasource independent from the PGVector datasource.
 */
@Configuration
@ConditionalOnClass(PgVectorStore.class)
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "pgvector")
@EnableConfigurationProperties({ PgVectorStoreProperties.class,
		PgVectorStoreConfiguration.PgVectorDataSourceProperties.class })
public class PgVectorStoreConfiguration {

	private JdbcTemplate pgVectorJdbcTemplate(PgVectorDataSourceProperties properties) {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setUrl(properties.getUrl());
		dataSource.setUsername(properties.getUsername());
		dataSource.setPassword(properties.getPassword());
		dataSource.setDriverClassName(properties.getDriverClassName());
		return new JdbcTemplate(dataSource);
	}

	@Bean
	@ConditionalOnMissingBean(VectorStore.class)
	public PgVectorStore pgVectorStore(PgVectorDataSourceProperties dataSourceProperties,
			EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
			ObjectProvider<ObservationRegistry> observationRegistry,
			ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
			BatchingStrategy batchingStrategy) {
		return PgVectorStore.builder(pgVectorJdbcTemplate(dataSourceProperties), embeddingModel)
			.schemaName(properties.getSchemaName())
			.idType(properties.getIdType())
			.vectorTableName(properties.getTableName())
			.vectorTableValidationsEnabled(properties.isSchemaValidation())
			.dimensions(properties.getDimensions())
			.distanceType(properties.getDistanceType())
			.removeExistingVectorStoreTable(properties.isRemoveExistingVectorStoreTable())
			.indexType(properties.getIndexType())
			.initializeSchema(properties.isInitializeSchema())
			.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
			.customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
			.batchingStrategy(batchingStrategy)
			.maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
			.build();
	}

	@ConfigurationProperties("spring.ai.vectorstore.pgvector.datasource")
	public static class PgVectorDataSourceProperties {

		private String url;

		private String username;

		private String password;

		private String driverClassName;

		public String getUrl() {
			return this.url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getDriverClassName() {
			return this.driverClassName;
		}

		public void setDriverClassName(String driverClassName) {
			this.driverClassName = driverClassName;
		}

	}

}
