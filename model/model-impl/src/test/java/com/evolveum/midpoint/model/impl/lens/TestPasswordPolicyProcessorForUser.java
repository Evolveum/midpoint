/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Testing password policy processor for user Jack.
 */
public class TestPasswordPolicyProcessorForUser extends TestPasswordPolicyProcessor<UserType> {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        deleteObject(UserType.class, USER_JACK_OID);
    }

    @Override
    Class<UserType> getType() {
        return UserType.class;
    }

    @Override
    TestResource<?> getTestResource() {
        return USER_JACK;
    }
}
