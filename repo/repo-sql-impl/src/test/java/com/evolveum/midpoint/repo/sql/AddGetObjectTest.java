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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
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

import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../application-context-sql-no-server-mode-test.xml",
        "../../../../../application-context-repository.xml",
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

    private RObjectReference simpleInsertConnector2(RUser user) {
        RConnector connector = new RConnector();
        connector.setName("connector");
        connector.setFramework("framework");
        RObjectReference reference = new RObjectReference();
        reference.setOwner(connector);
        reference.setTarget(user);
        connector.setConnectorHostRef(reference);

        Session session = open();
        session.save(connector);
        close(session);

        return reference;
    }

    private RConnector simpleInsertConnector3() {
        RConnector connector = new RConnector();
        connector.setName("connector1");

        Session session = open();
        session.save(connector);
        close(session);

        return connector;
    }

    private RConnector simpleInsertConnector1() {
        RConnector connector = new RConnector();
        connector.setName("connector0");
        connector.setFramework("framework");

        Session session = open();
        session.save(connector);
        close(session);

        return connector;
    }

    private RResourceObjectShadow simpleInsertShadow() {
        RResourceObjectShadow shadow = new RResourceObjectShadow();
        shadow.setObjectClass(new QName("object class", "local"));
        //extension
        RAnyContainer extension = new RAnyContainer();
        extension.setOwner(shadow);
        shadow.setExtension(extension);
        Set<RStringValue> strings = new HashSet<RStringValue>();
        strings.add(new RStringValue(null, null, "ext1"));
        strings.add(new RStringValue(null, null, "ext2"));
        extension.setStrings(strings);
        Set<RDateValue> dates = new HashSet<RDateValue>();
        extension.setDates(dates);
        dates.add(new RDateValue(new Date()));
        //attributes
        RAnyContainer attributes = new RAnyContainer();
        attributes.setOwner(shadow);
        shadow.setAttributes(attributes);
        strings = new HashSet<RStringValue>();
        strings.add(new RStringValue(null, null, "attr1"));
        strings.add(new RStringValue(null, null, "attr2"));
        attributes.setStrings(strings);
        dates = new HashSet<RDateValue>();
        attributes.setDates(dates);
        dates.add(new RDateValue(new Date()));

        Session session = open();
        session.save(shadow);
        close(session);

        return shadow;
    }

    private RUser simpleInsertUser(RConnector connector0, RConnector connector1) {
        RUser user = new RUser();
        user.setFullName("connector reference target");
        //assignment
        Set<RAssignment> aset = new HashSet<RAssignment>();
        RAssignment a = new RAssignment();
        a.setAccountConstruction("a1");
        a.setOwner(user);
        aset.add(a);
        a = new RAssignment();
        a.setAccountConstruction("a2");
        a.setOwner(user);
        aset.add(a);
        user.setAssignments(aset);
        //extension
        RAnyContainer userExt = new RAnyContainer();
        userExt.setOwner(a);
        a.setExtension(userExt);
        Set<RStringValue> userStrings = new HashSet<RStringValue>();
        userExt.setStrings(userStrings);
        userStrings.add(new RStringValue(null, null, "ass ext"));
        //refs
        Set<RObjectReference> accountRefs = new HashSet<RObjectReference>();
        RObjectReference reference = new RObjectReference();
        reference.setOwner(user);
        reference.setTarget(connector0);
        accountRefs.add(reference);
        reference = new RObjectReference();
        reference.setOwner(user);
        reference.setTarget(connector1);
        accountRefs.add(reference);
        user.setAccountRefs(accountRefs);
        //extensions
        RAnyContainer value = new RAnyContainer();
        value.setOwner(user);
        user.setExtension(value);
        Set<RStringValue> strings = new HashSet<RStringValue>();
        strings.add(new RStringValue(new QName("name namespace", "loc"), null, "str1"));
        strings.add(new RStringValue(null, new QName("name namespace", "loc"), "str1"));
        value.setStrings(strings);
        Set<RLongValue> longs = new HashSet<RLongValue>();
        longs.add(new RLongValue(123L));
        longs.add(new RLongValue(456L));
        value.setLongs(longs);
        Set<RDateValue> dates = new HashSet<RDateValue>();
        value.setDates(dates);
        dates.add(new RDateValue(new Date()));

        Session session = open();
        session.save(user);
        close(session);

        return user;
    }

    /**
     * Test just to check some parts of annotations mapping (for obvious errors)
     */
//    @Test
    public void simpleInsertTest() {
        Statistics stats = factory.getStatistics();
        stats.setStatisticsEnabled(true);

        String oid = null;

        long previousCycle = 0;
        int cycles = 1;
        long time = System.currentTimeMillis();
        for (int i = 0; i < cycles; i++) {
            if (i % 100 == 0) {
                LOGGER.info("Previous cycle time {}. Next cycle: {}", new Object[]{
                        (System.currentTimeMillis() - time - previousCycle), i});
                previousCycle = System.currentTimeMillis() - time;
            }

            RConnector connector0 = simpleInsertConnector1();
            RConnector connector1 = simpleInsertConnector3();

            RUser user = simpleInsertUser(connector0, connector1);
            simpleInsertConnector2(user);
            RResourceObjectShadow shadow = simpleInsertShadow();

            if (0.1 < Math.random()) {
                oid = shadow.getOid();
            }
        }
        LOGGER.info("I did {} cycles ({} objects) in {} ms.", new Object[]{
                cycles, (cycles * 5), (System.currentTimeMillis() - time)});

        if (oid != null) {
            Session session = open();
            Query query = session.createQuery("from RResourceObjectShadowType as s where s.oid = :oid");
            query.setString("oid", oid);
            RResourceObjectShadow shadow = (RResourceObjectShadow) query.uniqueResult();

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
    public <T extends ObjectType> void perfTest() throws Exception {
        Statistics stats = factory.getStatistics();
        stats.setStatisticsEnabled(true);

        final File OBJECTS_FILE = new File("./src/test/resources/10k-users.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(OBJECTS_FILE);

        long previousCycle = 0;
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            if (i % 500 == 0) {
                LOGGER.info("Previous cycle time {}. Next cycle: {}", new Object[]{
                        (System.currentTimeMillis() - time - previousCycle), i});
                previousCycle = System.currentTimeMillis() - time;
            }

            PrismObject<T> object = (PrismObject<T>) elements.get(i);
            repositoryService.addObject(object, new OperationResult("add performance test"));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(), (System.currentTimeMillis() - time)});

        stats.logSummary();
    }

    @Test
    public void simpleAddGetTest() throws Exception {
        final File OBJECTS_FILE = new File("./src/test/resources/objects.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(OBJECTS_FILE);
        List<String> oids = new ArrayList<String>();

        OperationResult result = new OperationResult("Simple Add Get Test");
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Adding object {}, type {}", new Object[]{(i + 1), object.getCompileTimeClass().getSimpleName()});
            oids.add(repositoryService.addObject(object, result));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(), (System.currentTimeMillis() - time),});

        int count = 0;
        elements = prismContext.getPrismDomProcessor().parseObjects(OBJECTS_FILE);
        for (int i = 0; i < elements.size(); i++) {
            LOGGER.info("*******************************************");
            try {
                PrismObject object = elements.get(i);
                object.asObjectable().setOid(oids.get(i));

                Class<? extends ObjectType> clazz = object.getCompileTimeClass();
                PrismObject<? extends ObjectType> newObject = repositoryService.getObject(clazz, oids.get(i), null, result);
                ObjectDelta delta = object.diff(newObject);
                if (delta == null) {
                    continue;
                }

                count += delta.getModifications().size();
                LOGGER.error(">>> {} Found {} changes for {}\n{}", new Object[]{(i + 1), delta.getModifications().size(),
                        newObject.toString(), delta.debugDump(3)});
                if (delta.getModifications().size() > 0) {
                    LOGGER.error("{}", newObject.debugDump(3));
                }
            } catch (Exception ex) {
                LOGGER.error("Exception occured", ex);
            }
        }

        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
    }
}
