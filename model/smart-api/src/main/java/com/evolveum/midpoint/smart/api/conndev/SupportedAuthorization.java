package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;

import java.util.Arrays;
import java.util.List;

public enum SupportedAuthorization {
    NONE(null, "No authorization"),
    HTTP_BASIC(ConnDevHttpAuthTypeType.BASIC, "HTTP Basic Authorization", "Username", "Password"),
    HTTP_BEARER(ConnDevHttpAuthTypeType.BEARER, "HTTP Bearer Token Authorization", "TokenName", "TokenValue"),
    HTTP_APIKEY(ConnDevHttpAuthTypeType.API_KEY, "HTTP API Key Authorization", "ApiKey");

    private final List<ItemName.WithoutPrefix> scimProperties;
    private final List<ItemName.WithoutPrefix> restProperties;

    SupportedAuthorization(ConnDevHttpAuthTypeType type, String name, String... properties) {
        baseAuthInfo = new ConnDevAuthInfoType()
                .type(type)
                .name(name);
        scimProperties = Arrays.stream(properties).map(p -> ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "scim" + p)).toList();
        restProperties = Arrays.stream(properties).map(p -> ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "rest" + p)).toList();
    }

    private final ConnDevAuthInfoType baseAuthInfo;

    public ConnDevAuthInfoType crateBasicInformation() {
        return baseAuthInfo.clone();
    }

    public static List<ItemName> attributesFor(ConnDevIntegrationType integration, ConnDevHttpAuthTypeType authorizationType) {
        var auth = forAuthorizationType(authorizationType);
        if (auth == null) {
            return List.of();
        }
        return (List) switch (integration) {
            case SCIM -> auth.scimProperties;
            case DUMMY, REST -> auth.restProperties;
        };
    }

    private static SupportedAuthorization forAuthorizationType(ConnDevHttpAuthTypeType authorizationType) {
        return Arrays.stream(SupportedAuthorization.values()).filter(v -> v.baseAuthInfo.getType().equals(authorizationType))
                .findFirst().orElse(null);
    }
}
