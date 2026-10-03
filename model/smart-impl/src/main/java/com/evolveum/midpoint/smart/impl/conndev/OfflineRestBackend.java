package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevAuthInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline backend for REST/SCIM connectors. Instead of querying the generation service, it offers
 * the standard HTTP authorization catalog (the subset the framework supports) and generates
 * REST/SCIM-shaped YAML skeletons (see {@link OfflineArtifactGenerator}).
 */
public class OfflineRestBackend extends OfflineBackend {

    public OfflineRestBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache) {
        // No generation service to scrape the available schemes from: offer the standard catalog
        // of the supported HTTP authorization types, recommending the most common ones.
        var catalog = new ArrayList<ConnDevAuthInfoType>();
        for (var auth : SupportedAuthorization.values()) {
            if (auth == SupportedAuthorization.NONE || auth == SupportedAuthorization.OTHER) {
                continue;
            }
            var info = auth.crateBasicInformation();
            if (isRecommended(auth)) {
                info.setRecommended(true);
            }
            catalog.add(info);
        }
        return catalog;
    }

    private static boolean isRecommended(SupportedAuthorization auth) {
        return switch (auth) {
            case HTTP_BASIC, HTTP_BEARER, HTTP_APIKEY, OAUTH2_CLIENT_CREDENTIALS -> true;
            default -> false;
        };
    }
}
