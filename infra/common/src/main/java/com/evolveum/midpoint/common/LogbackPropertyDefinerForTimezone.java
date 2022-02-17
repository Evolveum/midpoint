/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.util.TimeZone;

public class LogbackPropertyDefinerForTimezone extends LogbackPropertyDefiner {

    @Override
    protected String getDefaultValue() {
        return TimeZone.getDefault().getID();
    }
}
