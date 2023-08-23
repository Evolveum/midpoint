/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 *  Testing password policy processor for the printer service.
 */
public class TestPasswordPolicyProcessorForService extends TestPasswordPolicyProcessor<ServiceType> {

    private static final TestObject<ServiceType> SERVICE_PRINTER = TestObject.file(
            TEST_DIR, "service-printer.xml", "296f03b4-f642-4017-94d9-19ff83c32dcf");

    @Override
    Class<ServiceType> getType() {
        return ServiceType.class;
    }

    @Override
    TestObject<ServiceType> getTestResource() {
        return SERVICE_PRINTER;
    }
}
