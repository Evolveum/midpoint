/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.handlers;

import com.evolveum.midpoint.certification.api.AccessCertificationApiConstants;

/**
 * Provides a correct handler for a given handler URI.
 *
 * Very primitive implementation (for now).
 *
 * @author mederly
 */
public class CertGuiHandlerRegistry {

    public static CertGuiHandlerRegistry instance() {
        return new CertGuiHandlerRegistry();         // TODO
    }

    public CertGuiHandler getHandler(String uri) {
        if (uri == null) {
            return null;
        }
        switch (uri) {
            case AccessCertificationApiConstants.DIRECT_ASSIGNMENT_HANDLER_URI:
                return new DirectAssignmentCertGuiHandler();
            case AccessCertificationApiConstants.EXCLUSION_HANDLER_URI:
                return new DirectAssignmentCertGuiHandler();        // TODO
            default:
                throw new IllegalArgumentException("Unknown handler URI: " + uri);
        }
    }

    @SuppressWarnings("unused")
    private void doNothing() {
        // no nothing. Just for maven dependency analyze to properly detect the dependency.
        AccessCertificationApiConstants.noop();
    }
}
