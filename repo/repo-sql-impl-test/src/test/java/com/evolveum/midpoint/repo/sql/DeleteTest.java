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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DeleteTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(DeleteTest.class);

    @Test
    public void delete001() throws Exception {
        final File file = new File("./../../samples/dsee/odsee-localhost-advanced-sync.xml");
        if (!file.exists()) {
            LOGGER.warn("skipping addGetDSEESyncDoubleTest, file {} not found.",
                    new Object[]{file.getPath()});
            return;
        }

        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(file).parseObjects();
        List<String> oids = new ArrayList<>();

        OperationResult result = new OperationResult("Delete Test");
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Adding object {}, type {}", new Object[]{i, object.getCompileTimeClass().getSimpleName()});
            oids.add(repositoryService.addObject(object, null, result));
        }

        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Deleting object {}, type {}", new Object[]{i, object.getCompileTimeClass().getSimpleName()});

            repositoryService.deleteObject(object.getCompileTimeClass(), oids.get(i), result);
        }
    }

    @Test
    public void delete0002() throws Exception {
        PrismObject<SystemConfigurationType> configuration = prismContext.parseObject(new File(FOLDER_BASIC, "systemConfiguration.xml"));

        OperationResult result = new OperationResult("add system configuration");
        final String oid = repositoryService.addObject(configuration, null, result);
        repositoryService.deleteObject(SystemConfigurationType.class, oid, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void delete0003() throws Exception {
        PrismObject<ShadowType> shadow = prismContext.parseObject(new File(FOLDER_BASE, "delete/shadow.xml"));

        OperationResult result = new OperationResult("add shadow");
        final String oid = repositoryService.addObject(shadow, null, result);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, oid, null, result);
        shadow = prismContext.parseObject(new File(FOLDER_BASE, "delete/shadow.xml"));
        AssertJUnit.assertEquals(shadow, repoShadow);

        repositoryService.deleteObject(ShadowType.class, oid, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());

        Session session = getFactory().openSession();
        try {
            Query query = session.createNativeQuery("select count(*) from m_trigger where owner_oid = ?");
            query.setParameter(1, oid);

            Number count = (Number) query.uniqueResult();
            AssertJUnit.assertEquals(count.longValue(), 0L);
        } finally {
            session.close();
        }
    }

    @Test
    public void test100DeleteObjects() throws Exception {
//        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        List<PrismObject<? extends Objectable>> objects = prismContext.parserFor(new File(FOLDER_BASIC, "objects.xml")).parseObjects();
        OperationResult result = new OperationResult("add objects");

        List<String> oids = new ArrayList<>();
        for (PrismObject object : objects) {
            oids.add(repositoryService.addObject(object, null, result));
        }

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        for (int i=0; i< objects.size(); i++ ){
            repositoryService.deleteObject((Class) objects.get(i).getCompileTimeClass(), oids.get(i), result);
        }

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }
}
