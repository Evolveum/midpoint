/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.prism.delta.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;

import static org.testng.AssertJUnit.assertEquals;

/**
 * This is not real test, it's just used to check how hibernate handles insert/modify of different objects.
 *
 * @author lazyman
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyUser extends BaseSQLRepoTest {

    private static final String USER_FULLNAME = "Guybrush Threepwood";
    private String userOid;
    private String userBigOid;
    private String shadowOid;

    @Test
    public void test010Add() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user.xml"));
        userOid = repositoryService.addObject(user, null, new OperationResult("asdf"));

        user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user-big.xml"));
        userBigOid = repositoryService.addObject(user, null, new OperationResult("asdf"));

        PrismObject<ShadowType> shadow = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "account-shadow.xml"));
        shadowOid = repositoryService.addObject(shadow, null, new OperationResult("asdf"));
    }

    @Test
    public void test020ModifyUser() throws Exception {
        OperationResult result = createOperationResult();

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(FOLDER_BASIC, "t002.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);
        delta.setOid(userOid);

        // WHEN
        when();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, userOid, null, result);
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_FULLNAME));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_EMPLOYEE_NUMBER, "en1234");
    }

    @Test
    public void test021ModifyUserNoEmpNum() throws Exception {
        OperationResult result = createOperationResult();

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(FOLDER_BASIC, "t002a.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);
        delta.setOid(userOid);

        // WHEN
        when();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, userOid, null, result);
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_FULLNAME));
        PrismAsserts.assertNoItem(userAfter, UserType.F_EMPLOYEE_NUMBER);
    }

    @Test
    public void test022ModifyUserEmptyEmpNum() throws Exception {
        OperationResult result = createOperationResult();

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(FOLDER_BASIC, "t002b.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);
        delta.setOid(userOid);

        // WHEN
        when();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, userOid, null, result);
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_FULLNAME));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_EMPLOYEE_NUMBER, "");
    }

    @Test
    public void test030ModifyShadow() throws Exception {
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(FOLDER_BASIC, "t003.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, ShadowType.class, prismContext);
        delta.setOid(userOid);

        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), new OperationResult("asdf"));
    }

    @Test
    public void test040GetShadow() throws Exception {
        repositoryService.getObject(ShadowType.class, shadowOid, null, new OperationResult("asdf"));
    }

    @Test
    public void test050ModifyBigUser() throws Exception {
        PrismObjectDefinition<?> def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PropertyDelta<?> delta = prismContext.deltaFactory().property().createModificationReplaceProperty(ObjectType.F_DESCRIPTION, def,
                "new description");

        repositoryService.modifyObject(UserType.class, userBigOid, List.of(delta), new OperationResult("asdf"));
    }

    @Test
    public void test060GetBigUser() throws Exception {
        repositoryService.getObject(UserType.class, userBigOid, null, new OperationResult("asdf"));
    }

    @Test
    public void test070ModifyBigUser() throws Exception {
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(FOLDER_BASIC, "t004.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, userBigOid, delta.getModifications(), new OperationResult("asdf"));
    }

    @Test
    public void test100ModifyUserApproverMetadata() throws Exception {
        PrismObjectDefinition userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        ReferenceDelta delta1 = prismContext.deltaFactory().reference().createModificationAdd(
                ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_APPROVER_REF),
                userDefinition,
                itemFactory().createReferenceValue("target-oid-1", UserType.COMPLEX_TYPE));
        ReferenceDelta delta2 = prismContext.deltaFactory().reference().createModificationAdd(
                ItemPath.create(UserType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF),
                userDefinition,
                itemFactory().createReferenceValue("target-oid-1", UserType.COMPLEX_TYPE));            // the same as in delta1

        repositoryService.modifyObject(UserType.class, userOid, Arrays.asList(delta1, delta2), new OperationResult("asdf"));
    }

    @Test
    public void test120ModifyUserAddAuthenticationBehavior() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user-auth-behavior.xml"));
        String userOid = repositoryService.addObject(user, null, new OperationResult("addUser"));

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(FOLDER_BASIC, "user-auth-behavior-modification.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), new OperationResult("addAuthenticationBehavior"));

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, userOid, null, createOperationResult());
        assertEquals("wrong number of behavior authentication values", 2, userAfter.asObjectable().getBehavior().getAuthentication().size());
    }

}
