package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class ScimRestConfigurationProperties {

    public static final ItemName REST_TOKEN_NAME = new ItemName("restTokenName");
    public static final ItemName REST_TOKEN_VALUE = new ItemName("restTokenValue");
    public static final ItemName BASE_ADDRESS =  new ItemName("baseAddress");
    public static final ItemName TRUST_ALL_CERTIFICATES =  new ItemName("trustAllCertificates");
    public static final ItemName SCIM_BEARER_TOKEN =  new ItemName("scimBearerToken");
    public static final ItemName SCIM_BASE_URL = new ItemName("scimBaseUrl");
    public static final ItemName REST_PASSWORD = new ItemName("restPassword");
    public static final ItemName REST_USERNAME = new ItemName("restUsername");
    public static final ItemName REST_API_KEY = new ItemName("restApiKey");


}
