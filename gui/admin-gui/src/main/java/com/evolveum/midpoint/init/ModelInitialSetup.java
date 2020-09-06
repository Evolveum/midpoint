/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class ModelInitialSetup {

    private static final Trace LOGGER = TraceManager.getTrace(ModelInitialSetup.class);

    private ModelService model;

    public void setModel(ModelService model) {
        Validate.notNull(model, "Model service must not be null.");
        this.model = model;
    }

    public void init() {
        LOGGER.info("Model post initialization.");

        OperationResult mainResult = new OperationResult("Model Post Initialization");
        try {
            model.postInit(mainResult);
            LOGGER.info("Model post initialization finished successfully.");
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Model post initialization failed", ex);
            mainResult.recordFatalError("ModelInitialSetup.message.init.fatalError", ex);
        } finally {
            mainResult.computeStatus("ModelInitialSetup.message.init.fatalError");
        }
    }
}
