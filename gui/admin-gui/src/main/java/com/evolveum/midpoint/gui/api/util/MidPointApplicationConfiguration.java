/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface MidPointApplicationConfiguration {

    void init(MidPointApplication application);
}
