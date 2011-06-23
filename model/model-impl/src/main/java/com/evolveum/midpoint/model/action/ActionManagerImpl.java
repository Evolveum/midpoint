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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.action;

import java.util.Map;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.ModelService;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author Vilo Repan
 */
public class ActionManagerImpl<T extends Action> implements ActionManager<T> {

	private static transient Trace trace = TraceManager.getTrace(ActionManagerImpl.class);
	private Map<String, Class<T>> actionMap;
	private ModelPortType model;
	private ProvisioningService provisioning;
	private RepositoryService repository;
	private SchemaHandling schemaHandling;

	@Override
	public void setActionMapping(Map<String, Class<T>> actionMap) {
		this.actionMap = actionMap;
	}

	@Override
	public Action getActionInstance(String uri) {
		Class<T> clazz = actionMap.get(uri);
		if (clazz == null) {
			return null;
		}

		Action action = null;
		try {
			action = clazz.newInstance();
			((BaseAction) action).setModel(model);
			((BaseAction) action).setProvisioning((ProvisioningService) provisioning);
			((BaseAction) action).setSchemaHandling(schemaHandling);
			((BaseAction) action).setRepository(repository);
		} catch (InstantiationException ex) {
			trace.error("Couln't create action instance, reason: {}.", ex.getMessage());
			trace.debug("Couln't create action instance.", ex);
		} catch (IllegalAccessException ex) {
			trace.error("Couln't create action instance, reason: {}.", ex.getMessage());
			trace.debug("Couln't create action instance.", ex);
		}

		// TODO: Solve problem how to inject required objects into actions and
		// not to depend on FActory required by EJB and make test working
		// Action action = null;
		// try {
		// BeanFactoryLocator locator =
		// ContextSingletonBeanFactoryLocator.getInstance();
		// BeanFactoryReference bfr =
		// locator.useBeanFactory(EJB_SPRING_CONTEXT_BEAN);
		// BeanFactory fac = bfr.getFactory();
		// if (!(fac instanceof ApplicationContext)) {
		// throw new IllegalStateException("Bean '" + EJB_SPRING_CONTEXT_BEAN +
		// "' is not type of ApplicationContext.");
		// }
		//
		// ApplicationContext context = (ApplicationContext) fac;
		// AutowireCapableBeanFactory factory =
		// context.getAutowireCapableBeanFactory();
		// //set to true after removing model service from ejb
		// Object object = factory.autowire(clazz,
		// AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		// if (object instanceof Action) {
		// action = (Action) object;
		// //action.setModel((ModelService)model);
		// } else {
		// throw new IllegalArgumentException("Uri '" + uri +
		// "' maps on action class" +
		// " which doesn't implement com.evolveum.midpoint.model.Action interface.");
		// }
		// } catch (Exception ex) {
		// trace.error("Couln't create action instance, reason: {}.",
		// ex.getMessage());
		// trace.debug("Couln't create action instance.", ex);
		// }

		return action;
	}

	public void setModel(ModelPortType model) {
		this.model = model;
	}

	public void setProvisioning(ProvisioningService provisioning) {
		this.provisioning = provisioning;
	}

	public void setSchemaHandling(SchemaHandling schemaHandling) {
		this.schemaHandling = schemaHandling;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}

}
