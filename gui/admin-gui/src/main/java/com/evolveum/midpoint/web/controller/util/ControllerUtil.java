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
import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.bean.ResourceState;
import com.evolveum.midpoint.web.bean.ResourceStatus;
import com.evolveum.midpoint.web.bean.Selectable;
import com.evolveum.midpoint.web.model.AccountShadowManager;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType.ExtraTest;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public class ControllerUtil {

	private static final Trace LOGGER = TraceManager.getTrace(ControllerUtil.class);

	@SuppressWarnings("unchecked")
	public static <T> T getObjectFromModel(String oid, ModelPortType model, OperationResult result,
			Class<T> clazz) {
		// TODO: operation result handling

		OperationResult opResult = new OperationResult("Get Object");
		result.addSubresult(opResult);
		try {
			ObjectType object = model.getObject(oid, new PropertyReferenceListType(),
					new Holder<OperationResultType>(opResult.createOperationResultType()));

			if (clazz.isInstance(object)) {
				opResult.recordSuccess();
				return (T) object;
			} else {
				opResult.recordFatalError("Object type '" + object.getClass().getSimpleName()
						+ "' is not expected (" + clazz.getSimpleName() + ").");
			}
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object with oid {}", ex, oid);
			opResult.recordFatalError("Couldn't get object from model.", ex);
		}

		return null;
	}

	public static Element createQuery(String username) {
		Document document = DOMUtil.getDocument();
		Element and = document.createElementNS(SchemaConstants.NS_C, "c:and");
		document.appendChild(and);

		Element type = document.createElementNS(SchemaConstants.NS_C, "c:type");
		type.setAttribute("uri", "http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#UserType");
		and.appendChild(type);

		Element equal = document.createElementNS(SchemaConstants.NS_C, "c:equal");
		and.appendChild(equal);
		Element value = document.createElementNS(SchemaConstants.NS_C, "c:value");
		equal.appendChild(value);
		Element name = document.createElementNS(SchemaConstants.NS_C, "c:name");
		name.setTextContent(username);
		value.appendChild(name);

		return and;
	}

	private static boolean isEventAvailable(ValueChangeEvent evt) {
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
		ObjectManager<UserDto> objectManager = catalog.getObjectManager(UserType.class, UserDto.class);
		return (UserManager) (objectManager);
	}

	public static ResourceManager getResourceManager(ObjectTypeCatalog catalog) {
		ObjectManager<ResourceDto> manager = catalog.getObjectManager(ResourceType.class, ResourceDto.class);

		return (ResourceManager) manager;
	}

	public static AccountShadowManager getAccountManager(ObjectTypeCatalog catalog) {
		ObjectManager<AccountShadowDto> manager = catalog.getObjectManager(AccountShadowType.class,
				AccountShadowDto.class);
		return (AccountShadowManager) (manager);
	}

	public static void updateResourceState(ResourceState state, ResourceTestResultType result) {
		ExtraTest extra = result.getExtraTest();
		if (extra != null) {
			state.setExtraName(extra.getName());
			state.setExtra(getStatusFromResultType(extra.getResult()));
		}
		state.setConConnection(getStatusFromResultType(result.getConnectorConnection()));
		state.setConfValidation(getStatusFromResultType(result.getConfigurationValidation()));
		state.setConInitialization(getStatusFromResultType(result.getConnectorInitialization()));
		state.setConSanity(getStatusFromResultType(result.getConnectorSanity()));
		state.setConSchema(getStatusFromResultType(result.getConnectorSchema()));
	}

	private static ResourceStatus getStatusFromResultType(TestResultType result) {
		if (result == null) {
			return ResourceStatus.NOT_TESTED;
		}

		ResourceStatus status = result.isSuccess() ? ResourceStatus.SUCCESS : ResourceStatus.ERROR;

		List<JAXBElement<DiagnosticsMessageType>> messages = result.getErrorOrWarning();
		for (JAXBElement<DiagnosticsMessageType> element : messages) {
			DiagnosticsMessageType message = element.getValue();
			StringBuilder builder = new StringBuilder();
			builder.append(message.getMessage());
			if (!StringUtils.isEmpty(message.getDetails())) {
				builder.append("Reason: ");
				builder.append(message.getDetails());
			}
			if (message.getTimestamp() != null) {
				builder.append("Time: ");
				builder.append(message.getTimestamp().toGregorianCalendar().getTime());
			}
			FacesUtils.addErrorMessage(builder.toString());
		}

		return status;
	}
}
