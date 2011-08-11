/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.init;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.api.ModelService;

/**
 * 
 * @author lazyman
 * 
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

		OperationResult mainResult = new OperationResult("Model Post Initialisation");
		try {
			model.postInit(mainResult);
			LOGGER.info("Model post initialization finished successful.");
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Model post initialization failed", ex);
			mainResult.recordFatalError("Model post initialization failed", ex);
		} finally {
			mainResult.computeStatus();
		}
	}
}
