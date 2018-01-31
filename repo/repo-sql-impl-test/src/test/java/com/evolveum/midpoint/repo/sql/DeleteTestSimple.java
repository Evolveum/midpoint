/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DeleteTestSimple extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteTestSimple.class);

    @Test
    public void delete001() throws Exception {
        PrismObject<UserType> user = prismContext.parseObject(new File(FOLDER_BASIC, "user0.xml"));

        OperationResult result = new OperationResult("Delete Test");
        String oid = repositoryService.addObject(user, null, result);
        LOGGER.info("*** deleteObject ***");

//        Session session = open();
//        CriteriaQuery<RAssignment> aQ = session.getCriteriaBuilder().createQuery(RAssignment.class);
//        aQ.select(aQ.from(RAssignment.class));
//        List<RAssignment> aList = session.createQuery(aQ).getResultList();
//        System.out.println("RAssignment: " + aList);
//
//        CriteriaQuery<RAssignmentExtension> aeQ = session.getCriteriaBuilder().createQuery(RAssignmentExtension.class);
//        aeQ.select(aeQ.from(RAssignmentExtension.class));
//        List<RAssignmentExtension> aeList = session.createQuery(aeQ).getResultList();
//        System.out.println("RAssignmentExtension: " + aeList);
//
//        CriteriaQuery<RAExtBoolean> aebQ = session.getCriteriaBuilder().createQuery(RAExtBoolean.class);
//        aebQ.select(aebQ.from(RAExtBoolean.class));
//        List<RAExtBoolean> aebList = session.createQuery(aebQ).getResultList();
//        System.out.println("RAExtBoolean: " + aebList);
//
//        session.getTransaction().commit();

        repositoryService.deleteObject(UserType.class, oid, result);
    }
}
