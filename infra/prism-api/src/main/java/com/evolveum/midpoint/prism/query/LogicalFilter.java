/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.prism.query;

import java.util.List;

/**
 *
 */
public interface LogicalFilter extends ObjectFilter {

	List<ObjectFilter> getConditions();

	void setConditions(List<ObjectFilter> condition);

	void addCondition(ObjectFilter condition);

	boolean contains(ObjectFilter condition);

	LogicalFilter cloneEmpty();

	//List<ObjectFilter> getClonedConditions();

	boolean isEmpty();

	@Override
	void checkConsistence(boolean requireDefinitions);

	@Override
	void accept(Visitor visitor);

	//String getDebugDumpOperationName();
}
