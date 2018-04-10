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

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * TEMPORARY.
 *
 * @author mederly
 */
public class PolicyRuleApplication {

	static class Cause {
		private final ResourceShadowDiscriminator shadowDiscriminator;		// non-null for projection context
		@NotNull private final ItemPath itemPath;							// should be non-empty
		@NotNull private final PrismValue itemValue;

		Cause(ResourceShadowDiscriminator shadowDiscriminator, @NotNull ItemPath itemPath, @NotNull PrismValue itemValue) {
			this.shadowDiscriminator = shadowDiscriminator;
			this.itemPath = itemPath;
			this.itemValue = itemValue;
		}
	}

	@NotNull private final List<Cause> causes;
	@NotNull private final PolicyRuleType rule;

	public PolicyRuleApplication(@NotNull List<Cause> causes, @NotNull PolicyRuleType rule) {
		this.causes = causes;
		this.rule = rule;
	}

	@NotNull
	public List<Cause> getCauses() {
		return causes;
	}

	@NotNull
	public PolicyRuleType getRule() {
		return rule;
	}
}
