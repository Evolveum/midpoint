/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.UUID;

/**
 * @author semancik
 *
 */
public class OidUtil {

    public static String generateOid() {
        return UUID.randomUUID().toString();
    }

}
