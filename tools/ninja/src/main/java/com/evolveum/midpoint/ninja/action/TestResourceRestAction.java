/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.ninja.impl.RestService;
import com.evolveum.midpoint.ninja.opts.TestResourceOptions;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TestResourceRestAction extends RestAction<TestResourceOptions> {

    @Override
    public void execute() {
        RestService model = context.getRestService();

        OperationResult result = model.testResource(options.getOid());
        //todo print result
    }
}
