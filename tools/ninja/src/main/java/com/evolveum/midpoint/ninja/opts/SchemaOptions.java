package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescription = "schema")
public class SchemaOptions {

    public static final String P_TEST = "-t";
    public static final String P_TEST_LONG = "--test";

    public static final String P_INIT = "-i";
    public static final String P_INIT_LONG = "--init";

    @Parameter(names = {P_TEST, P_TEST_LONG}, descriptionKey = "schema.test")
    private boolean test = true;

    @Parameter(names = {P_INIT, P_INIT_LONG}, descriptionKey = "schema.init")
    private boolean init;

    public boolean isTest() {
        return test;
    }

    public boolean isInit() {
        return init;
    }
}
