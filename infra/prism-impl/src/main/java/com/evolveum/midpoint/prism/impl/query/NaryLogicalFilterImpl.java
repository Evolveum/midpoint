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

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;

import java.util.List;

public abstract class NaryLogicalFilterImpl extends LogicalFilterImpl implements NaryLogicalFilter {
	
	public NaryLogicalFilterImpl() {
		super();
	}
	
	public NaryLogicalFilterImpl(List<ObjectFilter> conditions) {
		setConditions(conditions);
	}

	public ObjectFilter getLastCondition() {
		List<ObjectFilter> conditions = getConditions();
		if (conditions.isEmpty()) {
			return null;
		} else {
			return conditions.get(conditions.size()-1);
		}
	}

	@Override
	public abstract NaryLogicalFilterImpl clone();

}
