/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.notorious;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
 * notorious role
 * |
 * +------+------+-----+-----+-....
 * |      |      |     |     |
 * v      v      v     v     v
 * Rb1    Rb2    Rb3   Rb4   Rb5
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
public class TestNotoriousRole extends AbstractNotoriousTest {

    public static final File ROLE_NOTORIOUS_FILE = new File(TEST_DIR, "role-notorious.xml");
    public static final String ROLE_NOTORIOUS_OID = "1e95a1b8-46d1-11e7-84c5-e36e43bb0f00";

    @Override
    protected String getNotoriousOid() {
        return ROLE_NOTORIOUS_OID;
    }

    @Override
    protected File getNotoriousFile() {
        return ROLE_NOTORIOUS_FILE;
    }

    @Override
    protected QName getNotoriousType() {
        return RoleType.COMPLEX_TYPE;
    }

    @Override
    protected QName getAltRelation() {
        return SchemaConstants.ORG_OWNER;
    }

    @Override
    protected int getNumberOfExtraRoles() {
        return 1;
    }

    @Override
    protected int getNumberOfExtraOrgs() {
        return 0;
    }

    @Override
    protected void addNotoriousRole(OperationResult result) throws Exception {
        PrismObject<RoleType> role = parseObject(getNotoriousFile());
        RoleType roleType = role.asObjectable();
        fillNotorious(roleType);
        logger.info("Adding {}:\n{}", role, role.debugDump(1));
        repositoryService.addObject(role, null, result);
    }

    // Owner relation is non-evaluated
    @Override
    protected int getTest15xRoleEvaluationIncrement() {
        return 1 + NUMBER_OF_LEVEL_B_ROLES;
    }

    // Owner relation is non-evaluated, therefore the B-level roles are not in roleMembershipRef here
    @Override
    protected void assertTest158RoleMembershipRef(PrismObject<UserType> userAfter) {
        assertRoleMembershipRef(userAfter, getAltRelation(), getNotoriousOid());
    }

    @Override
    protected int hackify2(int i) {
        return i*3;
    }

    @Override
    protected int hackify1(int i) {
        // TODO: ... or once ... why?!
        return i;
    }
}
