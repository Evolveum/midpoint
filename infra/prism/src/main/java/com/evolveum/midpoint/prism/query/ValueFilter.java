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

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public abstract class ValueFilter<T extends PrismValue> extends ObjectFilter {
	private static final long serialVersionUID = 1L;

	@NotNull private final ItemPath fullPath;
	/**
	 * Definition is not required and not final, because it can be filled-in after creation of the filter (e.g. in provisioning)
	 */
	@Nullable private ItemDefinition definition;
	@Nullable private QName matchingRule;

	// path is not empty
	protected ValueFilter(@NotNull ItemPath fullPath, @Nullable ItemDefinition definition) {
		this(fullPath, definition, null);
	}

	protected ValueFilter(@NotNull ItemPath fullPath, @Nullable ItemDefinition definition, @Nullable QName matchingRule) {
		Validate.isTrue(!ItemPath.isNullOrEmpty(fullPath), "path in filter is null or empty");
		this.fullPath = fullPath;
		this.definition = definition;
		this.matchingRule = matchingRule;
	}
		
	@Nullable
	public ItemDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(@Nullable ItemDefinition definition) {
		this.definition = definition;
	}
	
	@NotNull
	public ItemPath getFullPath() {
		return fullPath;
	}
	
	@Nullable
	public QName getMatchingRule() {
		return matchingRule;
	}
	
	public void setMatchingRule(@Nullable QName matchingRule) {
		this.matchingRule = matchingRule;
	}

	@NotNull
	public ItemPath getParentPath() {
		return fullPath.allExceptLast();
	}

	@NotNull
	public QName getElementName() {
		if (definition != null) {
			return definition.getName();		// this is more precise, as the name in path can be unqualified
		}
		ItemPathSegment lastPathSegement = fullPath.last();
		if (lastPathSegement instanceof NameItemPathSegment) {
			return ((NameItemPathSegment)lastPathSegement).getName();
		} else if (lastPathSegement == null) {
			throw new IllegalStateException("Empty full path in filter "+this);
		} else {
			throw new IllegalStateException("Got "+lastPathSegement+" as a last path segment in value filter "+this);
		}
	}
	
	protected MatchingRule getMatchingRuleFromRegistry(MatchingRuleRegistry matchingRuleRegistry, Item filterItem) {
		try {
			return matchingRuleRegistry.getMatchingRule(matchingRule, filterItem.getDefinition().getTypeName());
		} catch (SchemaException ex){
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	public abstract boolean isRaw();
	
	@Override
	public void checkConsistence(boolean requireDefinitions) {
		if (requireDefinitions && definition == null) {
			throw new IllegalArgumentException("Null definition in "+this);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + fullPath.hashCode();
		result = prime * result + ((matchingRule == null) ? 0 : matchingRule.hashCode());
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
		ValueFilter other = (ValueFilter) obj;
		if (exact) {
			if (definition == null) {
				if (other.definition != null)
					return false;
			} else if (!definition.equals(other.definition))
				return false;
		}
		if (!fullPath.equals(other.fullPath, exact))
			return false;
		if (matchingRule == null) {
			if (other.matchingRule != null)
				return false;
		} else if (!matchingRule.equals(other.matchingRule))
			return false;
		return true;
	}
	
}
