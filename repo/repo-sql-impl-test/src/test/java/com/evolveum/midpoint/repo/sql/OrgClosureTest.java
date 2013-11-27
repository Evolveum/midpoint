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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
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

    @Test(enabled = false)
    public void testSelectChildren() throws Exception {
        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

        List<PrismObject<? extends Objectable>> orgStruct = prismContext.getPrismDomProcessor().parseObjects(
                new File(ORG_STRUCT_OBJECTS));

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }

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

//            Query q = session.createQuery("select oc.descendant from ROrgClosure as oc " +
//                    "inner join oc.descendant where oc.ancestorOid = :oid and oc.ancestorId = :id " +
//                    "and oc.depth > :minDepth and oc.depth <= :maxDepth");

            Query q = session.createQuery("from RObject o where (o.id = :id and " +
                    "o.oid in (select oc.descendantOid from ROrgClosure oc where oc.ancestorOid = :oid and oc.ancestorId = :id and oc.depth > :minDepth and oc.depth <= :maxDepth))");

            q.setParameter("oid", parentOid);
            q.setParameter("id", 0L);
            q.setParameter("minDepth", 0);
            q.setParameter("maxDepth", 1);

            List l = q.list();
            for (Object o : l) {
                System.out.println(((RObject)o).getName());
            }

//            repositoryService.searchObjects(OrgType.class, query, null, opResult);
//            String sql = getInterpretedQuery(session, OrgType.class, query);
//            LOGGER.info(sql);
        } finally {
            close(session);
        }
    }
}
