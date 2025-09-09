package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevHttpAuthTypeType;

public enum SupportedAuthorization {
    NONE(null, "No authorization"),
    HTTP_BASIC(ConnDevHttpAuthTypeType.BASIC, "HTTP Basic Authorization"),
    HTTP_BEARER(ConnDevHttpAuthTypeType.BEARER, "HTTP Bearer Token Authorization"),
    HTTP_APIKEY(ConnDevHttpAuthTypeType.API_KEY, "HTTP API Key Authorization");

    SupportedAuthorization(ConnDevHttpAuthTypeType type, String name) {
        baseAuthInfo = new ConnDevAuthInfoType()
                .type(type)
                .name(name);
    }

    private final ConnDevAuthInfoType baseAuthInfo;

    public ConnDevAuthInfoType crateBasicInformation() {
        return baseAuthInfo.clone();
    }
}
