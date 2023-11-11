/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages")
public class KeepassProviderOptions {

    public static final String P_KEEPASS_FILE_LONG = "--keepass-file";
    public static final String P_KEEPASS_PASSWORD_LONG = "--keepass-password";
    public static final String P_KEEPASS_ASK_PASSWORD_LONG = "--keepass-password-ask";

    @Parameter(names = { P_KEEPASS_FILE_LONG }, descriptionKey = "keepass.file")
    private File file;

    @Parameter(names = { P_KEEPASS_PASSWORD_LONG }, descriptionKey = "keepass.password")
    private String password;

    @Parameter(names = { P_KEEPASS_ASK_PASSWORD_LONG }, password = true, descriptionKey = "keepass.askPassword")
    private String askPassword;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAskPassword() {
        return askPassword;
    }

    public void setAskPassword(String askPassword) {
        this.askPassword = askPassword;
    }

    public String getKeepassPassword() {
        return askPassword != null ? askPassword : password;
    }
}
