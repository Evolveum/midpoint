/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.test.DummyDefaultScenario;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same as {@link TestDummy} but with the dummy resource providing the associations natively.
 *
 * @see TestDummyRichAssociations
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyNativeAssociations extends TestDummy {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-native-associations.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected void extraDummyResourceInit() throws Exception {
        super.extraDummyResourceInit();
        DummyDefaultScenario.on(dummyResourceCtl)
                .initialize();
    }

    @Override
    protected void assertBareSchemaSanity(BareResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
        // schema is extended (account has +2 associations), displayOrders are changed
        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(
                resourceSchema, resourceType, false, 21);
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
        return group.getLinkedObjects(DummyDefaultScenario.Group.LinkNames.MEMBER_REF.local()).stream()
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

    @Override
    protected boolean areReferencesSupportedNatively() {
        return true;
    }
}
