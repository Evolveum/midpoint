/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl.assignments;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Tests assigning of roles 1..3 with explicitly assigned metaroles (with policy rules).
 */
@SuppressWarnings("Duplicates")
public class TestAssignmentApprovalMetaroleExplicit extends AbstractTestAssignmentApproval {

    @Override
    protected TestObject<RoleType> getRole(int number) {
        switch (number) {
            case 1: return ROLE1B;
            case 2: return ROLE2B;
            case 3: return ROLE3B;
            case 4: return ROLE4B;
            case 10: return ROLE10B;
            default: throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }
}
