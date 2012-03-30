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

package com.evolveum.midpoint.web.page.login;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.menu.top2.BottomMenuItem;
import com.evolveum.midpoint.web.page.PageBase;

/**
 * @author mserbak
 */
public class PageLogin extends PageBase {
	private static final long serialVersionUID = 4077059807145096420L;
	
	

	public PageLogin() {
        add(new Label("userNameLabel", new Model<String>("User")));
        add(new Label("passwordLabel", new Model<String>("Password")));
        add(new FeedbackPanel("feedback"));
	}
	
	@Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(new PackageResourceReference(PageLogin.class, "PageLogin.css"));
    }

	@Override
	public List<LeftMenuItem> getLeftMenuItems() {
		return new ArrayList<LeftMenuItem>();
	}

	@Override
	public List<TopMenuItem> getTopMenuItems() {
		return new ArrayList<TopMenuItem>();
	}

	@Override
	public List<BottomMenuItem> getBottomMenuItems() {
		return new ArrayList<BottomMenuItem>();
	}
}
