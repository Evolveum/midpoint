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

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 * @author mserbak
 */
public class ThreeStateCheckBox extends HiddenField<String> {

	public ThreeStateCheckBox(String id) {
		this(id, new Model<Boolean>(null));
	}

	public ThreeStateCheckBox(String id, IModel<Boolean> model) {
		super(id, createStringModel(model));
	}
	
	private static IModel<String> createStringModel(final IModel<Boolean> model) {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				String object = "";
				if (model.getObject() == null) {
					object = ThreeCheckState.UNDEFINED.toString();
				} else if (model.getObject()) {
					object = ThreeCheckState.CHECKED.toString();
				} else {
					object = ThreeCheckState.UNCHECKED.toString();
				}
				return object;
			}
		};
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
		boolean enabled = isEnabled();
		response.renderOnLoadJavaScript("initThreeStateCheckBox('" + getMarkupId() + "', '"+enabled+"')");
	}
}
