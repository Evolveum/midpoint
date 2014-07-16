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

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class ValueFilter<T extends PrismValue> extends ObjectFilter {
	private static final long serialVersionUID = 1L;
	private ItemPath fullPath;
	private ItemDefinition definition;
	private QName matchingRule;
	
	public ValueFilter() {
		super();
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition){
		this.fullPath = parentPath;
		this.definition = definition;
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition, QName matchingRule){
		this.fullPath = parentPath;
		this.definition = definition;
		this.matchingRule = matchingRule;
	}
		
	public ItemDefinition getDefinition() {
		return definition;
	}
	
	public void setDefinition(ItemDefinition definition) {
		this.definition = definition;
	}
	
	public ItemPath getFullPath() {
		return fullPath;
	}
	
	public void setFullPath(ItemPath path) {
		this.fullPath = path;
	}
	
	public QName getMatchingRule() {
		return matchingRule;
	}
	
	public void setMatchingRule(QName matchingRule) {
		this.matchingRule = matchingRule;
	}
	
	public ItemPath getParentPath(){
		if (fullPath == null){
			return null;
		}
		ItemPath parentPath = fullPath.allExceptLast();
		
		if (parentPath == null || parentPath.isEmpty()){
			return null;
		}
		
		return parentPath; 
	}
	
	public QName getElementName(){
		if (fullPath == null){
			return null;
		}
		ItemPathSegment lastPathSegement = fullPath.last();
		
		if (lastPathSegement == null) {
			return null;
		}
		
		if (lastPathSegement instanceof NameItemPathSegment) {
			return ((NameItemPathSegment)lastPathSegement).getName();
		} else {
			throw new IllegalStateException("Got "+lastPathSegement+" as a last path segment in value filter "+this);
		}
	}
	
	public MatchingRule getMatchingRuleFromRegistry(MatchingRuleRegistry matchingRuleRegistry, Item filterItem){
		MatchingRule matching = null;
		try{
		matching = matchingRuleRegistry.getMatchingRule(matchingRule, filterItem.getDefinition().getTypeName());
		} catch (SchemaException ex){
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		
		return matching;

	}
	
	static ItemDefinition findItemDefinition(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef) {
		ItemDefinition itemDef = containerDef.findItemDefinition(parentPath);
		if (itemDef == null) {
			throw new IllegalStateException("No definition for item " + parentPath + " in container definition "
					+ containerDef);
		}

		return itemDef;
	}
	
	static ItemDefinition findItemDefinition(ItemPath parentPath, Class type, PrismContext prismContext){
		PrismObjectDefinition<?> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return findItemDefinition(parentPath, objDef);
	}

	
	protected void cloneValues(ValueFilter clone) {
		super.cloneValues(clone);
		clone.fullPath = this.fullPath;
		clone.definition = this.definition;
		clone.matchingRule = this.matchingRule;
	}
	
	public abstract boolean isRaw();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((fullPath == null) ? 0 : fullPath.hashCode());
		result = prime * result + ((matchingRule == null) ? 0 : matchingRule.hashCode());
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
		ValueFilter other = (ValueFilter) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (fullPath == null) {
			if (other.fullPath != null)
				return false;
		} else if (!fullPath.equivalent(other.fullPath))        // TODO: ok?
			return false;
		if (matchingRule == null) {
			if (other.matchingRule != null)
				return false;
		} else if (!matchingRule.equals(other.matchingRule))
			return false;
		return true;
	}
	
}
