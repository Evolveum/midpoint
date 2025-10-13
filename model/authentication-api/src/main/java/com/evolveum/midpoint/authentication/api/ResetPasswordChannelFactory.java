/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * not use it, temporary needed interface for old reset password configuration
 */
@Experimental
public interface ResetPasswordChannelFactory {

    AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel);
}
