/*
 * Copyright (c) 2010-2017 Evolveum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

/**
 * Created by Honchar.
 */
public class AdminGuiConfigPanel extends BasePanel<SystemConfigurationDto> {

	private static final long serialVersionUID = 1L;
	private static final String ID_DASHBOARD_LINK_EDITOR = "dashboardLinkEditor";
	private static final String ID_ADDITIONAL_MENU_ITEM_EDITOR = "additionalMenuItemEditor";
	private static final String LABEL_SIZE = "col-md-4";
	private static final String INPUT_SIZE = "col-md-6";

	public AdminGuiConfigPanel(String id, IModel<SystemConfigurationDto> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {
		GenericMultiValueLabelEditPanel<RichHyperlinkType> dashboardLinkEditor = new GenericMultiValueLabelEditPanel<RichHyperlinkType>(
				ID_DASHBOARD_LINK_EDITOR,
				new PropertyModel<List<RichHyperlinkType>>(getModel(), "userDashboardLink"),
				createStringResource("AdminGuiConfigPanel.dashboardLinksConfig"), LABEL_SIZE, INPUT_SIZE,
				true) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createTextModel(final IModel<RichHyperlinkType> model) {
				return new PropertyModel<String>(model, "label");
			}

			@Override
			protected void editValuePerformed(AjaxRequestTarget target, IModel<RichHyperlinkType> rowModel) {
				RichHyperlinkConfigPanel contentPanel = new RichHyperlinkConfigPanel(
						getPageBase().getMainPopupBodyId(), rowModel.getObject(), false) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void savePerformed(AjaxRequestTarget target) {
						closeModalWindow(target);
						target.add(getDashboardLinkEditorContainer());
					}

					@Override
					public StringResourceModel getTitle() {
						return createStringResource("AdminGuiConfigPanel.dashboardLinkDialogTitle.title");
					}
				};
				showDialog(contentPanel, target);
			}

			@Override
			protected RichHyperlinkType createNewEmptyItem() {
				RichHyperlinkType link = new RichHyperlinkType();
				link.getAuthorization().add("");
				return link;
			}
		};
		dashboardLinkEditor.setOutputMarkupId(true);
		add(dashboardLinkEditor);

		GenericMultiValueLabelEditPanel<RichHyperlinkType> additionalMenuItemEditor = new GenericMultiValueLabelEditPanel<RichHyperlinkType>(
				ID_ADDITIONAL_MENU_ITEM_EDITOR,
				new PropertyModel<List<RichHyperlinkType>>(getModel(), "additionalMenuLink"),
				createStringResource("AdminGuiConfigPanel.additionalMenuItemConfig"), LABEL_SIZE, INPUT_SIZE,
				true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createTextModel(final IModel<RichHyperlinkType> model) {
				return new PropertyModel<String>(model, "label");
			}

			@Override
			protected void editValuePerformed(AjaxRequestTarget target, IModel<RichHyperlinkType> rowModel) {
				RichHyperlinkConfigPanel contentPanel = new RichHyperlinkConfigPanel(
						getPageBase().getMainPopupBodyId(), rowModel.getObject(), true) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void savePerformed(AjaxRequestTarget target) {
						closeModalWindow(target);
						target.add(getAdditionalMenuItemContainer());
					}

					@Override
					public StringResourceModel getTitle() {
						return createStringResource("AdminGuiConfigPanel.additionalMenuItemDialog.title");
					}
				};
				showDialog(contentPanel, target);
			}

			@Override
			protected RichHyperlinkType createNewEmptyItem() {
				RichHyperlinkType link = new RichHyperlinkType();
				link.getAuthorization().add("");
				return link;
			}
		};
		additionalMenuItemEditor.setOutputMarkupId(true);
		add(additionalMenuItemEditor);

	}

	private WebMarkupContainer getDashboardLinkEditorContainer() {
		return (WebMarkupContainer) get(ID_DASHBOARD_LINK_EDITOR);
	}

	private WebMarkupContainer getAdditionalMenuItemContainer() {
		return (WebMarkupContainer) get(ID_ADDITIONAL_MENU_ITEM_EDITOR);
	}
}
