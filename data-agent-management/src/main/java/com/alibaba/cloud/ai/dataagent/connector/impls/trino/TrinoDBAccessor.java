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

import com.alibaba.cloud.ai.dataagent.connector.accessor.AbstractAccessor;
import com.alibaba.cloud.ai.dataagent.connector.ddl.DdlFactory;
import com.alibaba.cloud.ai.dataagent.connector.pool.DBConnectionPoolFactory;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import org.springframework.stereotype.Service;

@Service("trinoAccessor")
public class TrinoDBAccessor extends AbstractAccessor {

	public TrinoDBAccessor(DdlFactory ddlFactory, DBConnectionPoolFactory poolFactory) {
		super(ddlFactory, poolFactory.getPoolByDbType(BizDataSourceTypeEnum.TRINO.getTypeName()));
	}

	@Override
	public String getAccessorType() {
		return "Trino_Accessor";
	}

	@Override
	public boolean supportedDataSourceType(String type) {
		return BizDataSourceTypeEnum.TRINO.getTypeName().equalsIgnoreCase(type);
	}

}
