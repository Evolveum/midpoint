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
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
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
public class OrgPhotoTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/photo");
    private static final File ORG_FILE = new File(TEST_DIR, "org.xml");
    private static final File T101_ADD_ORG_TYPE = new File(TEST_DIR, "t101-add-org-type.xml");
    private static final File T102_REMOVE_PHOTO = new File(TEST_DIR, "t102-remove-photo.xml");
    private static final File T103_RE_ADD_PHOTO = new File(TEST_DIR, "t103-re-add-photo.xml");
    private static final File T104_CHANGE_PHOTO = new File(TEST_DIR, "t104-change-photo.xml");
    private static final File T105_ADD_PHOTO_BY_ADD = new File(TEST_DIR, "t105-add-photo-by-add.xml");
    private static final File T106_ADD_PHOTO_BY_ADD_OTHER = new File(TEST_DIR, "t106-add-photo-by-add-other.xml");
    private static final File T107_REMOVE_PHOTO_BY_DELETE = new File(TEST_DIR, "t107-remove-photo-by-delete.xml");
    private static final File T108_REMOVE_OTHER_PHOTO_BY_DELETE = new File(TEST_DIR, "t108-remove-other-photo-by-delete.xml");

    private String orgOid;

    @Override
    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test010AddOrg() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test010AddOrg");

        PrismObject<OrgType> org = PrismTestUtil.parseObject(ORG_FILE);
        orgOid = repositoryService.addObject(org, null, result);

        checkObject(orgOid, ORG_FILE, result);
        checkObjectNoPhoto(orgOid, ORG_FILE, result);
    }

    @Test
    public void test020ModifyOrg() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test020ModifyOrg");

        ObjectDelta delta = parseDelta(orgOid, T101_ADD_ORG_TYPE);

        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta);
    }

    @Test
    public void test030RemovePhotoByReplace() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test030RemovePhotoByReplace");

        ObjectDelta delta1 = parseDelta(orgOid, T101_ADD_ORG_TYPE);
        ObjectDelta delta2 = parseDelta(orgOid, T102_REMOVE_PHOTO);

        repositoryService.modifyObject(OrgType.class, orgOid, delta2.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta1, delta2);
    }

    @Test
    public void test040ReAddPhoto() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test040ReAddPhoto");

        ObjectDelta delta1 = parseDelta(orgOid, T101_ADD_ORG_TYPE);
        ObjectDelta delta2 = parseDelta(orgOid, T102_REMOVE_PHOTO);
        ObjectDelta delta3 = parseDelta(orgOid, T103_RE_ADD_PHOTO);

        repositoryService.modifyObject(OrgType.class, orgOid, delta3.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta1, delta2, delta3);
    }

    @Test
    public void test050ChangePhoto() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test050ReplacePhoto");

        ObjectDelta delta1 = parseDelta(orgOid, T101_ADD_ORG_TYPE);
        ObjectDelta delta2 = parseDelta(orgOid, T102_REMOVE_PHOTO);
        ObjectDelta delta3 = parseDelta(orgOid, T103_RE_ADD_PHOTO);
        ObjectDelta delta4 = parseDelta(orgOid, T104_CHANGE_PHOTO);

        repositoryService.modifyObject(OrgType.class, orgOid, delta4.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta1, delta2, delta3, delta4);
    }

    /**
     * Checks that after removing an org the photo is removed as well.
     */
    @Test
    public void test099DeleteOrg() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test099DeleteOrg");

        repositoryService.deleteObject(OrgType.class, orgOid, result);

        PrismObject<OrgType> org = PrismTestUtil.parseObject(ORG_FILE);
        org.asObjectable().setJpegPhoto(null);
        String oid = repositoryService.addObject(org, null, result);
        assertEquals("Oid was changed", orgOid, oid);

        checkObject(orgOid, org, true, result);       // there should be no photo there
    }

    @Test
    public void test100AddPhotoByAdd() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test100AddPhotoByAdd");

        ObjectDelta delta = parseDelta(orgOid, T105_ADD_PHOTO_BY_ADD);
        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);

        checkObject(orgOid, ORG_FILE, result);        // no need to mention delta here, because object now should be equal to ORG_FILE
        checkObjectNoPhoto(orgOid, ORG_FILE, result);
    }

    @Test
    public void test110DuplicatePhotoAddSame() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test110DuplicatePhotoAddSame");

        ObjectDelta delta = parseDelta(orgOid, T105_ADD_PHOTO_BY_ADD);     // adding the same value again
        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);

        checkObject(orgOid, ORG_FILE, result);        // no need to mention delta here, because object now should be equal to ORG_FILE
        checkObjectNoPhoto(orgOid, ORG_FILE, result);
    }

    @Test
    public void test120DuplicatePhotoAddOther() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test120DuplicatePhotoAddOther");

        // because photo is single-value, the ADD operation will simply replace the old value
        ObjectDelta delta = parseDelta(orgOid, T106_ADD_PHOTO_BY_ADD_OTHER);
        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);

        checkObject(orgOid, ORG_FILE, result, delta);
    }

    @Test
    public void test130RemoveNonExistingPhotoByDelete() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test130RemoveNonExistingPhotoByDelete");

        ObjectDelta delta1 = parseDelta(orgOid, T106_ADD_PHOTO_BY_ADD_OTHER);
        ObjectDelta delta2 = parseDelta(orgOid, T107_REMOVE_PHOTO_BY_DELETE);
        repositoryService.modifyObject(OrgType.class, orgOid, delta2.getModifications(), result);     // should not remove the photo because the value is different

        checkObject(orgOid, ORG_FILE, result, delta1);
        checkObject(orgOid, ORG_FILE, result, delta1, delta2);        // should be equivalent
    }

    @Test
    public void test140RemoveExistingPhotoByDelete() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test140RemoveExistingPhotoByDelete");

        ObjectDelta delta1 = parseDelta(orgOid, T106_ADD_PHOTO_BY_ADD_OTHER);
        ObjectDelta delta2 = parseDelta(orgOid, T107_REMOVE_PHOTO_BY_DELETE);
        ObjectDelta delta3 = parseDelta(orgOid, T108_REMOVE_OTHER_PHOTO_BY_DELETE);
        repositoryService.modifyObject(OrgType.class, orgOid, delta3.getModifications(), result);     // this one should remove the photo

        checkObject(orgOid, ORG_FILE, result, delta1, delta2, delta3);

        // just to be 100% sure ;)
        ObjectDelta deltaRemoveByReplace = parseDelta(orgOid, T102_REMOVE_PHOTO);  // this deletes photo by setting jpegPhoto:=null
        checkObject(orgOid, ORG_FILE, result, deltaRemoveByReplace);
    }

    protected ObjectDelta<OrgType> parseDelta(String oid, File file) throws SchemaException, IOException {
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(file, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);
        delta.setOid(oid);
        return delta;
    }

    private void checkObject(String oid, File file, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<OrgType> expected = PrismTestUtil.parseObject(file);
        checkObject(oid, expected, true, result);
    }

    private void checkObjectNoPhoto(String oid, File file, OperationResult result) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<OrgType> expected = PrismTestUtil.parseObject(file);
        expected.asObjectable().setJpegPhoto(null);
        checkObject(oid, expected, false, result);
    }

    private void checkObject(String oid, PrismObject<OrgType> expected, boolean loadPhoto, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> options;
        if (loadPhoto) {
            options = Arrays.asList(SelectorOptions.create(FocusType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        } else {
            options = null;
        }
        PrismObject<OrgType> real = repositoryService.getObject(OrgType.class, oid, options, result);
        ObjectDelta<OrgType> delta = expected.diff(real);
        System.out.println("Expected object = \n" + expected.debugDump());
        System.out.println("Real object in repo = \n" + real.debugDump());
        System.out.println("Difference = \n" + delta.debugDump());
        if (!delta.isEmpty()) {
            fail("Objects are not equal.\n*** Expected:\n" + expected.debugDump() + "\n*** Got:\n" + real.debugDump() + "\n*** Delta:\n" + delta.debugDump());
        }
    }

    private void checkObject(String oid, File file, OperationResult result, ObjectDelta<OrgType>... appliedDeltas) throws SchemaException, IOException, ObjectNotFoundException {
        PrismObject<OrgType> expected = PrismTestUtil.parseObject(file);
        for (ObjectDelta<OrgType> delta : appliedDeltas) {
            delta.applyTo(expected);
        }
        checkObject(oid, expected, true, result);
    }
}
