/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public abstract class AbstractPhotoTest<T extends FocusType> extends BaseSQLRepoTest {

    static final File TEST_DIR = new File("src/test/resources/photo");

    protected abstract Class<T> getObjectType();

    void checkObject(String oid, File file, OperationResult result) throws SchemaException, IOException,
            ObjectNotFoundException {
        PrismObject<T> expected = PrismTestUtil.parseObject(file);
        checkObject(oid, expected, true, result);
    }

    void checkObjectNoPhoto(String oid, File file, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<T> expected = PrismTestUtil.parseObject(file);
        checkObject(oid, expected, false, result);
    }

    // if !loadPhoto this MODIFIES 'expected' object
    void checkObject(String oid, PrismObject<T> expected, boolean loadPhoto, OperationResult result) throws ObjectNotFoundException, SchemaException {
        boolean shouldPhotoExist = expected.asObjectable().getJpegPhoto() != null;
        Collection<SelectorOptions<GetOperationOptions>> options;
        if (loadPhoto) {
            options = Collections.singletonList(
                    SelectorOptions.create(prismContext.toUniformPath(FocusType.F_JPEG_PHOTO), GetOperationOptions.createRetrieve(
                            RetrieveOption.INCLUDE)));
        } else {
            expected.asObjectable().setJpegPhoto(null);
            options = null;
        }
        PrismObject<T> real = repositoryService.getObject(getObjectType(), oid, options, result);
        ObjectDelta<T> delta = expected.diff(real);
        System.out.println("Expected object = \n" + expected.debugDump());
        System.out.println("Real object in repo = \n" + real.debugDump());
        System.out.println("Difference = \n" + delta.debugDump());
        if (!delta.isEmpty()) {
            fail("Objects are not equal.\n*** Expected:\n" + expected.debugDump() + "\n*** Got:\n" + real.debugDump() + "\n*** Delta:\n" + delta.debugDump());
        }
        if (shouldPhotoExist) {
            PrismProperty<Object> photoProperty = real.findProperty(FocusType.F_JPEG_PHOTO);
            assertNotNull("No photo", photoProperty);
            if (loadPhoto) {
                assertFalse("Photo is marked as incomplete even when loaded", photoProperty.isIncomplete());
                assertEquals("Wrong # of photo values", 1, photoProperty.size());
            } else {
                assertTrue("Photo is marked as complete even when not loaded", photoProperty.isIncomplete());
                assertEquals("Wrong # of photo values", 0, photoProperty.size());
            }
        }
    }

    @SafeVarargs
    final void checkObject(String oid, File file, OperationResult result, ObjectDelta<T>... appliedDeltas) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<T> expected = PrismTestUtil.parseObject(file);
        for (ObjectDelta<T> delta : appliedDeltas) {
            delta.applyTo(expected);
        }
        checkObject(oid, expected, true, result);
    }

    ObjectDelta<T> parseDelta(String oid, File file) throws SchemaException, IOException {
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(file, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<T> delta = DeltaConvertor.createObjectDelta(modification, getObjectType(), prismContext);
        delta.setOid(oid);
        return delta;
    }
}
