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
import com.evolveum.midpoint.repo.sql.data.atest.*;
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
import javax.xml.namespace.QName;
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
    public void sample() throws Exception {
        Session session = factory.openSession();

        int cycles = 1;
        long time = System.currentTimeMillis();
        for (int i = 0; i < cycles; i++) {
            if (i % 100 == 0) {
                LOGGER.info("Cycle: {}", new Object[]{i});
            }
            session.beginTransaction();

            Connector connector0 = new Connector();
            connector0.setSomeName("without reference " + i);
            session.save(connector0);
            session.getTransaction().commit();
//**********************************************
            session.beginTransaction();

            Connector connector = new Connector();
            connector.setSomeName("with reference " + i);
            Reference ref = new Reference();
            ref.setOwner(connector);
            ref.setTarget(connector0);
            ref.setType(new QName("namespace", "connector"));
            connector.setConnectorHost(ref);
            session.save(connector);
            session.getTransaction().commit();
//*********************************************
            session.beginTransaction();

            Role role = new Role();
            role.setDescription("role description " + i);
            Set<Assignment> set = new HashSet<Assignment>();
            role.setAssignments(set);

            Assignment a = new Assignment();
            a.setOwner(role);
            a.setDescription("a1 description " + i);
            set.add(a);

            a = new Assignment();
            a.setOwner(role);
            a.setDescription("a2 description " + i);

            ref = new Reference();
            ref.setOwner(role);
            ref.setTarget(connector);
            ref.setType(new QName("namespace", "connector role ref"));
            a.setReference(ref);
            set.add(a);

            session.saveOrUpdate(role);
            session.getTransaction().commit();
//**********************************************
//            session.beginTransaction();
//
//            User otherUser = new User();
//            otherUser.setFullName("other user " + i);
//            session.saveOrUpdate(otherUser);
//            session.getTransaction().commit();
////**********************************************
//            session.beginTransaction();
//
//            User user = new User();
//            user.setFullName("vilkooo " + i);
//            Set<Reference> references = new HashSet<Reference>();
//            user.setReferences(references);
//            ref = new Reference();
//            ref.setOwner(user);
//            ref.setTarget(otherUser);
//            ref.setType(new QName("namespace", "user"));
//            references.add(ref);
//
//            ref = new Reference();
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
        }
        LOGGER.info("I did {} cycles in {} ms.", cycles, (System.currentTimeMillis() - time));
        session.close();
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
