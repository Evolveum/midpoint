/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.handlers;

import com.evolveum.midpoint.certification.api.AccessCertificationApiConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a correct handler for a given handler URI.
 *
 * Very primitive implementation (for now).
 */
@Component
public class CertGuiHandlerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(CertGuiHandlerRegistry.class);

    private Map<String, CertGuiHandler> handlers = new ConcurrentHashMap<>();

    public void registerCertGuiHandler(String uri, CertGuiHandler handler) {
        LOGGER.trace("Registering cert gui handler {} for {}", handler, uri);
        handlers.put(uri, handler);
    }

    public CertGuiHandler getHandler(String uri) {
        if (uri == null) {
            return null;
        }

        CertGuiHandler certGuiHandler = handlers.get(uri);
        if (certGuiHandler == null) {
            throw new IllegalArgumentException("Unknown handler URI: " + uri);
        }

        return certGuiHandler;
    }

    @SuppressWarnings("unused")
    private void doNothing() {
        // no nothing. Just for maven dependency analyze to properly detect the dependency.
        AccessCertificationApiConstants.noop();
    }
}
