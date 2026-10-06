/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EmptyCredentialsPolicyType;

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

    /**
     * Effective policy for a user without credentials required by the module. Deprecated acceptEmpty=true
     * is treated as {@link EmptyCredentialsPolicyType#SKIP}.
     *
     * @return null if nothing special should happen, i.e. the module is evaluated and authentication fails
     */
    public static EmptyCredentialsPolicyType getEmptyCredentialsPolicy(AuthenticationSequenceModuleType module) {
        if (module == null) {
            return null;
        }
        if (module.getEmptyCredentialsPolicy() != null) {
            return module.getEmptyCredentialsPolicy();
        }
        return Boolean.TRUE.equals(module.isAcceptEmpty()) ? EmptyCredentialsPolicyType.SKIP : null;
    }

    private static AuthenticationSequenceChannelType getChannel(AuthenticationSequenceType sequence) {
        if (sequence == null) {
            return null;
        }
        return sequence.getChannel();
    }
}
