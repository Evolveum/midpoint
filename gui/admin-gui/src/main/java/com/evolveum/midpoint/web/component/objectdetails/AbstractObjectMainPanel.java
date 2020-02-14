/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public abstract class AbstractObjectMainPanel<O extends ObjectType> extends Panel {
    private static final long serialVersionUID = 1L;

    public static final String PARAMETER_SELECTED_TAB = "tab";

    private static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";
    private static final String ID_EDIT_XML = "editXml";
    private static final String ID_PREVIEW_CHANGES = "previewChanges";

    private static final Trace LOGGER = TraceManager.getTrace(AbstractObjectMainPanel.class);

    private Form mainForm;

    private LoadableModel<PrismObjectWrapper<O>> objectModel;

    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {
        private static final long serialVersionUID = 1L;

        @Override
        protected ExecuteChangeOptionsDto load() {
            return ExecuteChangeOptionsDto.createFromSystemConfiguration();
        }
    };

    public AbstractObjectMainPanel(String id, LoadableModel<PrismObjectWrapper<O>> objectModel, PageAdminObjectDetails<O> parentPage) {
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

    public LoadableModel<PrismObjectWrapper<O>> getObjectModel() {
        return objectModel;
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
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
        optionsPanel.setOutputMarkupId(true);
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
        initLayoutEditXmlButton(parentPage);
    }

    protected void initLayoutSaveButton(final PageAdminObjectDetails<O> parentPage) {
        AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE, parentPage.createStringResource("pageAdminFocus.button.save")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                getDetailsPage().savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(parentPage.getFeedbackPanel());
            }
        };
        saveButton.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getObjectWrapper().isReadOnly() &&
                        !getDetailsPage().isForcedPreview();
            }

            @Override
            public boolean isEnabled() {
                //in case user isn't allowed to modify focus data but has
                // e.g. #assign authorization, Save button is disabled on page load.
                // Save button becomes enabled just if some changes are made
                // on the Assignments tab (in the use case with #assign authorization)
//                PrismContainerDefinition def = getObjectWrapper().getDefinition();
                if (ItemStatus.NOT_CHANGED.equals(getObjectWrapper().getStatus())
                        && !getObjectWrapper().canModify()){
                    return areSavePreviewButtonsEnabled();
                }
                return true;
            }
        });
        saveButton.setOutputMarkupId(true);
        saveButton.setOutputMarkupPlaceholderTag(true);
        mainForm.setDefaultButton(saveButton);
        mainForm.add(saveButton);
    }

    // TEMPORARY
    protected void initLayoutPreviewButton(final PageAdminObjectDetails<O> parentPage) {
        AjaxSubmitButton previewButton = new AjaxSubmitButton(ID_PREVIEW_CHANGES, parentPage.createStringResource("pageAdminFocus.button.previewChanges")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                getDetailsPage().previewPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
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
//                PrismContainerDefinition def = getObjectWrapper().getDefinition();
                if (ItemStatus.NOT_CHANGED.equals(getObjectWrapper().getStatus())
                        && !getObjectWrapper().canModify()){
                    return areSavePreviewButtonsEnabled();
                }
                return true;
            }
        });
        previewButton.setOutputMarkupId(true);
        previewButton.setOutputMarkupPlaceholderTag(true);
        mainForm.add(previewButton);
    }

    protected boolean isPreviewButtonVisible(){
        return !getObjectWrapper().isReadOnly();
    }

    protected void initLayoutBackButton(PageAdminObjectDetails<O> parentPage) {
        AjaxButton back = new AjaxButton(ID_BACK, parentPage.createStringResource("pageAdminFocus.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                backPerformed(target);
            }

        };
        back.setOutputMarkupId(true);
        back.setOutputMarkupPlaceholderTag(true);
        mainForm.add(back);
    }

    private void initLayoutEditXmlButton(final PageAdminObjectDetails<O> parentPage){
        AjaxButton editXmlButton = new AjaxButton(ID_EDIT_XML, parentPage.createStringResource("AbstractObjectMainPanel.editXmlButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfirmationPanel confirmationPanel = new ConfirmationPanel(parentPage.getMainPopupBodyId(),
                        parentPage.createStringResource("AbstractObjectMainPanel.confirmEditXmlRedirect")){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(PageDebugView.PARAM_OBJECT_ID, parentPage.getObjectWrapper().getOid());
                        parameters.add(PageDebugView.PARAM_OBJECT_TYPE, parentPage.getCompileTimeClass().getSimpleName());
                        parentPage.navigateToNext(PageDebugView.class, parameters);
                    }

                    @Override
                    public StringResourceModel getTitle() {
                        return new StringResourceModel("pageUsers.message.confirmActionPopupTitle");
                    }
                };

                parentPage.showMainPopup(confirmationPanel, target);
            }
        };
        editXmlButton.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                        AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUG_URL) &&
                        !getObjectWrapper().isReadOnly();
            }
        });
        mainForm.add(editXmlButton);

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
        if (getObjectWrapper().isReadOnly()){
            return false;
        }
//        PrismContainerDefinition def = getObjectWrapper().getDefinition();
        if (ItemStatus.NOT_CHANGED.equals(getObjectWrapper().getStatus())
                && !getObjectWrapper().canModify()){
            return false;
        }
        return true;
    }

    public void reloadSavePreviewButtons(AjaxRequestTarget target){
        target.add(AbstractObjectMainPanel.this.get(ID_MAIN_FORM).get(ID_PREVIEW_CHANGES));
        target.add(AbstractObjectMainPanel.this.get(ID_MAIN_FORM).get(ID_SAVE));

    }

    protected boolean areSavePreviewButtonsEnabled() {
        return false;
    }

    public TabbedPanel<ITab> getTabbedPanel() {
        return (TabbedPanel<ITab>) get(WebComponentUtil.getPageBase(this).createComponentPath(ID_MAIN_FORM, ID_TAB_PANEL));
    }
}
