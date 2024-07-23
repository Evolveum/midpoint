/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;
import jakarta.persistence.EntityManager;
import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ExtDictionaryTest extends BaseSQLRepoTest {

    private static final int ROUNDS = 50;
    private static final int THREADS = 10;
    private static final String NS_TEST = "http://test";

    static class TestingThread extends Thread {
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
                        MutablePrismPropertyDefinition<String> propertyDefinition = prismContext.definitionFactory().createPropertyDefinition(propertyName,
                                DOMUtil.XSD_STRING);
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
        EntityManager em = open();
        //noinspection unchecked
        List<RExtItem> extItems = em.createQuery("from RExtItem").getResultList();
        System.out.println("ext items: " + extItems.size());
        for (RExtItem extItem : extItems) {
            System.out.println(extItem);
            logger.info("{}", extItem);
        }
        em.close();
    }
}
