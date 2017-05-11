/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public abstract class AbstractObjectMainPanel<O extends ObjectType> extends Panel {

	public static final String PARAMETER_SELECTED_TAB = "tab";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_EXECUTE_OPTIONS = "executeOptions";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "save";
	private static final String ID_PREVIEW_CHANGES = "previewChanges";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractObjectMainPanel.class);
	
	private Form mainForm;
	
	private LoadableModel<ObjectWrapper<O>> objectModel;
	private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {
		@Override
		protected ExecuteChangeOptionsDto load() {
			return ExecuteChangeOptionsDto.createFromSystemConfiguration();
		}
	};

	public AbstractObjectMainPanel(String id, LoadableModel<ObjectWrapper<O>> objectModel, PageAdminObjectDetails<O> parentPage) {
		super(id, objectModel);
		Validate.notNull(objectModel, "Null object model");
		this.objectModel = objectModel;
		initLayout(parentPage);
	}

	@Override
	protected void onConfigure() {
		super.onConfigure();

		TabbedPanel tabbedPanel = (TabbedPanel) get(ID_MAIN_FORM + ":" + ID_TAB_PANEL);
		WebComponentUtil.setSelectedTabFromPageParameters(tabbedPanel, getPage().getPageParameters(),
				PARAMETER_SELECTED_TAB);
	}

	public LoadableModel<ObjectWrapper<O>> getObjectModel() {
		return objectModel;
	}
	
	public ObjectWrapper<O> getObjectWrapper() {
		return objectModel.getObject();
	}

	public PrismObject<O> getObject() {
		return objectModel.getObject().getObject();
	}
	
	public Form getMainForm() {
		return mainForm;
	}

	private void initLayout(PageAdminObjectDetails<O> parentPage) {
		mainForm = new Form<>(ID_MAIN_FORM, true);
		add(mainForm);
		initLayoutTabs(parentPage);
		initLayoutOptions();
		initLayoutButtons(parentPage);
	}
	
	protected void initLayoutTabs(final PageAdminObjectDetails<O> parentPage) {
		List<ITab> tabs = createTabs(parentPage);
		TabbedPanel<ITab> tabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, parentPage, tabs, null,
				PARAMETER_SELECTED_TAB);
		mainForm.add(tabPanel);
	}

	protected abstract List<ITab> createTabs(PageAdminObjectDetails<O> parentPage);

	protected void initLayoutOptions() {
		ExecuteChangeOptionsPanel optionsPanel = new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS,
				executeOptionsModel, true, false);
        optionsPanel.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return getOptionsPanelVisibility();
			}

		});
		mainForm.add(optionsPanel);
	}
	
	protected void initLayoutButtons(PageAdminObjectDetails<O> parentPage) {
		initLayoutPreviewButton(parentPage);
		initLayoutSaveButton(parentPage);
		initLayoutBackButton(parentPage);
	}

	protected void initLayoutSaveButton(final PageAdminObjectDetails<O> parentPage) {
		AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE, parentPage.createStringResource("pageAdminFocus.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				getDetailsPage().savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(parentPage.getFeedbackPanel());
			}
		};
        saveButton.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getObjectWrapper().isReadonly();
            }

            @Override
            public boolean isEnabled() {
                //in case user isn't allowed to modify focus data but has
                // e.g. #assign authorization, Save button is disabled on page load.
                // Save button becomes enabled just if some changes are made
                // on the Assignments tab (in the use case with #assign authorization)
                PrismContainerDefinition def = getObjectWrapper().getDefinition();
                if (ContainerStatus.MODIFYING.equals(getObjectWrapper().getStatus())
                        && !def.canModify()){
                    return areSavePreviewButtonsEnabled();
                }
                return true;
            }
        });
		mainForm.setDefaultButton(saveButton);
		mainForm.add(saveButton);
	}

	// TEMPORARY
	protected void initLayoutPreviewButton(final PageAdminObjectDetails<O> parentPage) {
		AjaxSubmitButton previewButton = new AjaxSubmitButton(ID_PREVIEW_CHANGES, parentPage.createStringResource("pageAdminFocus.button.previewChanges")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				getDetailsPage().previewPerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(parentPage.getFeedbackPanel());
			}
		};
        previewButton.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return AbstractObjectMainPanel.this.isPreviewButtonVisible();
            }

            @Override
            public boolean isEnabled() {
                PrismContainerDefinition def = getObjectWrapper().getDefinition();
                if (ContainerStatus.MODIFYING.equals(getObjectWrapper().getStatus())
                        && !def.canModify()){
                    return areSavePreviewButtonsEnabled();
                }
                return true;
            }
        });
		mainForm.add(previewButton);
	}

	protected boolean isPreviewButtonVisible(){
		return !getObjectWrapper().isReadonly();
	}

	protected void initLayoutBackButton(PageAdminObjectDetails<O> parentPage) {
		AjaxButton back = new AjaxButton(ID_BACK, parentPage.createStringResource("pageAdminFocus.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				backPerformed(target);
			}
			
		};
		mainForm.add(back);
	}
	
	public ExecuteChangeOptionsDto getExecuteChangeOptionsDto() {
		return executeOptionsModel.getObject();
	}

	private void backPerformed(AjaxRequestTarget target) {
		getDetailsPage().redirectBack();
	}
	
	protected PageAdminObjectDetails<O> getDetailsPage() {
		return (PageAdminObjectDetails<O>)getPage();
	}

    protected boolean getOptionsPanelVisibility(){
        if (getObjectWrapper().isReadonly()){
			return false;
		}
		PrismContainerDefinition def = getObjectWrapper().getDefinition();
		if (ContainerStatus.MODIFYING.equals(getObjectWrapper().getStatus())
				&& !def.canModify()){
			return false;
		}
		return true;
    }

    public void reloadSavePreviewButtons(AjaxRequestTarget target){
        target.add(AbstractObjectMainPanel.this.get(ID_MAIN_FORM).get(ID_PREVIEW_CHANGES));
        target.add(AbstractObjectMainPanel.this.get(ID_MAIN_FORM).get(ID_SAVE));

    }

    protected boolean areSavePreviewButtonsEnabled(){
        return false;
    }
}
