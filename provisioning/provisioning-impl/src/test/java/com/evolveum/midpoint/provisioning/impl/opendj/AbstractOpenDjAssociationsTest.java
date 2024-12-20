/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;

import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * Complex testing of associations-related functionality in OpenDJ resource. Treats all three kinds of associations/references:
 *
 * - native references ({@link TestOpenDjAssociationsNative}),
 * - modern simulated references,
 * - legacy simulated references/associations ({@link TestOpenDjAssociationsLegacySimulated}).
 *
 * OpenDJ was chosen because we want to test auxiliary object classes and delineations based on the base context.
 *
 * It is separate from main {@link TestOpenDj} to keep the main test class of manageable size and complexity.
 *
 * LIMITED FUNCTIONALITY FOR NOW: just reading the accounts and deleting them
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class AbstractOpenDjAssociationsTest extends AbstractOpenDjTest {

    protected static final File TEST_DIR = new File("src/test/resources/opendj/associations");

    private static final File RESOURCE_OPENDJ_TEMPLATE_FILE = new File(TEST_DIR, "resource-opendj-template.xml");

    private static final QName RI_GROUP = new QName(NS_RI, "group");
    private static final QName RI_POSIX_GROUP = new QName(NS_RI, "posixGroup");
    private static final QName RI_CAR = new QName(NS_RI, "car");

    private static final String DN_GROUP1 = "cn=group1,ou=groups,dc=example,dc=com";
    private static final String DN_GROUP2 = "cn=group2,ou=posixGroups,dc=example,dc=com";
    private static final String DN_INVISIBLE = "cn=invisible,dc=example,dc=com";
    private static final String DN_INVISIBLE2 = "cn=invisible2,dc=example,dc=com";
    private static final String DN_INVISIBLE3 = "cn=invisible3,dc=example,dc=com";
    private static final String DN_10001 = "cn=10001,ou=passengerCars,dc=example,dc=com";
    private static final String DN_10002 = "cn=10002,ou=lightTrucks,dc=example,dc=com";
    private static final String DN_TEST1 = "uid=test1,ou=people,dc=example,dc=com";
    private static final String DN_TEST2 = "uid=test2,ou=people,dc=example,dc=com";
    private static final String DN_TEST3 = "uid=test3,ou=people,dc=example,dc=com";
    private static final String DN_TEST4 = "uid=test4,ou=people,dc=example,dc=com";
    private static final String UID_TEST4 = "test4";
    private static final String DN_TEST5 = "uid=test5,ou=people,dc=example,dc=com";

    @AfterClass
    public void stopLdap() {
        doStopLdap();
    }

    @Override
    protected void beforeImportingResource(Task initTask, OperationResult initResult) throws Exception {
        super.beforeImportingResource(initTask, initResult);
        startLdap();
        addResourceFromFile(RESOURCE_OPENDJ_TEMPLATE_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, initResult);
    }

    /** For some strange reason, @BeforeClass does not work here. So we do this explicitly. */
    private void startLdap() throws Exception {
        doStartLdap();

        // Base context for posix groups
        openDJController.addEntry(
                """
                        dn: ou=posixGroups,dc=example,dc=com
                        objectclass: organizationalUnit
                        ou: posixGroups
                        """);

        // Base contexts for cars
        openDJController.addEntry(
                """
                        dn: ou=passengerCars,dc=example,dc=com
                        objectclass: organizationalUnit
                        ou: passengerCars
                        """);

        openDJController.addEntry(
                """
                        dn: ou=lightTrucks,dc=example,dc=com
                        objectclass: organizationalUnit
                        ou: lightTrucks
                        """);

        // Accounts
        // --------
        //
        // Baseline: account with no membership
        openDJController.addEntry(
                """
                        dn: uid=test1,ou=people,dc=example,dc=com
                        objectclass: inetOrgPerson
                        uid: test1
                        cn: test1
                        sn: test1
                        """);

        // Not a posix account, so no posix membership. Only a single visible and single invisible group membership.
        openDJController.addEntry(
                """
                        dn: uid=test2,ou=people,dc=example,dc=com
                        objectclass: inetOrgPerson
                        uid: test2
                        cn: test2
                        sn: test2
                        """);

        // A posix account, but without posix membership. Only LDAP membership is there.
        openDJController.addEntry(
                """
                        dn: uid=test3,ou=people,dc=example,dc=com
                        objectclass: inetOrgPerson
                        objectclass: posixAccount
                        uid: test3
                        cn: test3
                        sn: test3
                        uidNumber: 1003
                        gidNumber: 1003
                        homeDirectory: /home/test3
                        """);

        // A posix account, but with posix and LDAP membership.
        openDJController.addEntry(
                """
                        dn: uid=test4,ou=people,dc=example,dc=com
                        objectclass: inetOrgPerson
                        objectclass: posixAccount
                        uid: test4
                        cn: test4
                        sn: test4
                        uidNumber: 1004
                        gidNumber: 1004
                        homeDirectory: /home/test4
                        """);

        // A posix account with some cars - ONLY for legacy associations
        if (isLegacySimulation()) {
            openDJController.addEntry(
                    """
                            dn: uid=test5,ou=people,dc=example,dc=com
                            objectclass: inetOrgPerson
                            objectclass: posixAccount
                            uid: test5
                            cn: test5
                            sn: test5
                            uidNumber: 1005
                            gidNumber: 1005
                            homeDirectory: /home/test5
                            carLicense: 10000
                            carLicense: 10001
                            carLicense: 10002
                            """);
        }

        // Groups
        // ------
        //
        // Regular LDAP group
        openDJController.addEntry(
                """
                        dn: cn=group1,ou=groups,dc=example,dc=com
                        objectclass: groupOfUniqueNames
                        cn: group1
                        uniqueMember: uid=test2,ou=people,dc=example,dc=com
                        uniqueMember: uid=test3,ou=people,dc=example,dc=com
                        uniqueMember: uid=test4,ou=people,dc=example,dc=com
                        """);

        openDJController.assertUniqueMembers(DN_GROUP1, DN_TEST2, DN_TEST3, DN_TEST4);

        // An LDAP group that is not visible as "entitlement/group", because it's outside of the base context
        openDJController.addEntry(
                """
                        dn: cn=invisible,dc=example,dc=com
                        objectclass: groupOfUniqueNames
                        cn: invisible
                        uniqueMember: uid=test2,ou=people,dc=example,dc=com
                        """);

        openDJController.assertUniqueMembers(DN_INVISIBLE, DN_TEST2);

        // Regular posix group
        openDJController.addEntry(
                """
                        dn: cn=group2,ou=posixGroups,dc=example,dc=com
                        objectclass: groupOfNames
                        objectclass: posixGroup
                        cn: group2
                        gidNumber: 102
                        memberUid: test4
                        """);

        openDJController.assertMemberUids(DN_GROUP2, UID_TEST4);

        // An posix group that is not visible as "entitlement/posixGroup", because it's outside of the base context
        openDJController.addEntry(
                """
                        dn: cn=invisible2,dc=example,dc=com
                        objectclass: groupOfNames
                        objectclass: posixGroup
                        cn: invisible2
                        gidNumber: 2
                        memberUid: test4
                        """);

        openDJController.assertMemberUids(DN_INVISIBLE2, UID_TEST4);

        // Unclassified car
        openDJController.addEntry(
                """
                        dn: cn=invisible3,dc=example,dc=com
                        objectclass: groupOfNames
                        objectclass: posixGroup
                        cn: invisible3
                        gidNumber: 10000
                        """);

        // No members, as this is "subject-to-object" association (actually, the entry need not be a group at all...)
        // The same for the following two groups.

        // Passenger car
        openDJController.addEntry(
                """
                        dn: cn=10001,ou=passengerCars,dc=example,dc=com
                        objectclass: groupOfNames
                        objectclass: posixGroup
                        cn: 10001
                        gidNumber: 10001
                        """);

        // Light truck
        openDJController.addEntry(
                """
                        dn: cn=10002,ou=lightTrucks,dc=example,dc=com
                        objectclass: groupOfNames
                        objectclass: posixGroup
                        cn: 10002
                        gidNumber: 10002
                        """);
    }

    private boolean isLegacySimulation() {
        return this instanceof TestOpenDjAssociationsLegacySimulated;
    }

    private boolean areNativeReferences() {
        return this instanceof TestOpenDjAssociationsNative;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        assertSuccess(
                provisioningService.testResource(RESOURCE_OPENDJ_OID, initTask, initResult));
        resource = provisioningService.getObject(
                ResourceType.class, RESOURCE_OPENDJ_OID, null, initTask, initResult);
        resourceBean = resource.asObjectable();
    }

    @Test
    public void test100SearchAccounts() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("searching for accounts");
        var shadows = getAllTestShadows(task, result);

        then("account shadows are there");
        display("shadows", shadows);

        and("group shadows are there");
        var group1 = findShadowByPrismName(DN_GROUP1, resource, result);
        assertThat(group1).as("shadow for group1").isNotNull();
        var group2 = findShadowByPrismName(DN_GROUP2, resource, result);
        assertThat(group2).as("shadow for group2").isNotNull();
        var invisible = findShadowByPrismName(DN_INVISIBLE, resource, result);
        assertThat(invisible).as("shadow for invisible group").isNotNull();

        and("test1 has no association nor reference attribute");
        assertShadowNew(getShadow(shadows, DN_TEST1))
                .associations()
                .assertValuesCount(0)
                .end()
                .attributes()
                .assertNoAttribute(RI_GROUP);

        and("test2 has one association (visible membership) and one reference attribute value (invisible one)");
        assertShadowNew(getShadow(shadows, DN_TEST2))
                .associations()
                .assertSize(1)
                .association(RI_GROUP)
                .assertShadowOids(group1.getOid())
                .end()
                .end()
                .attributes()
                .referenceAttribute(RI_GROUP)
                .assertShadowOids(invisible.getOid());

        and("test3 has only one association (visible membership)");
        assertShadowNew(getShadow(shadows, DN_TEST3))
                .assertAuxiliaryObjectClasses(RI_POSIX_ACCOUNT)
                .associations()
                .assertSize(1)
                .association(RI_GROUP)
                .assertShadowOids(group1.getOid())
                .end()
                .end()
                .attributes()
                .assertNoAttribute(RI_GROUP);

        and("test4 has posix and LDAP association (visible membership)");
        assertShadowNew(getShadow(shadows, DN_TEST4))
                .assertAuxiliaryObjectClasses(RI_POSIX_ACCOUNT)
                .associations()
                .assertSize(2)
                .association(RI_GROUP)
                .assertShadowOids(group1.getOid())
                .end()
                .association(RI_POSIX_GROUP)
                .assertShadowOids(group2.getOid())
                .end()
                .end()
                .attributes()
                .assertNoAttribute(RI_GROUP);

        if (isLegacySimulation()) {
            and("test5 has 'car' associations (visible and invisible)");

            var invisible3 = findShadowByPrismName(DN_INVISIBLE3, resource, result);
            assertThat(invisible3).as("shadow for invisible3 group").isNotNull();
            var g10001 = findShadowByPrismName(DN_10001, resource, result);
            assertThat(g10001).as("shadow for group 10001").isNotNull();
            var g10002 = findShadowByPrismName(DN_10002, resource, result);
            assertThat(g10002).as("shadow for group 10002").isNotNull();

            assertShadowNew(getShadow(shadows, DN_TEST5))
                    .assertAuxiliaryObjectClasses(RI_POSIX_ACCOUNT)
                    .associations()
                    .assertSize(1)
                    .association(RI_CAR)
                    .assertShadowOids(g10001.getOid(), g10002.getOid())
                    .end()
                    .end()
                    .attributes()
                    .referenceAttribute(RI_CAR)
                    .assertShadowOids(invisible3.getOid());
        }
    }

    private AbstractShadow getShadow(Collection<? extends AbstractShadow> shadows, String name) {
        return shadows.stream()
                .filter(shadow -> name.equals(shadow.getName().getOrig()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No shadow with name " + name));
    }

    /** Can all accounts be safely deleted? There's the question of deleting them from the groups. */
    @Test
    public void test900DeleteAccounts() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("deleting all the accounts");
        var shadows = getAllTestShadows(task, result);
        for (var shadow : shadows) {
            provisioningService.deleteObject(ShadowType.class, shadow.getOid(), null, null, task, result);
        }

        then("no members are left in the groups");
        assertSuccess(result);

        if (areNativeReferences()) {
            // OpenDJ has no referential integrity, so this group is left with members
        } else {
            openDJController.assertUniqueMembers(DN_GROUP1);
        }

        openDJController.assertMemberUids(DN_GROUP2);

        // These groups are left with members, as they are outside the object-side delineation.
        // This is probably not correct; but the behavior is the same as in 4.8, so let's keep it for now.
//        openDJController.assertUniqueMembers(DN_INVISIBLE);
//        openDJController.assertMemberUids(DN_INVISIBLE2);
    }

    private @NotNull SearchResultList<? extends AbstractShadow> getAllTestShadows(Task task, OperationResult result)
            throws CommonException {
        return provisioningService.searchShadows(
                Resource.of(resource)
                        .queryFor(RI_INET_ORG_PERSON)
                        .and().item(ShadowType.F_ATTRIBUTES, QNAME_UID).startsWith("test")
                        .build(),
                null, task, result);
    }
}
