/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;

import net.sf.saxon.type.SchemaException;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class AccessChecker {
	
	public static final String OPERATION_NAME = AccessChecker.class.getName()+".accessCheck";
	private static final Trace LOGGER = TraceManager.getTrace(AccessChecker.class);

	public void checkAdd(ResourceType resource, PrismObject<ShadowType> shadow,
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createMinorSubresult(OPERATION_NAME);
		ResourceAttributeContainer attributeCont = ShadowUtil.getAttributesContainer(shadow);
		
		for (ResourceAttribute<?> attribute: attributeCont.getAttributes()) {
			RefinedAttributeDefinition attrDef = objectClassDefinition.findAttributeDefinition(attribute.getName());
			PropertyLimitations limitations = attrDef.getLimitations(LayerType.SCHEMA);
			if (limitations == null) {
				continue;
			}
			if (limitations.isIgnore()) {
				String message = "Attempt to create shadow with ignored attribute "+attribute.getName();
				LOGGER.error(message);
				throw new SchemaException(message);
			}
			PropertyAccessType access = limitations.getAccess();
			if (access == null) {
				continue;
			}
			if (access.isCreate() == null || !access.isCreate()) {
				String message = "Attempt to create shadow with non-createable attribute "+attribute.getName();
				LOGGER.error(message);
				result.recordFatalError(message);
				throw new SecurityException(message);
			}
		}
		result.recordSuccess();
	}
	
	

}
