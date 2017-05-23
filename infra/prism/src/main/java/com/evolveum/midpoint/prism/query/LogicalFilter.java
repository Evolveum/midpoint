/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.util.DebugUtil;

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
		if (this.conditions == null) {
			conditions = new ArrayList<ObjectFilter>();
		}
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
		for (ObjectFilter condition: conditions) {
			clonedConditions.add(condition.clone());
		}
		return clonedConditions;
	}
	
	public boolean isEmpty() {
		return conditions == null || conditions.isEmpty();
	}

	@Override
	public void checkConsistence(boolean requireDefinitions) {
		if (conditions == null) {
			throw new IllegalArgumentException("Null conditions in "+this);
		}
		if (conditions.isEmpty()) {
			throw new IllegalArgumentException("Empty conditions in "+this);
		}
		for (ObjectFilter condition: conditions) {
			if (condition == null) {
				throw new IllegalArgumentException("Null subfilter in "+this);
			}
			condition.checkConsistence(requireDefinitions);
		}
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
	public boolean equals(Object obj, boolean exact) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogicalFilter other = (LogicalFilter) obj;

		if (conditions != null) {
			if (conditions.size() != other.conditions.size()) {
				return false;
			}
			for (int i = 0; i < conditions.size(); i++) {
				ObjectFilter of1 = this.conditions.get(i);
				ObjectFilter of2 = other.conditions.get(i);
				if (!of1.equals(of2, exact)) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(getDebugDumpOperationName()).append(":");
		for (ObjectFilter filter : getConditions()){
			sb.append("\n");
			sb.append(filter.debugDump(indent + 1));
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getDebugDumpOperationName());
		sb.append("(");
		for (int i = 0; i < getConditions().size(); i++){
			sb.append(getConditions().get(i));
			if (i != getConditions().size() - 1) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	protected abstract String getDebugDumpOperationName();
}
