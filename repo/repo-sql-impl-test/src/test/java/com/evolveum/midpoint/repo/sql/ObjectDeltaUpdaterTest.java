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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RFocusPhoto;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.testing.QueryCountInterceptor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
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
import java.util.*;
import java.util.Objects;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

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

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationReplaceProperty(UserType.F_NAME, new PolyString("ášdf", "asdf"));
        delta.addModificationReplaceProperty(UserType.F_GIVEN_NAME, new PolyString("ášdf", "asdf"));
        delta.addModificationReplaceProperty(UserType.F_LOCALE, "en-US");
        delta.addModificationReplaceProperty(
                new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), ActivationStatusType.DISABLED);

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getName());
            AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getNameCopy());

            AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getGivenName());

            AssertJUnit.assertEquals(u.getLocale(), "en-US");

            AssertJUnit.assertEquals(RActivationStatus.DISABLED, u.getActivation().getAdministrativeStatus());
        } finally {
            session.close();
        }
    }

    @Test
    public void test115DeleteActivation() throws Exception {
        OperationResult result = new OperationResult("test115DeleteActivation");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);

        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.DISABLED);

        delta.addModificationDeleteContainer(UserType.F_ACTIVATION, activation.asPrismContainerValue());

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(3, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            AssertJUnit.assertNull(u.getActivation());
        } finally {
            session.close();
        }
    }

    @Test
    public void test110ReplaceNonIndexedExtensionProperty() throws Exception {
        OperationResult result = new OperationResult("test110ReplaceExtensionProperty");

        ObjectDelta<?> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_EXTENSION, LOOT), 34);

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(2, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            //RUser u = session.get(RUser.class, userOid);
            RExtItem extItemDef = extItemDictionary
                    .findItemByDefinition(delta.getModifications().iterator().next().getDefinition());
            assertNull("ext item definition for loot exists", extItemDef);
        } finally {
            session.close();
        }
    }

    private void assertAnyValues(Collection<? extends RAnyValue> collection, Integer extItemId, Object... values) {
        Collection<RAnyValue> filtered = new ArrayList<>();

        if (collection != null) {
            for (RAnyValue v : collection) {
                if (extItemId.equals(v.getItemId())) {
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

        ObjectDelta<?> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_EXTENSION, WEAPON), "weapon1", "weapon2");

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(5, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            RExtItem extItemDef = extItemDictionary.findItemByDefinition(delta.getModifications().iterator().next().getDefinition());
            assertAnyValues(u.getStrings(), extItemDef.getId(), "weapon1", "weapon2");
        } finally {
            session.close();
        }
    }

    @Test
    public void test140AddDeleteAssignment() throws Exception {
        OperationResult result = new OperationResult("test140AddDeleteAssignment");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);

        AssignmentType ass = new AssignmentType();
        ass.setId(1L);
        delta.addModificationDeleteContainer(UserType.F_ASSIGNMENT, ass);

        ass = new AssignmentType();
        ass.setDescription("asdf");
        ass.setTargetRef(createRef(OrgType.COMPLEX_TYPE, "444"));
        MetadataType metadata = new MetadataType();
        metadata.setCreateChannel("zzz");
        metadata.getModifyApproverRef().add(createRef(UserType.COMPLEX_TYPE, "555"));
        ass.setMetadata(metadata);
        delta.addModificationAddContainer(UserType.F_ASSIGNMENT, ass);

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        // todo this should be only 7 queries, these two aren't expected:
        // select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
        // select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(9, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());

            RAssignment a = assignments.iterator().next();
            AssertJUnit.assertEquals("zzz", a.getCreateChannel());

            ObjectReferenceType targetRef = a.getTargetRef().toJAXB(prismContext);
            AssertJUnit.assertEquals(createRef(OrgType.COMPLEX_TYPE, "444", SchemaConstants.ORG_DEFAULT), targetRef);

            assertReferences((Collection) a.getModifyApproverRef(),
                    RObjectReference.copyFromJAXB(createRef(UserType.COMPLEX_TYPE, "555", SchemaConstants.ORG_DEFAULT), new RObjectReference())
            );
        } finally {
            session.close();
        }
    }

    @Test
    public void test145AddActivationToAssignment() throws Exception {
        OperationResult result = new OperationResult("test145AddActivationToAssignment");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);

        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        delta.addModificationAddContainer(
                new ItemPath(UserType.F_ASSIGNMENT, 2, AssignmentType.F_ACTIVATION), activation.asPrismContainerValue());

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());

            RAssignment a = assignments.iterator().next();
            RActivation act = a.getActivation();
            AssertJUnit.assertNotNull(act);

            AssertJUnit.assertEquals(RActivationStatus.ENABLED, act.getAdministrativeStatus());
        } finally {
            session.close();
        }
    }

    @Test
    public void test150AddDeleteLinkRef() throws Exception {
        OperationResult result = new OperationResult("test150AddDeleteLinkRef");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        ObjectReferenceType linkRef = createRef(ShadowType.COMPLEX_TYPE, "456");
        delta.addModificationDeleteReference(UserType.F_LINK_REF, linkRef.asReferenceValue());

        linkRef = createRef(ShadowType.COMPLEX_TYPE, "789");
        delta.addModificationAddReference(UserType.F_LINK_REF, linkRef.asReferenceValue());

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(5, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            assertReferences((Collection) u.getLinkRef(),
                    RObjectReference.copyFromJAXB(createRef(ShadowType.COMPLEX_TYPE, "123", SchemaConstants.ORG_DEFAULT), new RObjectReference()),
                    RObjectReference.copyFromJAXB(createRef(ShadowType.COMPLEX_TYPE, "789", SchemaConstants.ORG_DEFAULT), new RObjectReference()));
        } finally {
            session.close();
        }
    }

    @Test
    public void test160AddDeleteParentRef() throws Exception {
        OperationResult result = new OperationResult("test160AddDeleteParentRef");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        ObjectReferenceType parentOrgRef = createRef(OrgType.COMPLEX_TYPE, "456");
        delta.addModificationDeleteReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());

        parentOrgRef = createRef(OrgType.COMPLEX_TYPE, "789");
        delta.addModificationAddReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(5, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            assertReferences((Collection) u.getParentOrgRef(),
                    RObjectReference.copyFromJAXB(createRef(OrgType.COMPLEX_TYPE, "123", SchemaConstants.ORG_DEFAULT), new RObjectReference()),
                    RObjectReference.copyFromJAXB(createRef(OrgType.COMPLEX_TYPE, "789", SchemaConstants.ORG_DEFAULT), new RObjectReference()));
        } finally {
            session.close();
        }
    }

    private ObjectReferenceType createRef(QName type, String oid) {
        return createRef(type, oid, null);
    }

    private ObjectReferenceType createRef(QName type, String oid, QName relation) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(type);
        ref.setOid(oid);
        ref.setRelation(relation);

        return ref;
    }

    private void assertReferences(Collection<ObjectReference> collection, ObjectReference... expected) {
        AssertJUnit.assertEquals(expected.length, collection.size());

        for (ObjectReference ref : collection) {
            boolean found = false;
            for (ObjectReference exp : expected) {
                if (Objects.equals(exp.getRelation(), ref.getRelation())
                        && Objects.equals(exp.getTargetOid(), ref.getTargetOid())
                        && Objects.equals(exp.getType(), ref.getType())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                AssertJUnit.fail("Reference doesn't match " + ref);
            }
        }
    }

    @Test
    public void test170ModifyEmployeeTypeAndMetadataCreateChannel() throws Exception {
        OperationResult result = new OperationResult("test170ModifyEmployeeTypeAndMetadataCreateChannel");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationAddProperty(UserType.F_EMPLOYEE_TYPE, "one", "two");
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL), "asdf");

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        RUser u = session.get(RUser.class, userOid);

        AssertJUnit.assertEquals("asdf", u.getCreateChannel());
        Set set = new HashSet<>();
        set.add("one");
        set.add("two");
        AssertJUnit.assertEquals(u.getEmployeeType(), set);
    }

    @Test
    public void test180ModifyMetadataChannel() throws Exception {
        OperationResult result = new OperationResult("test170ModifyEmployeeTypeAndMetadataCreateChannel");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        ObjectReferenceType ref = createRef(UserType.COMPLEX_TYPE, "111");
        delta.addModificationReplaceReference(new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_APPROVER_REF), ref.asReferenceValue());
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL), "zxcv");

        queryCountInterceptor.startCounter();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());
        }

        Session session = factory.openSession();
        RUser u = session.get(RUser.class, userOid);

        AssertJUnit.assertEquals("zxcv", u.getCreateChannel());
        AssertJUnit.assertEquals(1, u.getCreateApproverRef().size());

        assertReferences((Collection) u.getCreateApproverRef(),
                RObjectReference.copyFromJAXB(createRef(UserType.COMPLEX_TYPE, "111", SchemaConstants.ORG_DEFAULT), new RObjectReference()));
    }

    @Test
    public void test250ModifyShadow() throws Exception {
        // todo implement
        //account-delta.xml
//        [
//        attributes/ship
//            REPLACE: Flying Dutchman
//        attributes/title
//            DELETE: Very Nice Pirate
//        cachingMetadata
//            REPLACE: CachingMetadataType(retrievalTimestamp:2018-02-09T18:30:10.423+01:00)
//        ]
    }

    @Test
    public void test260ReplaceAssignmentExtension() throws Exception {
        OperationResult result = new OperationResult("test260ReplaceAssignmentExtension");

        String file = FOLDER_BASE + "/modify/user-with-assignment-extension.xml";
        PrismObject<UserType> user = prismContext.parseObject(new File(file));
        user.setOid(null);
        repositoryService.addObject(user, null, result);
        String userOid = user.getOid();

        QName SHIP_NAME_QNAME = new QName("http://example.com/p", "shipName");
        PrismPropertyDefinition<String> def1 = new PrismPropertyDefinitionImpl<>(SHIP_NAME_QNAME, DOMUtil.XSD_STRING, prismContext);
        ExtensionType extension = new ExtensionType(prismContext);
        PrismProperty<String> loot = def1.instantiate();
        loot.setRealValue("otherString");
        extension.asPrismContainerValue().add(loot);

        List<ItemDelta<?, ?>> deltas = DeltaBuilder.deltaFor(UserType.class, prismContext)
                .item(new ItemPath(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
                .replace(extension)
                .asItemDeltas();

        repositoryService.modifyObject(UserType.class, userOid, deltas, result);

        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION, SHIP_NAME_QNAME), def1).eq("otherString")
                .build();
        List list = repositoryService.searchObjects(UserType.class, query, null, result);
        LOGGER.info("*** query1 result:\n{}", DebugUtil.debugDump(list));
        assertEquals("Wrong # of query1 results", 1, list.size());

        Session session = open();
        RUser ruser = (RUser) session.createQuery("from RUser where oid = :o").setParameter("o", user.getOid()).getSingleResult();
        RAssignmentExtension ext = ruser.getAssignments().iterator().next().getExtension();
        assertEquals(1, ext.getStrings().size());
        close(session);

        // delete
        deltas = DeltaBuilder.deltaFor(UserType.class, prismContext)
                .item(new ItemPath(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
                .delete(extension.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, deltas, result);

        session = open();
        ruser = (RUser) session.createQuery("from RUser where oid = :o").setParameter("o", user.getOid()).getSingleResult();
        ext = ruser.getAssignments().iterator().next().getExtension();
        assertEquals(0, ext.getStrings().size());
        close(session);

        // add
        deltas = DeltaBuilder.deltaFor(UserType.class, prismContext)
                .item(new ItemPath(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
                .add(extension.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, deltas, result);

        session = open();
        ruser = (RUser) session.createQuery("from RUser where oid = :o").setParameter("o", user.getOid()).getSingleResult();
        ext = ruser.getAssignments().iterator().next().getExtension();
        assertEquals(1, ext.getStrings().size());
        close(session);
    }

    @Test
    public void test270modifyOperationExecution() throws Exception {
        OperationResult result = new OperationResult("test270modifyOperationExecution");

        String file = FOLDER_BASE + "/modify/user-with-assignment-extension.xml";
        PrismObject<UserType> user = prismContext.parseObject(new File(file));

        UserType userType = user.asObjectable();
        userType.setName(new PolyStringType("test270modifyOperationExecution"));
        OperationExecutionType oe = createOperationExecution("repo1");
        userType.getOperationExecution().add(oe);

        user.setOid(null);
        repositoryService.addObject(user, null, result);
        String userOid = user.getOid();

        assertOperationExecutionSize(userOid, 1);

        oe = createOperationExecution("repo2");
        Collection deltas = DeltaBuilder.deltaFor(UserType.class, prismContext)
                .item(UserType.F_OPERATION_EXECUTION)
                .add(oe.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, deltas, result);

        assertOperationExecutionSize(userOid, 2);
    }

    private OperationExecutionType createOperationExecution(String channel) {
        OperationExecutionType oe = new OperationExecutionType();
        oe.setChannel(channel);
        oe.setStatus(OperationResultStatusType.SUCCESS);
        oe.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(SystemObjectsType.USER_ADMINISTRATOR.value());
        ref.setType(UserType.COMPLEX_TYPE);
        oe.setInitiatorRef(ref);

        return oe;
    }

    private void assertOperationExecutionSize(String oid, int expectedSize) {

        Session session = open();
        try {
            RUser rUser = (RUser) session.createQuery("from RUser where oid = :o")
                    .setParameter("o", oid)
                    .getSingleResult();

            AssertJUnit.assertEquals(expectedSize, rUser.getOperationExecutions().size());
        } finally {
            session.close();
        }
    }

    @Test
    public void test280AddPhoto() throws Exception {
        OperationResult result = new OperationResult("test280AddPhoto");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationAddProperty(new ItemPath(UserType.F_JPEG_PHOTO), new byte[]{1, 2, 3});

        queryCountInterceptor.startCounter();

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(5, queryCountInterceptor.getQueryCount());
        }

        LOGGER.info("test280AddPhoto check");
        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            Set<RFocusPhoto> p = u.getJpegPhoto();
            AssertJUnit.assertEquals(1, p.size());

            RFocusPhoto photo = p.iterator().next();
            AssertJUnit.assertTrue(Arrays.equals(new byte[]{1,2,3}, photo.getPhoto()));
        } finally {
            session.close();
        }
    }

    @Test
    public void test290ReplacePhoto() throws Exception {
        OperationResult result = new OperationResult("test290ReplacePhoto");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_JPEG_PHOTO), new byte[]{4,5,6});

        queryCountInterceptor.startCounter();

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(5, queryCountInterceptor.getQueryCount());
        }

        LOGGER.info("test290ReplacePhoto check");
        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            Set<RFocusPhoto> p = u.getJpegPhoto();
            AssertJUnit.assertEquals(1, p.size());

            RFocusPhoto photo = p.iterator().next();
            AssertJUnit.assertTrue(Arrays.equals(new byte[]{4,5,6}, photo.getPhoto()));
        } finally {
            session.close();
        }
    }

    @Test
    public void test300DeletePhoto() throws Exception {
        OperationResult result = new OperationResult("test300DeletePhoto");

        ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        delta.addModificationDeleteProperty(new ItemPath(UserType.F_JPEG_PHOTO), new byte[]{4,5,6});

        queryCountInterceptor.startCounter();

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);

        if (baseHelper.getConfiguration().isUsingH2()) {
            AssertJUnit.assertEquals(4, queryCountInterceptor.getQueryCount());
        }

        LOGGER.info("test300DeletePhoto check");
        Session session = factory.openSession();
        try {
            RUser u = session.get(RUser.class, userOid);

            Set<RFocusPhoto> p = u.getJpegPhoto();
            AssertJUnit.assertEquals(0, p.size());
        } finally {
            session.close();
        }
    }
}
