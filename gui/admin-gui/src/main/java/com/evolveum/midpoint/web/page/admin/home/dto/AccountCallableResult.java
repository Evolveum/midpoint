/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.CallableResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AccountCallableResult<T> extends CallableResult<T> {

    private List<OperationResult> fetchResults;

    public List<OperationResult> getFetchResults() {
        if (fetchResults == null) {
            fetchResults = new ArrayList<>();
        }
        return fetchResults;
    }
}
