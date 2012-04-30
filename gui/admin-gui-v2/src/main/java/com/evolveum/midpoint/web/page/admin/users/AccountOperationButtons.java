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

package com.evolveum.midpoint.web.page.admin.users;

import java.awt.LayoutManager;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;

/**
 * @author mserbak
 */
public class AccountOperationButtons extends Panel {

	public AccountOperationButtons(String id, final IModel<ObjectWrapper> model) {
		super(id);

		initButtons(model);
	}

	private void initButtons(final IModel<ObjectWrapper> model) {
		AjaxLink link = new AjaxLink("link") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				linkOnClick(target);
			}
		};
		add(link);

		// TODO: change button if account is unlink
		// Image linkImg = new Image("linkImg", new AbstractReadOnlyModel() {
		//
		// @Override
		// public Object getObject() {
		// ObjectWrapper wrapper = model.getObject();
		// if (wrapper.isShowEmpty()) {
		// return new PackageResourceReference(AccountOperationButtons.class,
		// "ShowEmptyFalse.png");
		// }
		//
		// return new PackageResourceReference(AccountOperationButtons.class,
		// "ShowEmptyTrue.png");
		// }
		// });
		Image linkImg = new Image("linkImg", new PackageResourceReference(AccountOperationButtons.class,
				"Link.png"));
		
		linkImg.add(new AttributeAppender("title", ""));
		//TODO: change title text if account is link or unlink
//        if(model.getObject().isShowEmpty()){
//        	linkImg.add(new AttributeModifier("title", getString("prismOperationButtonPanel.link")));
//        } else {
//        	linkImg.add(new AttributeModifier("title", getString("prismOperationButtonPanel.unlink")));
//        }
        linkImg.add(new AttributeModifier("title", getString("prismOperationButtonPanel.link")));
		link.add(linkImg);

		AjaxLink delete = new AjaxLink("delete") {
			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteOnClick(target);
			}
		};
		add(delete);

		Image deleteImg = new Image("deleteImg", new PackageResourceReference(AccountOperationButtons.class,
				"delete.png"));
		delete.add(deleteImg);
	}

	public void deleteOnClick(AjaxRequestTarget target) {
	}

	public void linkOnClick(AjaxRequestTarget target) {
	}
}
