/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.a1.*;
import com.evolveum.midpoint.repo.sql.data.a1.StringValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})
public class AddGetObjectTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(AddGetObjectTest.class);
    @Autowired(required = true)
    RepositoryService repositoryService;
    @Autowired(required = true)
    PrismContext prismContext;
    @Autowired
    SessionFactory factory;

    @Test
    public void a() {
        Session session =null;

        Statistics stats = factory.getStatistics();
        stats.setStatisticsEnabled(true);

        String oid = null;

        long previousCycle = 0;

        int cycles = 100;
        long time = System.currentTimeMillis();
        for (int i = 0; i < cycles; i++) {
            if (i % 100 == 0) {
                LOGGER.info("Previous cycle time {}. Next cycle: {}", new Object[]{(System.currentTimeMillis() - time - previousCycle), i});
                previousCycle = System.currentTimeMillis() - time;
            }

            Connector connector0 = new Connector();
            connector0.setName("connector0");
            connector0.setFramework("framework");
            Connector connector1 = new Connector();
            connector1.setName("connector1");

            User user = new User();
            user.setFullName("connector reference target");

            Set<Assignment> aset = new HashSet<Assignment>();
            Assignment a = new Assignment();
            a.setAccountConstruction("a1");
            a.setOwner(user);
            aset.add(a);
            a = new Assignment();
            a.setAccountConstruction("a2");
            a.setOwner(user);
            aset.add(a);
            user.setAssignments(aset);

            AnyContainer userExt = new AnyContainer();
            userExt.setOwner(a);
            a.setExtension(userExt);
            Set<StringValue> userStrings = new HashSet<StringValue>();
            userExt.setStrings(userStrings);
            userStrings.add(new StringValue(null, null, "ass ext"));

            Connector connector = new Connector();
            connector.setName("connector");
            connector.setFramework("framework");
            Reference reference = new Reference();
            reference.setOwner(connector);
            reference.setTarget(user);
            connector.setConnectorRef(reference);

            //refs
            Set<Reference> accountRefs = new HashSet<Reference>();
            reference = new Reference();
            reference.setOwner(user);
            reference.setTarget(connector0);
            accountRefs.add(reference);
            reference = new Reference();
            reference.setOwner(user);
            reference.setTarget(connector1);
            accountRefs.add(reference);
            user.setAccountRefs(accountRefs);
            //extensions
            AnyContainer value = new AnyContainer();
            value.setOwner(user);
            user.setExtension(value);
            Set<StringValue> strings = new HashSet<StringValue>();
            strings.add(new StringValue(new QName("name namespace", "loc"), null, "str1"));
            strings.add(new StringValue(null, new QName("name namespace", "loc"), "str1"));
            value.setStrings(strings);
            Set<LongValue> longs = new HashSet<LongValue>();
            longs.add(new LongValue(123L));
            longs.add(new LongValue(456L));
            value.setLongs(longs);
            Set<DateValue> dates = new HashSet<DateValue>();
            value.setDates(dates);
            dates.add(new DateValue(new Date()));

            ResourceObjectShadow shadow = new ResourceObjectShadow();
            shadow.setObjectClass(new QName("object class", "local"));
            //extension
            AnyContainer extension = new AnyContainer();
            extension.setOwner(shadow);
            shadow.setExtension(extension);
            strings = new HashSet<StringValue>();
            strings.add(new StringValue(null, null, "ext1"));
            strings.add(new StringValue(null, null, "ext2"));
            extension.setStrings(strings);
            dates = new HashSet<DateValue>();
            extension.setDates(dates);
            dates.add(new DateValue(new Date()));
            //attributes
            AnyContainer attributes = new AnyContainer();
            attributes.setOwner(shadow);
            shadow.setAttributes(attributes);
            strings = new HashSet<StringValue>();
            strings.add(new StringValue(null, null, "attr1"));
            strings.add(new StringValue(null, null, "attr2"));
            attributes.setStrings(strings);
            dates = new HashSet<DateValue>();
            attributes.setDates(dates);
            dates.add(new DateValue(new Date()));

            session = open();
            session.save(connector0);
            close(session);
            
            session = open();
            session.save(connector1);
            close(session);
            
            session = open();
            session.save(user);
            close(session);

            session = open();
            session.save(connector);
            close(session);

            session = open();
            session.save(shadow);
            close(session);
            
            if (0.1 < Math.random()) {
                oid = shadow.getOid();
            }
        }
        LOGGER.info("I did {} cycles ({} objects) in {} ms.", new Object[]{cycles, (cycles * 5), (System.currentTimeMillis() - time)});

        if (oid != null) {
            session = open();
            Query query = session.createQuery("from ResourceObjectShadow as s where s.oid = :oid");
            query.setString("oid", oid);
            ResourceObjectShadow shadow = (ResourceObjectShadow) query.uniqueResult();

            LOGGER.info("shadow\n{}", ReflectionToStringBuilder.toString(shadow));
            session.close();
        }

        stats.logSummary();
    }
    
    private Session open() {
        Session session = factory.openSession();
        session.beginTransaction();
        return session;
    }
    
    private void close(Session session) {
        session.getTransaction().commit();
        session.close();
    }

    //    @Test
    public void sample() throws Exception {
//        Session session = factory.openSession();
//
//        Statistics stats = factory.getStatistics();
//        stats.setStatisticsEnabled(true);
//
//        int cycles = 1;
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < cycles; i++) {
//            if (i % 100 == 0) {
//                LOGGER.info("Cycle: {}", new Object[]{i});
//            }
//            session.beginTransaction();
//
//            Connector connector0 = new Connector();
//            connector0.setSomeName("without reference " + i);
//            session.save(connector0);
//            session.getTransaction().commit();
////**********************************************
//            session.beginTransaction();
//
//            Connector connector = new Connector();
//            connector.setSomeName("with reference " + i);
//            ObjectReference ref = new ObjectReference();
//            ref.setOwner(connector);
//            ref.setTarget(connector0);
//            ref.setType(new QName("namespace", "connector"));
//            connector.setConnectorHost(ref);
//            session.save(connector);
//            session.getTransaction().commit();
////*********************************************
//            session.beginTransaction();
//
//            Role role = new Role();
//            role.setDescription("role description " + i);
//            Set<Assignment> set = new HashSet<Assignment>();
//            role.setAssignments(set);
//
//            Assignment a = new Assignment();
//            a.setOwner(role);
//            a.setDescription("a1 description " + i);
//            set.add(a);
//
//            a = new Assignment();
//            a.setOwner(role);
//            a.setDescription("a2 description " + i);
//
//            Reference cRef = new Reference();
//            cRef.setType(new QName("namespace", "connector role ref"));
//            a.setReference(cRef);
//            a.setTarget(connector);
//            set.add(a);
//
//            session.saveOrUpdate(role);
//            session.getTransaction().commit();
////**********************************************
//            session.beginTransaction();
//
//            User otherUser = new User();
//            otherUser.setFullName("other user " + i);
//            session.saveOrUpdate(otherUser);
//            session.getTransaction().commit();
//////**********************************************
//            session.beginTransaction();
//
//            User user = new User();
//            user.setFullName("vilkooo " + i);
//            Set<ObjectReference> references = new HashSet<ObjectReference>();
//            user.setReferences(references);
//            ref = new ObjectReference();
//            ref.setOwner(user);
//            ref.setTarget(otherUser);
//            ref.setType(new QName("namespace", "user"));
//            references.add(ref);
//
//            ref = new ObjectReference();
//            ref.setOwner(user);
//            ref.setTarget(role);
//            ref.setType(new QName("namespace", "role"));
//            references.add(ref);
//
//            set = new HashSet<Assignment>();
//            user.setAssignments(set);
//            a = new Assignment();
//            a.setOwner(user);
//            a.setDescription("a3 user description " + i);
//            set.add(a);
//
//            session.saveOrUpdate(user);
//            session.getTransaction().commit();
//        }
//        LOGGER.info("I did {} cycles in {} ms.", cycles, (System.currentTimeMillis() - time));
//        session.close();
//
//        stats.logSummary();
    }

    //    @Test
    public void simpleAddGetTest() throws Exception {
        final File OBJECTS_FILE = new File("./src/test/resources/objects.xml");
        Objects objects = prismContext.getPrismJaxbProcessor().unmarshalRootObject(OBJECTS_FILE, Objects.class);
        List<JAXBElement<? extends ObjectType>> elements = objects.getObject();
        List<String> oids = new ArrayList<String>();

        OperationResult result = new OperationResult("Simple Add Get Test");
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            JAXBElement<? extends ObjectType> element = elements.get(i);
            ObjectType object = element.getValue();
            prismContext.adopt(object);
            oids.add(repositoryService.addObject(object.asPrismObject(), result));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(), (System.currentTimeMillis() - time),});

        int count = 0;
        objects = prismContext.getPrismJaxbProcessor().unmarshalRootObject(OBJECTS_FILE, Objects.class);
        for (int i = 0; i < elements.size(); i++) {
            JAXBElement<? extends ObjectType> element = elements.get(i);
            ObjectType object = element.getValue();
            object.setOid(oids.get(i));
            prismContext.adopt(object);

            Class<? extends ObjectType> clazz = object.getClass();
            PrismObject<? extends ObjectType> newObject = repositoryService.getObject(clazz, oids.get(i), null, result);
            ObjectDelta delta = object.asPrismObject().diff(newObject);
            if (delta == null) {
                continue;
            }

            count += delta.getModifications().size();
            LOGGER.error("Found changes for\n{}\n", new Object[]{newObject.toString(), delta.debugDump(3)});
        }

        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
    }
}
