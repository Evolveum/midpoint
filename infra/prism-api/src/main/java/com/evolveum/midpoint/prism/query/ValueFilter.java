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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *
 */
public interface ValueFilter<V extends PrismValue, D extends ItemDefinition> extends ObjectFilter, ItemFilter, Itemable {

	@NotNull
	@Override
	ItemPath getFullPath();

	@NotNull
	ItemPath getParentPath();

	@NotNull
	ItemName getElementName();

	@Nullable
	D getDefinition();

	void setDefinition(@Nullable D definition);

	@Nullable
	QName getMatchingRule();

	void setMatchingRule(@Nullable QName matchingRule);

	@NotNull
	//MatchingRule getMatchingRuleFromRegistry(MatchingRuleRegistry matchingRuleRegistry, Item filterItem);

	@Nullable
	List<V> getValues();

	//@Nullable
	//List<V> getClonedValues();

//	@Nullable
//	V getClonedValue();

	@Nullable
	V getSingleValue();

	/**
	 * @param value value, has to be parent-less
	 */
	void setValue(V value);

	@Nullable
	ExpressionWrapper getExpression();

	void setExpression(@Nullable ExpressionWrapper expression);

	@Nullable
	ItemPath getRightHandSidePath();

	void setRightHandSidePath(@Nullable ItemPath rightHandSidePath);

	@Nullable
	ItemDefinition getRightHandSideDefinition();

	void setRightHandSideDefinition(@Nullable ItemDefinition rightHandSideDefinition);

	@Override
	PrismContext getPrismContext();

	@Override
	ItemPath getPath();

	boolean isRaw();

	// TODO revise
	@Override
	boolean match(PrismContainerValue cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException;

	//@NotNull
	//Collection<PrismValue> getObjectItemValues(PrismContainerValue value);

	// TODO revise
//	@NotNull
//	Item getFilterItem() throws SchemaException;

	@Override
	ValueFilter clone();

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	boolean equals(Object o);

	@Override
	boolean equals(Object o, boolean exact);

	@Override
	int hashCode();

	@Override
	String debugDump();

	@Override
	String debugDump(int indent);

	@Override
	String toString();

	//String getFilterName();

	//void debugDump(int indent, StringBuilder sb);

	//String toString(StringBuilder sb){

	@Override
	void checkConsistence(boolean requireDefinitions);

}
