/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.repo.sql.util.RUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DeleteTest extends BaseSQLRepoTest {

    @Test
    public void delete001() throws Exception {
        final File file = new File("./../../samples/dsee/odsee-localhost-advanced-sync.xml");
        if (!file.exists()) {
            logger.warn("skipping addGetDSEESyncDoubleTest, file {} not found.", file.getPath());
            return;
        }

        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(file).parseObjects();
        List<String> oids = new ArrayList<>();

        OperationResult result = new OperationResult("Delete Test");
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            logger.info("Adding object {}, type {}", i, object.getCompileTimeClass().getSimpleName());
            oids.add(repositoryService.addObject(object, null, result));
        }

        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            logger.info("Deleting object {}, type {}", i, object.getCompileTimeClass().getSimpleName());

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

        try (EntityManager em = getFactory().createEntityManager()) {
            Query query = em.createNativeQuery("select count(*) from m_trigger where owner_oid = ?");
            query.setParameter(1, oid);

            Number count = RUtil.getSingleResultOrNull(query);
            AssertJUnit.assertEquals(count.longValue(), 0L);
        }
    }

    @Test
    public void test100DeleteObjects() throws Exception {
        List<PrismObject<? extends Objectable>> objects = prismContext.parserFor(new File(FOLDER_BASIC, "objects.xml")).parseObjects();
        OperationResult result = new OperationResult("add objects");

        List<String> oids = new ArrayList<>();
        for (PrismObject object : objects) {
            oids.add(repositoryService.addObject(object, null, result));
        }

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        for (int i = 0; i < objects.size(); i++) {
            repositoryService.deleteObject((Class) objects.get(i).getCompileTimeClass(), oids.get(i), result);
        }

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }
}
