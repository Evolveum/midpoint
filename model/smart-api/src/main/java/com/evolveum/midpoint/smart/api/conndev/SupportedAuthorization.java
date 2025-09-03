package com.evolveum.midpoint.smart.api.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;

public enum SupportedAuthorization {
    NONE("none", "No authorization"),
    HTTP_BASIC("basic", "HTTP Basic Authorization"),
    HTTP_BEARER("bearer", "HTTP Bearer Token Authorization");

    SupportedAuthorization(String type, String name) {
        baseAuthInfo = new ConnDevAuthInfoType()
                .type(type)
                .name(name);
    }

    private final ConnDevAuthInfoType baseAuthInfo;

    public ConnDevAuthInfoType crateBasicInformation() {
        return baseAuthInfo.clone();
    }
}
