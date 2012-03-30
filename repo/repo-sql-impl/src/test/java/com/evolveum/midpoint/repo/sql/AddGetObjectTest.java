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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    @Test(enabled = false)
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
                    LOGGER.error("{}", prismContext.getPrismDomProcessor().serializeObjectToString(newObject));
                }
            } catch (Exception ex) {
                LOGGER.error("Exception occured", ex);
            }
        }

        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
    }
}
