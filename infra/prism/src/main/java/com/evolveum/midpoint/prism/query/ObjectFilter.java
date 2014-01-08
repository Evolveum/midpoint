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

import java.io.Serializable;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;



public abstract class ObjectFilter implements Dumpable, DebugDumpable, Serializable{

//	private ItemPath fullPath;
	private Element expression;

	ObjectFilter(Element expression) {
		this.expression = expression;
	}
	
	ObjectFilter() {
	}
	
	public Element getExpression() {
		return expression;
	}

	public void setExpression(Element expression) {
		this.expression = expression;
	}
	
	public abstract ObjectFilter clone();
	
	public abstract <T extends Objectable> boolean match(PrismObject<T> object, MatchingRuleRegistry matchingRuleRegistry);
	
	protected void cloneValues(ObjectFilter clone) {
		clone.expression = this.expression;
	}
	
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
	
//	public ItemPath getFullPath() {
//		return fullPath;
//	}
//	
//	public ItemPath getParentPath(){
//		if (fullPath == null){
//			return null;
//		}
//		
//		return fullPath.allExceptLast();
//	}
	
}
