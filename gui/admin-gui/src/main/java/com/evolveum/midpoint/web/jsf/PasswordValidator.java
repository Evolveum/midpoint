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

package com.evolveum.midpoint.web.jsf;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Iterator;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Vilo Repan
 */
@FacesValidator("PasswordValidator")
public class PasswordValidator implements Validator {

	private static transient Trace LOGGER = TraceManager.getTrace(PasswordValidator.class);
	public static final String OTHER_COMPONENT_ID = "otherComponentId";

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		if (value == null) {
			return;
		}
		if (!(value instanceof String)) {
			throw createMessage("Password value is not string type.",
					"Password value is not string type, it's '" + value.getClass().getName() + "'.");
		}
		String password1 = (String) value;

		String otherComponentId = (String) component.getAttributes().get(OTHER_COMPONENT_ID);
		String compClientId = component.getClientId();
		compClientId = compClientId.replace(component.getId(), otherComponentId);
		UIInput comp = findComponent(context.getViewRoot(), otherComponentId, compClientId);
		if (comp == null) {
			LOGGER.warn("Can't find component with name '{}', Component with password validator doesn't "
					+ "have atttribute '{}' defined.", new Object[] { otherComponentId, OTHER_COMPONENT_ID });
			throw createMessage("Component not found.", "Component '" + otherComponentId
					+ "' not found, can't properly validate field.");
		}
		String password2 = (String) comp.getValue();

		boolean equal = StringUtils.isNotEmpty(password1) ? (StringUtils.isEmpty(password2)? false : password1.equals(password2) ) : (StringUtils.isEmpty(password2) ? true : false);
		if (!equal) {
			throw createMessage("Please check password fields.", "Passwords doesn't match.");
		}
	}

	private ValidatorException createMessage(String summary, String detail) {
		return new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
	}

	private UIInput findComponent(UIComponent parent, String id, String compClientId) {
	
//		if (parent.getParent() != null){
//			if (id.equals(parent.getId()) && compClientId.equals(parent.getClientId()) && (parent instanceof UIInput)){
//				return (UIInput) parent;
//			}
//		}
	
		if (id.equals(parent.getId()) && compClientId.equals(parent.getClientId()) && (parent instanceof UIInput)) {
			return (UIInput) parent;
		}

		for (Iterator<UIComponent> children = parent.getFacetsAndChildren();children.hasNext();){
			UIComponent child = children.next();
				UIInput input = findComponent(child, id, compClientId);
				if (input != null) {
					return input;
				}
		}

		return null;
	}
}
