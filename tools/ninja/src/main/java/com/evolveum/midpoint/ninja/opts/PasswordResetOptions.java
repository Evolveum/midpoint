package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "passwordReset")
public class PasswordResetOptions {

    public static final String P_OID = "-o";
    public static final String P_OID_LONG = "--oid";

    @Parameter(names = {P_OID, P_OID_LONG}, descriptionKey = "passwordReset.oid")
    private String oid;
}