/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
