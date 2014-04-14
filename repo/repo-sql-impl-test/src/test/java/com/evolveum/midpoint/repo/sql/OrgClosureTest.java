/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

    private int count = 0;

    @Test(enabled = false)
    public void loadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

        //109100 users and 5455 org. units
//        final int[] TREE_SIZE = {1, 5, 5, 7, 3, 3, 2};
//        loadOrgStructure(null, TREE_SIZE, 20, "", opResult);

        //4120 users and 206 org. units
        final int[] TREE_SIZE = {1, 5, 5, 7};
        loadOrgStructure(null, TREE_SIZE, 20, "", opResult);

        //total 44 objects
//        final int[] TREE_SIZE = {1, 2, 2, 2};
//        loadOrgStructure(null, TREE_SIZE, 2, "", opResult);
    }

    private void loadOrgStructure(String parentOid, int[] TREE_SIZE, int USER_COUNT, String oidPrefix,
                                  OperationResult result) throws Exception {
        if (TREE_SIZE.length == 0) {
            return;
        }

        for (int i = 0; i < TREE_SIZE[0]; i++) {
            String newOidPrefix = (TREE_SIZE[0] - i) + "a" + oidPrefix;
            PrismObject<OrgType> org = createOrg(parentOid, i, newOidPrefix);
            LOGGER.info("Creating {}, total {}", org, count);
            String oid = repositoryService.addObject(org, null, result);
            count++;

            for (int u = 0; u < USER_COUNT; u++) {
                PrismObject<UserType> user = createUser(oid, i, u, newOidPrefix);
                LOGGER.info("Creating {}, total {}", user, count);
                repositoryService.addObject(user, null, result);
                count++;
            }

            loadOrgStructure(oid, ArrayUtils.remove(TREE_SIZE, 0), USER_COUNT, newOidPrefix + i, result);
        }
    }

    private PrismObject<UserType> createUser(String parentOid, int i, int u, String oidPrefix)
            throws Exception {
        UserType user = new UserType();
        user.setOid("1" + createOid(u, oidPrefix + i));
        user.setName(createPolyString("u" + oidPrefix + i + u));
        user.setFullName(createPolyString("fu" + oidPrefix + i + u));
        user.setFamilyName(createPolyString("fa" + oidPrefix + i + u));
        user.setGivenName(createPolyString("gi" + oidPrefix + i + u));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            user.getParentOrgRef().add(ref);
        }

        prismContext.adopt(user);
        return user.asPrismContainer();
    }

    private PrismObject<OrgType> createOrg(String parentOid, int i, String oidPrefix)
            throws Exception {
        OrgType org = new OrgType();
        org.setOid("2" + createOid(i, oidPrefix));
        org.setDisplayName(createPolyString("o" + oidPrefix + i));
        org.setName(createPolyString("o" + oidPrefix + i));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            org.getParentOrgRef().add(ref);
        }

        prismContext.adopt(org);
        return org.asPrismContainer();
    }

    private String createOid(int i, String oidPrefix) {
        String oid = StringUtils.rightPad(oidPrefix + Integer.toString(i), 31, 'a');

        StringBuilder sb = new StringBuilder();
        sb.append(oid.substring(0, 7));
        sb.append('-');
        sb.append(oid.substring(7, 11));
        sb.append('-');
        sb.append(oid.substring(11, 15));
        sb.append('-');
        sb.append(oid.substring(15, 19));
        sb.append('-');
        sb.append(oid.substring(19, 31));

        return sb.toString();
    }

    private PolyStringType createPolyString(String orig) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);
        return poly;
    }

    @Test(enabled = false)
    public void testSelectChildren() throws Exception {
        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

        LOGGER.info("Starting import.");
        List<PrismObject<? extends Objectable>> orgStruct = prismContext.parseObjects(
                new File(ORG_STRUCT_OBJECTS));

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }

        orgStruct = prismContext.parseObjects(new File(ORG_SIMPLE_TEST));
        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }
        LOGGER.info("Import finished.");

        String parentOid = "00000000-8888-6666-0000-100000000001";

        OrgFilter orgFilter = OrgFilter.createOrg(parentOid, null, 1);
        ObjectQuery query = ObjectQuery.createObjectQuery(orgFilter);
        query.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

//        SELECT closure1_.descendant_id  AS y0_,
//                closure1_.descendant_oid AS y1_,
//        this_.name_orig          AS y2_
//        FROM   m_org this_
//        inner join m_org_closure closure1_
//        ON this_.id = closure1_.descendant_id
//        AND this_.oid = closure1_.descendant_oid
//        inner join m_object anc2_
//        ON closure1_.ancestor_id = anc2_.id
//        AND closure1_.ancestor_oid = anc2_.oid
//        WHERE  ( anc2_.id=0 and anc2_.oid = '00000000-8888-6666-0000-100000000006'
//                AND closure1_.depthvalue <= 1
//                AND closure1_.depthvalue > 0 )
//        GROUP  BY closure1_.descendant_id,
//                closure1_.descendant_oid,
//                this_.name_orig
//        ORDER  BY this_.name_orig ASC;

//        from Item i
//        where (
//                i.id in (select i2.id from AbcItem i2 where i2.abc = 5)
//                or i.id in (select id2.id from XyzItem i2 where i2.xyz = 7)
//        )
//        and ...
//        order by ...

        Session session = open();
        try {
//            Query q = session.createQuery("from ROrg as o " +
//                    "inner join ROrgClosure as oc on o.id=oc.descendandId and o.oid=oc.descendandOid");

            LOGGER.info("QUERY:");
            Query q = session.createQuery("select oc.descendant from ROrgClosure as oc " +
                    "inner join oc.descendant where oc.ancestorOid = :oid and oc.ancestorId = :id " +
                    "and oc.depth > :minDepth and oc.depth <= :maxDepth");
            q.setParameter("oid", parentOid);
            q.setParameter("id", 0L);
            q.setParameter("minDepth", 0);
            q.setParameter("maxDepth", 1);
            List l = q.list();
            for (Object o : l) {
                System.out.println(((RObject) o).getName());
            }
            LOGGER.info("QUERY:");

            q = session.createQuery("from ROrg o where (o.id = :id and o.oid in (" +
                    "select oc.descendantOid from ROrgClosure oc " +
                    "where oc.ancestorOid = :oid and oc.ancestorId = :id " +
                    "and oc.depth > :minDepth and oc.depth <= :maxDepth))");

            q.setParameter("oid", parentOid);
            q.setParameter("id", 0L);
            q.setParameter("minDepth", 0);
            q.setParameter("maxDepth", 1);

            l = q.list();
            for (Object o : l) {
                System.out.println(((RObject) o).getName());
            }

            LOGGER.info("QUERY:");
            repositoryService.searchObjects(OrgType.class, query, null, opResult);

//            String sql = getInterpretedQuery(session, OrgType.class, query);
//            LOGGER.info(sql);
        } finally {
            close(session);
        }
    }
}
