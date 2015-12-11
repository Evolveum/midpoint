/**
 * Copyright (c) 2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.detailspanel;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.BaseFocusPanel;
import com.evolveum.midpoint.web.page.admin.FocusDetailsTabPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public class FocusDetailsPanel<F extends FocusType> extends AbstractObjectDetailsPanel<F> {

	private LoadableModel<List<FocusProjectionDto>> projectionModel;
	
	public FocusDetailsPanel(String id, LoadableModel<ObjectWrapper<F>> objectModel, LoadableModel<List<FocusProjectionDto>> projectionModel, 
			PageAdminFocus<F> parentPage) {
		super(id, objectModel, parentPage);
		this.projectionModel = projectionModel;
	}

	@Override
	public void initLayout(final PageAdminObjectDetails<F> parentPage) {
		super.initLayout(parentPage);
		getMainForm().setMultiPart(true);
	}
	
	@Override
	protected List<ITab> createTabs(final PageAdminObjectDetails<F> parentPage) {
		List<ITab> tabs = new ArrayList<>();
		tabs.add(
				new AbstractTab(parentPage.createStringResource("pageAdminFocus.basic")){
					@Override
					public WebMarkupContainer getPanel(String panelId) {
						return createFocusDetailsPanel(panelId, parentPage); 
					}
				});
						
		return tabs;
	}

	protected WebMarkupContainer createFocusDetailsPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new FocusDetailsTabPanel<F>(panelId, getMainForm(), getObjectModel(), parentPage);
	}

}
