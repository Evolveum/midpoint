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
package com.evolveum.midpoint.web.component.menu;

import java.io.IOException;
import java.util.Iterator;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.icesoft.faces.component.ext.HtmlForm;

/**
 * 
 * @author lazyman
 *
 */
@FacesComponent(value = "HtmlTopMenu")
public class HtmlTopMenu extends HtmlForm {

	public static final String ATTR_SELECTED = "selected";
	private static final String STYLE_SELECTED = "selected-top";
	private static final String STYLE_LAST = "last";

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		super.encodeBegin(context);

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("ul", this);
		writer.writeAttribute("id", "top-nav", null);
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("ul");

		super.encodeEnd(context);
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		ValueExpression selectedExpr = getValueExpression(ATTR_SELECTED);
		String selectedId = selectedExpr == null ? null : (String) selectedExpr.getValue(context
				.getELContext());

		HtmlTopMenuItem last = null;
		HtmlTopMenuItem selected = null;
		Iterator<UIComponent> children = getFacetsAndChildren();
		while (children.hasNext()) {
			UIComponent child = children.next();
			if (!(child instanceof HtmlTopMenuItem)) {
				continue;
			}
			if (last == null) {
				selected = (HtmlTopMenuItem) child;
			}
			last = (HtmlTopMenuItem) child;
			if (last.getId().equals(selectedId)) {
				selected = last;
			}
		}

		children = getFacetsAndChildren();
		while (children.hasNext()) {
			UIComponent child = children.next();
			if (!(child instanceof HtmlTopMenuItem)) {
				child.encodeAll(context);
				continue;
			}

			HtmlTopMenuItem item = (HtmlTopMenuItem) child;
			StringBuilder styles = new StringBuilder();
			if (item.equals(last) && styles.indexOf(STYLE_LAST) == -1) {
				styles.append(STYLE_LAST);
			}

			if (item.equals(selected) && styles.indexOf(STYLE_SELECTED) == -1) {
				if (styles.length() != 0) {
					styles.append(" ");
				}
				styles.append(STYLE_SELECTED);
			}
			item.setStyleClass(styles.toString());
			item.encodeAll(context);
		}
	}
}
