/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import org.testng.annotations.Test;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListKeysTest extends BaseTest {

    @Test
    public void simpleListKeys() throws Exception {
        //todo asserts

        String[] input = new String[]{"-m", getMidpointHome(), "keys"};

        executeTest(null, null, input);
    }
}
