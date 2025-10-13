/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class AccessCertificationApiConstants {

    public static final String NS_HANDLERS_PREFIX = SchemaConstants.NS_CERTIFICATION + "/handlers-3";
    public static final String DIRECT_ASSIGNMENT_HANDLER_URI = AccessCertificationApiConstants.NS_HANDLERS_PREFIX + "#direct-assignment";
    public static final String EXCLUSION_HANDLER_URI = AccessCertificationApiConstants.NS_HANDLERS_PREFIX + "#exclusion";

    public static void noop() {
        // no nothing. Just for maven dependency analyze to properly detect the dependency.
    }
}
