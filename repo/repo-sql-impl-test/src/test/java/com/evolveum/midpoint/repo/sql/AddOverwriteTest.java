/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AddOverwriteTest extends BaseSQLRepoTest {

    private static final String ORG_STRUCT_OBJECTS = "src/test/resources/orgstruct/org-monkey-island.xml";
    private static final String IMPORT_OVERWRITE = "src/test/resources/basic/import-overwrite.xml";
    private static final File RESOURCE_OPENDJ_FILE = new File("src/test/resources/basic/resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final String ORG_OID = "00000000-8888-6666-0000-100000000001";

    @Test
    public void addWithOverwrite() throws Exception {
        List<PrismObject<?>> objects = prismContext.parserFor(new File(ORG_STRUCT_OBJECTS)).parseObjects();


        OperationResult opResult = new OperationResult("Import file");
        for (PrismObject o : objects) {
            repositoryService.addObject(o, null, opResult);
        }
        opResult.recordSuccess();
        opResult.recomputeStatus();

        AssertJUnit.assertTrue(opResult.isSuccess());

        //get carla, check oid and version
        PrismObject carla = getCarla(opResult);
        final String oid = carla.getOid();
        PrismAsserts.assertPropertyValue(carla, ObjectType.F_NAME, PrismTestUtil.createPolyString("carla"));
        PrismAsserts.assertPropertyValue(carla, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Carla"));
        AssertJUnit.assertEquals("0", carla.getVersion());
        AssertJUnit.assertNotNull(oid);

        //reimport carla, oid should stay the same, version must be incremented
        objects = prismContext.parserFor(new File(IMPORT_OVERWRITE)).parseObjects();
        PrismObject newCarla = objects.get(0);
        newCarla.setOid(oid);

        String newOid = repositoryService.addObject(newCarla, RepoAddOptions.createOverwrite(), opResult);
        AssertJUnit.assertEquals(oid, newOid);


        carla = getCarla(opResult);
        PrismAsserts.assertPropertyValue(carla, ObjectType.F_NAME, PrismTestUtil.createPolyString("carla"));
        PrismAsserts.assertPropertyValue(carla, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Carla imported"));
        AssertJUnit.assertEquals("1", carla.getVersion());
        AssertJUnit.assertEquals(oid, carla.getOid());
    }

    private PrismObject getCarla(OperationResult opResult) throws Exception {
        final String CARLA_NAME = "carla";
        PrismObjectDefinition userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eq(CARLA_NAME)
                .build();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);
        AssertJUnit.assertEquals(1, users.size());
        return users.get(0);
    }


    @Test(expectedExceptions = ObjectNotFoundException.class)
    public void test090GetVersionNonExisting() throws Exception {
        OperationResult result = new OperationResult("get version");
        try {
            repositoryService.getVersion(SystemConfigurationType.class, "989", result);
            AssertJUnit.fail();
        } finally {
            result.recomputeStatus();
            AssertJUnit.assertTrue(!result.isUnknown());
        }
    }

    @Test
    public void test091GetVersion() throws Exception {
        OperationResult result = new OperationResult("get version");

        String version = repositoryService.getVersion(OrgType.class, ORG_OID, result);
        AssertJUnit.assertEquals("0", version);

        PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class);
        Collection deltas = new ArrayList();
        deltas.add(prismContext.deltaFactory().property().createAddDelta(def, OrgType.F_ORG_TYPE, "asdf"));
        repositoryService.modifyObject(OrgType.class, ORG_OID, deltas, result);

        version = repositoryService.getVersion(OrgType.class, ORG_OID, result);
        AssertJUnit.assertEquals("1", version);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void addWithOverwriteResource() throws Exception {
        // GIVEN

        SchemaRegistry reg= prismContext.getSchemaRegistry();
        PrismPropertyDefinition def = reg.findPropertyDefinitionByElementName(CapabilitiesType.F_NATIVE);

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_FILE);
        OperationResult opResult = new OperationResult("Import resource");

        repositoryService.addObject(resource, null, opResult);

        opResult.computeStatus();
        AssertJUnit.assertTrue(opResult.isSuccess());

        PrismObject<ResourceType> resourceAfterAdd = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
        MidPointAsserts.assertOid(resourceAfterAdd, RESOURCE_OPENDJ_OID);
        MidPointAsserts.assertVersion(resourceAfterAdd, 0);

        // Precondition
        assertNotNull("no schema", resourceAfterAdd.asObjectable().getSchema());
        assertNotNull("no capabilities", resourceAfterAdd.asObjectable().getCapabilities());

        resource.asObjectable().setSchema(null);
        resource.asObjectable().setCapabilities(null);

        // WHEN
        repositoryService.addObject(resource, RepoAddOptions.createOverwrite(), opResult);

        // THEN
        opResult.computeStatus();
        AssertJUnit.assertTrue(opResult.isSuccess());

        PrismObject<ResourceType> resourceAfterOverwrite = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
        MidPointAsserts.assertOid(resourceAfterOverwrite, RESOURCE_OPENDJ_OID);
        MidPointAsserts.assertVersion(resourceAfterOverwrite, 1);

        assertNull("schema not gone", resourceAfterOverwrite.asObjectable().getSchema());
        assertNull("capabilities not gone", resourceAfterOverwrite.asObjectable().getCapabilities());
    }
}
