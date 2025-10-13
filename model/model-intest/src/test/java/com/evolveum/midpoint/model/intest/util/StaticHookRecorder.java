/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.util;

import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 */
public class StaticHookRecorder {
    private static Map<String,Integer> hookInvocationCountMap = new HashMap<>();

    public static void record(String hookName) {
        Integer count = hookInvocationCountMap.get(hookName);
        if (count == null) {
            hookInvocationCountMap.put(hookName,1);
        } else {
            hookInvocationCountMap.put(hookName,count + 1);
        }
    }

    public static void assertInvocationCount(String hookName, int expectedCount) {
        assertEquals("Wrong invocation count of hook '"+hookName+"'", (Integer)expectedCount, hookInvocationCountMap.get(hookName));
    }

    public static void reset() {
        hookInvocationCountMap = new HashMap<>();
    }

    public static String dump() {
        StringBuilder sb = new StringBuilder("StaticHookRecorder");
        for (Map.Entry<String,Integer> entry: hookInvocationCountMap.entrySet()) {
            sb.append("\n");
            sb.append(entry.getKey());
            sb.append(" -> ");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
}
