/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CountRepositoryAction extends RepositoryAction<CountOptions, Void> {

    private static final String DOT_CLASS = CountRepositoryAction.class.getName() + ".";

    private static final String OPERATION_COUNT = DOT_CLASS + "count";

    @Override
    public String getOperationName() {
        return "count objects";
    }

    @Override
    public LogTarget getLogTarget() {
        return LogTarget.SYSTEM_ERR;
    }

    @Override
    public Void execute() throws Exception {
        RepositoryService repository = context.getRepository();

        FileReference fileReference = options.getFilter();
        if (fileReference != null && options.getFilter() == null) {
            throw new NinjaException("Type must be defined");
        }

        List<ObjectTypes> types = NinjaUtils.getTypes(options.getType(), List.of(ObjectTypes.values()));

        int total = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT);
        for (ObjectTypes type : types) {
            ObjectQuery query = NinjaUtils.createObjectQuery(options.getFilter(), context, type.getClassDefinition());
            int count = repository.countObjects(type.getClassDefinition(), query, new ArrayList<>(), result);
            if (count == 0 && options.getType() == null) {
                continue;
            }
            log.info("{}:\t{}", type.name(), count);

            total += count;
        }

        log.info("===\nTotal:\t{}", total);

        return null;
    }
}
