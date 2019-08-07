/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 *
 */
public class ResourceAsserter<RA> extends PrismObjectAsserter<ResourceType,RA> {
	
	public ResourceAsserter(PrismObject<ResourceType> resource) {
		super(resource);
	}
	
	public ResourceAsserter(PrismObject<ResourceType> resource, String details) {
		super(resource, details);
	}
	
	public ResourceAsserter(PrismObject<ResourceType> resource, RA returnAsserter, String details) {
		super(resource, returnAsserter, details);
	}
	
	public static ResourceAsserter<Void> forResource(PrismObject<ResourceType> resource) {
		return new ResourceAsserter<>(resource);
	}
	
	public static ResourceAsserter<Void> forResource(PrismObject<ResourceType> resource, String details) {
		return new ResourceAsserter<>(resource, details);
	}
	
	@Override
	public ResourceAsserter<RA> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> assertName() {
		super.assertName();
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> assertName(String expectedOrig) {
		super.assertName(expectedOrig);
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> assertLifecycleState(String expected) {
		super.assertLifecycleState(expected);
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> assertActiveLifecycleState() {
		super.assertActiveLifecycleState();
		return this;
	}
	
	public ResourceAsserter<RA> assertHasSchema() {
		Element schemaElement = ResourceTypeUtil.getResourceXsdSchema(getObject());
		assertNotNull("No schema in "+desc(), schemaElement);
		return this;
	}
	
	
	@Override
	public ResourceAsserter<RA> display() {
		super.display();
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> display(String message) {
		super.display(message);
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> displayXml() throws SchemaException {
		super.displayXml();
		return this;
	}
	
	@Override
	public ResourceAsserter<RA> displayXml(String message) throws SchemaException {
		super.displayXml(message);
		return this;
	}

	@Override
	public ResourceAsserter<RA> assertOidDifferentThan(String oid) {
		super.assertOidDifferentThan(oid);
		return this;
	}

	@Override
	public ResourceAsserter<RA> assertNoTrigger() {
		super.assertNoTrigger();
		return this;
	}
}
