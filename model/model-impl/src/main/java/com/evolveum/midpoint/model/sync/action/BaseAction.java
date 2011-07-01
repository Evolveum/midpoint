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

package com.evolveum.midpoint.model.sync.action;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Vilo Repan
 */
public abstract class BaseAction implements Action {

	private ModelController model;
	private ProvisioningService provisioning;
	private RepositoryService repository;
	private SchemaHandling schemaHandling;
	private List<Object> parameters;

	@Override
	public List<Object> getParameters() {
		if (parameters == null) {
			parameters = new ArrayList<Object>();
		}
		return parameters;
	}

	@Override
	public void setParameters(List<Object> parameters) {
		this.parameters = parameters;
	}

	protected UserType getUser(String oid, OperationResult result) throws SynchronizationException {
		if (oid == null) {
			return null;
		}

		try {
			ObjectType object = model.getObject(oid, new PropertyReferenceListType(), result);
			if (object == null) {
				return null;
			}
			if (!(object instanceof UserType)) {
				throw new SynchronizationException("Returned object is null or not type of "
						+ UserType.class.getName() + ".");
			}
			return (UserType) object;
		} catch (ObjectNotFoundException ex) {
			// user was not found, we return null
		} catch (Exception ex) {
			throw new SynchronizationException("Can't get user. Unknown error occured.", ex);
		}

		return null;
	}

	public void setModel(ModelController model) {
		this.model = model;
	}

	protected ModelController getModel() {
		return model;
	}

	public ProvisioningService getProvisioning() {
		return provisioning;
	}

	public void setProvisioning(ProvisioningService provisioning) {
		this.provisioning = provisioning;
	}

	public RepositoryService getRepository() {
		return repository;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}

	public SchemaHandling getSchemaHandling() {
		return schemaHandling;
	}

	public void setSchemaHandling(SchemaHandling schemaHandling) {
		this.schemaHandling = schemaHandling;
	}

	protected ScriptsType getScripts(ResourceType resource) {
		ScriptsType scripts = resource.getScripts();
		if (scripts == null) {
			scripts = new ScriptsType();
		}

		return scripts;
	}
}
