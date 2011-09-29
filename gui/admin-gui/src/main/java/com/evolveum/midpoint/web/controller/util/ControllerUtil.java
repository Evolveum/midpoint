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
package com.evolveum.midpoint.web.controller.util;

import java.util.List;

import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ResourceState;
import com.evolveum.midpoint.web.bean.ResourceStatus;
import com.evolveum.midpoint.web.bean.Selectable;
import com.evolveum.midpoint.web.model.AccountManager;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.SystemManager;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.model.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class ControllerUtil {

	private static final Trace LOGGER = TraceManager.getTrace(ControllerUtil.class);

	public static Element createQuery(String username, ObjectTypes objectType) {
		Document document = DOMUtil.getDocument();
		Element equal = document.createElementNS(SchemaConstants.NS_C, "c:equal");
		Element value = document.createElementNS(SchemaConstants.NS_C, "c:value");
		equal.appendChild(value);
		Element name = document.createElementNS(SchemaConstants.NS_C, "c:name");
		name.setTextContent(username);
		value.appendChild(name);

		Element and = document.createElementNS(SchemaConstants.NS_C, "c:and");
		document.appendChild(and);

		if (objectType != null) {
			Element type = document.createElementNS(SchemaConstants.NS_C, "c:type");
			type.setAttribute("uri", objectType.getObjectTypeUri());
			and.appendChild(type);
		}

		and.appendChild(equal);
		return and;
	}

	public static boolean isEventAvailable(ValueChangeEvent evt) {
		if (evt.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();

			return false;
		}

		return true;
	}

	public static boolean selectPerformed(ValueChangeEvent evt, List<? extends Selectable> beans) {
		boolean selectedAll = false;
		if (isEventAvailable(evt)) {
			boolean selected = ((Boolean) evt.getNewValue()).booleanValue();
			if (!selected) {
				selectedAll = false;
			} else {
				selectedAll = true;
				for (Selectable item : beans) {
					if (!item.isSelected()) {
						selectedAll = false;
						break;
					}
				}
			}
		}

		return selectedAll;
	}

	public static void selectAllPerformed(ValueChangeEvent evt, List<? extends Selectable> beans) {
		if (isEventAvailable(evt)) {
			boolean selectAll = ((Boolean) evt.getNewValue()).booleanValue();
			for (Selectable item : beans) {
				item.setSelected(selectAll);
			}
		}
	}

	public static UserManager getUserManager(ObjectTypeCatalog catalog) {
		Validate.notNull(catalog, "Object type catalog must not be null.");
		ObjectManager<GuiUserDto> objectManager = catalog.getObjectManager(UserType.class, GuiUserDto.class);
		return (UserManager) (objectManager);
	}

	public static ResourceManager getResourceManager(ObjectTypeCatalog catalog) {
		Validate.notNull(catalog, "Object type catalog must not be null.");
		ObjectManager<GuiResourceDto> manager = catalog.getObjectManager(ResourceType.class,
				GuiResourceDto.class);

		return (ResourceManager) manager;
	}

	public static AccountManager getAccountManager(ObjectTypeCatalog catalog) {
		Validate.notNull(catalog, "Object type catalog must not be null.");
		ObjectManager<AccountShadowDto> manager = catalog.getObjectManager(AccountShadowType.class,
				AccountShadowDto.class);
		return (AccountManager) (manager);
	}

	public static SystemManager getSystemManager(ObjectTypeCatalog catalog) {
		Validate.notNull(catalog, "Object type catalog must not be null.");
		ObjectManager<SystemConfigurationDto> manager = catalog.getObjectManager(
				SystemConfigurationType.class, SystemConfigurationDto.class);
		return (SystemManager) (manager);
	}

	public static RoleManager getRoleManager(ObjectTypeCatalog catalog) {
		Validate.notNull(catalog, "Object type catalog must not be null.");
		ObjectManager<RoleDto> manager = catalog.getObjectManager(RoleType.class, RoleDto.class);
		return (RoleManager) (manager);
	}

	public static void updateResourceState(ResourceState state, OperationResult result) {
		Validate.notNull(result, "Operation result must not be null.");

		List<OperationResult> subResults = result.getSubresults();

		// state.setExtraName("Unknown (todo: fix)");
		// state.setExtra(getStatusFromResultType(ConnectorTestOperation.EXTRA_TEST,
		// subResults));
		state.setConConnection(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_CONNECTION,
				subResults));
		state.setConfValidation(getStatusFromResultType(ConnectorTestOperation.CONFIGURATION_VALIDATION,
				subResults));
		state.setConInitialization(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_INITIALIZATION,
				subResults));
		state.setConSanity(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_SANITY, subResults));
		state.setConSchema(getStatusFromResultType(ConnectorTestOperation.CONNECTOR_SCHEMA, subResults));
	}

	private static ResourceStatus getStatusFromResultType(ConnectorTestOperation operation,
			List<OperationResult> results) {
		ResourceStatus status = ResourceStatus.NOT_TESTED;

		OperationResult resultFound = null;
		for (OperationResult result : results) {
			try {
				if (operation.getOperation().equals(result.getOperation())) {
					resultFound = result;
					break;
				}
			} catch (IllegalArgumentException ex) {
				LOGGER.debug("Result operation name {} returned from test connection is not type of {}.",
						new Object[] { result.getOperation(), ConnectorTestOperation.class });
			}
		}
		
		if (resultFound == null) {
			return status;
		}
		
		switch (resultFound.getStatus()) {
			case SUCCESS:
				status = ResourceStatus.SUCCESS;
				break;
			case WARNING:
				status = ResourceStatus.WARNING;
				break;
			case FATAL_ERROR:
			case PARTIAL_ERROR:
				status = ResourceStatus.ERROR;
		}

		if (!resultFound.isSuccess() && !resultFound.isUnknown()) {
			FacesUtils.addErrorMessage(resultFound.getMessage());
		}
		
		return status;
	}
}
