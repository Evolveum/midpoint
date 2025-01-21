/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuAuthzUtil;

import com.evolveum.midpoint.web.page.error.PageError404;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.menu.DetailsNavigationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class AbstractPageObjectDetails<O extends ObjectType, ODM extends ObjectDetailsModels<O>> extends PageBase {

    public static final String PARAM_PANEL_ID = "panelId";

    private static final Trace LOGGER = TraceManager.getTrace(AbstractPageObjectDetails.class);

    private static final String DOT_CLASS = AbstractPageObjectDetails.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    protected static final String OPERATION_SAVE = DOT_CLASS + "save";
    protected static final String OPERATION_PREVIEW_CHANGES = DOT_CLASS + "previewChanges";
    protected static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_SUMMARY = "summary";
    private static final String ID_BUTTONS = "buttons";

    private static final String ID_DETAILS = "details";
    protected static final String ID_DETAILS_VIEW = "detailsView";

    private ODM objectDetailsModels;
    private final boolean isAdd;
    private boolean isShowedByWizard;

    public AbstractPageObjectDetails() {
        this(null, null, null);
    }

    public AbstractPageObjectDetails(PageParameters pageParameters) {
        this(pageParameters, null, null);
    }

    public AbstractPageObjectDetails(PrismObject<O> object) {
        this(null, object, null);
    }

    private AbstractPageObjectDetails(PageParameters params, PrismObject<O> object, List<BusinessRoleDto> patternDeltas) {
        super(params);
        isAdd = (params == null || params.isEmpty()) && object == null;
        objectDetailsModels = createObjectDetailsModels(object);

//        if (patternDeltas != null && !patternDeltas.isEmpty()) {
//            objectDetailsModels.addPatternDeltas(patternDeltas);
//        }
    }

    protected void postProcessModel(ODM objectDetailsModels) {

    }

    public AbstractPageObjectDetails(PrismObject<O> object, List<BusinessRoleDto> patternDeltas) {
        this(null, object, patternDeltas);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        postProcessModel(objectDetailsModels);
        initLayout();
    }

    @Override
    protected void onDetach() {
        objectDetailsModels.detach();

        super.onDetach();
    }

    protected boolean isAdd() {
        return isAdd;
    }

    protected void reloadObjectDetailsModel(PrismObject<O> prismObject) {
        objectDetailsModels = createObjectDetailsModels(prismObject);
    }

    public ODM getObjectDetailsModels() {
        return objectDetailsModels;
    }

    //TODO should be abstract??
    protected ODM createObjectDetailsModels(PrismObject<O> object) {
        return (ODM) new ObjectDetailsModels<>(createPrismObjectModel(object), this);
    }

    protected LoadableDetachableModel<PrismObject<O>> createPrismObjectModel(PrismObject<O> object) {
        return new LoadableDetachableModel<>() {

            @Override
            protected PrismObject<O> load() {
                if (object != null) {
                    return object;
                }
                return loadPrismObject();
            }
        };
    }

    protected LoadableDetachableModel<List<PrismObject<O>>> createPrismObjectModel(List<PrismObject<UserType>> object) {

        List<PrismObject<O>> test = new ArrayList<>();
        for (PrismObject<UserType> userTypePrismObject : object) {
            PrismObject<O> prismObject;

            prismObject = userTypePrismObject.asObjectable().asPrismContainer();
            test.add(prismObject);
        }
        return new LoadableDetachableModel<>() {

            @Override
            protected List<PrismObject<O>> load() {
                return test;
            }

            ;
        };
    }

    protected void initLayout() {

        DetailsFragment detailsFragment = createDetailsFragment();
        add(detailsFragment);

    }

    protected DetailsFragment createDetailsFragment() {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_DETAILS, AbstractPageObjectDetails.this) {

            @Override
            protected void initFragmentLayout() {
                add(initSummaryPanel());
                MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onDetach() {
                        resetValidatedValue();
                        super.onDetach();
                    }

                };
                form.add(new FormWrapperValidator(AbstractPageObjectDetails.this) {

                    @Override
                    protected PrismObjectWrapper getObjectWrapper() {
                        return getModelWrapperObject();
                    }
                });
                form.setMultiPart(true);
                add(form);

                initButtons(form);

                ContainerPanelConfigurationType defaultConfiguration = findDefaultConfiguration();
                initMainPanel(defaultConfiguration, form);

                form.add(initNavigation());
            }
        };
    }

    private Panel initSummaryPanel() {
        LoadableDetachableModel<O> summaryModel = objectDetailsModels.getSummaryModel();
        Panel summaryPanel = createSummaryPanel(ID_SUMMARY, summaryModel);
        summaryPanel.add(new VisibleBehaviour(() -> objectDetailsModels.getObjectStatus() != ItemStatus.ADDED));
        return summaryPanel;
    }

    private void initButtons(MidpointForm form) {
        OperationalButtonsPanel opButtonPanel = createButtonsPanel(ID_BUTTONS, objectDetailsModels.getObjectWrapperModel());
        opButtonPanel.setOutputMarkupId(true);
        form.add(opButtonPanel);
    }

    //TODO make abstract
    protected OperationalButtonsPanel<O> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<O>> wrapperModel) {
        return new OperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void addStateButtons(RepeatingView stateButtonsView) {
                initStateButtons(stateButtonsView);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                AbstractPageObjectDetails.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return AbstractPageObjectDetails.this.hasUnsavedChanges(target);
            }

            @Override
            protected boolean isSaveButtonVisible() {
                return super.isSaveButtonVisible();
            }
        };
    }

    public boolean hasUnsavedChanges(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SAVE);

        try {
            Collection<ObjectDelta<? extends ObjectType>> deltas = getObjectDetailsModels().collectDeltas(result);

            return !deltas.isEmpty();
        } catch (Throwable ex) {
            result.recordFatalError(getString("pageAdminObjectDetails.message.cantCreateObject"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute delta changes", ex);
            showResult(result);
            target.add(getFeedbackPanel());

            return true;
        }
    }

    public void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SAVE);
        saveOrPreviewPerformed(target, result, false);
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly) {
        return saveOrPreviewPerformed(target, result, previewOnly, null);
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly, Task task) {

        PrismObjectWrapper<O> objectWrapper = getModelWrapperObject();
        LOGGER.debug("Saving object {}", objectWrapper);

        if (task == null) {
            task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        }

        ExecuteChangeOptionsDto options = getExecuteChangesOptionsDto();
        Collection<ObjectDelta<? extends ObjectType>> deltas;
        try {
            if (isShowedByWizard()) {
                deltas = getObjectDetailsModels().collectDeltaWithoutSavedDeltas(result);
            } else {
                deltas = getObjectDetailsModels().collectDeltas(result);
            }
            checkValidationErrors(target, objectDetailsModels.getValidationErrors());

        } catch (Throwable ex) {
            String messageKey = isAdd() ? "pageAdminObjectDetails.message.cantCreateObject" : "pageAdminObjectDetails.message.cantModifyObject";
            result.recordFatalError(getString(messageKey), ex);
            LoggingUtils.logUnexpectedException(LOGGER, getString(messageKey), ex);
            showResult(result);
            target.add(getFeedbackPanel());
            return null;
        }

        LOGGER.trace("returning from saveOrPreviewPerformed");

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = executeChanges(deltas, previewOnly,
                options, task, result, target);

        if (!isShowedByWizard()) {
            postProcessResult(result, executedDeltas, target);
        } else {
            reloadObject(result, executedDeltas, target);
        }

        return executedDeltas;
    }

    private void reloadObject(OperationResult result, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, AjaxRequestTarget target) {
        if (!result.isError()) {
            if (executedDeltas != null) {
                String resourceOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
                if (resourceOid != null) {
                    Task task = createSimpleTask("load resource after save");
                    @Nullable PrismObject<O> object = WebModelServiceUtils.loadObject(
                            getType(),
                            resourceOid,
                            getOperationOptions(),
                            AbstractPageObjectDetails.this,
                            task,
                            task.getResult());
                    if (object != null) {
                        getObjectDetailsModels().reset();
                        getObjectDetailsModels().reloadPrismObjectModel(object);
                    }
                }
            }

            result.computeStatusIfUnknown();
        } else {
            target.add(getFeedbackPanel());
        }
    }

    protected void postProcessResult(OperationResult result,
            Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
            AjaxRequestTarget target) {
        result.computeStatusIfUnknown();
        if (allowRedirectBack() && !result.isError()) {
            navigateAction();
        } else {
            target.add(getFeedbackPanel());
        }
    }

    protected void navigateAction() {
        Class<? extends PageBase> objectListPage = DetailsPageUtil.getObjectListPage(getType());
        var pageClass = DetailsPageUtil.getObjectListPage(getType());
        if (!canRedirectBack() && pageClass != null && isAuthorized(pageClass)) {
            navigateToNext(objectListPage);
        } else {
            redirectBack();
        }
    }

    private boolean isAuthorized(Class<? extends PageBase> pageClass) {
        try {
            List<String> pageAuths = LeftMenuAuthzUtil.getAuthorizationsForPage(pageClass);
            for (String auth : pageAuths) {
                if (!isAuthorized(auth)) {
                    return false;
                }
            }
        } catch (Exception e) {
            //nothing to do here
        }
        return true;
    }

    protected Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, ExecuteChangeOptionsDto options, Task task, OperationResult result, AjaxRequestTarget target) {
        if (noChangesToExecute(deltas, options)) {
            if (!isShowedByWizard()) {
                result.recordWarning(getString("PageAdminObjectDetails.noChangesSave"));
                showResult(result);
            } else {
                result.recordSuccess();
            }
            return null;
        }
        //TODO force
        ////            if (!executeForceDelete(objectWrapper, task, options, result)) {
////                result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
////                LoggingUtils.logUnexpectedException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
////            } else {
////                result.recomputeStatus();
////            }

        //TODO this is just a quick hack.. for focus objects, feedback panel and results are processed by ProgressAware.finishProcessing()

        ObjectChangeExecutor changeExecutor;
        if (!isShowedByWizard()) {
            changeExecutor = getChangeExecutor();
        } else {
            changeExecutor = getDefaultChangeExecutor();
        }
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = changeExecutor.executeChanges(deltas, previewOnly, task, result, target);

        showResultAfterExecuteChanges(changeExecutor, result);

        return executedDeltas;
    }

    protected void showResultAfterExecuteChanges(ObjectChangeExecutor changeExecutor, OperationResult result) {
        if (changeExecutor instanceof ObjectChangesExecutorImpl
                && (!isShowedByWizard() || !result.isSuccess())) {
            showResult(result);
        }
    }

    protected boolean isShowedByWizard() {
        return isShowedByWizard;
    }

    protected void setShowedByWizard(boolean state) {
        getFeedbackPanel().setVisible(!state);
        isShowedByWizard = state;
    }

    protected boolean noChangesToExecute(Collection<ObjectDelta<? extends ObjectType>> deltas, ExecuteChangeOptionsDto options) {
        return deltas.isEmpty();
    }

    protected boolean allowRedirectBack() {
        return true;
    }

    protected ExecuteChangeOptionsDto getExecuteChangesOptionsDto() {
        return new ExecuteChangeOptionsDto();
    }

    protected void reviveModels() throws SchemaException {
        WebComponentUtil.revive(getModel(), getPrismContext());
    }

    protected ObjectChangeExecutor getChangeExecutor() {
        return getDefaultChangeExecutor();
    }

    private ObjectChangeExecutor getDefaultChangeExecutor() {
        return new ObjectChangesExecutorImpl();
    }

    private void checkValidationErrors(AjaxRequestTarget target, Collection<SimpleValidationError> validationErrors) {
        if (validationErrors != null && !validationErrors.isEmpty()) {
            for (SimpleValidationError error : validationErrors) {
                LOGGER.error("Validation error, attribute: '" + error.printAttribute()
                        + "', message: '" + error.getMessage() + "'.");
                error("Validation error, attribute: '" + error.printAttribute()
                        + "', message: '" + error.getMessage() + "'.");
            }

            target.add(getFeedbackPanel());
            throw new IllegalStateException("Validation errors found");
        }
    }

    protected void initStateButtons(RepeatingView stateButtonsView) {

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
        target.add(get(ID_DETAILS_VIEW));
        refreshTitle(target);
    }

    private ContainerPanelConfigurationType findDefaultConfiguration() {
        String panelId = WebComponentUtil.getPanelIdentifierFromParams(getPageParameters());

        ContainerPanelConfigurationType defaultConfiguration = findDefaultConfiguration(getPanelConfigurations().getObject(), panelId);

        if (defaultConfiguration != null && WebComponentUtil.getElementVisibility(defaultConfiguration.getVisibility())) {
            return defaultConfiguration;
        }

        if (panelId != null) {
            //wrong panel id or hidden panel
            getSession().error(
                    createStringResource(
                            "AbstractPageObjectDetails.panelNotFound", panelId, getPageTitleModel().getObject()).getString());
            throw new RestartResponseException(PageError404.class);
        }

        return getPanelConfigurations().getObject()
                .stream()
                .filter(config -> isApplicableForOperation(config) && WebComponentUtil.getElementVisibility(config.getVisibility()))
                .findFirst()
                .orElseGet(() -> null);
    }

    private ContainerPanelConfigurationType findDefaultConfiguration(List<ContainerPanelConfigurationType> configs, String panelIdentifier) {
        List<ContainerPanelConfigurationType> subConfigs = new ArrayList<>();
        for (ContainerPanelConfigurationType config : configs) {
            subConfigs.addAll(config.getPanel());
            if (panelIdentifier != null) {
                if (config.getIdentifier().equals(panelIdentifier)) {
                    return config;
                }
                continue;
            }
            if (isApplicable(config)) {
                return config;
            }
        }
        if (subConfigs.isEmpty()) {
            return null;
        }
        return findDefaultConfiguration(subConfigs, panelIdentifier);
    }

    private boolean isApplicable(ContainerPanelConfigurationType config) {
        return BooleanUtils.isTrue(config.isDefault()) && isApplicableForOperation(config) && WebComponentUtil.getElementVisibility(config.getVisibility());
    }

    private boolean isApplicableForOperation(ContainerPanelConfigurationType configurationType) {
        if (configurationType.getApplicableForOperation() == null) { //applicable for all
            return true;
        }

        if (configurationType.getApplicableForOperation() == OperationTypeType.ADD && !isEditObject()) {
            return true;
        }

        if (configurationType.getApplicableForOperation() == OperationTypeType.MODIFY && isEditObject()) {
            return true;
        }
        return false;
    }

    private void initMainPanel(ContainerPanelConfigurationType panelConfig, MidpointForm form) {
        if (panelConfig == null) {
            addErrorPanel(false, form, MessagePanel.MessagePanelType.WARN, "AbstractPageObjectDetails.noPanels");
            return;
        }

        getSessionStorage().setObjectDetailsStorage("details" + getType().getSimpleName(), panelConfig);
        String panelType = panelConfig.getPanelType();

        if (panelType == null && LOGGER.isDebugEnabled()) {
            //No panel defined, just grouping element, e.g. top "Assignments" in details navigation menu
            LOGGER.debug("AbstractPageObjectDetails.panelTypeUndefined {}", panelConfig.getIdentifier());
            form.addOrReplace(new WebMarkupContainer(ID_MAIN_PANEL));
            return;
        }

        Class<? extends Panel> panelClass = findObjectPanel(panelType);
        if (panelClass == null) {
            //panel type defined, but no class found. Something strange happened.
            addErrorPanel(false, form, MessagePanel.MessagePanelType.ERROR, "AbstractPageObjectDetails.panelTypeUndefined", panelConfig.getIdentifier());
            return;
        }

        Component panel = WebComponentUtil.createPanel(panelClass, ID_MAIN_PANEL, objectDetailsModels, panelConfig);
        if (panel != null) {
            panel.add(AttributeAppender.append("class", () -> {
                List panels = getPanelConfigurations().getObject();
                if (panels == null || panels.size() <= 1) {
                    return "flex-grow-1";
                }

                return null;
            }));
            form.addOrReplace(panel);
            return;
        }

        addErrorPanel(true, form, MessagePanel.MessagePanelType.ERROR, "AbstractPageObjectDetails.panelErrorInitialization", panelConfig.getIdentifier(), panelType);
    }

    private void addErrorPanel(boolean force, MidpointForm form, MessagePanel.MessagePanelType type, String message, Object... params) {
        if (!force && form.get(ID_MAIN_PANEL) != null) {
            return;
        }
        WebMarkupContainer panel = createMessagePanel(ID_MAIN_PANEL, type, message, params);
        panel.add(AttributeAppender.append("style", "margin-top: 20px;"));
        form.addOrReplace(panel);
    }

    private DetailsNavigationPanel initNavigation() {
        return createNavigationPanel(getPanelConfigurations());
    }

    private DetailsNavigationPanel<O> createNavigationPanel(IModel<List<ContainerPanelConfigurationType>> panels) {
        DetailsNavigationPanel panel = new DetailsNavigationPanel<>(AbstractPageObjectDetails.ID_NAVIGATION, objectDetailsModels, panels) {

            @Override
            protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                replacePanel(config, target);
            }
        };
        panel.add(new VisibleBehaviour(() -> panels.getObject() != null && panels.getObject().size() > 1));

        return panel;
    }

    public void replacePanel(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
        MidpointForm form = getMainForm();
        try {
            initMainPanel(config, form);
            target.add(getFeedbackPanel());

            if (config != null && config.getPanelType() != null) {
                overwritePageParameters(config);
            }
            target.add(AbstractPageObjectDetails.this);
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Can't instantiate panel based on config\n {}", config.debugDump(), e);
            }

            error(getString("AbstractPageObjectDetails.replacePanelException", e.getMessage(), e.getClass().getSimpleName()));
            target.add(getFeedbackPanel());
        }
    }

    private void overwritePageParameters(ContainerPanelConfigurationType config) {
        PageParameters newParams = new PageParameters(getPageParameters());
        newParams.set(PARAM_PANEL_ID, config.getIdentifier());
        getPageParameters().overwriteWith(newParams);
    }

    private PrismObject<O> loadPrismObject() {
        Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
        OperationResult result = task.getResult();
        PrismObject<O> prismObject = null;
        try {
            if (!isEditObject()) {
                prismObject = getPrismContext().createObject(getType());
            } else {
                String focusOid = getObjectOidParameter();
                prismObject = WebModelServiceUtils.loadObject(getType(), focusOid, getOperationOptions(), false, this, task, result);
                LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, prismObject);
            }
        } catch (RestartResponseException e) {
            //ignore restart exception
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
            throw redirectBackViaRestartResponseException();
        }
        result.computeStatusIfUnknown();
        if (prismObject == null && result.isFatalError()) {
            getSession().getFeedbackMessages().clear();
            getSession().error(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"));
            throw new RestartResponseException(PageError404.class);
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
        return OnePageParameterEncoder.getParameter(this);
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

    public abstract Class<O> getType();

    protected abstract Panel createSummaryPanel(String id, IModel<O> summaryModel);

    private MidpointForm getMainForm() {
        return (MidpointForm) get(createComponentPath(ID_DETAILS_VIEW, ID_MAIN_FORM));
    }

    protected Component getSummaryPanel() {
        return get(createComponentPath(ID_DETAILS_VIEW, ID_SUMMARY));
    }

    protected OperationalButtonsPanel getOperationalButtonsPanel() {
        return (OperationalButtonsPanel) get(createComponentPath(ID_DETAILS_VIEW, ID_MAIN_FORM, ID_BUTTONS));
    }

    public PrismObject<O> getPrismObject() {
        return getModelPrismObject();
    }

    protected SummaryPanelSpecificationType getSummaryPanelSpecification() {
        return getObjectDetailsModels().getSummaryPanelSpecification();
    }

    private void resetValidatedValue() {
        List<ItemWrapper> iws = new ArrayList<>();
        PrismObjectWrapper<O> wrapper = getModelWrapperObject();
        WebPrismUtil.collectWrappers(wrapper, iws);

        iws.forEach(iw -> iw.setValidated(false));
    }
}
