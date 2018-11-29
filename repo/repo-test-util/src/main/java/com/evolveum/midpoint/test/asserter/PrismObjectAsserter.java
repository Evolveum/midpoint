/**
 * Copyright (c) 2018 Evolveum
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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class PrismObjectAsserter<O extends ObjectType,RA> extends AbstractAsserter<RA> {
	
	private PrismObject<O> object;
	
	// Cache of focus-related objects: projections, targets, orgs, ...
	private Map<String,PrismObject<? extends ObjectType>> objectCache = new HashMap<>();

	public PrismObjectAsserter(PrismObject<O> object) {
		super();
		this.object = object;
	}
	
	public PrismObjectAsserter(PrismObject<O> object, String details) {
		super(details);
		this.object = object;
	}
	
	public PrismObjectAsserter(PrismObject<O> object, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.object = object;
	}
	
	public PrismObject<O> getObject() {
		return object;
	}

	public static <O extends ObjectType> PrismObjectAsserter<O,Void> forObject(PrismObject<O> shadow) {
		return new PrismObjectAsserter<>(shadow);
	}
	
	public static <O extends ObjectType> PrismObjectAsserter<O,Void> forObject(PrismObject<O> shadow, String details) {
		return new PrismObjectAsserter<>(shadow, details);
	}
	
	public PrismObjectAsserter<O,RA> assertOid() {
		assertNotNull("No OID in "+desc(), getObject().getOid());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertOid(String expected) {
		assertEquals("Wrong OID in "+desc(), expected, getObject().getOid());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertOidDifferentThan(String oid) {
		assertFalse("Expected that "+desc()+" will have different OID than "+oid+", but it has the same", oid.equals(getObject().getOid()));
		return this;
	}

	
	public PrismObjectAsserter<O,RA> assertName() {
		assertNotNull("No name in "+desc(), getObject().getName());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertName(String expectedOrig) {
		PrismAsserts.assertEqualsPolyString("Wrong name in "+desc(), expectedOrig, getObject().getName());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertDescription(String expected) {
		assertEquals("Wrong description in "+desc(), expected, getObject().asObjectable().getDescription());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertSubtype(String... expected) {
		PrismAsserts.assertEqualsCollectionUnordered("Wrong subtype in "+desc(), getObject().asObjectable().getSubtype(), expected);
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertTenantRef(String expectedOid) {
		ObjectReferenceType tenantRef = getObject().asObjectable().getTenantRef();
		if (tenantRef == null && expectedOid == null) {
			return this;
		}
		assertNotNull("No tenantRef in "+desc(), tenantRef);
		assertEquals("Wrong tenantRef OID in "+desc(), expectedOid, tenantRef.getOid());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertLifecycleState(String expected) {
		assertEquals("Wrong lifecycleState in "+desc(), expected, getObject().asObjectable().getLifecycleState());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertActiveLifecycleState() {
		String actualLifecycleState = getObject().asObjectable().getLifecycleState();
		if (actualLifecycleState != null) {
			assertEquals("Wrong lifecycleState in "+desc(), SchemaConstants.LIFECYCLE_ACTIVE, actualLifecycleState);
		}
		return this;
	}
	
	protected String desc() {
		return descWithDetails(object);
	}
	
	public PrismObjectAsserter<O,RA> display() {
		display(desc());
		return this;
	}
	
	public PrismObjectAsserter<O,RA> display(String message) {
		IntegrationTestTools.display(message, object);
		return this;
	}
	
	protected void assertPolyStringProperty(QName propName, String expectedOrig) {
		PrismProperty<PolyString> prop = getObject().findProperty(ItemName.fromQName(propName));
		assertNotNull("No "+propName.getLocalPart()+" in "+desc(), prop);
		PrismAsserts.assertEqualsPolyString("Wrong "+propName.getLocalPart()+" in "+desc(), expectedOrig, prop.getRealValue());
	}
	
	protected <T> void assertPropertyEquals(QName propName, T expected) {
		PrismProperty<T> prop = getObject().findProperty(ItemName.fromQName(propName));
		if (prop == null && expected == null) {
			return;
		}
		assertNotNull("No "+propName.getLocalPart()+" in "+desc(), prop);
		T realValue = prop.getRealValue();
		assertNotNull("No value in "+propName.getLocalPart()+" in "+desc(), realValue);
		assertEquals("Wrong "+propName.getLocalPart()+" in "+desc(), expected, realValue);
	}
	
	public PrismObjectAsserter<O,RA> assertNoItem(QName itemName) {
		Item<PrismValue, ItemDefinition> item = getObject().findItem(ItemName.fromQName(itemName));
		assertNull("Unexpected item "+itemName+" in "+desc(), item);
		return this;
	}
	
	public PrismObjectAsserter<O,RA> assertNoItem(UniformItemPath itemPath) {
		Item<PrismValue, ItemDefinition> item = getObject().findItem(itemPath);
		assertNull("Unexpected item "+itemPath+" in "+desc(), item);
		return this;
	}
	
	public String getOid() {
		return getObject().getOid();
	}
	
	public ParentOrgRefsAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> parentOrgRefs() {
		ParentOrgRefsAsserter<O,PrismObjectAsserter<O,RA>,RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public PrismObjectAsserter<O,RA> assertParentOrgRefs(String... expectedOids) {
		parentOrgRefs().assertRefs(expectedOids);
		return this;
	}

	<CO extends ObjectType> PrismObject<CO> getCachedObject(Class<CO> type, String oid) throws ObjectNotFoundException, SchemaException {
		PrismObject<CO> object = (PrismObject<CO>) objectCache.get(oid);
		if (object == null) {
			object = resolveObject(type, oid);
			objectCache.put(oid, object);
		}
		return object;
	}
	
	public ExtensionAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> extension() {
		ExtensionAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> asserter = new ExtensionAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	public TriggersAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> triggers() {
		TriggersAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
}
