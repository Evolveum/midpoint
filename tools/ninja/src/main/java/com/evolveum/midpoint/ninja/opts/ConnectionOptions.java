package com.evolveum.midpoint.ninja.opts;

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

    public static final String P_WEBSERVICE = "-w";
    public static final String P_WEBSERVICE_LONG = "--webservice";


    @Parameter(names = {P_URL, P_URL_LONG}, validateWith = URIConverter.class, descriptionKey = "connection.url")
    private String url;

    @Parameter(names = {P_USERNAME, P_USERNAME_LONG}, descriptionKey = "connection.username")
    private String username;

    @Parameter(names = {P_PASSWORD, P_PASSWORD_LONG}, descriptionKey = "connection.password")
    private String password;

    @Parameter(names = {P_ASK_PASSWORD, P_ASK_PASSWORD_LONG}, password = true, echoInput = true,
            descriptionKey = "connection.askPassword")
    private String askPassword;

    @Parameter(names = {P_MIDPOINT_HOME, P_MIDPOINT_HOME_LONG}, descriptionKey = "connection.midpointHome")
    private String midpointHome;

    @Parameter(names = {P_WEBSERVICE, P_WEBSERVICE_LONG}, descriptionKey = "connection.useWebservice")
    private boolean useWebservice;

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

    public boolean isUseWebservice() {
        return useWebservice;
    }
}
