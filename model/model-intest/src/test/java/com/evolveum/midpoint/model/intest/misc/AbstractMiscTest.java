/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Test with a resource that has unique primary identifier (ConnId UID), but non-unique secondary
 * identifier (ConnId NAME).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractMiscTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/misc");

    protected static final File USER_SKELLINGTON_FILE = new File(TEST_DIR, "user-skellington.xml");
    protected static final String USER_SKELLINGTON_OID = "637fbaf0-2476-11e9-a181-f73466184d45";
    protected static final String USER_SKELLINGTON_NAME = "skellington";
    protected static final String USER_SKELLINGTON_GIVEN_NAME = "Jack";
    protected static final String USER_SKELLINGTON_FAMILY_NAME = "Skellington";
    protected static final String USER_SKELLINGTON_FULL_NAME = "Jack Skellington";
}
