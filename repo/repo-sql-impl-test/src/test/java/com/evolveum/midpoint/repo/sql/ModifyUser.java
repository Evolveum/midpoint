/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

/**
 * This is not real test, it's just used to check how hibernate handles insert/modify of different objects.
 *
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyUser extends BaseSQLRepoTest {

    private String userOid;
    private String userBigOid;
    private String shadowOid;

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test001Add() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user.xml"));
        userOid = repositoryService.addObject(user, null, new OperationResult("asdf"));

        user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user-big.xml"));
        userBigOid = repositoryService.addObject(user, null, new OperationResult("asdf"));

        PrismObject<ShadowType> shadow = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "account-shadow.xml"));
        shadowOid = repositoryService.addObject(shadow, null, new OperationResult("asdf"));
    }

    @Test
    public void test002ModifyUser() throws Exception {
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(FOLDER_BASIC, "t002.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);
        delta.setOid(userOid);

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), new OperationResult("asdf"));
    }

    @Test
    public void test003ModifyShadow() throws Exception {
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(FOLDER_BASIC, "t003.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, ShadowType.class, prismContext);
        delta.setOid(userOid);

        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), new OperationResult("asdf"));
    }

    @Test
    public void test004GetShadow() throws Exception {
        repositoryService.getObject(ShadowType.class, shadowOid, null, new OperationResult("asdf"));
    }

    @Test
    public void test005ModifyBigUser() throws Exception {
        PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(ObjectType.F_DESCRIPTION, def,
                "new description");

        repositoryService.modifyObject(UserType.class, userBigOid, Arrays.asList(delta), new OperationResult("asdf"));
    }

    @Test
    public void test006GetBigUser() throws Exception {
        repositoryService.getObject(UserType.class, userBigOid, null, new OperationResult("asdf"));
    }
}
