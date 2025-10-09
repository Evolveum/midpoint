/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.manual;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestManualCapabilities extends TestManual {

    @Override
    protected File getResourceFile() {
        return RESOURCE_MANUAL_CAPABILITIES_FILE;
    }

    @Override
    protected boolean nativeCapabilitiesEntered() {
        return true;
    }

    @Override
    protected int getNumberOfAccountAttributeDefinitions() {
        return 5;
    }

}
