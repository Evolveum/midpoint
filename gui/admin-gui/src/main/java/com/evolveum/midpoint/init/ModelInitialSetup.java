/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

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
            mainResult.recordFatalError("Model post initialization failed.", ex);
        } finally {
            mainResult.computeStatus("Model post initialization failed.");
        }
    }
}
