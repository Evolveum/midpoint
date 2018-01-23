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
import com.evolveum.midpoint.repo.sql.data.common.RUser;
//import com.evolveum.midpoint.repo.sql.helpers.EntityModificationRegistry;
//import com.evolveum.midpoint.repo.sql.helpers.ObjectDeltaUpdater;
import com.evolveum.midpoint.repo.sql.util.SimpleTaskAdapter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ObjectDeltaUpdaterTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdaterTest.class);

    private static final File DATA_FOLDER = new File("./src/test/resources/update");

    private static final String FILE_USER = "user.xml";

//    @Autowired
//    private EntityModificationRegistry entityModificationRegistry;

    @Test
    public <T extends ObjectType> void addLinkRef() throws Exception {
        OperationResult result = new OperationResult("add linkref");

        PrismObject<UserType> user = prismContext.parseObject(new File(DATA_FOLDER, FILE_USER));

        String oid = repositoryService.addObject(user, new RepoAddOptions(), result);
        AssertJUnit.assertNotNull(oid);

        result.computeStatusIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        // ========
//        LOGGER.info("session start");
//
//        Session session = getFactory().openSession();
//        session.beginTransaction();
//
//        RUser ruser = session.byId(RUser.class).getReference(oid);
//
//        ruser.setVersion(123);
//        ruser.setFullObject(new byte[]{1,2,3});
//
//        ruser.setGivenName(new RPolyString("cz","xc"));
//        ruser.setFamilyName(new RPolyString("cvb","p"));
//
//        ruser.getEmployeeType().add("one");
//        ruser.getEmployeeType().add("two");
//
//        ruser.getActivation().setAdministrativeStatus(RActivationStatus.DISABLED);
//
//        ObjectReferenceType ref = new ObjectReferenceType();
//        ref.setOid("1234");
//        ref.setType(ShadowType.COMPLEX_TYPE);
//        ref.setRelation(ShadowType.COMPLEX_TYPE);
//        RObjectReference rref = RUtil.jaxbRefToRepo(ref, prismContext, ruser, RReferenceOwner.USER_ACCOUNT);
//        rref.setTransient(true);
//        ruser.getLinkRef().add(rref);
//
//        ruser.setLocale("en-US");
//
//        session.save(ruser);
//
//        session.getTransaction().commit();
//        session.close();
//
//        LOGGER.info("session finish");
//        if (1==1) return;
        // ========

//        ObjectDelta delta = ObjectDelta.createModificationAddReference(UserType.class, oid, UserType.F_LINK_REF,
//                prismContext, "123");

        ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(UserType.class, oid, UserType.F_GIVEN_NAME,
                prismContext, new PolyString("asdf", "asdf"));

        delta.addModificationReplaceProperty(UserType.F_GIVEN_NAME, new PolyString("one"));
        delta.addModificationReplaceProperty(UserType.F_FAMILY_NAME, new PolyString("one"));
        delta.addModificationAddProperty(UserType.F_EMPLOYEE_TYPE, "one","two");
        delta.addModificationReplaceProperty(new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL), "asdf");
        delta.addModificationReplaceProperty(
                new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), ActivationStatusType.DISABLED);
        delta.addModificationReplaceProperty(UserType.F_LOCALE, "en-US");

        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        delta.addModificationAddContainer(
                new ItemPath(UserType.F_ASSIGNMENT, 1, AssignmentType.F_ACTIVATION), activation.asPrismContainerValue());

//        ObjectReferenceType parentOrgRef = new ObjectReferenceType();
//        parentOrgRef.setType(OrgType.COMPLEX_TYPE);
//        parentOrgRef.setOid("123");
//        delta.addModificationDeleteReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());
//
//        parentOrgRef = new ObjectReferenceType();
//        parentOrgRef.setType(OrgType.COMPLEX_TYPE);
//        parentOrgRef.setOid("789");
//        delta.addModificationAddReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());


        // todo create modification for metadata/createApproverRef

        repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);

        LOGGER.info("==========");
        RUser u = getFactory().openSession().createQuery("from RUser u where u.oid=:oid", RUser.class).setParameter("oid", oid).getSingleResult();
        LOGGER.info(ToStringBuilder.reflectionToString(u, ToStringStyle.MULTI_LINE_STYLE));

//        result.computeStatus();
//        AssertJUnit.assertTrue(result.isSuccess());
//
//        user = prismContext.parseObject(new File(DATA_FOLDER, FILE_USER));
//        delta.applyTo(user);
//
//        PrismObject newUser = repositoryService.getObject(UserType.class, oid, GetOperationOptions.createRawCollection(), result);
//        AssertJUnit.assertNotNull(newUser);
//
//        result.computeStatus();
//        AssertJUnit.assertTrue(result.isSuccess());
//
//        AssertJUnit.assertTrue(user.diff(newUser).isEmpty());
    }

    @Test
    public void testAudit() throws Exception {
        AuditEventRecord record = new AuditEventRecord();
        record.setChannel("http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-3#import");
        record.setEventIdentifier("1511974895961-0-1");
        record.setEventStage(AuditEventStage.EXECUTION);
        record.setEventType(AuditEventType.ADD_OBJECT);

        ObjectDeltaOperation delta = new ObjectDeltaOperation();
        delta.setObjectDelta(ObjectDelta.createModificationAddReference(UserType.class, "1234", UserType.F_LINK_REF,
                prismContext, "123"));
        record.getDeltas().add(delta);

        delta = new ObjectDeltaOperation();
        delta.setObjectDelta(ObjectDelta.createModificationAddReference(UserType.class, "1234", UserType.F_LINK_REF,
                prismContext, "124"));
        record.getDeltas().add(delta);

        auditService.audit(record, new SimpleTaskAdapter());
    }

    @Test
    public void translateEntityColumnsToRealNames() throws Exception {
//        Session session = factory.openSession();
//        session.beginTransaction();
//
////        CriteriaBuilder cb = session.getCriteriaBuilder();
////
////        CriteriaUpdate update = cb.createCriteriaUpdate(RUser.class);
////        Root r = update.from(RUser.class);
////        update.set(r.get("givenName"), new RPolyString("a","a"));
////        update.where(cb.equal(r.get("oid"), "123"));
////        Query q = session.createQuery(update);
////        q.executeUpdate();
////
////        LOGGER.debug("asdf");
//
////        Query query = session.createQuery("update RUser u set u.givenName = :p where u.oid = :pp");
////        query.setParameter("p", new RPolyString("a", "a"));
////        query.setParameter("pp", "123");
////        int number = query.executeUpdate();
////        LOGGER.debug("Number: {}", number);
//
//
//
//
//        session.getTransaction().commit();
//
//        System.out.println(factory);
////
////        Map<String, Map<Operation, Object>> changes = new HashMap<>();
////
////        update(RUser.class, changes);
    }
}
