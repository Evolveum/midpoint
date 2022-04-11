/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrg;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgClosure;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class OrgHierarchyPerfTest extends SqaleRepoBaseTest {

    public static final SecureRandom RANDOM = new SecureRandom();

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        refreshOrgClosureForce();
        assertThat(count(QOrg.CLASS)).isZero();
        assertThat(count(new QOrgClosure())).isZero();

        createOrgsFor(null, 5, 6, result);

        assertThatOperationResult(result).isSuccess();
    }

    private void createOrgsFor(
            OrgType parent, int levels, int typicalCountPerLevel, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        if (levels == 0) {
            // typical count is used as max for user generation
            createUsersFor(parent, typicalCountPerLevel, result);
            return;
        }

        // for roots we want the exact number, so it does not vary wildly
        int orgs = parent != null
                ? RANDOM.nextInt(typicalCountPerLevel) + typicalCountPerLevel / 2 + 1
                : typicalCountPerLevel;
        for (int i = 1; i <= orgs; i++) {
            // names use only chars that are preserved by normalization to avoid collision
            String name = parent != null ? parent.getName() + "x" + i : "org" + i;
            OrgType org = new OrgType().name(name);
            if (parent != null) {
                org.parentOrgRef(parent.getOid(), OrgType.COMPLEX_TYPE);
            }

            repositoryService.addObject(org.asPrismObject(), null, result);
            createOrgsFor(org, levels - 1, typicalCountPerLevel, result);
        }
    }

    private void createUsersFor(OrgType parent, int maxCountPerLevel, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException {
        int users = RANDOM.nextInt(maxCountPerLevel) + 1;
        for (int i = 1; i <= users; i++) {
            repositoryService.addObject(
                    new UserType().name("user" + parent.getName() + "v" + i)
                            .metadata(new MetadataType()
                                    .createChannel("create-channel")
                                    .createTimestamp(MiscUtil.asXMLGregorianCalendar(System.currentTimeMillis())))
                            .parentOrgRef(parent.getOid(), OrgType.COMPLEX_TYPE)
                            .asPrismObject(),
                    null, result);
        }
    }

    @Test
    public void test100Xxx() throws Exception {
        given("there are orgs and users, closure is not yet updated");
        OperationResult operationResult = createOperationResult();
        display("Orgs: " + count(QOrg.CLASS));
        display("Users: " + count(QUser.class));
        assertThat(count(new QOrgClosure())).isZero();
        OrgType org1x1x1 = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .item(ObjectType.F_NAME).eq(PolyString.fromOrig("org1x1x1"))
                        .build(),
                operationResult).get(0);

        when("search for user under some org is initiated");
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .isChildOf(org1x1x1.getOid())
                        .build(),
                operationResult);

        then("non-empty result is returned and org closure has been initialized");
        assertThat(result).isNotEmpty();
        assertThat(count(new QOrgClosure())).isPositive();
        display("Orgs: " + count(QOrg.CLASS));
        display("Org closure: " + count(new QOrgClosure()));
        display("Users: " + count(QUser.class));
    }
}
