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
package com.evolveum.midpoint.web.jsf.menu;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import com.evolveum.midpoint.web.jsf.AbstractStateHolder;

/**
 * 
 * @author lazyman
 *
 */
public class TopMenuActionListener extends AbstractStateHolder implements ActionListener {	

	@Override
	public void processAction(ActionEvent event) throws AbortProcessingException {
		UIComponent parent = event.getComponent().getParent();
		if (!(parent instanceof HtmlTopMenu)) {
			throw new IllegalStateException("Parent of topMenuItem must be topMenu");
		}
		HtmlTopMenu menu = (HtmlTopMenu) parent;
		ValueExpression selected = menu.getValueExpression(HtmlTopMenu.ATTR_SELECTED);
		if (selected != null) {
			selected.setValue(FacesContext.getCurrentInstance().getELContext(), event.getComponent().getId());
		}
	}
}
