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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.controller.SchemaHandler;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Vilo Repan
 */
public abstract class BaseAction implements Action {

	private ModelController model;
	private SchemaHandler schemaHandler;
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
		if (StringUtils.isEmpty(oid)) {
			return null;
		}

		try {
			return model.getObject(oid, new PropertyReferenceListType(), result, UserType.class);
		} catch (ObjectNotFoundException ex) {
			// user was not found, we return null
		} catch (Exception ex) {
			throw new SynchronizationException("Can't get user with oid '" + oid
					+ "'. Unknown error occured.", ex);
		}

		return null;
	}

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		Validate.notNull(change, "Resource object change description must not be null.");
		Validate.notNull(situation, "Synchronization situation must not be null.");
		Validate.notNull(shadowAfterChange, "Resource object shadow after change must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		return null;
	}

	public void setModel(ModelController model) {
		this.model = model;
	}

	protected ModelController getModel() {
		return model;
	}

	protected SchemaHandler getSchemaHandler() {
		return schemaHandler;
	}

	public void setSchemaHandler(SchemaHandler schemaHandler) {
		this.schemaHandler = schemaHandler;
	}
}
