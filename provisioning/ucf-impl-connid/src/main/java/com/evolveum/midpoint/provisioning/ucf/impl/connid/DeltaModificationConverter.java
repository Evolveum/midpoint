/**
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
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class DeltaModificationConverter extends AbstractModificationConverter {
	
	private Set<AttributeDelta> attributesDelta = new HashSet<>();

	public Set<AttributeDelta> getAttributesDelta() {
		return attributesDelta;
	}

	@Override
	protected <T> void collect(String connIdAttrName, PropertyDelta<T> delta, PlusMinusZero isInModifiedAuxilaryClass, CollectorValuesConverter<T> valuesConverter) throws SchemaException {
		AttributeDeltaBuilder deltaBuilder = new AttributeDeltaBuilder();
		deltaBuilder.setName(connIdAttrName);
		if (delta.isAdd()) {
			List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToAdd(), delta.getElementName());
			if (delta.getDefinition().isMultiValue()) {
				deltaBuilder.addValueToAdd(connIdAttributeValues);
			} else {
				// Force "update" for single-valued attributes instead of "add". This is saving one
				// read in some cases. It should also make no substantial difference in such case.
				// But it is working around some connector bugs.
				deltaBuilder.addValueToReplace(connIdAttrName, connIdAttributeValues);
			}
		}
		if (delta.isDelete()) {
			if (delta.getDefinition().isMultiValue() || isInModifiedAuxilaryClass == PlusMinusZero.MINUS) {
				List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToDelete(), delta.getElementName());
				deltaBuilder.addValueToRemove(connIdAttributeValues);
			} else {
				// Force "update" for single-valued attributes instead of "add". This is saving one
				// read in some cases.
				// Update attribute to no values. This will efficiently clean up the attribute.
				// It should also make no substantial difference in such case.
				// But it is working around some connector bugs.
				// update with EMTPY value. The connIdAttributeValues is NOT used in this branch
				deltaBuilder.addValueToReplace(connIdAttrName, Collections.EMPTY_LIST);
			}
		}
		if (delta.isReplace()) {
			List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToReplace(), delta.getElementName());
			if (isInModifiedAuxilaryClass == PlusMinusZero.PLUS) {
				deltaBuilder.addValueToAdd(connIdAttributeValues);
			} else {
				deltaBuilder.addValueToReplace(connIdAttrName, connIdAttributeValues);
			}
		}
		attributesDelta.add(deltaBuilder.build());
	}

	@Override
	protected <T> void collectReplace(String connIdAttrName, T connIdAttrValue) throws SchemaException {
		if (connIdAttrValue == null) {
			attributesDelta.add(AttributeDeltaBuilder.build(connIdAttrName, Collections.EMPTY_LIST));
		} else {
			attributesDelta.add(AttributeDeltaBuilder.build(connIdAttrName, connIdAttrValue));
		}
	}

	@Override
	protected void debugDumpOutput(StringBuilder sb, int indent) {
		DebugUtil.debugDumpWithLabelLn(sb, "attributesDelta", attributesDelta, indent + 1);
	}
}
