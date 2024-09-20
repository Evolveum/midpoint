/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator;

import com.evolveum.midpoint.ninja.action.RepositoryAction;
import com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Repository action for generating role mining data in the system, including importing initial objects and performing
 * mining-related tasks.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public class GeneratorMiningRepositoryAction extends RepositoryAction<GeneratorOptions, Void> {

    public static final String OPERATION_SHORT_NAME = "generateRbacData";
    public static final String OPERATION_NAME = GeneratorMiningRepositoryAction.class.getName() + "." + OPERATION_SHORT_NAME;

    @Override
    public String getOperationName() {
        return "generate rbac data";
    }

    @Override
    public Void execute() throws Exception {
        OperationResult result = new OperationResult(OPERATION_NAME);

        ImportAction initialObjects = new ImportAction(context, options, result);
        initialObjects.executeImport();
        return null;
    }

}
