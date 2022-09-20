/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.Validate;

/**
 * @author lazyman
 */
public class ProvisioningInitialSetup {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningInitialSetup.class);

    private ProvisioningService provisioning;

    public void setProvisioning(ProvisioningService provisioning) {
        Validate.notNull(provisioning, "Provisioning service must not be null.");
        this.provisioning = provisioning;
    }

    public void init() {
        LOGGER.info("Provisioning post initialization.");

        OperationResult mainResult = new OperationResult("Provisioning Post Initialization");
        try {
            provisioning.postInit(mainResult);

            if (mainResult.isUnknown()) {
                mainResult.computeStatus();
            }

            LOGGER.info("Provisioning post initialization finished successfully.");
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Provisioning post initialization failed", ex);
            mainResult.recordFatalError("ProvisioningInitialSetup.message.init.fatalError", ex);
        } finally {
            mainResult.computeStatus("ProvisioningInitialSetup.message.init.fatalError");
        }
    }
}
