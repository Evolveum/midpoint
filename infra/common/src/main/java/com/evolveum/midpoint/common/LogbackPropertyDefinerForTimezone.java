/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common;

import java.util.TimeZone;

public class LogbackPropertyDefinerForTimezone extends LogbackPropertyDefiner {

    @Override
    protected String getDefaultValue() {
        return TimeZone.getDefault().getID();
    }
}
