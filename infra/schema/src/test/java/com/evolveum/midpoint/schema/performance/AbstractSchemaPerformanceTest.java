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
package com.evolveum.midpoint.schema.performance;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.CheckedProducer;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

/**
 *
 */
public class AbstractSchemaPerformanceTest {

	protected static final Trace LOGGER = TraceManager.getTrace(AbstractSchemaPerformanceTest.class);

	public static final File TEST_DIR = new File("src/test/resources/performance");
	public static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");

	public static final int DEFAULT_EXECUTION = 10000;
	protected static final String NS_FOO = "http://www.example.com/foo";

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	protected int measure(String label, CheckedProducer<?> producer) throws CommonException {
		return measure(label, producer, DEFAULT_EXECUTION);
	}

	protected int measure(String label, CheckedProducer<?> producer, long executionTime) throws CommonException {
		long until = System.currentTimeMillis() + executionTime;
		int iteration = 0;
		while (System.currentTimeMillis() < until) {
			if (producer.get() == null) {
				// just to make sure the result is used somehow (and not optimized away)
				throw new IllegalStateException("null result from the producer");
			}
			iteration++;
		}
		String message = label + ": " + iteration + " iterations in " + executionTime + " milliseconds (" +
				((double) executionTime) * 1000 / (double) iteration + " us per iteration)";
		System.out.println(message);
		LOGGER.info(message);
		return iteration;
	}

	@NotNull
	public PrismObject<UserType> getJack() throws SchemaException, IOException {
		return getPrismContext().parserFor(USER_JACK_FILE).parse();
	}
}
