/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.model.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.ldap.AbstractResourceController;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author semancik
 *
 */
public class DummyResourceContoller extends AbstractResourceController {
	
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME = "fullname";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME = "title";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME = "location";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME = "loot";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME = "ship";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME = "weapon";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME = "drink";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME = "quote";
	
	private DummyResource dummyResource;
	private boolean isExtendedSchema = false;
	
	public static DummyResourceContoller create(String instanceName, PrismObject<ResourceType> resource) {
		DummyResourceContoller ctl = new DummyResourceContoller();
		
		ctl.dummyResource = DummyResource.getInstance();
		ctl.dummyResource.reset();
		ctl.populateWithDefaultSchema();
		
		ctl.resource = resource;
		
		return ctl;
	}
	
	public DummyResource getDummyResource() {
		return dummyResource;
	}


	public void populateWithDefaultSchema() {
		dummyResource.populateWithDefaultSchema();
	}

	public void extendDummySchema() throws ConnectException, FileNotFoundException {
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		DummyAttributeDefinition titleAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, String.class, false, true);
		accountObjectClass.add(titleAttrDef);
		DummyAttributeDefinition shipAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, String.class, false, false);
		accountObjectClass.add(shipAttrDef);
		DummyAttributeDefinition locationAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, String.class, false, false);
		accountObjectClass.add(locationAttrDef);
		DummyAttributeDefinition lootAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, Integer.class, false, false);
		accountObjectClass.add(lootAttrDef);
		DummyAttributeDefinition weaponAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, String.class, false, true);
		accountObjectClass.add(weaponAttrDef);
		DummyAttributeDefinition drinkAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class, false, true);
		accountObjectClass.add(drinkAttrDef);
		DummyAttributeDefinition quoteAttrDef = new DummyAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, String.class, false, true);
		accountObjectClass.add(quoteAttrDef);
		isExtendedSchema = true;
	}
	
	public QName getAttributeFullnameQName() {
		return new QName(getNamespace(), DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
	}

	public ItemPath getAttributeFullnamePath() {
		return new ItemPath(AccountShadowType.F_ATTRIBUTES, getAttributeFullnameQName());
	}
	
	public QName getAttributeWeaponQName() {
		assertExtendedSchema();
		return new QName(getNamespace(), DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
	}

	public ItemPath getAttributeWeaponPath() {
		assertExtendedSchema();
		return new ItemPath(AccountShadowType.F_ATTRIBUTES, getAttributeWeaponQName());
	}
	
	private void assertExtendedSchema() {
		assert isExtendedSchema : "Resource "+resource+" does not have extended schema yet an extedned attribute was requested";
	}

	public void assertRefinedSchemaSanity(RefinedResourceSchema refinedSchema) {
		
		RefinedAccountDefinition accountDef = refinedSchema.getDefaultAccountDefinition();
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canCreate());
		assertFalse("UID has update",uidDef.canUpdate());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertTrue("No NAME update",nameDef.canUpdate());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS,"password")));
		
	}

	public void addAccount(String userId, String fullName) throws ObjectAlreadyExistsException, SchemaViolationException {
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		dummyResource.addAccount(account);
	}

	public void addAccount(String userId, String fullName, String location) throws ObjectAlreadyExistsException, SchemaViolationException {
		assertExtendedSchema();
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, location);
		dummyResource.addAccount(account);
	}

}
