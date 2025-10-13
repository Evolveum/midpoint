/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;

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
