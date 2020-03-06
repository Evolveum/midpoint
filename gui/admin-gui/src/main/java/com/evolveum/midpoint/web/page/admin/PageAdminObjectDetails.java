/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.*;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.show.PagePreviewChanges;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.page.admin.server.OperationalButtonsPanel;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;

/**
 * @author semancik
 */
public abstract class PageAdminObjectDetails<O extends ObjectType> extends PageAdmin
        implements ProgressReportingAwarePage {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageAdminObjectDetails.class.getName() + ".";

    public static final String PARAM_RETURN_PAGE = "returnPage";

    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_LOAD_PARENT_ORGS = DOT_CLASS + "loadParentOrgs";
    private static final String OPERATION_LOAD_GUI_CONFIGURATION = DOT_CLASS + "loadGuiConfiguration";
    protected static final String OPERATION_SAVE = DOT_CLASS + "save";
    protected static final String OPERATION_PREVIEW_CHANGES = DOT_CLASS + "previewChanges";
    protected static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
    protected static final String OPERATION_LOAD_ARCHETYPE_REF = DOT_CLASS + "loadArchetypeRef";
    protected static final String OPERATION_EXECUTE_CHANGES = DOT_CLASS + "executeChangesTask";
    protected static final String OPERATION_EXECUTE_ARCHETYPE_CHANGES = DOT_CLASS + "executeArchetypeChanges";
    protected static final String OPERATION_LOAD_FILTERED_ARCHETYPES = DOT_CLASS + "loadFilteredArchetypes";

    protected static final String ID_SUMMARY_PANEL = "summaryPanel";
    protected static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_PROGRESS_PANEL = "progressPanel";
    private static final String ID_BUTTONS = "buttons";

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminObjectDetails.class);

    private LoadableModel<PrismObjectWrapper<O>> objectModel;
    private LoadableModel<List<FocusSubwrapperDto<OrgType>>> parentOrgModel;

    private ProgressPanel progressPanel;

    // used to determine whether to leave this page or stay on it (after
    // operation finishing)
    private ObjectDelta<O> delta;

    private AbstractObjectMainPanel<O> mainPanel;
    private boolean saveOnConfigure;        // ugly hack - whether to invoke 'Save' when returning to this page

    private boolean editingFocus = false;             //before we got isOidParameterExists status depending only on oid parameter existence
                                                    //we should set editingFocus=true not only when oid parameter exists but also
                                                    //when object is given as a constructor parameter

    public boolean isEditingFocus() {
        return editingFocus;
    }

    @Override
    protected void createBreadcrumb() {
        createInstanceBreadcrumb();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (saveOnConfigure) {
            saveOnConfigure = false;
            add(new AbstractAjaxTimerBehavior(Duration.milliseconds(100)) {
                @Override
                protected void onTimer(AjaxRequestTarget target) {
                    stop(target);
                    savePerformed(target);
                }
            });
        }
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        if (PageAdminObjectDetails.this instanceof PageSystemConfiguration){
            return super.createPageTitleModel();
        }
        String simpleName = getObjectSimpleName();
        String lokalizedSimpleName = new StringResourceModel("ObjectType." + simpleName).setDefaultValue(simpleName).getString();
        if (isAdd()) {
            return createStringResource("PageAdminObjectDetails.title.new", lokalizedSimpleName);
        }

        String name = null;
        if (getObjectWrapper() != null && getObjectWrapper().getObject() != null) {
            name = WebComponentUtil.getName(getObjectWrapper().getObject());
        }

        return createStringResource("PageAdminObjectDetails.title.edit.readonly.${readOnly}", getObjectModel(), lokalizedSimpleName, name);
    }

    private String getObjectSimpleName(){
        if (getObjectWrapper() != null && getObjectWrapper().getObject() != null
                && getObjectWrapper().getObject().asObjectable() instanceof AssignmentHolderType){
            AssignmentHolderType assignmentHolderObj = (AssignmentHolderType) getObjectWrapper().getObject().asObjectable();
            DisplayType displayType = WebComponentUtil
                    .getArchetypePolicyDisplayType(assignmentHolderObj, PageAdminObjectDetails.this);
            if (displayType == null){
                ObjectReferenceType archetypeReference = getObjectArchetypeRef();
                if (archetypeReference != null){
                    OperationResult result = new OperationResult(OPERATION_LOAD_ARCHETYPE_REF);
                    Task task = createSimpleTask(OPERATION_LOAD_ARCHETYPE_REF);
                    PrismObject<ArchetypeType> archetypeObj = WebModelServiceUtils.resolveReferenceNoFetch(archetypeReference,
                            this, task, result);
                    displayType = archetypeObj != null && archetypeObj.asObjectable().getArchetypePolicy() != null ?
                            archetypeObj.asObjectable().getArchetypePolicy().getDisplay() : null;
                }
            }
            if (displayType != null && displayType.getLabel() != null) {
                String archetypeLocalizedName = getLocalizationService()
                        .translate(displayType.getLabel().toPolyString(), WebComponentUtil.getCurrentLocale(), true);
                if (StringUtils.isNotEmpty(archetypeLocalizedName)) {
                    return archetypeLocalizedName;
                }
            }
        }
        return getCompileTimeClass().getSimpleName();
    }

    public boolean isAdd() {
        return !isOidParameterExists() && !editingFocus;
    }

    public LoadableModel<PrismObjectWrapper<O>> getObjectModel() {
        return objectModel;
    }

    protected AbstractObjectMainPanel<O> getMainPanel() {
        return mainPanel;
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
        return objectModel.getObject();
    }

    public List<FocusSubwrapperDto<OrgType>> getParentOrgs() {
        return parentOrgModel.getObject();
    }

    public ObjectDelta<O> getDelta() {
        return delta;
    }

    public void setDelta(ObjectDelta<O> delta) {
        this.delta = delta;
    }

    public ProgressPanel getProgressPanel() {
        return progressPanel;
    }

    protected void reviveModels() throws SchemaException {
        WebComponentUtil.revive(objectModel, getPrismContext());
        WebComponentUtil.revive(parentOrgModel, getPrismContext());
    }

    public abstract Class<O> getCompileTimeClass();


    public void initialize(final PrismObject<O> objectToEdit) {
        boolean isNewObject = objectToEdit == null && StringUtils.isEmpty(getObjectOidParameter());

        initialize(objectToEdit, isNewObject, false);
    }

    public void initialize(final PrismObject<O> objectToEdit, boolean isNewObject) {
        initialize(objectToEdit, isNewObject, false);
    }

    public void initialize(final PrismObject<O> objectToEdit, boolean isNewObject, boolean isReadonly) {
        initializeModel(objectToEdit, isNewObject, isReadonly);
//        initLayout();
    }

    protected void initializeModel(final PrismObject<O> objectToEdit, boolean isNewObject, boolean isReadonly) {
        editingFocus = !isNewObject;

        objectModel = new LoadableModel<PrismObjectWrapper<O>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PrismObjectWrapper<O> load() {
                PrismObjectWrapper<O> wrapper = loadObjectWrapper(objectToEdit, isReadonly);
//                wrapper.sort();
                return wrapper;
            }
        };

        parentOrgModel = new LoadableModel<List<FocusSubwrapperDto<OrgType>>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<FocusSubwrapperDto<OrgType>> load() {
                return loadOrgWrappers();
            }
        };
    }

    private ObjectReferenceType getObjectArchetypeRef() {
        ObjectReferenceType archetypeReference = null;
        if (getObjectWrapper() != null && getObjectWrapper().getObject() != null
                && getObjectWrapper().getObject().asObjectable() instanceof AssignmentHolderType) {
            AssignmentHolderType assignmentHolderObj = (AssignmentHolderType) getObjectWrapper().getObject().asObjectable();
            if (assignmentHolderObj.getAssignment() != null) {
                for (AssignmentType assignment : assignmentHolderObj.getAssignment()) {
                    if (assignment.getTargetRef() != null && assignment.getTargetRef().getType() != null
                            && QNameUtil.match(assignment.getTargetRef().getType(), ArchetypeType.COMPLEX_TYPE)) {
                        archetypeReference = assignment.getTargetRef();
                        break;
                    }
                }
            }
        }
        return archetypeReference;
    }

    protected List<FocusSubwrapperDto<OrgType>> loadOrgWrappers() {
        // WRONG!! TODO: fix
        return null;
    }

    protected abstract O createNewObject();

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        initLayoutSummaryPanel();
        initOperationalButtonsPanel();

        mainPanel = createMainPanel(ID_MAIN_PANEL);
        mainPanel.setOutputMarkupId(true);
        add(mainPanel);

        progressPanel = new ProgressPanel(ID_PROGRESS_PANEL);
        add(progressPanel);
    }

    protected abstract ObjectSummaryPanel<O> createSummaryPanel();

    protected void initLayoutSummaryPanel() {

        ObjectSummaryPanel<O> summaryPanel = createSummaryPanel();
        summaryPanel.setOutputMarkupId(true);

        setSummaryPanelVisibility(summaryPanel);
        add(summaryPanel);
    }

    protected void setSummaryPanelVisibility(ObjectSummaryPanel<O> summaryPanel){
        summaryPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isOidParameterExists() || editingFocus;
            }
        });
    }

    private void initOperationalButtonsPanel(){
        OperationalButtonsPanel opButtonPanel = new OperationalButtonsPanel(ID_BUTTONS) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void addButtons(RepeatingView repeatingView) {
                initOperationalButtons(repeatingView);
            }
        };
        if (getAdditionalOperationalButtonPanelBehaviors() != null && !getAdditionalOperationalButtonPanelBehaviors().isEmpty()){
            getAdditionalOperationalButtonPanelBehaviors().forEach(this::add);
        }
        opButtonPanel.setOutputMarkupId(true);
        opButtonPanel.add(new VisibleBehaviour(() -> isEditingFocus() && opButtonPanel.buttonsExist()));
        add(opButtonPanel);
    }

    protected List<? extends Behavior> getAdditionalOperationalButtonPanelBehaviors(){
        return new ArrayList<>();
    }

    protected void initOperationalButtons(RepeatingView repeatingView){
        if (getObjectArchetypeRef() != null) {
            AjaxButton changeArchetype = new AjaxButton(repeatingView.newChildId(), createStringResource("PageAdminObjectDetails.button.changeArchetype")) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    changeArchetypeButtonClicked(target);
                }
            };
            changeArchetype.add(new VisibleBehaviour(() -> getObjectArchetypeRef() != null));
            changeArchetype.add(AttributeAppender.append("class", "btn-default"));
            repeatingView.add(changeArchetype);
        }
    }

    protected OperationalButtonsPanel getOperationalButtonsPanel(){
        return (OperationalButtonsPanel) get(ID_BUTTONS);
    }

    private void changeArchetypeButtonClicked(AjaxRequestTarget target){

        AssignmentPopup changeArchetypePopup = new AssignmentPopup(getMainPopupBodyId()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void addPerformed(AjaxRequestTarget target, List newAssignmentsList) {
                super.addPerformed(target, newAssignmentsList);
                try {
                    PrismContainerWrapper<AssignmentType> assignmentsWrapper = getObjectWrapper().findContainer(FocusType.F_ASSIGNMENT);
                    ((List<AssignmentType>) newAssignmentsList).forEach(assignment -> {
                        PrismContainerValue<AssignmentType> newAssignment = assignmentsWrapper.getItem().createNewValue();
                        assignmentsWrapper.getValues().forEach(assignmentValue -> {
                            if (assignmentValue.getRealValue().getTargetRef() != null
                            && assignmentValue.getRealValue().getTargetRef().getType() != null
                            && QNameUtil.match(assignmentValue.getRealValue().getTargetRef().getType(), ArchetypeType.COMPLEX_TYPE)){
                                assignmentValue.setStatus(ValueStatus.DELETED);
                            }
                        });
                        AssignmentType assignmentType = newAssignment.asContainerable();
                        assignmentType.setTargetRef(assignment.getTargetRef());
                        WebPrismUtil.createNewValueWrapper(assignmentsWrapper, newAssignment, PageAdminObjectDetails.this, target);
                        OperationResult result = new OperationResult(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
                        Task task = createSimpleTask(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
                        try {
                            ObjectDelta<O> archetypeDelta = getObjectWrapper().getObjectDelta();
                            if (!archetypeDelta.isEmpty()) {
                                archetypeDelta.revive(getPrismContext());
                                getModelService().executeChanges(WebComponentUtil.createDeltaCollection(archetypeDelta), null, task, result);
                                result.computeStatus();
                            }
                        } catch (Exception e) {
                            LOGGER.error("Cannot save archetype assignment changes: {}", e.getMessage());
                        }
                        showResult(result);
                    });
                } catch (SchemaException e) {
                    LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage());
                }
                target.add(PageAdminObjectDetails.this.getFeedbackPanel());
            }

            @Override
            protected List<ITab> createAssignmentTabs() {
                List<ITab> tabs = new ArrayList<>();

                tabs.add(new PanelTab(getPageBase().createStringResource("ObjectTypes.ARCHETYPE"),
                        new VisibleBehaviour(() -> true)) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel<ArchetypeType>(panelId, ObjectTypes.ARCHETYPE) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                                PrismContainerWrapper<AssignmentType> assignmentsWrapper = null;
                                try {
                                    assignmentsWrapper = getObjectWrapper().findContainer(FocusType.F_ASSIGNMENT);
                                } catch (SchemaException e) {
                                    LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage());
                                }
                                return assignmentsWrapper;
                            }

                            @Override
                            protected List<QName> getSupportedRelations() {
                                return Collections.singletonList(SchemaConstants.ORG_DEFAULT);
                            }

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<ArchetypeType>> rowModel) {
                                target.add(getObjectListPanel());
                                tabLabelPanelUpdate(target);
                            }

                            @Override
                            protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<ArchetypeType>> rowModel){
                                if (rowModel == null){
                                    return Model.of(false);
                                }
                                List selectedObjects = getSelectedObjectsList();
                                return Model.of(selectedObjects == null || selectedObjects.size() == 0
                                        || (rowModel.getObject() != null && rowModel.getObject().isSelected()));
                            }


                            @Override
                            protected ObjectTypes getObjectType() {
                                return ObjectTypes.ARCHETYPE;
                            }

                            @Override
                            protected ObjectQuery addFilterToContentQuery(ObjectQuery query){
                                super.addFilterToContentQuery(query);
                                if (query == null) {
                                    query = getPrismContext().queryFactory().createQuery();
                                }
                                List<String> archetypeOidsList = getFilteredArchetypeOidsList();
                                ObjectFilter filter = getPrismContext().queryFor(ArchetypeType.class)
                                        .id(archetypeOidsList.toArray(new String[0]))
                                        .buildFilter();
                                query.addFilter(filter);
                                return query;
                            }
                        };
                    }
                });
                return tabs;
            }
        };

        changeArchetypePopup.setOutputMarkupPlaceholderTag(true);
        showMainPopup(changeArchetypePopup, target);

    }

    private List<String> getFilteredArchetypeOidsList(){
        OperationResult result = new OperationResult(OPERATION_LOAD_FILTERED_ARCHETYPES);
        PrismObject obj = getObjectWrapper().getObject();
        List<String> oidsList = new ArrayList<>();
        try {
            List<ArchetypeType> filteredArchetypes = getModelInteractionService().getFilteredArchetypesByHolderType(obj, result);
            if (filteredArchetypes != null){
                filteredArchetypes.forEach(archetype -> oidsList.add(archetype.getOid()));
            }
        } catch (SchemaException ex){
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment target specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return oidsList;
    }

    protected abstract AbstractObjectMainPanel<O> createMainPanel(String id);

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

    protected ObjectSummaryPanel<O> getSummaryPanel() {
        return (ObjectSummaryPanel<O>) get(ID_SUMMARY_PANEL);
    }

    public boolean isOidParameterExists() {
        return getObjectOidParameter() != null;
    }

    protected PrismObjectWrapper<O> loadObjectWrapper(PrismObject<O> objectToEdit, boolean isReadonly) {
        Task task = createSimpleTask(OPERATION_LOAD_OBJECT);
        OperationResult result = task.getResult();
        PrismObject<O> object = null;
        try {
            if (!isOidParameterExists()) {
                if (objectToEdit == null) {
                    LOGGER.trace("Loading object: New object (creating)");
                    O focusType = createNewObject();

                    // Apply subtype using page parameters
                    List<StringValue> subtypes = getPageParameters().getValues(ObjectType.F_SUBTYPE.getLocalPart());
                    subtypes.stream().filter(p -> !p.isEmpty()).forEach(c -> focusType.subtype(c.toString()));

                    getMidpointApplication().getPrismContext().adopt(focusType);
                    object = (PrismObject<O>) focusType.asPrismObject();
                } else {
                    LOGGER.trace("Loading object: New object (supplied): {}", objectToEdit);
                    object = objectToEdit;
                }
            } else {

                String focusOid = getObjectOidParameter();
                object = WebModelServiceUtils.loadObject(getCompileTimeClass(), focusOid, buildGetOptions(), this, task, result);
                LOGGER.trace("Loading object: Existing object (loadled): {} -> {}", focusOid, object);
            }

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
        }

        showResult(result, false);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded object:\n{}", object.debugDump());
        }

        if (object == null) {
            if (isOidParameterExists()) {
                getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
            } else {
                getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
            }
            throw new RestartResponseException(getRestartResponsePage());
        }

        ItemStatus itemStatus = isOidParameterExists() || editingFocus ? ItemStatus.NOT_CHANGED : ItemStatus.ADDED;
        PrismObjectWrapper<O> wrapper;

        PrismObjectWrapperFactory<O> factory = getRegistry().getObjectWrapperFactory(object.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(ItemStatus.ADDED == itemStatus);
        //we don't want to set to false.. refactor this method to take either enum (READONLY, AUTO, ...) or
        // Boolean instead of boolean isReadonly
        if (isReadonly) {
            context.setReadOnly(isReadonly);
        }


        try {
            wrapper = factory.createObjectWrapper(object.clone(), itemStatus, context);
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAdminObjectDetails.message.loadObjectWrapper.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
            showResult(result, false);
            throw new RestartResponseException(getRestartResponsePage());
        }

        showResult(result, false);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded focus wrapper:\n{}", wrapper.debugDump());
        }

        return wrapper;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> buildGetOptions() {
        return getOperationOptionsBuilder()
                .item(UserType.F_JPEG_PHOTO).retrieve()
                .build();
    }

//    private void loadParentOrgs(PrismObjectWrapper<O> wrapper, Task task, OperationResult result) {
//        OperationResult subResult = result.createMinorSubresult(OPERATION_LOAD_PARENT_ORGS);
//        PrismObject<O> focus = wrapper.getObject();
//        // Load parent organizations (full objects). There are used in the
//        // summary panel and also in the main form.
//        // Do it here explicitly instead of using resolve option to have ability
//        // to better handle (ignore) errors.
//        for (ObjectReferenceType parentOrgRef : focus.asObjectable().getParentOrgRef()) {
//
//            PrismObject<OrgType> parentOrg = null;
//            try {
//
//                parentOrg = getModelService().getObject(OrgType.class, parentOrgRef.getOid(), null, task,
//                        subResult);
//                LOGGER.trace("Loaded parent org with result {}",
//                        new Object[] { subResult.getLastSubresult() });
//            } catch (AuthorizationException e) {
//                // This can happen if the user has permission to read parentOrgRef but it does not have
//                // the permission to read target org
//                // It is OK to just ignore it.
//                subResult.muteLastSubresultError();
//                LOGGER.debug("User {} does not have permission to read parent org unit {} (ignoring error)", task.getOwner().getName(), parentOrgRef.getOid());
//            } catch (Exception ex) {
//                subResult.recordWarning(createStringResource("PageAdminObjectDetails.message.loadParentOrgs.warning", parentOrgRef.getOid()).getString(), ex);
//                LOGGER.warn("Cannot load parent org {}: {}", parentOrgRef.getOid(), ex.getMessage(), ex);
//            }
//
//            if (parentOrg != null) {
//                wrapper.getParentOrgs().add(parentOrg);
//            }
//        }
//        subResult.computeStatus();
//    }

    protected abstract Class<? extends Page> getRestartResponsePage();

    public Object findParam(String param, String oid, OperationResult result) {

        Object object = null;

        for (OperationResult subResult : result.getSubresults()) {
            if (subResult != null && subResult.getParams() != null) {
                if (subResult.getParams().get(param) != null
                        && subResult.getParams().get(OperationResult.PARAM_OID) != null
                        && subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
                    return subResult.getParams().get(param);
                }
                object = findParam(param, oid, subResult);

            }
        }
        return object;
    }

    // TODO put this into correct place
    protected boolean previewRequested;

    /**
     * This will be called from the main form when save button is pressed.
     */
    public void savePerformed(AjaxRequestTarget target) {
        progressPanel.onBeforeSave();
        OperationResult result = new OperationResult(OPERATION_SAVE);
        previewRequested = false;
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

    public void saveOrPreviewPerformed(AjaxRequestTarget target, OperationResult result, boolean previewOnly, Task task) {
        boolean delegationChangesExist = processDeputyAssignments(previewOnly);

        PrismObjectWrapper<O> objectWrapper = getObjectWrapper();
        LOGGER.debug("Saving object {}", objectWrapper);

        // todo: improve, delta variable is quickfix for MID-1006
        // redirecting to user list page everytime user is created in repository
        // during user add in gui,
        // and we're not taking care about account/assignment create errors
        // (error message is still displayed)
        delta = null;

        if(task == null) {
        task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        }

        ModelExecuteOptions options = getOptions(previewOnly);

        LOGGER.debug("Using execute options {}.", options);

        try {
            reviveModels();

            delta = objectWrapper.getObjectDelta();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("pageAdminObjectDetails.message.cantCreateObject"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Create Object failed", ex);
            showResult(result);
            target.add(getFeedbackPanel());
            return;
        }

        switch (objectWrapper.getStatus()) {
            case ADDED:
                try {
                    PrismObject<O> objectToAdd = delta.getObjectToAdd();
                    WebComponentUtil.encryptCredentials(objectToAdd, true, getMidpointApplication());
                    prepareObjectForAdd(objectToAdd);
                    getPrismContext().adopt(objectToAdd, getCompileTimeClass());
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
                    }

                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());

                        final Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
                        final Collection<SimpleValidationError> validationErrors = performCustomValidation(objectToAdd, deltas);
                        if (checkValidationErrors(target, validationErrors)) {
                            return;
                        }
                        if (isSaveInBackground()){
                            progressPanel.executeChangesInBackground(deltas, previewOnly, options, task, result, target);
                        } else {
                            progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                        }
                    } else {
                        result.recordSuccess();
                    }
                } catch (Exception ex) {
                    result.recordFatalError(getString("pageFocus.message.cantCreateFocus"), ex);
                    LoggingUtils.logUnexpectedException(LOGGER, "Create user failed", ex);
                    showResult(result);
                }
                break;

            case NOT_CHANGED:
                try {
                    WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
                    prepareObjectDeltaForModify(delta); //preparing of deltas for projections (ADD, DELETE, UNLINK)

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
                    }

                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());
                        deltas.add(delta);
                    }

                    List<ObjectDelta<? extends ObjectType>> additionalDeltas = getAdditionalModifyDeltas(result);
                    if (additionalDeltas != null) {
                        for (ObjectDelta additionalDelta : additionalDeltas) {
                            if (!additionalDelta.isEmpty()) {
                                additionalDelta.revive(getPrismContext());
                                deltas.add(additionalDelta);
                            }
                        }
                    }

                    if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
                        ObjectDelta emptyDelta = getPrismContext().deltaFactory().object().createEmptyModifyDelta(getCompileTimeClass(),
                                objectWrapper.getObject().getOid());
                        deltas.add(emptyDelta);

                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
                        if (checkValidationErrors(target, validationErrors)) {
                            return;
                        }
                        if (isSaveInBackground()){
                            progressPanel.executeChangesInBackground(deltas, previewOnly, options, task, result, target);
                        } else {
                            progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                        }
                    } else if (!deltas.isEmpty()) {
                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
                        if (checkValidationErrors(target, validationErrors)) {
                            return;
                        }
                        if (isSaveInBackground()){
                            progressPanel.executeChangesInBackground(deltas, previewOnly, options, task, result, target);
                        } else {
                            progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                        }
                    } else if (previewOnly && delta.isEmpty() && delegationChangesExist){
                        if (isSaveInBackground()){
                            progressPanel.executeChangesInBackground(deltas, previewOnly, options, task, result, target);
                        } else {
                            progressPanel.executeChanges(deltas, previewOnly, options, task, result, target);
                        }
                    } else {
                        progressPanel.clearProgressPanel();            // from previous attempts (useful only if we would call finishProcessing at the end, but that's not the case now)
                        if (!previewOnly) {
                            if (!delegationChangesExist) {
                                result.recordWarning(getString("PageAdminObjectDetails.noChangesSave"));
                                showResult(result);
                            }
                            redirectBack();
                        } else {
                            if (!delegationChangesExist) {
                                warn(getString("PageAdminObjectDetails.noChangesPreview"));
                                target.add(getFeedbackPanel());
                            }
                        }
                    }

                } catch (Exception ex) {
                    if (!executeForceDelete(objectWrapper, task, options, result)) {
                        result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
                        LoggingUtils.logUnexpectedException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
                    } else {
                        result.recomputeStatus();
                    }
                    showResult(result);
                }
                break;
            // support for add/delete containers (e.g. delete credentials)
            default:
                error(getString("pageAdminFocus.message.unsupportedState", objectWrapper.getStatus()));
        }

//        result.recomputeStatus();
//
//        if (!result.isInProgress()) {
//            LOGGER.trace("Result NOT in progress, calling finishProcessing");
//            finishProcessing(target, result, false);
//        }

        LOGGER.trace("returning from saveOrPreviewPerformed");
    }

    protected boolean processDeputyAssignments(boolean previewOnly){
        return false;
    }

    protected boolean checkValidationErrors(AjaxRequestTarget target, Collection<SimpleValidationError> validationErrors) {
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

    @Override
    public void startProcessing(AjaxRequestTarget target, OperationResult result) {
        LOGGER.trace("startProcessing called, making main panel invisible");
        mainPanel.setVisible(false);
        target.add(mainPanel);
    }

    @NotNull
    protected ModelExecuteOptions getOptions(boolean previewOnly) {
        ModelExecuteOptions options = mainPanel.getExecuteChangeOptionsDto().createOptions();
        if (previewOnly) {
            options.getOrCreatePartialProcessing().setApprovals(PartialProcessingTypeType.PROCESS);
        }
        return options;
    }

    protected void prepareObjectForAdd(PrismObject<O> object) throws SchemaException {

    }

    protected void prepareObjectDeltaForModify(ObjectDelta<O> objectDelta) throws SchemaException {

    }

    protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
        return null;
    }

    protected boolean executeForceDelete(PrismObjectWrapper<O> userWrapper, Task task, ModelExecuteOptions options,
            OperationResult parentResult) {
        return isForce();
    }

    protected boolean isForce() {
        return getMainPanel().getExecuteChangeOptionsDto().isForce();
    }

    protected boolean isKeepDisplayingResults() {
        return getMainPanel().getExecuteChangeOptionsDto().isKeepDisplayingResults();
    }

    protected boolean isSaveInBackground(){
        return getMainPanel().getExecuteChangeOptionsDto().isSaveInBackground();
    }

    protected Collection<SimpleValidationError> performCustomValidation(PrismObject<O> object,
            Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
        Collection<SimpleValidationError> errors = null;

        if (object == null) {
            if (getObjectWrapper() != null && getObjectWrapper().getObjectOld() != null) {
                object = getObjectWrapper().getObjectOld().clone();        // otherwise original object could get corrupted e.g. by applying the delta below

                for (ObjectDelta delta : deltas) {
                    // because among deltas there can be also ShadowType deltas
                    if (UserType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                        delta.applyTo(object);
                    }
                }
            }
        } else {
            object = object.clone();
        }

        performAdditionalValidation(object, deltas, errors);

        for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
            if (errors == null) {
                errors = validator.validateObject(object, deltas);
            } else {
                errors.addAll(validator.validateObject(object, deltas));
            }
        }

        return errors;
    }

    protected void performAdditionalValidation(PrismObject<O> object,
            Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) throws SchemaException {

    }

    public List<ObjectFormType> getObjectFormTypes() {
        Task task = createSimpleTask(OPERATION_LOAD_GUI_CONFIGURATION);
        OperationResult result = task.getResult();
        CompiledGuiProfile adminGuiConfiguration;
        try {
            adminGuiConfiguration = getModelInteractionService().getCompiledGuiProfile(task, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            throw new SystemException("Cannot load GUI configuration: "+e.getMessage(), e);
        }
        ObjectFormsType objectFormsType = adminGuiConfiguration.getObjectForms();
        if (objectFormsType == null) {
            return null;
        }
        List<ObjectFormType> objectForms = objectFormsType.getObjectForm();
        if (objectForms == null || objectForms.isEmpty()) {
            return objectForms;
        }
        List<ObjectFormType> validObjectForms = new ArrayList<>();
        for (ObjectFormType objectForm: objectForms) {
            if (isSupportedObjectType(objectForm.getType())) {
                validObjectForms.add(objectForm);
            }
        }
        return validObjectForms;
    }

    protected boolean isSupportedObjectType(QName type) {
        ObjectTypes objectType = ObjectTypes.getObjectType(getCompileTimeClass());
        return QNameUtil.match(objectType.getTypeQName(),type);
    }

    public void setSaveOnConfigure(boolean saveOnConfigure) {
        this.saveOnConfigure = saveOnConfigure;
    }

    public boolean isSaveOnConfigure() {
        return saveOnConfigure;
    }

    public boolean isForcedPreview() {
        GuiObjectDetailsPageType objectDetails = getCompiledGuiProfile().findObjectDetailsConfiguration(getCompileTimeClass());
        return objectDetails != null && DetailsPageSaveMethodType.FORCED_PREVIEW.equals(objectDetails.getSaveMethod());
    }

    //TODO moved from PageAdminFocus.. maybe we need PageAssignmentHolderDetails?
    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

        if (previewRequested) {
            finishPreviewProcessing(target, result);
            return;
        }
        if (result.isSuccess() && getDelta() != null  && SecurityUtils.getPrincipalUser().getOid().equals(getDelta().getOid())) {
            Session.get().setLocale(WebModelServiceUtils.getLocale());
            LOGGER.debug("Using {} as locale", getLocale());
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(WebModelServiceUtils.getTimezone());
            LOGGER.debug("Using {} as time zone", WebSession.get().getClientInfo().getProperties().getTimeZone());
        }
        boolean focusAddAttempted = getDelta() != null && getDelta().isAdd();
        boolean focusAddSucceeded = focusAddAttempted && StringUtils.isNotEmpty(getDelta().getOid());

        // we don't want to allow resuming editing if a new focal object was created (on second 'save' there would be a conflict with itself)
        // and also in case of partial errors, like those related to projections (many deltas would be already executed, and this could cause problems on second 'save').
        boolean canContinueEditing = !focusAddSucceeded && result.isFatalError();

        boolean canExitPage;
        if (returningFromAsync) {
            canExitPage = getProgressPanel().isAllSuccess() || result.isInProgress() || result.isHandledError(); // if there's at least a warning in the progress table, we would like to keep the table open
        } else {
            canExitPage = !canContinueEditing;                            // no point in staying on page if we cannot continue editing (in synchronous case i.e. no progress table present)
        }

        if (!isKeepDisplayingResults() && canExitPage) {
            showResult(result);
            redirectBack();
        } else {
            if (returningFromAsync) {
                getProgressPanel().showBackButton(target);
                getProgressPanel().hideAbortButton(target);
            }
            showResult(result);
            target.add(getFeedbackPanel());

            if (canContinueEditing) {
                getProgressPanel().hideBackButton(target);
                getProgressPanel().showContinueEditingButton(target);
            }
        }
    }

    private void finishPreviewProcessing(AjaxRequestTarget target, OperationResult result) {
        getMainPanel().setVisible(true);
        getProgressPanel().hide();
        getProgressPanel().hideAbortButton(target);
        getProgressPanel().hideBackButton(target);
        getProgressPanel().hideContinueEditingButton(target);

        showResult(result);
        target.add(getFeedbackPanel());

        Map<PrismObject<O>, ModelContext<? extends ObjectType>> modelContextMap = new LinkedHashMap<>();
        modelContextMap.put(getObjectWrapper().getObject(), getProgressPanel().getPreviewResult());

        processAdditionalFocalObjectsForPreview(modelContextMap);

        navigateToNext(new PagePreviewChanges(modelContextMap, getModelInteractionService()));
    }

    protected void processAdditionalFocalObjectsForPreview(Map<PrismObject<O>, ModelContext<? extends ObjectType>> modelContextMap){
    }
}
