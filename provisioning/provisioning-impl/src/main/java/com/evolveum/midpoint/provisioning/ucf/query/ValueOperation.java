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

package com.evolveum.midpoint.provisioning.ucf.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.InFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.IcfNameMapper;
import com.evolveum.midpoint.provisioning.ucf.util.UcfUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ValueOperation extends Operation {

	public ValueOperation(FilterInterpreter interpreter) {
		super(interpreter);
		// TODO Auto-generated constructor stub
	}

	@Override
	public <T> Filter interpret(ObjectFilter objectFilter, IcfNameMapper icfNameMapper) throws SchemaException {

		OperationResult parentResult = new OperationResult("interpret");

		ValueFilter valueFilter= (ValueFilter) objectFilter;
		if (valueFilter.getParentPath() != null && !valueFilter.getParentPath().isEmpty()
				&& valueFilter.getParentPath().equivalent(new ItemPath(ShadowType.F_ATTRIBUTES))) {
			try {
				QName propName = valueFilter.getDefinition().getName();
				String icfName = icfNameMapper.convertAttributeNameToIcf(propName, getInterpreter()
						.getResourceSchemaNamespace());
				
				if (objectFilter instanceof EqualFilter) {
					EqualFilter<T> eq = (EqualFilter<T>) objectFilter;
					
					Collection<Object> convertedValues = convertValues(propName, eq.getValues());
					if (convertedValues.isEmpty()) {
						// See MID-1460
						throw new UnsupportedOperationException("Equals filter with a null value is NOT supported by ICF");
					} else {
						Attribute attr = AttributeBuilder.build(icfName, convertedValues);
						if (valueFilter.getDefinition().isSingleValue()) {
							return FilterBuilder.equalTo(attr);
						} else {
							return FilterBuilder.containsAllValues(attr);
						}
					}
					
				} else if (objectFilter instanceof InFilter) {
					InFilter<T> in = (InFilter<T>) objectFilter;
					
					Collection convertedValues = convertValues(propName, (List) in.getValues());
					if (convertedValues.isEmpty()) {
						throw new IllegalArgumentException("In filter with a null value makes no sense");
					} else {
						Attribute attr = AttributeBuilder.build(icfName, convertedValues);
						return FilterBuilder.equalTo(attr);
					}
				
				} else if (objectFilter instanceof SubstringFilter) {
					SubstringFilter substring = (SubstringFilter) objectFilter;
                    Collection<Object> convertedValues = convertValues(propName, substring.getValues());
                    if (convertedValues.isEmpty()) {
                        throw new IllegalArgumentException("Substring filter with null value makes no sense");
                    } else {
                        if (convertedValues.size() != 1) {
                            throw new IllegalArgumentException("Substring filter with multiple values makes no sense");
                            //maybe it does, through OR clauses
                        }
                        return FilterBuilder.contains(AttributeBuilder.build(icfName, convertedValues.iterator().next()));
                    }
				} else {
					throw new UnsupportedOperationException("Unsupported filter type: " + objectFilter.debugDump());
				}
			} catch (SchemaException ex) {
				throw ex;

			}
		} else {
			throw new UnsupportedOperationException("Unsupported parent path "+valueFilter.getParentPath()+" in filter: " + objectFilter.debugDump());
		}
	}

    private <T> Collection<Object> convertValues(QName propName, List<PrismPropertyValue<T>> values) throws SchemaException {
        Collection<Object> convertedValues = new ArrayList<>();
        for (PrismValue value : values) {
            Object converted = UcfUtil.convertValueToIcf(value, null, propName);
            convertedValues.add(converted);
        }

        return convertedValues;
    }
}
