/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.test.asserter.prism.PolyStringAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public class UserAsserter<RA> extends FocusAsserter<UserType, RA> {

    public UserAsserter(PrismObject<UserType> focus) {
        super(focus);
    }

    public UserAsserter(PrismObject<UserType> focus, String details) {
        super(focus, details);
    }

    public UserAsserter(PrismObject<UserType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static UserAsserter<Void> forUser(PrismObject<UserType> focus) {
        return new UserAsserter<>(focus);
    }

    public static UserAsserter<Void> forUser(PrismObject<UserType> focus, String details) {
        return new UserAsserter<>(focus, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public UserAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public UserAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public UserAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public UserAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public UserAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public UserAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public UserAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    @Override
    public UserAsserter<RA> assertIndestructible(Boolean expected) {
        super.assertIndestructible(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertIndestructible() {
        super.assertIndestructible();
        return this;
    }

    @Override
    public UserAsserter<RA> assertDestructible() {
        super.assertDestructible();
        return this;
    }

    public UserAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
        ActivationType activation = getActivation();
        if (activation == null) {
            if (expected == null) {
                return this;
            } else {
                fail("No activation in " + desc());
            }
        }
        assertEquals("Wrong activation administrativeStatus in " + desc(), expected, activation.getAdministrativeStatus());
        return this;
    }

    private ActivationType getActivation() {
        return getObject().asObjectable().getActivation();
    }

    public UserAsserter<RA> assertPassword(String expectedClearPassword) throws SchemaException, EncryptionException {
        super.assertPassword(expectedClearPassword);
        return this;
    }

    public UserAsserter<RA> assertPassword(String expectedClearPassword, CredentialsStorageTypeType storageType) throws SchemaException, EncryptionException {
        super.assertPassword(expectedClearPassword, storageType);
        return this;
    }

    public UserAsserter<RA> assertHasPassword() throws SchemaException, EncryptionException {
        super.assertHasPassword();
        return this;
    }

    public UserAsserter<RA> assertHasPassword(CredentialsStorageTypeType storageType) throws SchemaException, EncryptionException {
        super.assertHasPassword(storageType);
        return this;
    }

    public UserAsserter<RA> display() {
        super.display();
        return this;
    }

    public UserAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public UserAsserter<RA> displayXml() throws SchemaException {
        super.displayXml();
        return this;
    }

    @Override
    public UserAsserter<RA> displayXml(String message) throws SchemaException {
        super.displayXml(message);
        return this;
    }

    @Override
    public ActivationAsserter<UserAsserter<RA>> activation() {
        ActivationAsserter<UserAsserter<RA>> asserter = new ActivationAsserter<>(getObject().asObjectable().getActivation(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public LinksAsserter<UserType, UserAsserter<RA>, RA> links() {
        LinksAsserter<UserType, UserAsserter<RA>, RA> asserter = new LinksAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public UserAsserter<RA> assertLiveLinks(int expected) {
        super.assertLiveLinks(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertRelatedLinks(int expected) {
        super.assertRelatedLinks(expected);
        return this;
    }

    @Override
    public AssignmentsAsserter<UserType, UserAsserter<RA>, RA> assignments() {
        AssignmentsAsserter<UserType, UserAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PolyStringAsserter<UserAsserter<RA>> fullName() {
        PolyStringAsserter<UserAsserter<RA>> asserter = new PolyStringAsserter<>(getPolyStringPropertyValue(UserType.F_FULL_NAME), this, "fullName in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public UserAsserter<RA> assertFullName(String expectedOrig) {
        assertPolyStringProperty(UserType.F_FULL_NAME, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertNoFullName() {
        assertNoItem(UserType.F_FULL_NAME);
        return this;
    }

    public UserAsserter<RA> assertGivenName(String expectedOrig) {
        assertPolyStringProperty(UserType.F_GIVEN_NAME, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertNoGivenName() {
        assertNoItem(UserType.F_GIVEN_NAME);
        return this;
    }

    public UserAsserter<RA> assertFamilyName(String expectedOrig) {
        assertPolyStringProperty(UserType.F_FAMILY_NAME, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertNoFamilyName() {
        assertNoItem(UserType.F_FAMILY_NAME);
        return this;
    }

    public UserAsserter<RA> assertAdditionalName(String expectedOrig) {
        assertPolyStringProperty(UserType.F_ADDITIONAL_NAME, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertNoAdditionalName() {
        assertNoItem(UserType.F_ADDITIONAL_NAME);
        return this;
    }

    public UserAsserter<RA> assertTitle(String expectedOrig) {
        assertPolyStringProperty(UserType.F_TITLE, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertNoTitle() {
        assertNoItem(UserType.F_TITLE);
        return this;
    }

    public UserAsserter<RA> assertEmployeeNumber(String expected) {
        assertEquals("Wrong employeeNumber in " + desc(), expected, getObject().asObjectable().getEmployeeNumber());
        return this;
    }

    public UserAsserter<RA> assertEmployeeNumber() {
        assertNotNull("Missing employeeNumber in " + desc(), getObject().asObjectable().getEmployeeNumber());
        return this;
    }

    public UserAsserter<RA> assertEmailAddress(String expected) {
        assertEquals("Wrong emailAddress in " + desc(), expected, getObject().asObjectable().getEmailAddress());
        return this;
    }

    public UserAsserter<RA> assertTelephoneNumber(String expected) {
        assertEquals("Wrong telephoneNumber in " + desc(), expected, getObject().asObjectable().getTelephoneNumber());
        return this;
    }

    public UserAsserter<RA> assertJpegPhoto() {
        PrismProperty<Object> photoProperty = getObject().findProperty(UserType.F_JPEG_PHOTO);
        assertTrue("Missing jpegPhoto in " + desc(), photoProperty != null &&
                (photoProperty.isIncomplete() || photoProperty.size() == 1 && photoProperty.getRealValue() != null));
        return this;
    }

    public UserAsserter<RA> assertJpegPhoto(byte[] expected) {
        assertEquals("Wrong jpegPhoto in " + desc(), expected, getObject().asObjectable().getJpegPhoto());
        return this;
    }

    @Override
    public UserAsserter<RA> assertLocality(String expected) {
        super.assertLocality(expected);
        return this;
    }

    @Override
    public UserAsserter<RA> assertCostCenter(String expected) {
        super.assertCostCenter(expected);
        return this;
    }

    public UserAsserter<RA> assertOrganizationalUnit(String expectedOrig) {
        assertPolyStringProperty(UserType.F_ORGANIZATIONAL_UNIT, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertOrganizationalUnits(String... expectedOrig) {
        assertPolyStringPropertyMulti(UserType.F_ORGANIZATIONAL_UNIT, expectedOrig);
        return this;
    }

    public UserAsserter<RA> assertNoOrganizationalUnit() {
        assertNoItem(UserType.F_ORGANIZATIONAL_UNIT);
        return this;
    }

    public UserAsserter<RA> assertOrganizations(String... expectedOrig) {
        assertPolyStringPropertyMulti(UserType.F_ORGANIZATION, expectedOrig);
        return this;
    }

    @Override
    public UserAsserter<RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        super.assertHasProjectionOnResource(resourceOid);
        return this;
    }

    @Override
    public ShadowAsserter<UserAsserter<RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        return super.projectionOnResource(resourceOid);
    }

    @Override
    public UserAsserter<RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
        super.displayWithProjections();
        return this;
    }

    @Override
    public ShadowReferenceAsserter<UserAsserter<RA>> singleLink() {
        return (ShadowReferenceAsserter<UserAsserter<RA>>) super.singleLink();
    }

    @Override
    public UserAsserter<RA> assertAssignments(int expected) {
        super.assertAssignments(expected);
        return this;
    }

    @Override
    public ParentOrgRefsAsserter<UserType, UserAsserter<RA>, RA> parentOrgRefs() {
        ParentOrgRefsAsserter<UserType, UserAsserter<RA>, RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public UserAsserter<RA> assertParentOrgRefs(String... expectedOids) {
        super.assertParentOrgRefs(expectedOids);
        return this;
    }

    @Override
    public RoleMembershipRefsAsserter<UserType, ? extends UserAsserter<RA>, RA> roleMembershipRefs() {
        RoleMembershipRefsAsserter<UserType, UserAsserter<RA>, RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public UserAsserter<RA> assertRoleMembershipRefs(int expected) {
        super.assertRoleMembershipRefs(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<UserType, ? extends UserAsserter<RA>> extension() {
        ExtensionAsserter<UserType, UserAsserter<RA>> asserter = new ExtensionAsserter<>(getObjectable(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public ValueMetadataAsserter<? extends UserAsserter<RA>> valueMetadata(ItemPath path)
            throws SchemaException {
        //noinspection unchecked
        return (ValueMetadataAsserter<? extends UserAsserter<RA>>) super.valueMetadata(path);
    }

    @Override
    public ValueMetadataAsserter<? extends UserAsserter<RA>> valueMetadata(ItemPath path, ValueSelector<?> valueSelector) throws SchemaException {
        //noinspection unchecked
        return (ValueMetadataAsserter<? extends UserAsserter<RA>>) super.valueMetadata(path, valueSelector);
    }

    @Override
    public ValueMetadataValueAsserter<? extends UserAsserter<RA>> valueMetadataSingle(ItemPath path) throws SchemaException {
        //noinspection unchecked
        return (ValueMetadataValueAsserter<? extends UserAsserter<RA>>) super.valueMetadataSingle(path);
    }

    @Override
    public ValueMetadataValueAsserter<? extends UserAsserter<RA>> valueMetadataSingle(ItemPath path, ValueSelector<?> valueSelector) throws SchemaException {
        //noinspection unchecked
        return (ValueMetadataValueAsserter<? extends UserAsserter<RA>>) super.valueMetadataSingle(path, valueSelector);
    }

    @Override
    public TriggersAsserter<UserType, ? extends UserAsserter<RA>, RA> triggers() {
        TriggersAsserter<UserType, ? extends UserAsserter<RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public UserAsserter<RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    @Override
    public UserAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        super.assertArchetypeRef(expectedArchetypeOid);
        return this;
    }

    @Override
    public UserAsserter<RA> assertNoArchetypeRef() {
        super.assertNoArchetypeRef();
        return this;
    }

    @Override
    public UserAsserter<RA> assertArchetypeRefs(int size) {
        super.assertArchetypeRefs(size);
        return this;
    }

    @Override
    public UserAsserter<RA> assertHasArchetype(String expectedArchetypeOid) {
        super.assertHasArchetype(expectedArchetypeOid);
        return this;
    }

    @Override
    public UserAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }

    @Override
    public UserAsserter<RA> assertPolyStringProperty(QName propName, String expectedOrig) {
        return (UserAsserter<RA>) super.assertPolyStringProperty(propName, expectedOrig);
    }

    @Override
    public UserAsserter<RA> assertExtensionValue(String localName, Object realValue) {
        return (UserAsserter<RA>) super.assertExtensionValue(localName, realValue);
    }

    @Override
    public UserAsserter<RA> withObjectResolver(SimpleObjectResolver objectResolver) {
        return (UserAsserter<RA>) super.withObjectResolver(objectResolver);
    }
}
