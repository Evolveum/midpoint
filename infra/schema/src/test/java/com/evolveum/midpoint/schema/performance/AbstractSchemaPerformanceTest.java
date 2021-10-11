/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.performance;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class AbstractSchemaPerformanceTest extends AbstractUnitTest {

    protected static final String LABEL = "new-mapxnode";

    public static final File TEST_DIR = new File("src/test/resources/performance");
    public static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");

    public static final File RESULTS_FILE = new File("target/results.csv");

    public static final int DEFAULT_EXECUTION = 3000;
    public static final int DEFAULT_REPEATS = 5;
    protected static final String NS_FOO = "http://www.example.com/foo";

    private final long runId = System.currentTimeMillis();

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        PrismTestUtil.getPrismContext().setExtraValidation(false);
        assert !InternalsConfig.isConsistencyChecks();
    }

    protected void measure(String label, CheckedProducer<?> producer) throws CommonException, IOException {
        measure(label, producer, DEFAULT_EXECUTION, DEFAULT_REPEATS);
    }

    protected void measure(String label, CheckedProducer<?> producer, long executionTime, int repeats) throws CommonException, IOException {
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < repeats; i++) {
            double micros = measureSingle(label, producer, executionTime);
            times.add(micros);
        }

        PrintWriter resultsWriter = new PrintWriter(new FileWriter(RESULTS_FILE, true));
        double min = times.stream().min(Double::compareTo).orElse(0.0);
        double max = times.stream().max(Double::compareTo).orElse(0.0);
        double sum = times.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / repeats;
        double avg2 = (sum - min - max) / (repeats - 2);
        resultsWriter.print(runId + ";" + new Date() + ";" + LABEL + ";" + label + ";" + executionTime + ";" + repeats + ";" + avg2 + ";" + avg + ";" + min + ";" + max);
        for (Double time : times) {
            resultsWriter.print(";" + time);
        }
        resultsWriter.println();
        resultsWriter.close();

        System.out.println(label + ": Average without the best and the worst = " + avg2);
    }

    protected double measureSingle(String label, CheckedProducer<?> producer, long executionTime) throws CommonException {
        long until = System.currentTimeMillis() + executionTime;
        int iteration = 0;
        while (System.currentTimeMillis() < until) {
            if (producer.get() == null) {
                // just to make sure the result is used somehow (and not optimized away)
                throw new IllegalStateException("null result from the producer");
            }
            iteration++;
        }
        double micros = ((double) executionTime) * 1000 / (double) iteration;
        String message = label + ": " + iteration + " iterations in " + executionTime + " milliseconds (" + micros + " us per iteration)";
        System.out.println(message);
        logger.info(message);

        return micros;
    }

    @NotNull
    public PrismObject<UserType> getJack() throws SchemaException, IOException {
        return getPrismContext().parserFor(USER_JACK_FILE).parse();
    }
}
