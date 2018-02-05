/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.testing.QueryCountInterceptor;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.SimpleTaskAdapter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

//import com.evolveum.midpoint.repo.sql.helpers.EntityModificationRegistry;
//import com.evolveum.midpoint.repo.sql.helpers.ObjectDeltaUpdater;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ObjectDeltaUpdaterTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdaterTest.class);

    private static final File DATA_FOLDER = new File("./src/test/resources/update");

    private static final String FILE_USER = "user.xml";

    private static final String NS_P = "http://example.com/p";

    private static final QName LOOT = new QName(NS_P, "loot");
    private static final QName WEAPON = new QName(NS_P, "weapon");

    @Autowired
    private QueryCountInterceptor queryCountInterceptor;

    private String userOid;

    @AfterMethod
    public void afterMethod() {
        queryCountInterceptor.clearCounter();
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        OperationResult result = new OperationResult("setup");

        PrismObject<UserType> user = prismContext.parseObject(new File(DATA_FOLDER, FILE_USER));

        userOid = repositoryService.addObject(user, new RepoAddOptions(), result);
        AssertJUnit.assertNotNull(userOid);

        result.computeStatusIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test100UpdateGivenNameAndActivation() throws Exception {
        OperationResult result = new OperationResult("test100UpdateGivenNameAndActivation");

        ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(UserType.class, userOid, UserType.F_GIVEN_NAME,
                prismContext, new PolyString("ášdf", "asdf"));
        delta.addModificationReplaceProperty(
                new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), ActivationStatusType.DISABLED);

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());

        Session session = factory.openSession();
        RUser u = session.get(RUser.class, userOid);

        AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getGivenName());
        AssertJUnit.assertEquals(RActivationStatus.DISABLED, u.getActivation().getAdministrativeStatus());
    }

    @Test
    public void test110ReplaceExtensionProperty() throws Exception {
        OperationResult result = new OperationResult("test110ReplaceExtensionProperty");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_EXTENSION, LOOT), 34);

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        AssertJUnit.assertEquals(2, queryCountInterceptor.getQueryCount());

        Session session = factory.openSession();
        RUser u = session.get(RUser.class, userOid);

        assertAnyValues(u.getLongs(), LOOT);
    }

    private void assertAnyValues(Collection<? extends RAnyValue> collection, QName name, Object... values) {
        Collection<RAnyValue> filtered = new ArrayList();

        if (collection != null) {
            for (RAnyValue v : collection) {
                if (RUtil.qnameToString(name).equals(v.getName())) {
                    filtered.add(v);
                }
            }
        }

        AssertJUnit.assertEquals(values.length, filtered.size());

        for (Object value : values) {
            boolean found = false;
            for (RAnyValue v : filtered) {
                if (v.getValue().equals(value)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                AssertJUnit.fail("Couldn't find '" + value + "' in extension collection");
            }
        }
    }

    @Test
    public void test120AddExtensionProperty() throws Exception {
        OperationResult result = new OperationResult("test120AddExtensionProperty");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_EXTENSION, WEAPON), "weapon1", "weapon2");

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());

        Session session = factory.openSession();
        RUser u = session.get(RUser.class, userOid);

        assertAnyValues(u.getStrings(), WEAPON, "weapon1", "weapon2");
    }

    @Test
    public void test140AddAssignment() throws Exception {
        // todo implement
    }

    @Test
    public void test150DeleteAssignment() throws Exception {
        // todo impelment
    }

    @Test
    public void test160AddDeleteParentRef() throws Exception {
        OperationResult result = new OperationResult("test160AddDeleteParentRef");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        ObjectReferenceType parentOrgRef = createRef(OrgType.COMPLEX_TYPE, "123");
        delta.addModificationDeleteReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());

        parentOrgRef = createRef(OrgType.COMPLEX_TYPE, "789");
        delta.addModificationAddReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        AssertJUnit.assertEquals(5, queryCountInterceptor.getQueryCount());

        Session session = factory.openSession();
        RUser u = session.get(RUser.class, userOid);

        assertReferences((Collection) u.getParentOrgRef(),
                RObjectReference.copyFromJAXB(createRef(OrgType.COMPLEX_TYPE, "456", SchemaConstants.ORG_DEFAULT), new RObjectReference()),
                RObjectReference.copyFromJAXB(createRef(OrgType.COMPLEX_TYPE, "789", SchemaConstants.ORG_DEFAULT), new RObjectReference()));
    }

    private ObjectReferenceType createRef(QName type, String oid) {
        return createRef(type, oid, null);
    }

    private ObjectReferenceType createRef(QName type, String oid, QName relation) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(type);
        ref.setOid(oid);

        return ref;
    }

    private void assertReferences(Collection<ObjectReference> collection, ObjectReference... expected) {
        AssertJUnit.assertEquals(expected.length, collection.size());

        for (ObjectReference ref : collection) {
            boolean found = false;
            for (ObjectReference exp : expected) {
                if (ref.equals(exp)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                AssertJUnit.fail("Reference doesn't match " + ref);
            }
        }
    }


    public <T extends ObjectType> void addLinkRef() throws Exception {

        OperationResult result = new OperationResult("add linkref");

        PrismObject<UserType> user = prismContext.parseObject(new File(DATA_FOLDER, FILE_USER));

        String oid = repositoryService.addObject(user, new RepoAddOptions(), result);
        AssertJUnit.assertNotNull(oid);

        result.computeStatusIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

//        ObjectDelta delta = ObjectDelta.createModificationAddReference(UserType.class, oid, UserType.F_LINK_REF,
//                prismContext, "123");

        ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(UserType.class, oid, UserType.F_GIVEN_NAME,
                prismContext, new PolyString("asdf", "asdf"));

//        delta.addModificationAddProperty(new ItemPath(UserType.F_EXTENSION,
//                new QName("http://example.com/p", "weapon")), "glock");

//        delta.addModificationReplaceProperty(UserType.F_NAME, new PolyString("super name"));
//
//        delta.addModificationReplaceProperty(UserType.F_GIVEN_NAME, new PolyString("one"));
//        delta.addModificationReplaceProperty(UserType.F_FAMILY_NAME, new PolyString("one"));
//        delta.addModificationAddProperty(UserType.F_EMPLOYEE_TYPE, "one","two");
//        delta.addModificationReplaceProperty(new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL), "asdf");
//        delta.addModificationReplaceProperty(
//                new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), ActivationStatusType.DISABLED);
//        delta.addModificationReplaceProperty(UserType.F_LOCALE, "en-US");

//        ActivationType activation = new ActivationType();
//        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
//        delta.addModificationAddContainer(
//                new ItemPath(UserType.F_ASSIGNMENT, 1, AssignmentType.F_ACTIVATION), activation.asPrismContainerValue());

//        AssignmentType ass = new AssignmentType();
//        ass.setId(1L);
//        delta.addModificationDeleteContainer(UserType.F_ASSIGNMENT, ass);
//
//        ass = new AssignmentType();
//        ass.setDescription("asdf");
//        delta.addModificationAddContainer(UserType.F_ASSIGNMENT, ass);

//        delta.addModificationReplaceProperty(
//                new ItemPath(UserType.F_EXTENSION, new QName("http://example.com/p", "loot")), 34);


        // todo create modification for metadata/createApproverRef
        queryCountInterceptor.startCounter();

        repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);
    }
}
