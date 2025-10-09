/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja;

import org.testng.annotations.Test;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListKeysTest implements NinjaTestMixin {

    @Test
    public void simpleListKeys() throws Exception {
        // todo assertions
        executeTest(null, null, "-m", getMidpointHome(), "keys");
    }
}
