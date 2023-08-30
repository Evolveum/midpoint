/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.duosecurity.Client;

import com.duosecurity.exception.DuoException;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.module.authentication.RemoteModuleAuthenticationImpl;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil.getBasePath;

/**
 * @author skublik
 */

public class DuoModuleWebSecurityConfiguration extends RemoteModuleWebSecurityConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(DuoModuleWebSecurityConfiguration.class);

    private Client duoClient;

    private DuoModuleWebSecurityConfiguration() {
    }

    public static DuoModuleWebSecurityConfiguration build(DuoAuthenticationModuleType modelType, String prefixOfSequence,
                                                          String publicHttpUrlPattern, ServletRequest request) {
        DuoModuleWebSecurityConfiguration configuration = buildInternal(modelType, prefixOfSequence, publicHttpUrlPattern, request);
        configuration.validate();
        return configuration;
    }

    private static DuoModuleWebSecurityConfiguration buildInternal(DuoAuthenticationModuleType duoModule, String prefixOfSequence,
                                                                   String publicHttpUrlPattern, ServletRequest request) {
        DuoModuleWebSecurityConfiguration configuration = new DuoModuleWebSecurityConfiguration();
        build(configuration, duoModule, prefixOfSequence);

        UriComponentsBuilder redirectUri = UriComponentsBuilder.fromUriString(
                StringUtils.isNotBlank(publicHttpUrlPattern) ? publicHttpUrlPattern : getBasePath((HttpServletRequest) request));
        redirectUri.pathSegment(
                DEFAULT_PREFIX_OF_MODULE,
                AuthUtil.stripSlashes(prefixOfSequence),
                AuthUtil.stripSlashes(getAuthenticationModuleIdentifier(duoModule)),
                AuthUtil.stripSlashes(RemoteModuleAuthenticationImpl.AUTHENTICATION_REQUEST_PROCESSING_URL_SUFFIX));

        try {
            Client.Builder builder = new Client.Builder(
                    duoModule.getClientId(),
                    protector.decryptString(duoModule.getClientSecret()),
                    duoModule.getApiHostname(),
                    redirectUri.toUriString());

            List<String> certs = duoModule.getCACerts();
            if (!certs.isEmpty()) {
                builder.setCACerts(certs.toArray(new String[0]));
            }

            configuration.duoClient = builder.build();
        } catch (EncryptionException e) {
            LOGGER.error("Couldn't obtain clear string for client secret", e);
        } catch (DuoException e) {
            LOGGER.error("Couldn't build duo client", e);
        }

        return configuration;
    }

    public Client getDuoClient() {
        return duoClient;
    }

    @Override
    protected void validate() {
        super.validate();
        if (duoClient == null) {
            throw new IllegalArgumentException("Duo client is null");
        }
    }

    public String getPrefixOfSequence() {
        return DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + AuthUtil.stripSlashes(getSequenceSuffix());
    }
}
