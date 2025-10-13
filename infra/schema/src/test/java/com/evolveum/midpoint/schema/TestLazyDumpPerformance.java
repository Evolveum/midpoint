/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class TestLazyDumpPerformance extends AbstractSchemaTest {

    private static final long ITERATIONS = 4000000;

    @Test
    public void toShortStringLazyPerformance() {
        UserType user = new UserType(PrismTestUtil.getPrismContext()).name("jack");
        for (long i = 0; i < 100000; i++) {     // warm-up
            logger.trace("{}", ObjectTypeUtil.toShortStringLazy(user));
        }

        long startLazy = System.currentTimeMillis();
        for (long i = 0; i < ITERATIONS; i++) {
            logger.trace("{}", ObjectTypeUtil.toShortStringLazy(user));
        }
        long lazy = System.currentTimeMillis() - startLazy;
        System.out.println("Lazy: " + lazy + " ms = " + (lazy * 1000000) / ITERATIONS + " ns per iteration");

        long startNormal = System.currentTimeMillis();
        for (long i = 0; i < ITERATIONS; i++) {
            logger.trace("{}", user);
        }
        long normal = System.currentTimeMillis() - startNormal;
        System.out.println("Normal: " + normal + " ms = " + (normal * 1000000) / ITERATIONS + " ns per iteration");

        long startOptimized = System.currentTimeMillis();
        for (long i = 0; i < ITERATIONS; i++) {
            if (logger.isTraceEnabled()) {
                logger.trace("{}", ObjectTypeUtil.toShortString(user));
            }
        }
        long optimized = System.currentTimeMillis() - startOptimized;
        System.out.println("Optimized: " + optimized + " ms = " + (optimized * 1000000) / ITERATIONS + " ns per iteration");

        long startNaive = System.currentTimeMillis();
        for (long i = 0; i < ITERATIONS; i++) {
            logger.trace("{}", ObjectTypeUtil.toShortString(user));
        }
        long naive = System.currentTimeMillis() - startNaive;
        System.out.println("Naive: " + naive + " ms = " + (naive * 1000000) / ITERATIONS + " ns per iteration");
    }
}
