/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

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
    protected void assertShadowPassword(ShadowType provisioningShadow) throws Exception {
        CredentialsType credentials = provisioningShadow.getCredentials();
        if (credentials == null) {
            return;
        }
        PasswordType passwordType = credentials.getPassword();
        if (passwordType == null) {
            return;
        }
        ProtectedStringType passwordValue = passwordType.getValue();
        assertNotNull("Missing password value in "+provisioningShadow, passwordValue);
        assertFalse("Empty password value in "+provisioningShadow, passwordValue.isEmpty());
        String clearPassword = protector.decryptString(passwordValue);
        display("Clear password of "+provisioningShadow+": "+clearPassword);

        //noinspection unchecked
        PrismContainerValue<PasswordType> passwordContainer = passwordType.asPrismContainerValue();
        PrismProperty<ProtectedStringType> valueProp = passwordContainer.findProperty(PasswordType.F_VALUE);
        assertFalse("Incomplete password value in "+provisioningShadow, valueProp.isIncomplete());
    }

    @Override
    protected boolean isActivationCapabilityClassSpecific() {
        return false;
    }
}
