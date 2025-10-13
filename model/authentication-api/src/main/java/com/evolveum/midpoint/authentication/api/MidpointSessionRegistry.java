/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import java.util.List;

public interface MidpointSessionRegistry extends SessionRegistry {

    List<SessionInformation> getLoggedInUsersSession(Object principal);

}
