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

import java.io.IOException;
import java.util.Iterator;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.icesoft.faces.component.ext.HtmlForm;

/**
 * 
 * @author lazyman
 *
 */
@FacesComponent(value = "HtmlLeftMenu")
public class HtmlLeftMenu extends HtmlForm {

	public static final String ATTR_SELECTED = "selected";
	private static final Trace TRACE = TraceManager.getTrace(HtmlLeftMenu.class);
	private static final String STYLE_SELECTED = "selected";

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {		
		ValueExpression selectedExpr = getValueExpression(ATTR_SELECTED);
		String selectedId = selectedExpr == null ? null : (String) selectedExpr.getValue(context
				.getELContext());
		TRACE.debug("encodeChildren: " + selectedId);
		
		Iterator<UIComponent> children = getFacetsAndChildren();
		while (children.hasNext()) {
			UIComponent container = children.next();
			if (!(container instanceof HtmlLeftMenuContainer)) {
				container.encodeAll(context);
				continue;
			}
			Iterator<UIComponent> containerChildren = container.getFacetsAndChildren();
			container.encodeBegin(context);
			TRACE.debug("container");
			while (containerChildren.hasNext()) {
				UIComponent item = containerChildren.next();
				if (!(item instanceof HtmlLeftMenuItem)) {
					item.encodeAll(context);
					continue;
				}
				HtmlLeftMenuItem left = (HtmlLeftMenuItem)item;
				if (StringUtils.isEmpty(selectedId)) {
					selectedId = left.getId();
				}
				
				if (left.getId().equals(selectedId)) {
					left.setStyleClass(STYLE_SELECTED);
				} else {
					left.setStyleClass("");
				}
				TRACE.debug("item: " + left.getStyleClass());
				left.encodeAll(context);
			}
			container.encodeEnd(context);
		}
		
		TRACE.debug("encodeChildren::end");
	}
}
