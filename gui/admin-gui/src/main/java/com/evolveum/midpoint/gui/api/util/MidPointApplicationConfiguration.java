/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface MidPointApplicationConfiguration {

    void init(MidPointApplication application);
}
