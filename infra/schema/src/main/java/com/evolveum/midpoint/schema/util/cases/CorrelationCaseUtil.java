/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.QNameUtil.uriToQName;

public class CorrelationCaseUtil {

    public static boolean isNewOwner(@NotNull String outcomeUri) {
        return SchemaConstants.CORRELATION_NONE.equals(
                getLocalPart(outcomeUri));
    }

    private static String getLocalPart(@NotNull String outcomeUri) {
        return uriToQName(outcomeUri, true)
                .getLocalPart();
    }

    public static String getExistingOwnerId(@NotNull String outcomeUri) {
        String localPart = getLocalPart(outcomeUri);
        if (localPart.startsWith(SchemaConstants.CORRELATION_OPTION_PREFIX)) {
            return localPart.substring(SchemaConstants.CORRELATION_OPTION_PREFIX.length());
        } else {
            return null;
        }
    }
}
