/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

import org.apache.commons.lang3.StringUtils;

public class AuthenticationSequenceTypeUtil {

    public static boolean isDefaultChannel(AuthenticationSequenceType sequence) {
        AuthenticationSequenceChannelType channel = getChannel(sequence);
        if (channel == null) {
            return false;
        }
        return Boolean.TRUE.equals(channel.isDefault());
    }

    public static boolean hasChannelId(AuthenticationSequenceType sequence, String channelId) {
        AuthenticationSequenceChannelType channel = getChannel(sequence);
        if (channel == null) {
            return false;
        }
        return channel.getChannelId().equals(channelId);
    }

    public static String getSequenceDisplayName(AuthenticationSequenceType sequence) {
        return sequence.getDisplayName() != null ? sequence.getDisplayName() : getSequenceIdentifier(sequence);
    }

    public static String getSequenceIdentifier(AuthenticationSequenceType sequence) {
        return StringUtils.isNotEmpty(sequence.getIdentifier()) ? sequence.getIdentifier() : sequence.getName();
    }

    private static AuthenticationSequenceChannelType getChannel(AuthenticationSequenceType sequence) {
        if (sequence == null) {
            return null;
        }
        return sequence.getChannel();
    }
}
