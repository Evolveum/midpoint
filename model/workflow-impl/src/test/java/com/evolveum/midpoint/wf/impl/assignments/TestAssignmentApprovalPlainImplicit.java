/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.assignments;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Tests assigning of roles 1..3 with implicitly defined approvers (i.e. via org:approver assignment).
 * As for policy rules, the default ones are used.
 */
@SuppressWarnings("Duplicates")
public class TestAssignmentApprovalPlainImplicit extends AbstractTestAssignmentApproval {

    @Override
    protected TestObject<RoleType> getRole(int number) {
        switch (number) {
            case 1: return ROLE1;
            case 2: return ROLE2;
            case 3: return ROLE3;
            case 4: return ROLE4;
            case 10: return ROLE10;
            default: throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }
}
