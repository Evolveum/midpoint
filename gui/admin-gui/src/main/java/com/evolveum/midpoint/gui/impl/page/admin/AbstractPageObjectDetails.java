/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.time.Duration;
import java.util.*;

import com.evolveum.midpoint.gui.impl.component.menu.DetailsNavigationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;

import com.evolveum.midpoint.prism.delta.ChangeType;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class AbstractPageObjectDetails<O extends ObjectType, ODM extends ObjectDetailsModels<O>> extends PageBase {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractPageObjectDetails.class);

    private static final String DOT_CLASS = AbstractPageObjectDetails.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    protected static final String OPERATION_SAVE = DOT_CLASS + "save";
    protected static final String OPERATION_PREVIEW_CHANGES = DOT_CLASS + "previewChanges";
    protected static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
    protected static final String OPERATION_EXECUTE_ARCHETYPE_CHANGES = DOT_CLASS + "executeArchetypeChanges";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_SUMMARY = "summary";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_PROGRESS_PANEL = "progressPanel";
    private static final String ID_DETAILS = "details";

    private ODM objectDetailsModels;

    private ProgressPanel progressPanel;
    protected boolean previewRequested;

    public AbstractPageObjectDetails(PageParameters pageParameters) {
        super(pageParameters);

        objectDetailsModels = createObjectDetailsModels();

        initLayout();

    }

    public ObjectDetailsModels<O> getObjectDetailsModels() {
        return objectDetailsModels;
    }

    //TODO should be abstract??
    protected ODM createObjectDetailsModels() {
        return (ODM) new ObjectDetailsModels<>(createPrismObejctModel(), this);
    }

    protected LoadableModel<PrismObject<O>> createPrismObejctModel() {
        return new LoadableModel<>(false) {

            @Override
            protected PrismObject<O> load() {
                return loadPrismObject();
            }
        };
    }

    private void initLayout() {
        initDetailsPanel();

        progressPanel = new ProgressPanel(ID_PROGRESS_PANEL);
        add(progressPanel);
    }

    private void initDetailsPanel() {
        WebMarkupContainer detialsPanel = new WebMarkupContainer(ID_DETAILS);
        detialsPanel.setOutputMarkupId(true);
        add(detialsPanel);

        detialsPanel.add(initSummaryPanel());
        MidpointForm form = new MidpointForm(ID_MAIN_FORM);
        detialsPanel.add(form);

        initButtons(form);

        ContainerPanelConfigurationType defaultConfiguration = findDefaultConfiguration();
        initMainPanel(defaultConfiguration, form);

        detialsPanel.add(initNavigation());
    }

    private Panel initSummaryPanel() {
        LoadableModel<O> summaryModel = objectDetailsModels.getSummaryModel();
        return createSummaryPanel(ID_SUMMARY, summaryModel);
    }

    private void initButtons(MidpointForm form) {
        OperationalButtonsPanel opButtonPanel = createButtonsPanel(ID_BUTTONS, objectDetailsModels.getObjectWrapperModel());
        opButtonPanel.setOutputMarkupId(true);
//        opButtonPanel.add(new VisibleBehaviour(() -> isOperationalButtonsVisible() && opButtonPanel.buttonsExist()));

        AjaxSelfUpdatingTimerBehavior behavior = new AjaxSelfUpdatingTimerBehavior(Duration.ofMillis(getRefreshInterval())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                refresh(target);
            }

            @Override
            protected boolean shouldTrigger() {
                return isRefreshEnabled();
            }
        };

        opButtonPanel.add(behavior);

        form.add(opButtonPanel);
    }

    //TODO make abstract
    protected OperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<O>> wrapperModel) {
        return new OperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected void addStateButtons(RepeatingView stateButtonsView) {
                initStateButtons(stateButtonsView);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                AbstractPageObjectDetails.this.savePerformed(target);
            }

        };
    }


    public void savePerformed(AjaxRequestTarget target) {
        progressPanel.onBeforeSave();
        previewRequested = false;
        OperationResult result = new OperationResult(OPERATION_SAVE);
        saveOrPreviewPerformed(target, result, false);
    }

    public void previewPerformed(AjaxRequestTarget target) {
        progressPanel.onBeforeSave();
        OperationResult result = new OperationResult(OPERATION_PREVIEW_CHANGES);
        previewRequested = true;
        saveOrPreviewPerformed(target, result, true);
    }

    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
        saveOrPreviewPerformed(target, result, previewOnly, null);
    }

//    private ObjectDelta<O> delta;

    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly, Task task) {


        PrismObjectWrapper<O> objectWrapper = getModelWrapperObject();
        LOGGER.debug("Saving object {}", objectWrapper);

        // todo: improve, delta variable is quickfix for MID-1006
        // redirecting to user list page everytime user is created in repository
        // during user add in gui,
        // and we're not taking care about account/assignment create errors
        // (error message is still displayed)
//        delta = null;

        if (task == null) {
            task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        }

        Collection<ObjectDelta<? extends ObjectType>> deltas;
        try {
            deltas = objectDetailsModels.collectDeltas(result);
        } catch (Throwable ex) {
            result.recordFatalError(getString("pageAdminObjectDetails.message.cantCreateObject"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Create Object failed", ex);
            showResult(result);
            target.add(getFeedbackPanel());
            return;
        }

//        ModelExecuteOptions options = getOptions(previewOnly);

//            LOGGER.debug("Using execute options {}.", options);

//            delta = prepareDelta(objectWrapper, result, target);

            switch (objectWrapper.getStatus()) {
                case ADDED:
                    executeAddDelta(deltas, previewOnly, getExecuteChangesOptionsDto(), task, result, target);
                    break;

                case NOT_CHANGED:
                    executeModifyDelta(deltas, previewOnly, getExecuteChangesOptionsDto(), task, result, target);
                    break;
                // support for add/delete containers (e.g. delete credentials)
                default:
                    error(getString("pageAdminFocus.message.unsupportedState", objectWrapper.getStatus()));
            }


        LOGGER.trace("returning from saveOrPreviewPerformed");
    }

//    @NotNull
//    protected ModelExecuteOptions getOptions(boolean previewOnly) {
//        ModelExecuteOptions options = mainPanel.getExecuteChangeOptionsDto().createOptions(getPrismContext());
//        if (previewOnly) {
//            options.getOrCreatePartialProcessing().setApprovals(PartialProcessingTypeType.PROCESS);
//        }
//        return options;
//    }

    protected ExecuteChangeOptionsDto getExecuteChangesOptionsDto() {
        return getOperationalButtonsPanel().getExecuteChangeOptions();
    }

    protected void reviveModels() throws SchemaException {
        WebComponentUtil.revive(getModel(), getPrismContext());
//        WebComponentUtil.revive(parentOrgModel, getPrismContext());
    }



    private void executeAddDelta(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, ExecuteChangeOptionsDto executeChangeOptionsDto, Task task, OperationResult result, AjaxRequestTarget target) {
        try {
            if (!deltas.isEmpty()) {
                if (checkValidationErrors(target, objectDetailsModels.getValidationErrors())) {
                    return;
                }
                progressPanel.executeChanges(deltas, previewOnly, executeChangeOptionsDto, task, result, target);
            } else {
                result.recordSuccess();
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("pageFocus.message.cantCreateFocus"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Create user failed", ex);
            showResult(result);
        }
    }

    private void executeModifyDelta(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, ExecuteChangeOptionsDto executeChangeOptionsDto, Task task, OperationResult result, AjaxRequestTarget target) {
        //TODO only in UserDetialsPanel
        //boolean delegationChangesExist = processDeputyAssignments(previewOnly);
        try {
            if (deltas.isEmpty() && !executeChangeOptionsDto.isReconcile()) {
                progressPanel.clearProgressPanel();            // from previous attempts (useful only if we would call finishProcessing at the end, but that's not the case now)
                if (!previewOnly) {
//                    if (!delegationChangesExist) {
//                        result.recordWarning(getString("PageAdminObjectDetails.noChangesSave"));
//                        showResult(result);
//                    }  //TODO user page
                    redirectBack();
                } else {
//                    if (!delegationChangesExist) {
//                        warn(getString("PageAdminObjectDetails.noChangesPreview"));
//                        target.add(getFeedbackPanel());
//                    } //TODO user page
                }
                return;
            }
            if (deltas.isEmpty() && executeChangeOptionsDto.isReconcile()) {
                ObjectDelta emptyDelta = getPrismContext().deltaFor(getType()).asObjectDelta(getModelPrismObject().getOid());
                deltas.add(emptyDelta);
            }
            if (checkValidationErrors(target, objectDetailsModels.getValidationErrors())) {
                return;
            }
            progressPanel.executeChanges(deltas, previewOnly, executeChangeOptionsDto, task, result, target);

        } catch (Exception ex) {
            //TODO force
//            if (!executeForceDelete(objectWrapper, task, options, result)) {
//                result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
//                LoggingUtils.logUnexpectedException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
//            } else {
//                result.recomputeStatus();
//            }
            showResult(result);
        }
    }

    private boolean checkValidationErrors(AjaxRequestTarget target, Collection<SimpleValidationError> validationErrors) {
        if (validationErrors != null && !validationErrors.isEmpty()) {
            for (SimpleValidationError error : validationErrors) {
                LOGGER.error("Validation error, attribute: '" + error.printAttribute()
                        + "', message: '" + error.getMessage() + "'.");
                error("Validation error, attribute: '" + error.printAttribute()
                        + "', message: '" + error.getMessage() + "'.");
            }

            target.add(getFeedbackPanel());
            return true;
        }
        return false;
    }

    protected void initStateButtons(RepeatingView stateButtonsView) {

    }

    public int getRefreshInterval() {
        return 30;
    }

    public boolean isRefreshEnabled() {
        return false;
    }

    public void refresh(AjaxRequestTarget target) {
        refresh(target, true);
    }

    public void refresh(AjaxRequestTarget target, boolean soft) {

        if (isEditObject()) {
            objectDetailsModels.reset();
        }
        target.add(getSummaryPanel());
        target.add(getOperationalButtonsPanel());
        target.add(getFeedbackPanel());
        refreshTitle(target);

//        if (soft) {
//            for (Component component : getMainPanel().getTabbedPanel()) {
//                if (component instanceof RefreshableTabPanel) {
//                    for (Component c : ((RefreshableTabPanel) component).getComponentsToUpdate()) {
//                        target.add(c);
//                    }
//                }
//            }
//        } else {
//            target.add(getMainPanel().getTabbedPanel());
//        }
    }

    private ContainerPanelConfigurationType findDefaultConfiguration() {
        //TODO support for second level panel as a default, e.g. assignment -> role
        Optional<ContainerPanelConfigurationType> basicPanelConfig = getPanelConfigurations().getObject().stream().filter(panel -> BooleanUtils.isTrue(panel.isDefault())).findFirst();
        if (basicPanelConfig.isPresent()) {
            return basicPanelConfig.get();
        }

        return getPanelConfigurations().getObject().stream().findFirst().get();
    }

    private void initMainPanel(ContainerPanelConfigurationType panelConfig, MidpointForm form) {
        getSessionStorage().setObjectDetailsStorage("details" + getType().getSimpleName(), panelConfig);
        String panelType = panelConfig.getPanelType();
        if (panelType == null) {
            return;
        }
        Class<? extends Panel> panelClass = findObjectPanel(panelConfig.getPanelType());
        Panel panel = WebComponentUtil.createPanel(panelClass, ID_MAIN_PANEL, objectDetailsModels, panelConfig);
        form.addOrReplace(panel);
    }

    private DetailsNavigationPanel initNavigation() {
        return createNavigationPanel(ID_NAVIGATION, getPanelConfigurations());
    }

    private DetailsNavigationPanel createNavigationPanel(String id, IModel<List<ContainerPanelConfigurationType>> panels) {

        DetailsNavigationPanel panel = new DetailsNavigationPanel(id, objectDetailsModels, panels) {
            @Override
            protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                MidpointForm form = getMainForm();
                initMainPanel(config, form);
                target.add(form);
            }
        };
        return panel;
    }


    private PrismObject<O> loadPrismObject() {
        Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
        OperationResult result = task.getResult();
        PrismObject<O> prismObject;
        try {
            if (!isEditObject()) {
                prismObject = getPrismContext().createObject(getType());
            } else {
                String focusOid = getObjectOidParameter();
                prismObject = WebModelServiceUtils.loadObject(getType(), focusOid, getOperationOptions(), this, task, result);
                LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, prismObject);
            }
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
            prismObject = null;
        }

        showResult(result, false);
        return prismObject;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return null;
    }

    public boolean isEditObject() {
        return getObjectOidParameter() != null;
    }

    protected String getObjectOidParameter() {
        PageParameters parameters = getPageParameters();
        LOGGER.trace("Page parameters: {}", parameters);
        StringValue oidValue = parameters.get(OnePageParameterEncoder.PARAMETER);
        LOGGER.trace("OID parameter: {}", oidValue);
        if (oidValue == null) {
            return null;
        }
        String oid = oidValue.toString();
        if (StringUtils.isBlank(oid)) {
            return null;
        }
        return oid;
    }

    protected LoadableModel<PrismObjectWrapper<O>> getModel() {
        return objectDetailsModels.getObjectWrapperModel();
    }

    protected PrismObject<O> getModelPrismObject() {
        return getModelWrapperObject().getObject();
    }

    protected O getModelObjectType() {
        return getModelPrismObject().asObjectable();
    }

    protected PrismObjectWrapper<O> getModelWrapperObject() {
        return getModel().getObject();
    }

    public IModel<List<ContainerPanelConfigurationType>> getPanelConfigurations() {
        return new PropertyModel<>(objectDetailsModels.getObjectDetailsPageConfiguration(), GuiObjectDetailsPageType.F_PANEL.getLocalPart());
    }

    protected abstract Class<O> getType();
    protected abstract Panel createSummaryPanel(String id, LoadableModel<O> summaryModel);

    protected Component getDetailsPanel() {
        return get(ID_DETAILS);
    }

    private MidpointForm getMainForm() {
        return (MidpointForm) get(createComponentPath(ID_DETAILS, ID_MAIN_FORM));
    }
    protected Component getSummaryPanel() {
        return get(createComponentPath(ID_DETAILS, ID_SUMMARY));
    }
    protected OperationalButtonsPanel getOperationalButtonsPanel() {
        return (OperationalButtonsPanel) get(createComponentPath(ID_DETAILS, ID_MAIN_FORM, ID_BUTTONS));
    }

    public PrismObject<O> getPrismObject() {
        return getModelPrismObject();
    }

//    @Override
//    public void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result) {
//
//        if (previewRequested) {
//            finishPreviewProcessing(target, result);
//            return;
//        }
//        if (result.isSuccess() && getDelta() != null && SecurityUtils.getPrincipalUser().getOid().equals(getDelta().getOid())) {
//            Session.get().setLocale(WebComponentUtil.getLocale());
//            LOGGER.debug("Using {} as locale", getLocale());
//            WebSession.get().getClientInfo().getProperties().
//                    setTimeZone(WebModelServiceUtils.getTimezone());
//            LOGGER.debug("Using {} as time zone", WebSession.get().getClientInfo().getProperties().getTimeZone());
//        }
//        boolean focusAddAttempted = getDelta() != null && getDelta().isAdd();
//        boolean focusAddSucceeded = focusAddAttempted && StringUtils.isNotEmpty(getDelta().getOid());
//
//        // we don't want to allow resuming editing if a new focal object was created (on second 'save' there would be a conflict with itself)
//        // and also in case of partial errors, like those related to projections (many deltas would be already executed, and this could cause problems on second 'save').
//        boolean canContinueEditing = !focusAddSucceeded && result.isFatalError();
//
//        boolean canExitPage;
//        if (returningFromAsync) {
//            canExitPage = getProgressPanel().isAllSuccess() || result.isInProgress() || result.isHandledError(); // if there's at least a warning in the progress table, we would like to keep the table open
//        } else {
//            canExitPage = !canContinueEditing;                            // no point in staying on page if we cannot continue editing (in synchronous case i.e. no progress table present)
//        }
//
//        if (!isKeepDisplayingResults() && canExitPage) {
//            showResult(result);
//            redirectBack();
//        } else {
//            if (returningFromAsync) {
//                getProgressPanel().showBackButton(target);
//                getProgressPanel().hideAbortButton(target);
//            }
//            showResult(result);
//            target.add(getFeedbackPanel());
//
//            if (canContinueEditing) {
//                getProgressPanel().hideBackButton(target);
//                getProgressPanel().showContinueEditingButton(target);
//            }
//        }
//    }

//    private void finishPreviewProcessing(AjaxRequestTarget target, OperationResult result) {
//        getMainPanel().setVisible(true);
//        getProgressPanel().hide();
//        getProgressPanel().hideAbortButton(target);
//        getProgressPanel().hideBackButton(target);
//        getProgressPanel().hideContinueEditingButton(target);
//
//        showResult(result);
//        target.add(getFeedbackPanel());
//
//        Map<PrismObject<O>, ModelContext<? extends ObjectType>> modelContextMap = new LinkedHashMap<>();
//        modelContextMap.put(getModelPrismObject(), getProgressPanel().getPreviewResult());
//
//        //TODO
////        processAdditionalFocalObjectsForPreview(modelContextMap);
//
//        navigateToNext(new PagePreviewChanges(modelContextMap, getModelInteractionService()));
//    }

    protected ProgressPanel getProgressPanel() {
        return (ProgressPanel) get(ID_PROGRESS_PANEL);
    }

    @Override
    protected void createBreadcrumb() {
        createInstanceBreadcrumb();
    }
}
