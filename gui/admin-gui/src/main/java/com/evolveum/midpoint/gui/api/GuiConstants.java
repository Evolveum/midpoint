/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

public class GuiConstants {

    public static final String NS_UI_PREFIX = SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX + "ui/";
    public static final String NS_UI_FEATURE = NS_UI_PREFIX + "feature";

    public static final String DEFAULT_PATH_AFTER_LOGIN = "/self/dashboard";
    public static final String DEFAULT_PATH_AFTER_LOGOUT = "/";
}
