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

        executeTest(input, null, null);
    }
}
