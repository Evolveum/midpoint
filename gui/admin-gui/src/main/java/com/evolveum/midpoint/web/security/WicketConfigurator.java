/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import org.apache.wicket.Application;

public interface WicketConfigurator {
    void configure(Application application);
}
