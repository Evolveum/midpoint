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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
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
 * notorious org
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
public class TestNotoriousOrg extends AbstractNotoriousTest {

    public static final File ORG_NOTORIOUS_FILE = new File(TEST_DIR, "org-notorious.xml");
    public static final String ORG_NOTORIOUS_OID = "f79fc21a-4d0a-11e7-ad8d-f7fe1a23c68a";

    private static final Trace LOGGER = TraceManager.getTrace(TestNotoriousOrg.class);

    @Override
    protected String getNotoriousOid() {
        return ORG_NOTORIOUS_OID;
    }

    @Override
    protected File getNotoriousFile() {
        return ORG_NOTORIOUS_FILE;
    }

    @Override
    protected QName getNotoriousType() {
        return OrgType.COMPLEX_TYPE;
    }

    @Override
    protected QName getAltRelation() {
        return SchemaConstants.ORG_MANAGER;
    }

    @Override
    protected int getNumberOfExtraRoles() {
        return 0;
    }

    @Override
    protected int getNumberOfExtraOrgs() {
        return 1;
    }

    @Override
    protected void addNotoriousRole(OperationResult result) throws Exception {
        PrismObject<OrgType> org = parseObject(getNotoriousFile());
        OrgType orgType = org.asObjectable();
        fillNotorious(orgType);
        LOGGER.info("Adding {}:\n{}", org, org.debugDump(1));
        repositoryService.addObject(org, null, result);
    }

    @Override
    protected void assertNotoriousParentOrgRefRelations(PrismObject<UserType> userAfter, QName... relations) {
        for (QName relation : relations) {
            assertHasOrg(userAfter, getNotoriousOid(), relation);
        }
    }
}
