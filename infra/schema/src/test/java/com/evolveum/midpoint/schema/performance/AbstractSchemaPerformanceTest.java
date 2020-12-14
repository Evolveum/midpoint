/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.performance;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.IOException;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.tools.testng.PerformanceTestClassMixin;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class AbstractSchemaPerformanceTest extends AbstractUnitTest implements PerformanceTestClassMixin {

    protected static final String LABEL = "new-mapxnode";

    public static final File TEST_DIR = new File("src/test/resources/performance");
    public static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");

    public static final File RESULTS_FILE = new File("target/results.csv");

    public static final int DEFAULT_EXECUTION = 3000;
    public static final int DEFAULT_REPEATS = 5;
    protected static final String NS_FOO = "http://www.example.com/foo";

    private final long runId = System.currentTimeMillis();

    MatchingRuleRegistry matchingRuleRegistry = MatchingRuleRegistryFactory.createRegistry();

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        PrismTestUtil.getPrismContext().setExtraValidation(false);
        assert !InternalsConfig.isConsistencyChecks();
    }

    @BeforeClass
    @Override
    public void initTestMonitor() {
        PerformanceTestClassMixin.super.initTestMonitor();
    }

    protected void measure(String label, String note, CheckedProducer<?> producer) throws CommonException, IOException {
        measure(label, note, producer, DEFAULT_EXECUTION, DEFAULT_REPEATS);
    }

    protected void measure(String label, String note, CheckedProducer<?> producer, long executionTime, int repeats) throws CommonException, IOException {
        Stopwatch watch = stopwatch(label, note);
        for (int i = 0; i < repeats; i++) {
            double micros = measureSingle(label, producer, executionTime, watch);

        }
    }

    protected double measureSingle(String label, CheckedProducer<?> producer, long executionTime, Stopwatch watch) throws CommonException {
        long until = System.currentTimeMillis() + executionTime;
        int iteration = 0;
        while (System.currentTimeMillis() < until) {
            Object result = null;
            iteration++;
            try (Split split = watch.start()) {
                result = producer.get();
            }
            if (result == null) {
                // just to make sure the result is used somehow (and not optimized away)
                throw new IllegalStateException("null result from the producer");
            }
        }
        double micros = ((double) executionTime) * 1000 / iteration;
        String message = label + ": " + iteration + " iterations in " + executionTime + " milliseconds (" + micros + " us per iteration)";
        System.out.println(message);
        logger.info(message);
        return micros;
    }

    @NotNull
    public PrismObject<UserType> getJack() throws SchemaException, IOException {
        return getPrismContext().parserFor(USER_JACK_FILE).parse();
    }

    @AfterClass
    @Override
    public void dumpReport() {
        PerformanceTestClassMixin.super.dumpReport();
    }
}
