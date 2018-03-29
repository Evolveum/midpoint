/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.fail;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ExtDictionaryTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ExtDictionaryTest.class);
    private static final int ROUNDS = 50;
    private static final int THREADS = 10;
    private static final String NS_TEST = "http://test";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    class TestingThread extends Thread {
        Throwable throwable;
        TestingThread(Runnable target) {
            super(target);
        }
        @Override
        public void run() {
            try {
                super.run();
            } catch (Throwable t) {
                throwable = t;
                t.printStackTrace();
            }
        }
    }

    @Test
    public void test100ParallelAdd() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            List<TestingThread> threads = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                final int round1 = round;
                final int thread1 = i;
                Runnable runnable = () -> {
                    try {
                        UserType user = new UserType(prismContext)
                                .name("u-" + round1 + "-" + thread1);
                        QName propertyName = new QName(NS_TEST, "round" + round1);
                        PrismPropertyDefinitionImpl<String> propertyDefinition = new PrismPropertyDefinitionImpl<>(propertyName,
                                DOMUtil.XSD_STRING, prismContext);
                        PrismProperty<String> property = propertyDefinition.instantiate();
                        property.setRealValue("value");
                        user.asPrismObject().addExtensionItem(property);
                        repositoryService.addObject(user.asPrismObject(), null, new OperationResult("addObject"));
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new SystemException(e);
                    }
                };
                TestingThread thread = new TestingThread(runnable);
                threads.add(thread);
                thread.start();
            }
            for (int i = 0; i < THREADS; i++) {
                TestingThread thread = threads.get(i);
                thread.join(60000L);
                if (thread.throwable != null) {
                    fail("Exception in " + thread + ": " + thread.throwable);
                }
            }
        }
        Session session = open();
        //noinspection unchecked
        List<RExtItem> extItems = session.createQuery("from RExtItem").list();
        System.out.println("ext items: " + extItems.size());
        for (RExtItem extItem : extItems) {
            System.out.println(extItem);
            LOGGER.info("{}", extItem);
        }
        session.close();
    }
}
