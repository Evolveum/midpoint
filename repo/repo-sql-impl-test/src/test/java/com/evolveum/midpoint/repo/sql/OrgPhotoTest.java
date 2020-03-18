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
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class OrgPhotoTest extends AbstractPhotoTest<OrgType> {

    private static final File ORG_FILE = new File(TEST_DIR, "org.xml");
    private static final File T101_ADD_ORG_TYPE = new File(TEST_DIR, "t101-add-org-type.xml");
    private static final File T102_REMOVE_PHOTO = new File(TEST_DIR, "t102-remove-photo.xml");
    private static final File T103_RE_ADD_PHOTO = new File(TEST_DIR, "t103-re-add-photo.xml");
    private static final File T104_CHANGE_PHOTO = new File(TEST_DIR, "t104-change-photo.xml");
    private static final File T105_ADD_PHOTO_BY_ADD = new File(TEST_DIR, "t105-add-photo-by-add.xml");
    private static final File T106_ADD_PHOTO_BY_ADD_OTHER = new File(TEST_DIR, "t106-add-photo-by-add-other.xml");
    private static final File T107_REMOVE_NON_EXISTING_PHOTO_BY_DELETE = new File(TEST_DIR, "t107-remove-non-existing-photo-by-delete.xml");
    private static final File T108_REMOVE_PHOTO_BY_DELETE = new File(TEST_DIR, "t108-remove-photo-by-delete.xml");

    private String orgOid;

    @Override
    protected Class<OrgType> getObjectType() {
        return OrgType.class;
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

        ObjectDelta<OrgType> delta = parseDelta(orgOid, T101_ADD_ORG_TYPE);

        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta);
    }

    @Test
    public void test030RemovePhotoByReplace() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test030RemovePhotoByReplace");

        ObjectDelta<OrgType> delta1 = parseDelta(orgOid, T101_ADD_ORG_TYPE);
        ObjectDelta<OrgType> delta2 = parseDelta(orgOid, T102_REMOVE_PHOTO);

        repositoryService.modifyObject(OrgType.class, orgOid, delta2.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta1, delta2);
    }

    @Test
    public void test040ReAddPhoto() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test040ReAddPhoto");

        ObjectDelta<OrgType> delta1 = parseDelta(orgOid, T101_ADD_ORG_TYPE);
        ObjectDelta<OrgType> delta2 = parseDelta(orgOid, T102_REMOVE_PHOTO);
        ObjectDelta<OrgType> delta3 = parseDelta(orgOid, T103_RE_ADD_PHOTO);

        repositoryService.modifyObject(OrgType.class, orgOid, delta3.getModifications(), result);
        checkObject(orgOid, ORG_FILE, result, delta1, delta2, delta3);
    }

    @Test
    public void test050ChangePhoto() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test050ReplacePhoto");

        ObjectDelta<OrgType> delta1 = parseDelta(orgOid, T101_ADD_ORG_TYPE);
        ObjectDelta<OrgType> delta2 = parseDelta(orgOid, T102_REMOVE_PHOTO);
        ObjectDelta<OrgType> delta3 = parseDelta(orgOid, T103_RE_ADD_PHOTO);
        ObjectDelta<OrgType> delta4 = parseDelta(orgOid, T104_CHANGE_PHOTO);

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

        ObjectDelta<OrgType> delta = parseDelta(orgOid, T105_ADD_PHOTO_BY_ADD);
        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);

        checkObject(orgOid, ORG_FILE, result);        // no need to mention delta here, because object now should be equal to ORG_FILE
        checkObjectNoPhoto(orgOid, ORG_FILE, result);
    }

    @Test
    public void test110DuplicatePhotoAddSame() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test110DuplicatePhotoAddSame");

        ObjectDelta<OrgType> delta = parseDelta(orgOid, T105_ADD_PHOTO_BY_ADD);     // adding the same value again
        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);

        checkObject(orgOid, ORG_FILE, result);        // no need to mention delta here, because object now should be equal to ORG_FILE
        checkObjectNoPhoto(orgOid, ORG_FILE, result);
    }

    @Test(expectedExceptions = SchemaException.class)       // there is already a photo
    public void test120DuplicatePhotoAddOther() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test120DuplicatePhotoAddOther");

        ObjectDelta<OrgType> delta = parseDelta(orgOid, T106_ADD_PHOTO_BY_ADD_OTHER);
        repositoryService.modifyObject(OrgType.class, orgOid, delta.getModifications(), result);
    }

    @Test
    public void test130RemoveNonExistingPhotoByDelete() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test130RemoveNonExistingPhotoByDelete");

        ObjectDelta<OrgType> delta2 = parseDelta(orgOid, T107_REMOVE_NON_EXISTING_PHOTO_BY_DELETE);
        repositoryService.modifyObject(OrgType.class, orgOid, delta2.getModifications(), result);     // should not remove the photo because the value is different

        checkObject(orgOid, ORG_FILE, result);
        checkObject(orgOid, ORG_FILE, result, delta2);        // should be equivalent
    }

    @Test
    public void test140RemoveExistingPhotoByDelete() throws Exception {
        OperationResult result = new OperationResult(OrgPhotoTest.class.getName() + ".test140RemoveExistingPhotoByDelete");

        ObjectDelta<OrgType> delta2 = parseDelta(orgOid, T107_REMOVE_NON_EXISTING_PHOTO_BY_DELETE);
        ObjectDelta<OrgType> delta3 = parseDelta(orgOid, T108_REMOVE_PHOTO_BY_DELETE);
        repositoryService.modifyObject(OrgType.class, orgOid, delta3.getModifications(), result);     // this one should remove the photo

        checkObject(orgOid, ORG_FILE, result, delta2, delta3);

        // just to be 100% sure ;)
        ObjectDelta<OrgType> deltaRemoveByReplace = parseDelta(orgOid, T102_REMOVE_PHOTO);  // this deletes photo by setting jpegPhoto:=null
        checkObject(orgOid, ORG_FILE, result, deltaRemoveByReplace);
    }

}
