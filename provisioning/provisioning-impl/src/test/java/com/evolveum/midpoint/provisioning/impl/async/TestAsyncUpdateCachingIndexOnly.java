/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.impl.async;

import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;

/**
 *
 */
public class TestAsyncUpdateCachingIndexOnly extends TestAsyncUpdateCaching {

	@Autowired
	@Qualifier("sqlRepositoryServiceImpl")
	private SqlRepositoryServiceImpl sqlRepositoryService;

	@Override
	protected File getResourceFile() {
		return RESOURCE_ASYNC_CACHING_INDEX_ONLY_FILE;
	}

	@Override
	protected int getNumberOfAccountAttributes() {
		return 3;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		// These are experimental features, so they need to be explicitly enabled. This will be eliminated later,
		// when we make them enabled by default.
		sqlRepositoryService.getConfiguration().setEnableIndexOnlyItems(true);
		sqlRepositoryService.getConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
		sqlRepositoryService.getConfiguration().setEnableNoFetchExtensionValuesDeletion(true);

		super.initSystem(initTask, initResult);
	}

}
