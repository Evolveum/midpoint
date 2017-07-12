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

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public interface RefinedAttributeDefinition<T> extends ResourceAttributeDefinition<T> {
	boolean isTolerant();

	boolean isSecondaryIdentifier();

	boolean canAdd(LayerType layer);

	boolean canRead(LayerType layer);

	boolean canModify(LayerType layer);

	boolean isIgnored(LayerType layer);

	String getDescription();

	ResourceAttributeDefinition<T> getAttributeDefinition();

	MappingType getOutboundMappingType();

	boolean hasOutboundMapping();

	List<MappingType> getInboundMappingTypes();

	int getMaxOccurs(LayerType layer);

	int getMinOccurs(LayerType layer);

	boolean isOptional(LayerType layer);

	boolean isMandatory(LayerType layer);

	boolean isMultiValue(LayerType layer);

	boolean isSingleValue(LayerType layer);

	boolean isExlusiveStrong();

	PropertyLimitations getLimitations(LayerType layer);

	AttributeFetchStrategyType getFetchStrategy();

	List<String> getTolerantValuePattern();

	List<String> getIntolerantValuePattern();

	boolean isVolatilityTrigger();

	@NotNull
	@Override
	RefinedAttributeDefinition<T> clone();

	@Override
	RefinedAttributeDefinition<T> deepClone(Map<QName, ComplexTypeDefinition> ctdMap);

	String debugDump(int indent, LayerType layer);

	Integer getModificationPriority();

	Boolean getReadReplaceMode();

	boolean isDisplayNameAttribute();
}
