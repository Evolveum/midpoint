/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

public abstract class AbstractQueryWrapper implements QueryWrapper {

    public static final String F_ERROR = "advancedError";
    private String advancedError;
    private String serializedQuery;

    @Override
    public String getAdvancedError() {
        return advancedError;
    }

    @Override
    public void setAdvancedError(String advancedError) {
        this.advancedError = advancedError;
    }
}
