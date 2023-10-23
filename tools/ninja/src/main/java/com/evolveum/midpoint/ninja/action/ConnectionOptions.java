/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.util.URIConverter;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages")
public class ConnectionOptions {

    public static final String P_URL = "-U";
    public static final String P_URL_LONG = "--url";

    public static final String P_USERNAME = "-u";
    public static final String P_USERNAME_LONG = "--username";

    public static final String P_PASSWORD = "-p";
    public static final String P_PASSWORD_LONG = "--password";

    public static final String P_ASK_PASSWORD = "-P";
    public static final String P_ASK_PASSWORD_LONG = "--password-ask";

    public static final String P_MIDPOINT_HOME = "-m";
    public static final String P_MIDPOINT_HOME_LONG = "--midpoint-home";

    public static final String P_JDBC = "-j";
    public static final String P_JDBC_LONG = "--jdbc";

    /**
     * This option is not directly used by ninja code, however command parser needs it to be defined.
     * Otherwise, command parser will throw an exception because of unknown parameter.
     */
    @Parameter(names = { P_JDBC, P_JDBC_LONG }, descriptionKey = "connection.jdbc", hidden = true)
    private File jdbc;

    @Parameter(names = { P_URL, P_URL_LONG }, validateWith = URIConverter.class, descriptionKey = "connection.url")
    private String url;

    @Parameter(names = { P_USERNAME, P_USERNAME_LONG }, descriptionKey = "connection.username")
    private String username;

    @Parameter(names = { P_PASSWORD, P_PASSWORD_LONG }, descriptionKey = "connection.password")
    private String password;

    @Parameter(names = { P_ASK_PASSWORD, P_ASK_PASSWORD_LONG }, password = true,
            descriptionKey = "connection.askPassword")
    private String askPassword;

    @Parameter(names = { P_MIDPOINT_HOME, P_MIDPOINT_HOME_LONG }, descriptionKey = "connection.midpointHome")
    private String midpointHome;

    public String getAskPassword() {
        return askPassword;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getMidpointHome() {
        return midpointHome;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAskPassword(String askPassword) {
        this.askPassword = askPassword;
    }

    public void setMidpointHome(String midpointHome) {
        this.midpointHome = midpointHome;
    }

    public File jdbc() {
        return jdbc;
    }

    public void setJdbc(File jdbc) {
        this.jdbc = jdbc;
    }
}
