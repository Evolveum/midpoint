/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.misc;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.testng.AssertJUnit.assertEquals;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ShadowAttributeIdSyncStoreReadTest extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");

    private static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR,"resource-dummy-id.xml");
    private static final String RESOURCE_ID = "40000000-0000-0000-0000-000000000004";
    private static final String SHADOW_ID =   "b5a8b51d-d834-4803-a7d0-c81bcc58113e";

    private static final String RI = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";

    private DummyResourceContoller dummy;

    private ItemPath RI_ID = ItemPath.create(new QName(RI, "id"));

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummy = initDummyResource("id", RESOURCE_DUMMY_FILE, RESOURCE_ID, controller -> {
            controller.extendSchemaPirate();
            DummyObjectClass accountClazz = controller.getDummyResource().getAccountObjectClass();
            accountClazz.addAttributeDefinition("familyName");
            accountClazz.addAttributeDefinition("id");
        }, initTask, initResult);

        DummyAccount testAccount = dummy.addAccount("idTest");
        //testAccount.setId(SHADOW_ID);
        //testAccount.addAttributeValue("oid", SHADOW_ID);
        testAccount.setEnabled(true);
        testAccount.addAttributeValue("id", SHADOW_ID);
        testAccount.addAttributeValue("familyName", "Snow");


        List<PrismObject<ShadowType>> shadows = repositoryService
                .searchObjects(ShadowType.class, null, null, initResult);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), initResult);
        }


        //repoAddObjectFromFile(SHADOW_3_FILE, initResult);
    }

    @Test
    public void test100ImportIdItemFromResourceReadFromRepository() throws Exception {
        login(userAdministrator);
        OperationResult result = new OperationResult(ShadowAttributeIdSyncStoreReadTest.class.getName() + ".test100");
        //modelService.importFromResource(RESOURCE_DUMMY_FOR_CHECKER_OID, new QName(RI, "account1"),initTask, initResult);

        Task importTask = createTask();
        importTask.setOwner(userAdministrator);
        modelService.importFromResource(RESOURCE_ID, RI_ACCOUNT_OBJECT_CLASS, importTask, result.createSubresult("import"));
        waitForTaskFinish(importTask);

        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, null, null, result);

        PrismObject<ShadowType> object = shadows.get(0);
        @NotNull
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder().raw().build();

        PrismObject<ShadowType> raw = repositoryService.getObject(ShadowType.class, object.getOid(), options, result);
        PrismObject<ShadowType> provisioning = provisioningService.getObject(ShadowType.class, object.getOid(), null, null, result);


        PrismContainerValue<?> dbAttributes = object.getAnyValue().getValue().getAttributes().asPrismContainerValue();
        PrismContainerValue<?> provisioningAttributes = provisioning.getAnyValue().getValue().getAttributes().asPrismContainerValue();

        SerializationOptions opt2 = SerializationOptions.createSerializeForExport();
        String serializedXml = prismContext.xmlSerializer().options(opt2).serialize(object);
        String serializedRaw = prismContext.xmlSerializer().options(opt2).serialize(raw);
        String serializedJson = prismContext.jsonSerializer().serialize(object);
        // WHEN

        when();

        repositoryService.deleteObject(ShadowType.class, object.getOid(), result);
        PrismObject<ShadowType> deserializedDb = prismContext.parseObject(serializedRaw);
        repositoryService.addObject(deserializedDb, null, result);

        // THEN
        then();
        verifyHasId(provisioning.getAnyValue());
        verifyHasId(object.getAnyValue());

    }

    private void verifyHasId(PrismContainerValue<ShadowType> anyValue) {
        PrismProperty<?> id = (PrismProperty<?>) anyValue.asContainerable().getAttributes().asPrismContainerValue().findItem(RI_ID, PrismProperty.class );
        Object loadedId = id.getValue().getValue();
        assertEquals(SHADOW_ID, loadedId instanceof RawType ? ((RawType)loadedId).extractString() : loadedId);
    }
}
