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
        RestService model = context.getModel();

        OperationResult result = model.testResource(options.getOid());
        //todo print result
    }
}
