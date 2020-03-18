/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class UserPhotoTest extends AbstractPhotoTest<UserType> {

    private static final File TEST_DIR = new File("src/test/resources/photo");
    private static final File USER_FILE = new File(TEST_DIR, "user.xml");
    private static final File T001_ADD_EMPLOYEE_TYPE = new File(TEST_DIR, "t001-add-employeeType.xml");
    private static final File T002_REMOVE_PHOTO = new File(TEST_DIR, "t002-remove-photo.xml");
    private static final File T003_RE_ADD_PHOTO = new File(TEST_DIR, "t003-re-add-photo.xml");
    private static final File T004_CHANGE_PHOTO = new File(TEST_DIR, "t004-change-photo.xml");
    private static final File T005_ADD_PHOTO_BY_ADD = new File(TEST_DIR, "t005-add-photo-by-add.xml");
    private static final File T006_ADD_PHOTO_BY_ADD_OTHER = new File(TEST_DIR, "t006-add-photo-by-add-other.xml");
    private static final File T007_REMOVE_NON_EXISTING_PHOTO_BY_DELETE = new File(TEST_DIR, "t007-remove-non-existing-photo-by-delete.xml");
    private static final File T008_REMOVE_PHOTO_BY_DELETE = new File(TEST_DIR, "t008-remove-photo-by-delete.xml");

    private String userOid;

    @Override
    protected Class<UserType> getObjectType() {
        return UserType.class;
    }

    @Test
    public void test010AddUser() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test010AddUser");

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_FILE);
        userOid = repositoryService.addObject(user, null, result);

        checkObject(userOid, USER_FILE, result);
        checkObjectNoPhoto(userOid, USER_FILE, result);
    }

    @Test
    public void test020ModifyUser() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test020ModifyUser");

        ObjectDelta<UserType> delta = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta);
    }

    @Test
    public void test030RemovePhotoByReplace() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test030RemovePhotoByReplace");

        ObjectDelta<UserType> delta1 = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);
        ObjectDelta<UserType> delta2 = parseDelta(userOid, T002_REMOVE_PHOTO);

        repositoryService.modifyObject(UserType.class, userOid, delta2.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta1, delta2);
    }

    @Test
    public void test040ReAddPhoto() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test040ReAddPhoto");

        ObjectDelta<UserType> delta1 = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);
        ObjectDelta<UserType> delta2 = parseDelta(userOid, T002_REMOVE_PHOTO);
        ObjectDelta<UserType> delta3 = parseDelta(userOid, T003_RE_ADD_PHOTO);

        repositoryService.modifyObject(UserType.class, userOid, delta3.getModifications(), result);
        checkObject(userOid, USER_FILE, result, delta1, delta2, delta3);
    }

    @Test
    public void test050ChangePhoto() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test050ReplacePhoto");

        ObjectDelta<UserType> delta1 = parseDelta(userOid, T001_ADD_EMPLOYEE_TYPE);
        ObjectDelta<UserType> delta2 = parseDelta(userOid, T002_REMOVE_PHOTO);
        ObjectDelta<UserType> delta3 = parseDelta(userOid, T003_RE_ADD_PHOTO);
        ObjectDelta<UserType> delta4 = parseDelta(userOid, T004_CHANGE_PHOTO);

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

    @Test
    public void test100AddPhotoByAdd() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test100AddPhotoByAdd");

        ObjectDelta<UserType> delta = parseDelta(userOid, T005_ADD_PHOTO_BY_ADD);
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        checkObject(userOid, USER_FILE, result);        // no need to mention delta here, because object now should be equal to USER_FILE
        checkObjectNoPhoto(userOid, USER_FILE, result);
    }

    @Test
    public void test110DuplicatePhotoAddSame() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test110DuplicatePhotoAddSame");

        ObjectDelta<UserType> delta = parseDelta(userOid, T005_ADD_PHOTO_BY_ADD);     // adding the same value again
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        checkObject(userOid, USER_FILE, result);        // no need to mention delta here, because object now should be equal to USER_FILE
        checkObjectNoPhoto(userOid, USER_FILE, result);
    }

    @Test(expectedExceptions = SchemaException.class)       // photo already exists
    public void test120DuplicatePhotoAddOther() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test120DuplicatePhotoAddOther");

        ObjectDelta<UserType> delta = parseDelta(userOid, T006_ADD_PHOTO_BY_ADD_OTHER);
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
    }

    @Test
    public void test130RemoveNonExistingPhotoByDelete() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test130RemoveNonExistingPhotoByDelete");

        ObjectDelta<UserType> delta2 = parseDelta(userOid, T007_REMOVE_NON_EXISTING_PHOTO_BY_DELETE);
        repositoryService.modifyObject(UserType.class, userOid, delta2.getModifications(), result);     // should not remove the photo because the value is different

        checkObject(userOid, USER_FILE, result);
        checkObject(userOid, USER_FILE, result, delta2);        // should be equivalent
    }

    @Test
    public void test140RemoveExistingPhotoByDelete() throws Exception {
        OperationResult result = new OperationResult(UserPhotoTest.class.getName() + ".test140RemoveExistingPhotoByDelete");

        ObjectDelta<UserType> delta2 = parseDelta(userOid, T007_REMOVE_NON_EXISTING_PHOTO_BY_DELETE);
        ObjectDelta<UserType> delta3 = parseDelta(userOid, T008_REMOVE_PHOTO_BY_DELETE);
        repositoryService.modifyObject(UserType.class, userOid, delta3.getModifications(), result);     // this one should remove the photo

        checkObject(userOid, USER_FILE, result, delta2, delta3);

        // just to be 100% sure ;)
        ObjectDelta<UserType> deltaRemoveByReplace = parseDelta(userOid, T002_REMOVE_PHOTO);  // this deletes photo by setting jpegPhoto:=null
        checkObject(userOid, USER_FILE, result, deltaRemoveByReplace);
    }
}
