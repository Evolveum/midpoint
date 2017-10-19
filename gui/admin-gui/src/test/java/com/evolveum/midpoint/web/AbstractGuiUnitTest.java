/*
 * Copyright (c) 2016-2017 Evolveum
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

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;

import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

/**
 * @author lazyman
 */
public abstract class AbstractGuiUnitTest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractGuiUnitTest.class);

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    protected ModelServiceLocator getServiceLocator() {
		return new ModelServiceLocator() {

			@Override
			public ModelService getModelService() {
				return null;
			}

			@Override
			public ModelInteractionService getModelInteractionService() {
				return null;
			}

			@Override
			public Task createSimpleTask(String operationName) {
				return null;
			}

			@Override
			public PrismContext getPrismContext() {
				return PrismTestUtil.getPrismContext();
			}

			@Override
			public SecurityEnforcer getSecurityEnforcer() {
				return null;
			}
			
			@Override
			public SecurityContextManager getSecurityContextManager() {
				return null;
			}

			@Override
			public AdminGuiConfigurationType getAdminGuiConfiguration() {
				return null;
			}

			@Override
			public Task getPageTask() {
				return null;
			}

			@Override
			public ExpressionFactory getExpressionFactory() {
				return null;
			}
		};
	}

}
