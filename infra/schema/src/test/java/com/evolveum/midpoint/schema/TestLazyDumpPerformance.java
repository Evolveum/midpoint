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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author mederly
 */
public class TestLazyDumpPerformance {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestLazyDumpPerformance.class);

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	private static final long ITERATIONS = 4000000;

	@Test
	public void toShortStringLazyPerformance() {

		UserType user = new UserType(PrismTestUtil.getPrismContext()).name("jack");
		for (long i = 0; i < 100000; i++) {     // warm-up
			LOGGER.trace("{}", ObjectTypeUtil.toShortStringLazy(user));
		}

		long startLazy = System.currentTimeMillis();
		for (long i = 0; i < ITERATIONS; i++) {
			LOGGER.trace("{}", ObjectTypeUtil.toShortStringLazy(user));
		}
		long lazy = System.currentTimeMillis() - startLazy;
		System.out.println("Lazy: " + lazy + " ms = " + (lazy * 1000000) / ITERATIONS + " ns per iteration");

		long startNormal = System.currentTimeMillis();
		for (long i = 0; i < ITERATIONS; i++) {
			LOGGER.trace("{}", user);
		}
		long normal = System.currentTimeMillis() - startNormal;
		System.out.println("Normal: " + normal + " ms = " + (normal * 1000000) / ITERATIONS + " ns per iteration");

		long startOptimized = System.currentTimeMillis();
		for (long i = 0; i < ITERATIONS; i++) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{}", ObjectTypeUtil.toShortString(user));
			}
		}
		long optimized = System.currentTimeMillis() - startOptimized;
		System.out.println("Optimized: " + optimized + " ms = " + (optimized * 1000000) / ITERATIONS + " ns per iteration");

		long startNaive = System.currentTimeMillis();
		for (long i = 0; i < ITERATIONS; i++) {
			LOGGER.trace("{}", ObjectTypeUtil.toShortString(user));
		}
		long naive = System.currentTimeMillis() - startNaive;
		System.out.println("Naive: " + naive + " ms = " + (naive * 1000000) / ITERATIONS + " ns per iteration");
	}
}
