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
import com.evolveum.midpoint.repo.sql.data.a0.Assignment;
import com.evolveum.midpoint.repo.sql.data.a0.ExtensionValue;
import com.evolveum.midpoint.repo.sql.data.a0.StringExtensionValue;
import com.evolveum.midpoint.repo.sql.data.a0.User;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Session session = factory.openSession();

        User user = new User();
        user.setFullName("user name");
        Set<Assignment> aset = new HashSet<Assignment>();
        user.setAssignments(aset);

        Assignment a0 = new Assignment();
        a0.setOwner(user);
        a0.setDescription("a0");
        aset.add(a0);
        Set<ExtensionValue> eset = new HashSet<ExtensionValue>();
        a0.setExtensions(eset);
        ExtensionValue e0 = new ExtensionValue();
        e0.setObject(a0);
        e0.setValue("a0-0");
        eset.add(e0);
        e0 = new ExtensionValue();
        e0.setObject(a0);
        e0.setValue("a0-1");
        eset.add(e0);

        a0 = new Assignment();
        a0.setOwner(user);
        a0.setDescription("a1");
        aset.add(a0);
        eset = new HashSet<ExtensionValue>();
        a0.setExtensions(eset);
        e0 = new ExtensionValue();
        e0.setObject(a0);
        e0.setValue("a1-0");
        eset.add(e0);

        session.beginTransaction();
        session.saveOrUpdate(user);
        session.getTransaction().commit();
        session.close();

//        A0 a0= new A0();
//        Set<A1> set = new HashSet<A1>();
//        a0.setAset(set);
//        a0.setValue("a0");
//        
//        A1s a1 = new A1s();
//
//        a1.setOwner(a0);
//        a1.setValue("a1-1");
//        set.add(a1);
//
//        A1c a1c = new A1c();
//        a1.setOwner(a0);
//        a1.setValue("a1-2");
//        set.add(a1);
//        
//        session.beginTransaction();
//        session.saveOrUpdate(a0);
//        session.getTransaction().commit();
//        session.close();

//        R r = new R();
//        Set<A> aset = new HashSet<A>();
//        r.setAset(aset);
//
//        A a = new A();
//        a.setValue("a1");
//        a.setR(r);
//        Set<E> eset = new HashSet<E>();
////        a.setEset(eset);
//        E e = new E();
//        e.setValue("e1");
//        eset.add(e);
//        e = new E();
//        e.setValue("e2");
//        eset.add(e);
//        aset.add(a);
//
//        a = new A();
//        a.setValue("a2");
//        a.setR(r);
//        eset = new HashSet<E>();
////        a.setEset(eset);
//        e = new E();
//        e.setValue("e3");
//        eset.add(e);
//        e = new E();
//        e.setValue("e4");
//        eset.add(e);
//        aset.add(a);
//
//        session.beginTransaction();
//        session.save(r);
//        session.getTransaction().commit();
//        session.close();
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
