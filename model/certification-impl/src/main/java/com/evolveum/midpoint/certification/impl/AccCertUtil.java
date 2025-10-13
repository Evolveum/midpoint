/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@SuppressWarnings("WeakerAccess")
public class AccCertUtil {

    public static int normalizeIteration(Integer iteration) {
        return defaultIfNull(iteration, 1);
    }
}
