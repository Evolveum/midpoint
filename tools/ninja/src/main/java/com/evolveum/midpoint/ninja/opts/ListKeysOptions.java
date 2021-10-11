/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.opts;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "listKeys")
public class ListKeysOptions {

    public static final String P_KEY_PASSWORD = "-k";
    public static final String P_KEY_PASSWORD_LONG = "--key-password";

    public static final String P_KEY_ASK_PASSWORD = "-K";
    public static final String P_KEY_ASK_PASSWORD_LONG = "--key-password-ask";

    @Parameter(names = {P_KEY_PASSWORD, P_KEY_PASSWORD_LONG}, descriptionKey = "listKeys.keyPassword")
    private String keyPassword;

    @Parameter(names = {P_KEY_ASK_PASSWORD}, password = true,
            descriptionKey = "listKeys.askKeyPassword")
    private String askKeyPassword;

    public String getKeyPassword() {
        return keyPassword;
    }

    public String getAskKeyPassword() {
        return askKeyPassword;
    }
}
