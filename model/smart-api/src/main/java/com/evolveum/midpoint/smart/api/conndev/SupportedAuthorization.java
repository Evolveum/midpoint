package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;

import java.util.Arrays;
import java.util.List;

public enum SupportedAuthorization {
    NONE( null, new ConnDevAuthInfoType()
            .name("No authorization")),
    HTTP_BASIC(ConnDevHttpAuthTypeType.BASIC, new ConnDevAuthInfoType()
            .name("HTTP Basic Authorization")
            .description("Basic authorization using username and password")
            , "Username", "Password"),
    HTTP_BEARER(ConnDevHttpAuthTypeType.BEARER, new ConnDevAuthInfoType()
            .name("HTTP Bearer Token Authorization")
            .description("Authorization using Bearer token.")
            , "TokenName", "TokenValue"),
    HTTP_APIKEY(ConnDevHttpAuthTypeType.API_KEY, new ConnDevAuthInfoType()
            .name("HTTP API Key Authorization")
            .description("Authorization using preshared API key"), "ApiKey");

    private final List<ItemName.WithoutPrefix> scimProperties;
    private final List<ItemName.WithoutPrefix> restProperties;

    SupportedAuthorization(ConnDevHttpAuthTypeType type, ConnDevAuthInfoType info, String... properties) {
        baseAuthInfo = info
                .type(type);
        scimProperties = Arrays.stream(properties).map(p -> ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "scim" + p)).toList();
        restProperties = Arrays.stream(properties).map(p -> ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "rest" + p)).toList();
    }

    private final ConnDevAuthInfoType baseAuthInfo;

    public static ConnDevAuthInfoType fromAiType(String type) {
        return switch (type) {
            case "basic" -> HTTP_BASIC.crateBasicInformation();
            case "bearer" -> HTTP_BEARER.crateBasicInformation();
            case "apikey" -> HTTP_APIKEY.crateBasicInformation();
            default -> null;
        };
    }

    public ConnDevAuthInfoType crateBasicInformation() {
        return baseAuthInfo.clone();
    }

    public static List<ItemName> attributesFor(ConnDevIntegrationType integration, ConnDevHttpAuthTypeType authorizationType) {
        var auth = forAuthorizationType(authorizationType);
        if (auth == null) {
            return List.of();
        }
        return auth.attributesFor(integration);
    }

    public List<ItemName> attributesFor(ConnDevIntegrationType integration) {
        return (List) switch (integration) {
            case SCIM -> scimProperties;
            case DUMMY, REST -> restProperties;
        };
    }

    public static SupportedAuthorization forAuthorizationType(ConnDevHttpAuthTypeType authorizationType) {
        return Arrays.stream(SupportedAuthorization.values()).filter(v -> v.baseAuthInfo.getType() != null && v.baseAuthInfo.getType().equals(authorizationType))
                .findFirst().orElse(null);
    }
}
