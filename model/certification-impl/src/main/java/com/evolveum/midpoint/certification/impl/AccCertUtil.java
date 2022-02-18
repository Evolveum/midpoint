/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@SuppressWarnings("WeakerAccess")
public class AccCertUtil {

    public static int normalizeIteration(Integer iteration) {
        return defaultIfNull(iteration, 1);
    }
}
