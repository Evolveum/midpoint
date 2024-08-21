/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;
import static com.evolveum.midpoint.test.DummyDefaultScenario.Group.LinkNames.MEMBER_REF;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.test.DummyDefaultScenario;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The same as {@link TestDummy} but with the dummy resource providing the associations natively.
 *
 * @see TestDummyComplexAssociations
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyNativeAssociations extends TestDummy {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-native-associations.xml");

    private DummyDefaultScenario dummyScenario;

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected void extraDummyResourceInit() throws Exception {
        super.extraDummyResourceInit();
        dummyScenario = DummyDefaultScenario.on(dummyResourceCtl)
                .initialize();
    }

    @Override
    protected void assertBareSchemaSanity(BareResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
        // schema is extended (account has +2 associations), displayOrders are changed
        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(
                resourceSchema, resourceType, false, 21);
    }

    @Override
    protected void assertCompleteSchema(CompleteResourceSchema completeSchema) throws CommonException {
        ItemName groupName = DummyDefaultScenario.Account.LinkNames.GROUP.q();
        var objectRefDef = completeSchema.getObjectTypeDefinitionRequired(ACCOUNT_DEFAULT)
                .findAssociationDefinitionRequired(groupName)
                .findReferenceDefinition(ShadowAssociationValueType.F_OBJECTS.append(groupName));
        displayDumpable("objectRefDef", objectRefDef);
        assertThat(objectRefDef.getMinOccurs())
                .as("minOccurs for " + groupName + " in the association")
                .isEqualTo(1);
        assertThat(objectRefDef.getMaxOccurs())
                .as("maxOccurs for " + groupName + " in the association")
                .isEqualTo(1);
    }

    /**
     * Native associations currently do not update `member` group attribute. So, we have to check differently.
     *
     * This should be resolved somehow better in the future.
     */
    @Override
    protected void assertMember(DummyGroup group, String accountId) {
        assertThat(getGroupMembersNames(group))
                .as("members of " + group)
                .contains(accountId);
    }

    @Override
    protected void assertNoMember(DummyGroup group, String accountId) {
        assertThat(getGroupMembersNames(group))
                .as("members of " + group)
                .doesNotContain(accountId);
    }

    private static @NotNull Set<String> getGroupMembersNames(DummyGroup group) {
        return group.getLinkedObjects(MEMBER_REF.local()).stream()
                .map(member -> member.getName())
                .collect(Collectors.toSet());
    }

    /**
     * Just like for members, the privileges are no longer provided in the `privileges` attribute.
     *
     * Moreover, the {@link #PRIVILEGE_NONSENSE_NAME} is not to be found there, as it does not exist as an object on the resource.
     */
    @Override
    protected void assertPrivileges(DummyAccount dummyAccount, String... expected) {
        var withoutNonsense = Arrays.stream(expected)
                .filter(privilege -> !privilege.equals(PRIVILEGE_NONSENSE_NAME))
                .toList();
        assertThat(getAccountPrivilegesNames(dummyAccount))
                .as("privileges of " + dummyAccount)
                .containsExactlyInAnyOrderElementsOf(withoutNonsense);
    }

    private static @NotNull Set<String> getAccountPrivilegesNames(DummyAccount dummyAccount) {
        return dummyAccount.getLinkedObjects(DummyDefaultScenario.Account.LinkNames.PRIV.local()).stream()
                .map(member -> member.getName())
                .collect(Collectors.toSet());
    }

    /** Tests the treatment of reference values without object class information (currently disabled). */
    @Test
    public void test980UnclassifiedReferenceValues() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("there is a new pirate");
        var account = dummyScenario.account.add(accountName);
        var group = dummyScenario.group.getByNameRequired(GROUP_PIRATES_NAME);
        dummyScenario.groupMembership.add(account, group);

        displayDumpable("pirates on resource", group);

        when("pirates are retrieved (memberRef not explicitly requested)");
        var groupShadow = provisioningService.getShadow(GROUP_PIRATES_OID, null, task, result);

        then("everything is OK, no memberRef values are present");
        assertSuccess(result);
        displayDumpable("pirates", groupShadow);
        assertThat(groupShadow.getReferenceAttributeValues(MEMBER_REF.q()))
                .isEmpty();

        when("pirates are retrieved (memberRef explicitly requested)");
        try {
            provisioningService.getShadow(
                    GROUP_PIRATES_OID,
                    GetOperationOptionsBuilder.create()
                            .item(ShadowType.F_ATTRIBUTES.append(MEMBER_REF.q()))
                            .retrieve()
                            .build(),
                    task, result);
        } catch (SchemaException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Reference attribute values without object class information are currently not supported");
        }
    }

    @Override
    protected boolean areReferencesSupportedNatively() {
        return true;
    }
}
