/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.assignments;

/**
 * Tests assigning of roles 1..3 with explicitly assigned metaroles (with policy rules).
 */
@SuppressWarnings("Duplicates")
public class TestAssignmentApprovalMetaroleExplicit extends AbstractTestAssignmentApproval {

    @Override
    protected String getRoleOid(int number) {
        switch (number) {
            case 1: return ROLE1B.oid;
            case 2: return ROLE2B.oid;
            case 3: return ROLE3B.oid;
            case 4: return ROLE4B.oid;
            case 10: return ROLE10B.oid;
            default: throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }

    @Override
    protected String getRoleName(int number) {
        switch (number) {
            case 1: return "Role1b";
            case 2: return "Role2b";
            case 3: return "Role3b";
            case 4: return "Role4b";
            case 10: return "Role10b";
            default: throw new IllegalArgumentException("Wrong role number: " + number);
        }
    }
}
