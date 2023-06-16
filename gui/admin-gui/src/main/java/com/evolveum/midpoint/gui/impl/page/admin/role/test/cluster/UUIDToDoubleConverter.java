/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.test.cluster;

import java.util.UUID;

public class UUIDToDoubleConverter {

    public static double convertUUid(String oid) {

        UUID uuid = UUID.fromString(oid);

        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();

        long combinedBits = mostSignificantBits ^ leastSignificantBits;

        return (double) (combinedBits & 0x000fffffffffffffL) / (1L << 52);
    }
}
