/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

/**
 *  Very ugly hack. Assumes single-threaded tests.
 */
public class TestNameHolder {

    private static String currentTestName;

    public static String getCurrentTestName() {
        return currentTestName;
    }

    public static void setCurrentTestName(String currentTestName) {
        TestNameHolder.currentTestName = currentTestName;
    }
}
