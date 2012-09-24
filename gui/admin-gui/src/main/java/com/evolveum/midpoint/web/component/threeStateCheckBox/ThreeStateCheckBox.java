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

package com.evolveum.midpoint.web.component.threeStateCheckBox;

import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;

/**
 * @author lazyman
 * @author mserbak
 */
public class ThreeStateCheckBox extends HiddenField<String> {
	private String label = "";

	public ThreeStateCheckBox(String id) {
		this(id, new Model<String>(ThreeCheckState.UNDEFINED.toString()));
	}

	public ThreeStateCheckBox(String id, final IModel<String> model) {
		super(id, model);
		setOutputMarkupId(true);
	}

	public ThreeStateCheckBox(String id, final IModel<String> label, final IModel<String> model) {
		this(id, model);
		this.label = label.getObject();
	}
	
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);

		if (tag.isOpenClose()) {
			tag.setType(XmlTag.TagType.OPEN);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.renderJavaScriptReference(new PackageResourceReference(ThreeStateCheckBox.class,
				"ThreeStateCheckBox.js"));
		response.renderCSSReference(new PackageResourceReference(ThreeStateCheckBox.class,
				"ThreeStateCheckBox.css"));
		response.renderOnLoadJavaScript("initThreeStateCheckBox('" + getMarkupId() + "', '" + label + "')");
	}
	
	
}
