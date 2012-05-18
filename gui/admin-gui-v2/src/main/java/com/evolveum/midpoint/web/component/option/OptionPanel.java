/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.option;

import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.xml.ace.AceEditor;

/**
 * @author lazyman
 */
public class OptionPanel extends Border {
	private Boolean hidden = false;

	public OptionPanel(String id, IModel<String> title) {
		super(id);

		WebMarkupContainer parent = new WebMarkupContainer("parent");
		parent.setOutputMarkupId(true);
		addToBorder(parent);

		parent.add(new Label("title", title));
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		response.renderJavaScriptReference(new PackageResourceReference(OptionPanel.class, "OptionPanel.js"));
		response.renderOnLoadJavaScript("initOptionPanel(" + hidden + ")");
	}
}
