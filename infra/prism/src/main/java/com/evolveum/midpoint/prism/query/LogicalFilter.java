/*
 * Copyright (c) 2010-2014 Evolveum
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

import java.util.ArrayList;
import java.util.List;

public abstract class LogicalFilter extends ObjectFilter {
	
	protected List<ObjectFilter> conditions;
	
	public LogicalFilter(){

	}
	
	public List<ObjectFilter> getConditions() {
		if (conditions == null){
			conditions = new ArrayList<ObjectFilter>();
		}
		return conditions;
	}
	
	public void setConditions(List<ObjectFilter> condition) {
		this.conditions = condition;
	}
	
	public void addCondition(ObjectFilter condition) {
		this.conditions.add(condition);
	}
	
	public boolean contains(ObjectFilter condition) {
		return this.conditions.contains(condition);
	}
	
	abstract public LogicalFilter cloneEmpty();
	
	protected List<ObjectFilter> getClonedConditions() {
		if (conditions == null) {
			return null;
		}
		List<ObjectFilter> clonedConditions = new ArrayList<ObjectFilter>(conditions.size());
		for (ObjectFilter connditio: conditions) {
			clonedConditions.add(connditio.clone());
		}
		return clonedConditions;
	}
	
	public boolean isEmpty() {
		return conditions == null || conditions.isEmpty();
	}

	@Override
	public void accept(Visitor visitor) {
		super.accept(visitor);
		for (ObjectFilter condition: getConditions()) {
			condition.accept(visitor);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogicalFilter other = (LogicalFilter) obj;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		return true;
	}
	
}
