/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(url = "/admin/certification/definitions", action = {
		@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
				label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
				description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_DEFINITIONS,
				label = PageAdminCertification.AUTH_CERTIFICATION_DEFINITIONS_LABEL,
				description = PageAdminCertification.AUTH_CERTIFICATION_DEFINITIONS_DESCRIPTION)
})
public class PageCertDefinitions extends PageAdminWorkItems {

	private static final long serialVersionUID = 1L;

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TABLE = "table";

	private static final String DOT_CLASS = PageCertDefinitions.class.getName() + ".";
	private static final String OPERATION_CREATE_CAMPAIGN = DOT_CLASS + "createCampaign";
	private static final String OPERATION_DELETE_DEFINITION = DOT_CLASS + "deleteDefinition";

	private static final Trace LOGGER = TraceManager.getTrace(PageCertDefinitions.class);

	private AccessCertificationDefinitionType singleDelete;

	public PageCertDefinitions() {
		initLayout();
	}

	private void initLayout() {
		Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
		add(mainForm);

		MainObjectListPanel<AccessCertificationDefinitionType> mainPanel = new MainObjectListPanel<AccessCertificationDefinitionType>(
				ID_TABLE, AccessCertificationDefinitionType.class, TableId.PAGE_CERT_DEFINITIONS_PANEL, null, this) {
			private static final long serialVersionUID = 1L;

			@Override
			protected IColumn<SelectableBean<AccessCertificationDefinitionType>, String> createCheckboxColumn() {
				return null;
			}

			@Override
			public void objectDetailsPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType service) {
				PageCertDefinitions.this.detailsPerformed(target, service);
			}

			@Override
			protected PrismObject<AccessCertificationDefinitionType> getNewObjectListObject(){
				return (new AccessCertificationDefinitionType()).asPrismObject();
			}

			@Override
			protected List<IColumn<SelectableBean<AccessCertificationDefinitionType>, String>> createColumns() {
				return PageCertDefinitions.this.initColumns();
			}

			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				return null;
			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				navigateToNext(PageCertDefinition.class);
			}
		};
		mainPanel.setOutputMarkupId(true);
		mainPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_CERT_DEF_BOX_CSS_CLASSES);
		mainForm.add(mainPanel);
	}

	private MainObjectListPanel<AccessCertificationDefinitionType> getDefinitionsTable() {
		return (MainObjectListPanel<AccessCertificationDefinitionType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private IModel<String> createDeleteConfirmString() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				if (singleDelete == null) {
					return "";
				} else {
					return createStringResource("PageCertDefinitions.deleteDefinitionConfirmSingle", singleDelete.getName()).getString();
				}
			}
		};
	}

	private List<IColumn<SelectableBean<AccessCertificationDefinitionType>, String>> initColumns() {
		List<IColumn<SelectableBean<AccessCertificationDefinitionType>, String>> columns = new ArrayList<>();

		IColumn column;

		column = new PropertyColumn(createStringResource("PageCertDefinitions.table.description"), "value.description");
		columns.add(column);

		column = new MultiButtonColumn<SelectableBean<AccessCertificationDefinitionType>>(new Model(), 3) {

			private final String[] captionKeys = {
					"PageCertDefinitions.button.createCampaign",
					"PageCertDefinitions.button.showCampaigns",
					"PageCertDefinitions.button.deleteDefinition"
			};

			private final DoubleButtonColumn.BUTTON_COLOR_CLASS[] colors = {
					DoubleButtonColumn.BUTTON_COLOR_CLASS.PRIMARY,
					DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT,
					DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER
			};

			@Override
			public String getButtonTitle(int id) {
				return PageCertDefinitions.this.createStringResource(captionKeys[id]).getString();
			}

			@Override
			public String getButtonColorCssClass(int id) {
				return colors[id].toString();
			}

			@Override
			public void clickPerformed(int id, AjaxRequestTarget target, IModel<SelectableBean<AccessCertificationDefinitionType>> model) {
				switch (id) {
					case 0: createCampaignPerformed(target, model.getObject().getValue()); break;
					case 1: showCampaignsPerformed(target, model.getObject().getValue()); break;
					case 2: deleteConfirmation(target, model.getObject().getValue()); break;
				}
			}

			@Override
			public boolean isButtonEnabled(int id, IModel<SelectableBean<AccessCertificationDefinitionType>> model) {
				return id != 0 || !Boolean.TRUE.equals(model.getObject().getValue().isAdHoc());
			}
		};
		columns.add(column);

		return columns;
	}

	protected void detailsPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType service) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, service.getOid());
		navigateToNext(PageCertDefinition.class, parameters);
	}

	private void showCampaignsPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, definition.getOid());
		navigateToNext(PageCertCampaigns.class, parameters);
	}

	private void createCampaignPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
		LOGGER.debug("Create certification campaign performed for {}", definition.asPrismObject());

		OperationResult result = new OperationResult(OPERATION_CREATE_CAMPAIGN);
		try {
			Task task = createSimpleTask(OPERATION_CREATE_CAMPAIGN);
			if (!Boolean.TRUE.equals(definition.isAdHoc())) {
				getCertificationService().createCampaign(definition.getOid(), task, result);
			} else {
				result.recordWarning("Definition '" + definition.getName() + "' is for ad-hoc campaigns that cannot be started manually.");
			}
		} catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		showResult(result);
		target.add(getFeedbackPanel());
	}

	private void deleteConfirmation(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {

		this.singleDelete = definition;
		showMainPopup(getDeleteDefinitionConfirmationPanel(),
				target);
	}

	private void deleteDefinitionPerformed(AjaxRequestTarget target, AccessCertificationDefinitionType definition) {
		OperationResult result = new OperationResult(OPERATION_DELETE_DEFINITION);
		try {
			Task task = createSimpleTask(OPERATION_DELETE_DEFINITION);
			ObjectDelta<AccessCertificationDefinitionType> delta =
					ObjectDelta.createDeleteDelta(AccessCertificationDefinitionType.class, definition.getOid(),
							getPrismContext());
			getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
		} catch (Exception ex) {
			result.recordPartialError("Couldn't delete campaign definition.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete campaign definition", ex);
		}

		result.computeStatusIfUnknown();
		if (result.isSuccess()) {
			result.recordStatus(OperationResultStatus.SUCCESS, "The definition has been successfully deleted.");    // todo i18n
		}

		getDefinitionsTable().clearCache();

		showResult(result);
		target.add(getFeedbackPanel(), getDefinitionsTable());
	}

	private Popupable getDeleteDefinitionConfirmationPanel() {
		return new ConfirmationPanel(getMainPopupBodyId(),
				createDeleteConfirmString()) {
			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				ModalWindow modalWindow = findParent(ModalWindow.class);
				if (modalWindow != null) {
					modalWindow.close(target);
					deleteDefinitionPerformed(target, singleDelete);
				}
			}
		};
	}


}
