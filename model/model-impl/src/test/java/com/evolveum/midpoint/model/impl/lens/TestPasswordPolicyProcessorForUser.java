/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
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
    TestObject<?> getTestResource() {
        return USER_JACK;
    }
}
