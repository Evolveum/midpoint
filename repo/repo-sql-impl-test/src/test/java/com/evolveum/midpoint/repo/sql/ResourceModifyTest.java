/*
 * Copyright (c) 2010-2015 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.testing.CarefulAnt;
import com.evolveum.midpoint.repo.sql.testing.ResourceCarefulAntUtil;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ResourceModifyTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceModifyTest.class);
    private static final File TEST_DIR = new File("src/test/resources/modify");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
	private static final int MAX_SEQUENCE_ITERATIONS = 15;
	private static final int MAX_RANDOM_SEQUENCE_ITERATIONS = 30;

	private static List<CarefulAnt<ResourceType>> ants = new ArrayList<>();
	private static CarefulAnt<ResourceType> descriptionAnt;
    private static String lastVersion;
    private static Random rnd = new Random();

    @BeforeClass
    public void initAnts() {
    	ResourceCarefulAntUtil.initAnts(ants, RESOURCE_OPENDJ_FILE, prismContext);
    	descriptionAnt = ants.get(0);
    }

    @Test
    public void test010AddResource() throws Exception {
    	final String TEST_NAME = "test010AddResource";
    	LOGGER.info("===[ {} ]===", TEST_NAME);

    	// GIVEN
        OperationResult result = new OperationResult(ResourceModifyTest.class.getName()+"."+TEST_NAME);
        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_FILE);

        // WHEN
        String addOid = repositoryService.addObject(resource, null, result);

        // THEN
        assertEquals("Wrong OID after add", RESOURCE_OPENDJ_OID, addOid);

        PrismObject<ResourceType> resourceAfter = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
        SqlRepoTestUtil.assertVersionProgress(null, resourceAfter.getVersion());
        lastVersion = resourceAfter.getVersion();

        // TODO: look inside the resource?
    }

    @Test
    public void test020SingleDescriptionModify() throws Exception {
    	final String TEST_NAME = "test020SingleDescriptionModify";
    	LOGGER.info("===[ {} ]===", TEST_NAME);

    	OperationResult result = new OperationResult(ResourceModifyTest.class.getName()+"."+TEST_NAME);
    	singleModify(descriptionAnt, -1, result);
    }

    @Test
    public void test030DescriptionModifySequence() throws Exception {
    	final String TEST_NAME = "test030DescriptionModifySequence";
    	LOGGER.info("===[ {} ]===", TEST_NAME);

    	OperationResult result = new OperationResult(ResourceModifyTest.class.getName()+"."+TEST_NAME);

    	for(int i=0; i <= MAX_SEQUENCE_ITERATIONS; i++) {
    		singleModify(descriptionAnt, i, result);
    	}
    }

    @Test
    public void test040RadomModifySequence() throws Exception {
    	final String TEST_NAME = "test040RadomModifySequence";
    	LOGGER.info("===[ {} ]===", TEST_NAME);

    	OperationResult result = new OperationResult(ResourceModifyTest.class.getName()+"."+TEST_NAME);

    	for(int i=0; i <= MAX_RANDOM_SEQUENCE_ITERATIONS; i++) {
    		singleRandomModify(i, result);
    	}
    }

    private void singleRandomModify(int iteration, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
    	int i = rnd.nextInt(ants.size());
    	CarefulAnt<ResourceType> ant = ants.get(i);
    	singleModify(ant, iteration, result);
    }

    private void singleModify(CarefulAnt<ResourceType> ant, int iteration, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

    	// GIVEN
    	ItemDelta<?,?> itemDelta = ant.createDelta(iteration);
		Collection<? extends ItemDelta<?,?>> modifications = MiscSchemaUtil.createCollection(itemDelta);

		System.out.println("itemDelta: " + itemDelta.debugDump());

		// WHEN
		repositoryService.modifyObject(ResourceType.class, RESOURCE_OPENDJ_OID, modifications, result);

		// THEN
		PrismObject<ResourceType> resourceAfter = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
		SqlRepoTestUtil.assertVersionProgress(lastVersion, resourceAfter.getVersion());
        lastVersion = resourceAfter.getVersion();
        System.out.println("Version: "+lastVersion);

        ant.assertModification(resourceAfter, iteration);
    }

}
