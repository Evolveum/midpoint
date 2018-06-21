/**
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
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import org.jetbrains.annotations.NotNull;

/**
 * Interface that allows location of model and model-like services,
 * such as ModelService and ModelInteractionService.
 * Used by GUI components that need to interact with the midPoint IDM model,
 * especially for loading data.
 * Usually implemented by PageBase and similar "central" GUI classes.
 *
 * @author Radovan Semancik
 */
public interface ModelServiceLocator {

	ModelService getModelService();

	ModelInteractionService getModelInteractionService();

	Task createSimpleTask(String operationName);

	/**
	 * Returns a task, that is used to retrieve and render the entire content
	 * of the page. A single task is created to render the whole page, so
	 * the summary result can be collected in the task result.
	 */
	Task getPageTask();

	PrismContext getPrismContext();

	SecurityEnforcer getSecurityEnforcer();
	
	SecurityContextManager getSecurityContextManager();

	ExpressionFactory getExpressionFactory();

	/**
	 * Returns adminGuiConfiguraiton applicable to currently logged-in user.
	 * Strictly speaking, this can be retrieved from modelInteractionService.
	 * But having a separate function for that allows to get rid of
	 * task and result parameters. And more importantly: this allows to
	 * cache adminGuiConfig in the page (in case many components need it).
	 */
	@NotNull
	AdminGuiConfigurationType getAdminGuiConfiguration();

	default ObjectResolver getModelObjectResolver() {
		return null;
	}
}
