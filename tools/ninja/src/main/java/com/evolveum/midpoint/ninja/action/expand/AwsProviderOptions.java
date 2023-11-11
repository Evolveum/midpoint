/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages")
public class AwsProviderOptions {

    public static final String P_CACHE_VALUES_LONG = "--aws-cache-values";

    public static final String P_TIMEOUT = "--aws-timeout";

    @Parameter(names = { P_CACHE_VALUES_LONG }, descriptionKey = "aws.cacheValues")
    private boolean cacheValues = true;

    @Parameter(names = { P_TIMEOUT }, descriptionKey = "aws.timeout")
    private int timeout = 30;

    public boolean isCacheValues() {
        return cacheValues;
    }

    public void setCacheValues(boolean cacheValues) {
        this.cacheValues = cacheValues;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
