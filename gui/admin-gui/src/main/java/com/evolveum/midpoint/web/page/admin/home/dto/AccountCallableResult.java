/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
