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
import javax.xml.ws.Holder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.bean.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
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
}
