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

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 */
public class ModelInvocationContext<T extends ObjectType> {
	@NotNull public final PrismContext prismContext;
	@NotNull public final ModelContext<T> modelContext;
	@Nullable public final WfConfigurationType wfConfiguration;
	@NotNull public final Task taskFromModel;

	public ModelInvocationContext(@NotNull PrismContext prismContext, @NotNull ModelContext<T> modelContext,
			@Nullable WfConfigurationType wfConfiguration,
			@NotNull Task taskFromModel) {
		this.prismContext = prismContext;
		this.modelContext = modelContext;
		this.wfConfiguration = wfConfiguration;
		this.taskFromModel = taskFromModel;
	}
}
