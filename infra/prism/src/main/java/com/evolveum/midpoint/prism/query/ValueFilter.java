/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class ValueFilter extends ObjectFilter {
	
	private ItemPath fullPath;
	private ItemDefinition definition;
	private String matchingRule;
	
	public ValueFilter() {
		// TODO Auto-generated constructor stub
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition){
		this.fullPath = parentPath;
		this.definition = definition;
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule){
		this.fullPath = parentPath;
		this.definition = definition;
		this.matchingRule = matchingRule;
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition, String matchingRule, Element expression){
		super(expression);
		this.fullPath = parentPath;
		this.definition = definition;
		this.matchingRule = matchingRule;
	}
	
	public ValueFilter(ItemPath parentPath, ItemDefinition definition, Element expression){
		super(expression);
		this.fullPath = parentPath;
		this.definition = definition;
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
	
	public String getMatchingRule() {
		return matchingRule;
	}
	
	public void setMatchingRule(String matchingRule) {
		this.matchingRule = matchingRule;
	}
	
	public ItemPath getParentPath2(){
		if (fullPath == null){
			return null;
		}
		ItemPath parentPath = fullPath.allExceptLast();
		
		if (parentPath == null || parentPath.isEmpty()){
			return null;
		}
		
		return parentPath; 
	}
	
	public MatchingRule getMatchingRuleFromRegistry(MatchingRuleRegistry matchingRuleRegistry, Item filterItem){
		QName matchingRule = null;
		if (StringUtils.isNotBlank(getMatchingRule())){
			matchingRule = new QName(PrismConstants.NS_MATCHING_RULE, getMatchingRule());
		} 
//		Item filterItem = getFilterItem();
		MatchingRule matching = null;
		try{
		matching = matchingRuleRegistry.getMatchingRule( matchingRule, filterItem.getDefinition().getTypeName());
		} catch (SchemaException ex){
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		
		return matching;

	}
	
	protected void cloneValues(ValueFilter clone) {
		super.cloneValues(clone);
		clone.fullPath = this.fullPath;
		clone.definition = this.definition;
		clone.matchingRule = this.matchingRule;
	}

}
