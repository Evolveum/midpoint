/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserPhotoTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/photo");
    private static final File USER_FILE = new File(TEST_DIR, "user.xml");
    private static final File T001_ADD_EMPLOYEE_TYPE = new File(TEST_DIR, "t001-add-employeeType.xml");
    private static final File T002_REMOVE_PHOTO = new File(TEST_DIR, "t002-remove-photo.xml");
    private static final File T003_RE_ADD_PHOTO = new File(TEST_DIR, "t003-re-add-photo.xml");
    private static final File T004_CHANGE_PHOTO = new File(TEST_DIR, "t004-change-photo.xml");

    private String userOid;

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test010Add() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test010Add");

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_FILE);
        userOid = repositoryService.addObject(user, null, result);

        checkObject(userOid, USER_FILE, result);
        checkObjectNoPhoto(userOid, USER_FILE, result);
    }

    @Test
    public void test020ModifyUser() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test020ModifyUser");

        ObjectDelta delta = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta);
    }

    @Test
    public void test030RemovePhoto() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test030RemovePhoto");

        ObjectDelta delta1 = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);
        ObjectDelta delta2 = parseDelta(userOid, T002_REMOVE_PHOTO);

        repositoryService.modifyObject(UserType.class, userOid, delta2.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta1, delta2);
    }

    @Test
    public void test040ReAddPhoto() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test040ReAddPhoto");

        ObjectDelta delta1 = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);
        ObjectDelta delta2 = parseDelta(userOid, T002_REMOVE_PHOTO);
        ObjectDelta delta3 = parseDelta(userOid, T003_RE_ADD_PHOTO);

        repositoryService.modifyObject(UserType.class, userOid, delta3.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta1, delta2, delta3);
    }

    @Test
    public void test050ChangePhoto() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test050ReplacePhoto");

        ObjectDelta delta1 = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);
        ObjectDelta delta2 = parseDelta(userOid, T002_REMOVE_PHOTO);
        ObjectDelta delta3 = parseDelta(userOid, T003_RE_ADD_PHOTO);
        ObjectDelta delta4 = parseDelta(userOid, T004_CHANGE_PHOTO);

        repositoryService.modifyObject(UserType.class, userOid, delta4.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta1, delta2, delta3, delta4);
    }

    /**
     * Checks that after removing a user the photo is removed as well.
     */
    @Test
    public void test099DeleteUser() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test099DeleteUser");

        repositoryService.deleteObject(UserType.class, userOid, result);

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_FILE);
        user.asObjectable().setJpegPhoto(null);
        String oid = repositoryService.addObject(user, null, result);
        assertEquals("Oid was changed", userOid, oid);

        checkObject(userOid, user, true, result);       // there should be no photo there
    }

    protected ObjectDelta<UserType> parseDelta(String oid, File file) throws SchemaException, IOException {
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(file, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);
        delta.setOid(oid);
        return delta;
    }

    private void checkObject(String oid, File file, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<UserType> expected = PrismTestUtil.parseObject(file);
        checkObject(oid, expected, true, result);
    }

    private void checkObjectNoPhoto(String oid, File file, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<UserType> expected = PrismTestUtil.parseObject(file);
        expected.asObjectable().setJpegPhoto(null);
        checkObject(oid, expected, false, result);
    }

    private void checkObject(String oid, PrismObject<UserType> expected, boolean loadPhoto, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> options;
        if (loadPhoto) {
            options = Arrays.asList(SelectorOptions.create(UserType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        } else {
            options = null;
        }
        PrismObject<UserType> real = repositoryService.getObject(UserType.class, oid, options, result);
        ObjectDelta<UserType> delta = expected.diff(real);
        System.out.println("Expected object = \n" + expected.debugDump());
        System.out.println("Real object in repo = \n" + real.debugDump());
        System.out.println("Difference = \n" + delta.debugDump());
        if (!delta.isEmpty()) {
            fail("Objects are not equal.\n*** Expected:\n" + expected.debugDump() + "\n*** Got:\n" + real.debugDump() + "\n*** Delta:\n" + delta.debugDump());
        }
    }

    private void checkObject(String oid, File file, OperationResult result, ObjectDelta<UserType>... appliedDeltas) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<UserType> expected = PrismTestUtil.parseObject(file);
        for (ObjectDelta<UserType> delta : appliedDeltas) {
            delta.applyTo(expected);
        }
        checkObject(oid, expected, true, result);
    }
}
