/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.notorious;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Testing bushy roles hierarchy. Especially reuse of the same role
 * in the rich role hierarchy. It looks like this:
 * <p>
 * user
 * |
 * +------+------+-----+-----+-....
 * |      |      |     |     |
 * v      v      v     v     v
 * Ra1    Ra2    Ra3   Ra4   Ra5
 * |      |      |     |     |
 * +------+------+-----+-----+
 * |
 * v
 * +--assignment--> supernotorious org
 * |                  |
 * |    +------+------+-----+-----+-....
 * |    |      |      |     |     |
 * |    v      v      v     v     v
 * +-- Rb1    Rb2    Rb3   Rb4   Rb5 ---..
 * <p>
 * Naive mode of evaluation would imply cartesian product of all Rax and Rbx
 * combinations. That's painfully inefficient. Therefore make sure that the
 * notorious roles is evaluated only once and the results of the evaluation
 * are reused.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSupernotoriousOrg extends TestNotoriousOrg {

    public static final File ORG_SUPERNOTORIOUS_FILE = new File(TEST_DIR, "org-supernotorious.xml");
    public static final String ORG_SUPERNOTORIOUS_OID = "16baebbe-5046-11e7-82a0-eb7b7e3400f6";

    @Override
    protected String getNotoriousOid() {
        return ORG_SUPERNOTORIOUS_OID;
    }

    @Override
    protected File getNotoriousFile() {
        return ORG_SUPERNOTORIOUS_FILE;
    }

    @Override
    protected void fillLevelBRole(RoleType roleType, int i) {
        super.fillLevelBRole(roleType, i);
        roleType.beginAssignment()
                .targetRef(getNotoriousOid(), getNotoriousType())
                .end();
    }

    @Test
    public void test010LevelBRolesSanity() throws Exception {
        ObjectQuery query = queryFor(RoleType.class)
                .item(RoleType.F_ROLE_TYPE).eq(ROLE_LEVEL_B_ROLETYPE)
                .build();
        searchObjectsIterative(RoleType.class, query,
                role -> assertRoleMembershipRef(role, getNotoriousOid()), NUMBER_OF_LEVEL_B_ROLES);
    }

    @Override
    protected void assertRoleEvaluationCount(int numberOfNotoriousAssignments, int numberOfOtherAssignments) {
        inspector.assertRoleEvaluations(getNotoriousOid(), hackify(numberOfNotoriousAssignments));
    }
}
