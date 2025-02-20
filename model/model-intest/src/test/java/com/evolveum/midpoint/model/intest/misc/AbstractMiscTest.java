/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractMiscTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/misc");

    static final File USER_SKELLINGTON_FILE = new File(TEST_DIR, "user-skellington.xml");
    static final String USER_SKELLINGTON_OID = "637fbaf0-2476-11e9-a181-f73466184d45";
    static final String USER_SKELLINGTON_NAME = "skellington";
    static final String USER_SKELLINGTON_GIVEN_NAME = "Jack";
    static final String USER_SKELLINGTON_FULL_NAME = "Jack Skellington";
}
