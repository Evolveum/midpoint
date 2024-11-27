/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for provisioning service implementation using embedded OpenDj instance.
 * This is the same test as TestOpenDj, but the configuration allows password reading.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjReadablePassword extends TestOpenDj {

    private static final File RESOURCE_OPENDJ_READABLE_PASSWORD_FILE = new File(TEST_DIR, "resource-opendj-readable-password.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_READABLE_PASSWORD_FILE;
    }

    @Override
    protected void assertPasswordCapability(PasswordCapabilityType capPassword) {
        assertThat(capPassword.isReadable())
                .as("password capability readable flag")
                .isTrue();
    }

    @Override
    protected void assertProvisioningShadowPassword(ShadowType provisioningShadow, String expectedClearText) throws Exception {
        // The cleartext is different, like "{SSHA}ehkK+5px6g..."
        assertEncryptedShadowPassword(provisioningShadow.asPrismObject(), null);
    }

    @Override
    protected void assertRepoShadowPasswordFetched(ShadowType repoShadow, String expectedClearText) throws Exception {
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            // The cleartext is different, like "{SSHA}ehkK+5px6g..."
            assertEncryptedShadowPassword(repoShadow.asPrismObject(), null);
        } else {
            assertNoShadowPassword(repoShadow.asPrismObject());
        }
    }

    @Override
    protected void assertRepoShadowPasswordWrittenAndFetched(ShadowType repoShadow, String expectedClearText) throws Exception {
        // With readable passwords there is no difference between written+fetched and fetched.
        assertRepoShadowPasswordFetched(repoShadow, expectedClearText);
    }

    @Override
    protected void assertRepoShadowPasswordWrittenAndUnsetAndFetched(ShadowType repoShadow, String lastWritten) {
        // We know that there is no password, so it should be gone from the shadow.
        assertNoShadowPassword(repoShadow.asPrismObject());
    }

    @Override
    protected boolean isActivationCapabilityClassSpecific() {
        return false;
    }
}
