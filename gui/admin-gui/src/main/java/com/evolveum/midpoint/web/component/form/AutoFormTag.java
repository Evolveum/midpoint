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

package com.evolveum.midpoint.web.component.form;

import java.util.List;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;

/**
 * 
 * @author lazyman
 */
public class AutoFormTag extends ComponentHandler {

	private final TagAttribute bean;
	private final TagAttribute index;
	private final TagAttribute editable;

	public AutoFormTag(ComponentConfig config) {
		super(config);

		editable = getAttribute(AutoForm.ATTR_EDITABLE);
		index = getAttribute(AutoForm.ATTR_INDEX);
		bean = getRequiredAttribute(AutoForm.ATTR_BEAN);
		if (bean == null) {
			throw new TagAttributeException(bean, "Form object must be specified.");
		}
	}

	@Override
	public void setAttributes(FaceletContext ctx, Object instance) {
		if (!(instance instanceof AutoForm)) {
			throw new IllegalArgumentException("Tag is associated with type of '"
					+ instance.getClass().getName() + "',\nit must be associated type of '"
					+ AutoForm.class.getName() + "'.");
		}

		AutoForm grid = (AutoForm) instance;
		if (index == null) {
			grid.setValueExpression(AutoForm.ATTR_BEAN, bean.getValueExpression(ctx, FormObject.class));
		} else {
			grid.setValueExpression(AutoForm.ATTR_BEAN, bean.getValueExpression(ctx, List.class));
		}
		grid.setValueExpression(AutoForm.ATTR_EDITABLE, editable.getValueExpression(ctx, Boolean.class));
		if (index != null) {
			grid.setValueExpression(AutoForm.ATTR_INDEX, index.getValueExpression(ctx, Integer.class));
		}
	}
}
