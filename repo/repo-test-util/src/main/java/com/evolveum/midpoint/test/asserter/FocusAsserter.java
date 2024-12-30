/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class FocusAsserter<F extends FocusType,RA> extends AssignmentHolderAsserter<F,RA> {

    public FocusAsserter(PrismObject<F> focus) {
        super(focus);
    }

    public FocusAsserter(PrismObject<F> focus, String details) {
        super(focus, details);
    }

    public FocusAsserter(PrismObject<F> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static <F extends FocusType> FocusAsserter<F,Void> forFocus(PrismObject<F> focus) {
        return new FocusAsserter<>(focus);
    }

    public static <F extends FocusType> FocusAsserter<F,Void> forFocus(PrismObject<F> focus, String details) {
        return new FocusAsserter<>(focus, details);
    }

    // ::::::::::::::::::::::::::::::::::::::::::
    // : NOTE : WARNING : ATTENTION : LOOK HERE :
    // ::::::::::::::::::::::::::::::::::::::::::
    //
    // If you add any method here, add it also in UserAsserter, OrgAsserter and other subclasses.
    //
    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.

    @Override
    public FocusAsserter<F,RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    public FocusAsserter<F,RA> assertLocality(String expectedOrig) {
        assertPolyStringProperty(OrgType.F_LOCALITY, expectedOrig);
        return this;
    }

    public FocusAsserter<F,RA> assertCostCenter(String expected) {
        assertEquals("Wrong costCenter in "+desc(), expected, getObject().asObjectable().getCostCenter());
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertIndestructible(Boolean expected) {
        super.assertIndestructible(expected);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertIndestructible() {
        super.assertIndestructible();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertDestructible() {
        super.assertDestructible();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertEffectiveMark(String oid) {
        super.assertEffectiveMark(oid);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertEffectiveMarks(String... oids) {
        super.assertEffectiveMarks(oids);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertNoEffectiveMark(String oid) {
        super.assertNoEffectiveMark(oid);
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertNoEffectiveMarks() {
        super.assertNoEffectiveMarks();
        return this;
    }

    public ActivationAsserter<? extends FocusAsserter<F,RA>> activation() {
        ActivationAsserter<FocusAsserter<F,RA>> asserter = new ActivationAsserter<>(getObject().asObjectable().getActivation(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusAsserter<F,RA> assertAdministrativeStatus(ActivationStatusType expected) {
        ActivationType activation = getActivation();
        if (activation == null) {
            if (expected == null) {
                return this;
            } else {
                fail("No activation in "+desc());
            }
        }
        assertEquals("Wrong activation administrativeStatus in "+desc(), expected, activation.getAdministrativeStatus());
        return this;
    }

    private ActivationType getActivation() {
        return getObject().asObjectable().getActivation();
    }

    public FocusAsserter<F,RA> display() {
        super.display();
        return this;
    }

    public FocusAsserter<F,RA> display(String message) {
        super.display(message);
        return this;
    }

    public LinksAsserter<F, ? extends FocusAsserter<F,RA>, RA> links() {
        LinksAsserter<F,FocusAsserter<F,RA>,RA> asserter = new LinksAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusAsserter<F,RA> assertLiveLinks(int expected) {
        links().assertLiveLinks(expected);
        return this;
    }

    public FocusAsserter<F,RA> assertRelatedLinks(int expected) {
        links().assertDeadLinks(expected);
        return this;
    }

    public FocusAsserter<F,RA> assertLinks(int live, int related) {
        links().assertLinks(live, related);
        return this;
    }

    public ShadowReferenceAsserter<? extends FocusAsserter<F,RA>> singleLink() {
        PrismReference linkRef = getObject().findReference(FocusType.F_LINK_REF);
        if (linkRef == null) {
            fail("Expected single link, but is no linkRef in "+desc());
            return null; // not reached
        }
        assertEquals("Wrong number of links in " + desc(), 1, linkRef.size());
        ShadowReferenceAsserter<FocusAsserter<F, RA>> asserter = new ShadowReferenceAsserter<>(linkRef.getAnyValue(), null, this, "link in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public ParentOrgRefsAsserter<F, ? extends FocusAsserter<F,RA>, RA> parentOrgRefs() {
        ParentOrgRefsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public FocusAsserter<F,RA> assertParentOrgRefs(String... expectedOids) {
        super.assertParentOrgRefs(expectedOids);
        return this;
    }

    public FocusAsserter<F,RA> assertHasProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        PrismObject<ShadowType> shadow = findProjectionOnResource(resourceOid);
        assertNotNull("Projection for resource "+resourceOid+" not found in "+desc(), shadow);
        return this;
    }

    public <A extends FocusAsserter<F,RA>> ShadowAsserter<A> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        PrismObject<ShadowType> shadow = findProjectionOnResource(resourceOid);
        assertNotNull("Projection for resource "+resourceOid+" not found in "+desc(), shadow);
        ShadowAsserter<A> asserter = new ShadowAsserter<A>(shadow, (A)this, "projection of "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    private PrismObject<ShadowType> findProjectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        F focusType = getObject().asObjectable();
        for (PrismObject<ShadowType> shadow: getLinkTargets()) {
            if (resourceOid.equals(shadow.asObjectable().getResourceRef().getOid())) {
                return shadow;
            }
        }
        return null;
    }

    public FocusAsserter<F,RA> assertLinked(String targetOid) throws ObjectNotFoundException, SchemaException {
        F focusType = getObject().asObjectable();
        for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
            if (targetOid.equals(linkRefType.getOid())) {
                return this;
            }
        }
        fail("Target "+targetOid+" not linked to "+desc());
        return null; // not reached
    }

    private List<PrismObject<ShadowType>> getLinkTargets() throws ObjectNotFoundException, SchemaException {
        F focusType = getObject().asObjectable();
        List<PrismObject<ShadowType>> shadows = new ArrayList<>();
        for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
            String linkTargetOid = linkRefType.getOid();
            assertFalse("No linkRef oid in "+desc(), StringUtils.isBlank(linkTargetOid));
            shadows.add(getCachedObject(ShadowType.class, linkTargetOid));
        }
        return shadows;
    }

    public  FocusAsserter<F,RA> displayWithProjections() throws ObjectNotFoundException, SchemaException {
        StringBuilder sb = new StringBuilder();
        List<ObjectReferenceType> linkRefs = getObject().asObjectable().getLinkRef();
        sb.append(getObject()).append(" ").append(linkRefs.size()).append(" projections:");
        for (ObjectReferenceType linkRefType: linkRefs) {
            sb.append("\n  ");
            String linkTargetOid = linkRefType.getOid();
            assertFalse("No linkRef oid in "+desc(), StringUtils.isBlank(linkTargetOid));
            try {
                PrismObject<ShadowType> linkTarget = getCachedObject(ShadowType.class, linkTargetOid);
                sb.append(linkTarget);
                ShadowType shadowType = linkTarget.asObjectable();
                ObjectReferenceType resourceRef = shadowType.getResourceRef();
                sb.append(", resource=").append(resourceRef.getOid());
                appendFlag(sb, "dead", shadowType.isDead());
                appendFlag(sb, "exists", shadowType.isExists());
                List<PendingOperationType> pendingOperations = shadowType.getPendingOperation();
                if (!pendingOperations.isEmpty()) {
                    sb.append(", ").append(pendingOperations.size()).append(" pending operations");
                }
            } catch (ObjectNotFoundException e) {
                sb.append("shadow(").append(linkTargetOid).append(": NOT FOUND");
            } catch (Exception e) {
                sb.append("shadow(").append(linkTargetOid).append("): ERROR(")
                    .append(e.getClass().getSimpleName()).append("): ").append(e.getMessage());
            }

        }
        IntegrationTestTools.display(desc(), sb.toString());
        return this;
    }

    private void appendFlag(StringBuilder sb, String label, Boolean val) {
        if (val != null) {
            sb.append(", ").append(label).append("=").append(val);
        }
    }

    public AssignmentsAsserter<F, ? extends FocusAsserter<F,RA>, RA> assignments() {
        AssignmentsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusAsserter<F,RA> assertAssignments(int expected) {
        assignments().assertAssignments(expected);
        return this;
    }

    public RoleMembershipRefsAsserter<F, ? extends FocusAsserter<F,RA>, RA> roleMembershipRefs() {
        RoleMembershipRefsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new RoleMembershipRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusAsserter<F,RA> assertRoleMembershipRefs(int expected) {
        roleMembershipRefs().assertRoleMemberhipRefs(expected);
        return this;
    }

    public ArchetypeRefsAsserter<F, ? extends FocusAsserter<F,RA>, RA> archetypesRefs() {
        ArchetypeRefsAsserter<F,FocusAsserter<F,RA>,RA> asserter = new ArchetypeRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusAsserter<F,RA> assertArchetypeRefs(int expected) {
        archetypesRefs().assertArchetypeRefs(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<F, ? extends FocusAsserter<F, RA>> extension() {
        ExtensionAsserter<F, FocusAsserter<F, RA>> asserter = new ExtensionAsserter<>(getObjectable(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public TriggersAsserter<F, ? extends FocusAsserter<F,RA>, RA> triggers() {
        TriggersAsserter<F, ? extends FocusAsserter<F,RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    public FocusAsserter<F,RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    public FocusAsserter<F,RA> assertArchetypeRef(String expectedArchetypeOid) {
        super.assertArchetypeRef(expectedArchetypeOid);
        return this;
    }

    public FocusAsserter<F,RA> assertNoArchetypeRef() {
        super.assertNoArchetypeRef();
        return this;
    }

    @Override
    public FocusAsserter<F,RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }

    public FocusAsserter<F,RA> assertPassword(String expectedClearPassword) throws SchemaException, EncryptionException {
        assertPassword(expectedClearPassword, CredentialsStorageTypeType.ENCRYPTION);
        return this;
    }

    public FocusAsserter<F,RA> assertHasPassword() throws SchemaException, EncryptionException {
        assertHasPassword(CredentialsStorageTypeType.ENCRYPTION);
        return this;
    }

    public FocusAsserter<F,RA> assertPassword(String expectedClearPassword, CredentialsStorageTypeType storageType) throws SchemaException, EncryptionException {
        CredentialsType creds = getObject().asObjectable().getCredentials();
        assertNotNull("No credentials in "+desc(), creds);
        PasswordType password = creds.getPassword();
        assertNotNull("No password in "+desc(), password);
        ProtectedStringType protectedActualPassword = password.getValue();
        IntegrationTestTools.assertProtectedString("Password for "+desc(), expectedClearPassword, protectedActualPassword, storageType, getProtector());
        return this;
    }

    public FocusAsserter<F,RA> assertHasPassword(CredentialsStorageTypeType storageType) throws SchemaException, EncryptionException {
        CredentialsType creds = getObject().asObjectable().getCredentials();
        assertNotNull("No credentials in "+desc(), creds);
        PasswordType password = creds.getPassword();
        assertNotNull("No password in "+desc(), password);
        IntegrationTestTools.assertHasProtectedString("Password for "+desc(), password.getValue(), storageType, getProtector());
        return this;
    }

    public FocusIdentitiesAsserter<FocusAsserter<F, RA>> identities() {
        //noinspection unchecked
        FocusIdentitiesAsserter<FocusAsserter<F, RA>> asserter =
                new FocusIdentitiesAsserter<>(
                        (PrismContainerValue<FocusIdentitiesType>)
                                MiscUtil.requireNonNull(
                                                getObjectable().getIdentities(),
                                                () -> new AssertionError("No identities in " + getObjectable()))
                                        .asPrismContainerValue(),
                        this,
                        getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public FocusAsserter<F, RA> withObjectResolver(SimpleObjectResolver objectResolver) {
        return (FocusAsserter<F, RA>) super.withObjectResolver(objectResolver);
    }
}
