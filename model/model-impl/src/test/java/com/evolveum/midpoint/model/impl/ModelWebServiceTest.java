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

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.impl.util.ModelTUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author lazyman
 */

@ContextConfiguration(locations = {"classpath:ctx-model-test-no-repo.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ModelWebServiceTest extends AbstractTestNGSpringContextTests {

    private static final File TEST_FOLDER_CONTROLLER = new File("./src/test/resources/controller");
    @Autowired(required = true)
    ModelPortType modelService;
    @Autowired(required = true)
    ProvisioningService provisioningService;
    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    RepositoryService repositoryService;
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @BeforeMethod
    public void before() {
        Mockito.reset(provisioningService, repositoryService);
    }

// TODO maybe implement in executeChanges
//    @Test(expectedExceptions = FaultMessage.class)
//    public void addNullObject() throws FaultMessage {
//        try {
//            modelService.addObject(null, new Holder<String>(), new Holder<OperationResultType>());
//        } catch (FaultMessage ex) {
//            ModelTUtil.assertIllegalArgumentFault(ex);
//        }
//        Assert.fail("Add must fail.");
//    }

//    @Test(expectedExceptions = FaultMessage.class)
//    @SuppressWarnings("unchecked")
//    public void addUserWithoutName() throws Exception {
//        final UserType expectedUser = PrismTestUtil.unmarshalObject(new File(
//                TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml"), UserType.class);
//        setSecurityContext(expectedUser);
//        try {
//            modelService.addObject(expectedUser, new Holder<String>(), new Holder<OperationResultType>());
//        } catch (FaultMessage ex) {
//            ModelTUtil.assertIllegalArgumentFault(ex);
//        } finally {
//            SecurityContextHolder.getContext().setAuthentication(null);
//        }
//        Assert.fail("add must fail.");
//    }
    
	@Test(expectedExceptions = FaultMessage.class)
    public void testGetNullOid() throws FaultMessage {
        try {
            modelService.getObject(UserType.COMPLEX_TYPE, null, new SelectorQualifiedGetOptionsType(),
                    new Holder<ObjectType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }
        Assert.fail("get must fail");
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void testGetEmptyOid() throws FaultMessage {
        try {
            modelService.getObject(UserType.COMPLEX_TYPE, "", new SelectorQualifiedGetOptionsType(),
                    new Holder<ObjectType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }
        Assert.fail("get must fail");
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void testGetNullOidAndPropertyRef() throws FaultMessage {
        try {
            modelService.getObject(UserType.COMPLEX_TYPE, null, null,
                    new Holder<ObjectType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }
        Assert.fail("get must fail");
    }

//    @Test(expectedExceptions = FaultMessage.class)
    public void testGetNullPropertyRef() throws FaultMessage, SchemaException, IOException, JAXBException {
    	final UserType expectedUser = (UserType) PrismTestUtil.parseObject(new File(
                TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml")).asObjectable();
        setSecurityContext(expectedUser);
    	try {
            modelService.getObject(UserType.COMPLEX_TYPE, "001", null,
                    new Holder<ObjectType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }  finally {
        	SecurityContextHolder.getContext().setAuthentication(null);
        }
        Assert.fail("get must fail");
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void getNonexistingObject() throws FaultMessage, ObjectNotFoundException, SchemaException, IOException, JAXBException {
    	final UserType expectedUser = (UserType) PrismTestUtil.parseObject(new File(TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml")).asObjectable();
        setSecurityContext(expectedUser);
        try {
            final String oid = "abababab-abab-abab-abab-000000000001";
            when(
                    repositoryService.getObject(any(Class.class), eq(oid),
                            any(Collection.class), any(OperationResult.class))).thenThrow(
                    new ObjectNotFoundException("Object with oid '" + oid + "' not found."));

            modelService.getObject(UserType.COMPLEX_TYPE, oid, new SelectorQualifiedGetOptionsType(),
                    new Holder<ObjectType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertObjectNotFoundFault(ex);
        } finally {
        	SecurityContextHolder.getContext().setAuthentication(null);
        }
        Assert.fail("get must fail");
    }

//    @Test(expectedExceptions = FaultMessage.class)
//    public void testDeleteNullOid() throws FaultMessage {
//        try {
//            modelService.deleteObject(UserType.COMPLEX_TYPE, null);
//        } catch (FaultMessage ex) {
//            ModelTUtil.assertIllegalArgumentFault(ex);
//        }
//        Assert.fail("delete must fail");
//    }

//    @Test(expectedExceptions = FaultMessage.class)
//    public void testDeleteEmptyOid() throws FaultMessage {
//        try {
//            modelService.deleteObject(UserType.COMPLEX_TYPE, "");
//        } catch (FaultMessage ex) {
//            ModelTUtil.assertIllegalArgumentFault(ex);
//        }
//        Assert.fail("delete must fail");
//    }

//    @Test(expectedExceptions = FaultMessage.class)
//    public void testDeleteNonExisting() throws FaultMessage, ObjectNotFoundException, SchemaException, JAXBException, FileNotFoundException {
//        try {
//            final String oid = "abababab-abab-abab-abab-000000000001";
//            when(
//                    repositoryService.getObject(any(Class.class), eq(oid),
//                            any(Collection.class), any(OperationResult.class))).thenThrow(
//                    new ObjectNotFoundException("Object with oid '' not found."));
//
//            final UserType user = PrismTestUtil.unmarshalObject(new File(
//                    TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml"), UserType.class);
//            setSecurityContext(user);
//
//            modelService.deleteObject(UserType.COMPLEX_TYPE, oid);
//        } catch (FaultMessage ex) {
//            ModelTUtil.assertObjectNotFoundFault(ex);
//        } finally {
//            SecurityContextHolder.getContext().setAuthentication(null);
//        }
//        Assert.fail("delete must fail");
//    }

//    @Test(expectedExceptions = FaultMessage.class)
    public void nullQueryType() throws FaultMessage, SchemaException, IOException, JAXBException {
    	
        try {
        	final UserType expectedUser = (UserType) PrismTestUtil.parseObject(new File(
                    TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml")).asObjectable();
            setSecurityContext(expectedUser);
            modelService.searchObjects(UserType.COMPLEX_TYPE, null, null,
                    new Holder<ObjectListType>(),
                    new Holder<OperationResultType>());
            Assert.fail("Illegal argument exception was not thrown.");
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        } finally {
        	SecurityContextHolder.getContext().setAuthentication(null);
        }
        
    }

//    @Test(expectedExceptions = FaultMessage.class)
    public void nullQueryTypeAndPaging() throws FaultMessage, SchemaException, IOException, JAXBException {
        try {
        	final UserType expectedUser = (UserType) PrismTestUtil.parseObject(new File(
                    TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml")).asObjectable();
            setSecurityContext(expectedUser);
            modelService.searchObjects(UserType.COMPLEX_TYPE, null, null,
                    new Holder<ObjectListType>(),
                    new Holder<OperationResultType>());
            Assert.fail("Illegal argument exception was not thrown.");
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        } finally {
        	SecurityContextHolder.getContext().setAuthentication(null);
        }
        
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void badPagingSearch() throws FaultMessage, SchemaException, IOException, JAXBException {
        PagingType paging = new PagingType();
        paging.setMaxSize(-1);
        paging.setOffset(-1);

        final UserType expectedUser = (UserType) PrismTestUtil.parseObject(new File(TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml")).asObjectable();
        setSecurityContext(expectedUser);
        try {
        	QueryType queryType = new QueryType();
        	queryType.setPaging(paging);
            modelService.searchObjects(UserType.COMPLEX_TYPE, queryType, null,
                    new Holder<ObjectListType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        } finally {
        	SecurityContextHolder.getContext().setAuthentication(null);
        }
        Assert.fail("Illegal argument exception was not thrown.");
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void nullChangeModify() throws FaultMessage {
        try {
            modelService.executeChanges(null, null);
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void nonExistingUidModify() throws FaultMessage, ObjectNotFoundException, SchemaException, JAXBException, IOException {
        final String oid = "1";
        ObjectDeltaType objectDeltaType = new ObjectDeltaType();
        objectDeltaType.setChangeType(ChangeTypeType.MODIFY);
        objectDeltaType.setObjectType(UserType.COMPLEX_TYPE);
        objectDeltaType.setOid(oid);

        ItemDeltaType mod1 = new ItemDeltaType();
        mod1.setModificationType(ModificationTypeType.ADD);
//        ItemDeltaType.Value value = new ItemDeltaType.Value();
//        value.getAny().add(DOMUtil.createElement(DOMUtil.getDocument(), new QName(SchemaConstants.NS_C, "fullName")));
        mod1.setPath(new ItemPathType(new ItemPath(UserType.F_FULL_NAME)));
        objectDeltaType.getItemDelta().add(mod1);

        when(
                repositoryService.getObject(any(Class.class), eq(oid),
                       any(Collection.class), any(OperationResult.class))).thenThrow(
                new ObjectNotFoundException("Oid '" + oid + "' not found."));

        final UserType user = (UserType) PrismTestUtil.parseObject(new File(
                TEST_FOLDER_CONTROLLER, "./addObject/add-user-without-name.xml")).asObjectable();
        setSecurityContext(user);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(objectDeltaType);

        try {
            modelService.executeChanges(deltaListType, null);
        } catch (FaultMessage ex) {
            ModelTUtil.assertObjectNotFoundFault(ex);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void nullAccountOidListAccountShadowOwner() throws FaultMessage {
        try {
            modelService.findShadowOwner(null, new Holder<UserType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }
        Assert.fail("Illegal argument excetion must be thrown");
    }

    @Test(expectedExceptions = FaultMessage.class)
    public void emptyAccountOidListAccountShadowOwner() throws FaultMessage {
        try {
            modelService.findShadowOwner("", new Holder<UserType>(),
                    new Holder<OperationResultType>());
        } catch (FaultMessage ex) {
            ModelTUtil.assertIllegalArgumentFault(ex);
        }
        Assert.fail("Illegal argument exception must be thrown");
    }
    
	private void setSecurityContext(UserType user) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				new MidPointPrincipal(user), null));
	}

}
