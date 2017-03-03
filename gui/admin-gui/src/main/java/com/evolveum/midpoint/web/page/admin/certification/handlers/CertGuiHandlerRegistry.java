/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
