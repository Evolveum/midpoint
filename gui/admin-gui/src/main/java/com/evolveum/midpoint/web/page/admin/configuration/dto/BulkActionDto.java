/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class BulkActionDto implements Serializable {

    public static final String F_SCRIPT = "script";
    public static final String F_ASYNC = "async";

    private String script;
    private boolean async = true;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
