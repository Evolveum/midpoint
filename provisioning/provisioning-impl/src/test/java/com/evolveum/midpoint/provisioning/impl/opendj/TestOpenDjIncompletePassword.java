/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;

/**
 * Test for provisioning service implementation using embedded OpenDj instance.
 * This is the same test as TestOpenDj, but the configuration allows incomplete
 * password reading. This means that the actual password value is not revealed,
 * but it is indicated that there is a password set.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjIncompletePassword extends TestOpenDj {

    private static final File RESOURCE_OPENDJ_INCOMPLETE_PASSWORD_FILE = new File(TEST_DIR, "resource-opendj-incomplete-password.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_INCOMPLETE_PASSWORD_FILE;
    }

    @Override
    protected void assertPasswordCapability(PasswordCapabilityType capPassword) {
        assertThat(capPassword.isReadable())
                .as("password capability readable flag")
                .isTrue();
    }

    @Override
    protected void assertProvisioningShadowPassword(ShadowType provisioningShadow, String expectedClearText) {
        assertIncompleteShadowPassword(provisioningShadow.asPrismObject());
    }

    @Override
    protected void assertRepoShadowPasswordFetched(ShadowType repoShadow, String expectedClearText) {
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            assertIncompleteShadowPassword(repoShadow.asPrismObject());
        } else {
            assertNoShadowPassword(repoShadow.asPrismObject());
        }
    }

    @Override
    protected void assertRepoShadowPasswordWrittenAndFetched(ShadowType repoShadow, String expectedClearText)
            throws SchemaException, EncryptionException {
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            // It is encrypted, because it was written first (with known value), and fetched second (with incomplete=true)
            assertEncryptedShadowPassword(repoShadow.asPrismObject(), expectedClearText);
        } else {
            assertNoShadowPassword(repoShadow.asPrismObject());
        }
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
