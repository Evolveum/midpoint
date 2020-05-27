/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRbacNoInbounds extends TestRbac {

    @Override
    protected ModelExecuteOptions getDefaultOptions() {
        return executeOptions().partialProcessing(
                new PartialProcessingOptionsType().inbound(PartialProcessingTypeType.SKIP));
    }
}
