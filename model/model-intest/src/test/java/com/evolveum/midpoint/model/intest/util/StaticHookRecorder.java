/*
 * Copyright (c) 2013 Evolveum
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
