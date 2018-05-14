/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.repo.sql.data;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class RepositoryContext {
	@NotNull public final RepositoryService repositoryService;
	@NotNull public final PrismContext prismContext;
	@NotNull public final ExtItemDictionary extItemDictionary;

	public RepositoryContext(@NotNull RepositoryService repositoryService, @NotNull PrismContext prismContext, @NotNull ExtItemDictionary extItemDictionary) {
		this.repositoryService = repositoryService;
		this.prismContext = prismContext;
		this.extItemDictionary = extItemDictionary;
	}
}
