/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import org.testng.annotations.Test;

public class UpgradeObjectsFilesTest implements NinjaTestMixin {

    @Test
    public void test10UpgradeFiles() throws Exception {
        executeTest(null, null, "-v", "-m", getMidpointHome(), "upgrade-objects", "--file", "./target/files");
    }

    @Test
    public void testRunSql() throws Exception {
        executeTest(null, null, "-v", "run-sql", "--scripts", "non-existing.sql");
    }
}
