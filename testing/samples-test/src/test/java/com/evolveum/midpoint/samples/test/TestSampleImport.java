/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.samples.test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Try to import selected samples to a real repository in an initialized system.
 *
 * We cannot import all the samples as some of them are mutually exclusive.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-samples-test-main.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class TestSampleImport extends AbstractSampleTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void importOpenDJBasic() throws FileNotFoundException, SchemaException {
        importSample(new File(SAMPLES_DIRECTORY, "resources/opendj/opendj-localhost-basic.xml"), ResourceType.class, "Basic Localhost OpenDJ");
    }

    @Test
    public void importOpenDJAdvanced() throws FileNotFoundException, SchemaException {
        importSample(new File(SAMPLES_DIRECTORY, "resources/opendj/opendj-localhost-resource-sync-advanced.xml"), ResourceType.class, "Localhost OpenDJ");
    }

    // Connector not part of the build, therefore this fails
//    @Test
//    public void importDBTableSimple() throws FileNotFoundException, SchemaException {
//        importSample(new File(SAMPLE_DIRECTORY_NAME + "databasetable/localhost-dbtable-simple.xml"), ResourceType.class, "Localhost DBTable");
//    }

    public <T extends ObjectType> void importSample(File sampleFile, Class<T> type, String objectName) throws FileNotFoundException, SchemaException {
        TestUtil.displayTestTitle(this, "Import sample "+sampleFile.getPath());
        // GIVEN
        sampleFile.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        result.addParam("file", sampleFile.getPath());
        FileInputStream stream = new FileInputStream(sampleFile);

        // WHEN
        modelService.importObjectsFromStream(stream, PrismContext.LANG_XML, MiscSchemaUtil.getDefaultImportOptions(), task, result);

        // THEN
        result.computeStatus();
        display("Result after good import", result);
        TestUtil.assertSuccessOrWarning("Import has failed (result)", result,1);

        ObjectQuery query = ObjectQueryUtil.createNameQuery(objectName, prismContext);

        List<PrismObject<T>> objects = repositoryService.searchObjects(type, query, null, result);
        for (PrismObject<T> o : objects) {
            T object = o.asObjectable();
            display("Found object",object);
        }
        assertEquals("Unexpected search result: "+objects,1,objects.size());

    }

}
