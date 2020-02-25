/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValueWrapper;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.prism.show.PagePreviewChanges;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;

public abstract class PageAdminFocus<F extends FocusType> extends PageAdminObjectDetails<F>
        implements ProgressReportingAwarePage {
    private static final long serialVersionUID = 1L;

    private LoadableModel<List<ShadowWrapper>> projectionModel;
    private LoadableModel<List<AssignmentEditorDto>> delegatedToMeModel;

    private static final String DOT_CLASS = PageAdminFocus.class.getName() + ".";
    private static final String OPERATION_RECOMPUTE_ASSIGNMENTS = DOT_CLASS + "recomputeAssignments";

    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);

    public PageAdminFocus() {
        initialize(null);
    }

    public PageAdminFocus(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    public PageAdminFocus(final PrismObject<F> userToEdit) {
        initialize(userToEdit);
    }

    public PageAdminFocus(final PrismObject<F> unitToEdit, boolean isNewObject)  {
        initialize(unitToEdit, isNewObject);
    }

    public PageAdminFocus(final PrismObject<F> unitToEdit, boolean isNewObject, boolean isReadonly)  {
        initialize(unitToEdit, isNewObject, isReadonly);
    }


    @Override
    protected void initializeModel(final PrismObject<F> objectToEdit, boolean isNewObject, boolean isReadonly) {
        super.initializeModel(objectToEdit, isNewObject, isReadonly);

        projectionModel = new LoadableModel<List<ShadowWrapper>>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ShadowWrapper> load() {
                return loadShadowWrappers(true);
            }
        };

        delegatedToMeModel= new LoadableModel<List<AssignmentEditorDto>>(false) {

            private static final long serialVersionUID = 1L;
            @Override
            protected List<AssignmentEditorDto> load() {
                return loadDelegatedToMe();
            }
        };

    }

    public LoadableModel<List<ShadowWrapper>> getProjectionModel() {
        return projectionModel;
    }

    public LoadableModel<List<AssignmentEditorDto>> getDelegatedToMeModel() {
        return delegatedToMeModel;
    }

    public List<ShadowWrapper> getFocusShadows() {
        return projectionModel.getObject();
    }

    protected void reviveModels() throws SchemaException {
        super.reviveModels();
        WebComponentUtil.revive(projectionModel, getPrismContext());
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

        if (previewRequested) {
            finishPreviewProcessing(target, result);
            return;
        }
        if (result.isSuccess() && getDelta() != null  && SecurityUtils.getPrincipalUser().getOid().equals(getDelta().getOid())) {
            FocusType focus = null;
            if (getObjectWrapper().getObject().asObjectable() instanceof UserType){
                focus = getObjectWrapper().getObject().asObjectable();
            }
            Session.get().setLocale(WebModelServiceUtils.getLocale(focus));
            LOGGER.debug("Using {} as locale", getLocale());
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(WebModelServiceUtils.getTimezone(focus));
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

        Map<PrismObject<F>, ModelContext<? extends ObjectType>> modelContextMap = new LinkedHashMap<>();
        modelContextMap.put(getObjectWrapper().getObject(), getProgressPanel().getPreviewResult());

        processAdditionalFocalObjectsForPreview(modelContextMap);

        navigateToNext(new PagePreviewChanges(modelContextMap, getModelInteractionService()));
    }

    protected void processAdditionalFocalObjectsForPreview(Map<PrismObject<F>, ModelContext<? extends ObjectType>> modelContextMap){
    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {
        getMainPanel().setVisible(true);
        getProgressPanel().hide();
        getProgressPanel().hideAbortButton(target);
        getProgressPanel().hideBackButton(target);
        getProgressPanel().hideContinueEditingButton(target);
        target.add(this);
    }

    private List<ShadowWrapper> loadShadowWrappers(boolean noFetch) {
        List<ShadowWrapper> list = new ArrayList<>();

        PrismObjectWrapper<F> focusWrapper = getObjectModel().getObject();
        PrismObject<F> focus = focusWrapper.getObject();
        PrismReference prismReference = focus.findReference(UserType.F_LINK_REF);
        if (prismReference == null || prismReference.isEmpty()) {
            return new ArrayList<>();
        }
        List<PrismReferenceValue> references = prismReference.getValues();

        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
        for (PrismReferenceValue reference : references) {
            if(reference == null || (reference.getOid() == null && reference.getTargetType() == null)) {
                LOGGER.trace("Skiping reference for shadow with null oid");
                continue; // default value
            }
            OperationResult subResult = task.getResult().createMinorSubresult(OPERATION_LOAD_SHADOW);
            PrismObject<ShadowType> projection = getPrismObjectForWrapper(ShadowType.class, reference.getOid(),
                    noFetch, task, subResult, createLoadOptionForShadowWrapper());

            if(projection == null) {
//                showResult(subResult, "pageAdminFocus.message.couldntLoadShadowProjection");
                LOGGER.error("Couldn't load shadow projection");
                continue;
            }

            try {
                ShadowWrapper wrapper = loadShadowWrapper(projection, task, subResult);
                wrapper.setLoadWithNoFetch(noFetch);

                if (wrapper != null) {
                    list.add((ShadowWrapper)wrapper);
                } else {
                    showResult(subResult, "pageAdminFocus.message.shadowWrapperIsNull");
                    LOGGER.error("ShadowWrapper is null");
                }

                //TODO catch Exception/Runtim,eException, Throwable
            } catch (SchemaException e) {
                showResult(subResult, "pageAdminFocus.message.couldntCreateShadowWrapper");
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create shadow wrapper", e);
            }
        }
        return list;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createLoadOptionForShadowWrapper(){
        return getSchemaHelper().getOperationOptionsBuilder()
                    .item(ShadowType.F_RESOURCE_REF).resolve().readOnly()
                    .build();
    }

    public ShadowWrapper loadShadowWrapper(PrismObject<ShadowType> projection, Task task, OperationResult result) throws SchemaException{
        PrismObjectWrapperFactory<ShadowType> factory = getRegistry().getObjectWrapperFactory(projection.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(false);
        ShadowWrapper wrapper = (ShadowWrapper) factory.createObjectWrapper(projection, ItemStatus.NOT_CHANGED, context);
        wrapper.setProjectionStatus(UserDtoStatus.MODIFY);
        return wrapper;
    }

    public void loadFullShadow(PrismObjectValueWrapper<ShadowType> shadowWrapperValue, AjaxRequestTarget target) {
        if(shadowWrapperValue.getRealValue() == null) {
            error(getString("pageAdminFocus.message.couldntCreateShadowWrapper"));
            LOGGER.error("Couldn't create shadow wrapper, because RealValue is null in " + shadowWrapperValue);
            return;
        }
        String oid = shadowWrapperValue.getRealValue().getOid();
        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
        OperationResult result = task.getResult();
        PrismObject<ShadowType> projection = getPrismObjectForWrapper(ShadowType.class, oid, false, task,
                result, createLoadOptionForShadowWrapper());

        if (projection == null) {
            result.recordFatalError(getString("PageAdminFocus.message.loadFullShadow.fatalError", shadowWrapperValue.getRealValue()));
            showResult(result);
            target.add(getFeedbackPanel());
            return;
        }

        ShadowWrapper shadowWrapperNew;
        try {
            shadowWrapperNew = loadShadowWrapper(projection, task, result);

            if (shadowWrapperNew == null) {
                error(getString("pageAdminFocus.message.shadowWrapperIsNull"));
                LOGGER.error("ShadowWrapper is null");
                return;
            }

            shadowWrapperValue.getItems().clear();
            shadowWrapperValue.getItems().addAll((Collection) shadowWrapperNew.getValue().getItems());
            ((ShadowWrapper)shadowWrapperValue.getParent()).setLoadWithNoFetch(false);
        } catch (SchemaException e) {
            error(getString("pageAdminFocus.message.couldntCreateShadowWrapper"));
            LOGGER.error("Couldn't create shadow wrapper", e);
        }
    }

//    @Override
//    protected List<FocusSubwrapperDto<OrgType>> loadOrgWrappers() {
//        return loadSubwrappers(OrgType.class, UserType.F_PARENT_ORG_REF, false);
//    }

//    private <S extends ObjectType> List<FocusSubwrapperDto<S>> loadSubwrappers(Class<S> type,
//            ItemName propertyToLoad, boolean noFetch) {
//        List<FocusSubwrapperDto<S>> list = new ArrayList<>();
//
//        PrismObjectWrapper<F> focusWrapper = getObjectModel().getObject();
//        PrismObject<F> focus = focusWrapper.getObject();
//        PrismReference prismReference = focus.findReference(propertyToLoad);
//        if (prismReference == null) {
//            return new ArrayList<>();
//        }
//        List<PrismReferenceValue> references = prismReference.getValues();
//
//        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
//        for (PrismReferenceValue reference : references) {
//            FocusSubwrapperDto<S> subWrapper = loadSubWrapperDto(type, reference.getOid(), noFetch, task);
//            if (subWrapper != null) {
//                list.add(subWrapper);
//            }
//        }
//
//        return list;
//    }

    private <S extends ObjectType> PrismObject<S> getPrismObjectForWrapper(Class<S> type, String oid, boolean noFetch,
            Task task, OperationResult subResult, Collection<SelectorOptions<GetOperationOptions>> loadOptions){
        if (oid == null) {
            return null;
        }

        if (noFetch) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(loadOptions);
            if (rootOptions == null) {
                loadOptions.add(new SelectorOptions<>(GetOperationOptions.createNoFetch()));
            } else {
                rootOptions.setNoFetch(true);
            }
        }

        PrismObject<S> projection = WebModelServiceUtils.loadObject(type, oid, loadOptions, this, task, subResult);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded projection {} ({}):\n{}", oid, loadOptions, projection==null?null:projection.debugDump());
        }

        return projection;
    }

//    private <S extends ObjectType> FocusSubwrapperDto<S> loadSubWrapperDto(Class<S> type, String oid, boolean noFetch, Task task) {
//        OperationResult subResult = task.getResult().createMinorSubresult(OPERATION_LOAD_SHADOW);
//        Collection<SelectorOptions<GetOperationOptions>> loadOptions = new ArrayList<>();
//        PrismObject<S> projection = getPrismObjectForWrapper(type, oid, noFetch, task, subResult, loadOptions);
//        if (projection == null) {
//            // No access or error
//            // TODO actually it would be nice to show an error if the shadow repo object does not exist
//            return null;
//        }
//        String resourceName = null;
//        try {
//            S projectionType = projection.asObjectable();
//
//            OperationResultType fetchResult = projectionType.getFetchResult();
//            StringBuilder description = new StringBuilder();
//            if (ShadowType.class.equals(type)) {
//                ShadowType shadowType = (ShadowType) projectionType;
//                ResourceType resource = shadowType.getResource();
//                resourceName = WebComponentUtil.getName(resource);
//
//                if (shadowType.getIntent() != null) {
//                    description.append(shadowType.getIntent()).append(", ");
//                }
//            } else if (OrgType.class.equals(type)) {
//                OrgType orgType = (OrgType) projectionType;
//                resourceName = orgType.getDisplayName() != null
//                        ? WebComponentUtil.getOrigStringFromPoly(orgType.getDisplayName()) : "";
//            }
//            description.append(WebComponentUtil.getOrigStringFromPoly(projectionType.getName()));
//
//            ObjectWrapperOld<S> wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName,
//                    description.toString(), projection, ContainerStatus.MODIFYING, task, this);
//            wrapper.setLoadOptions(loadOptions);
//            wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
//            wrapper.setSelectable(true);
//            wrapper.setMinimalized(true);
//
////            wrapper.initializeContainers(this);
//
//            subResult.computeStatus();
//            FocusSubwrapperDto ret = new FocusSubwrapperDto<>(wrapper, UserDtoStatus.MODIFY);
//            return ret;
//
//        } catch (Exception ex) {
//            subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load account", ex);
//            subResult.computeStatus();
//            return new FocusSubwrapperDto<>(false, resourceName, subResult);
//        }
//    }

    private List<AssignmentEditorDto> loadDelegatedToMe() {
        List<AssignmentEditorDto> list = new ArrayList<>();

        PrismObjectWrapper<F> focusWrapper = getObjectModel().getObject();
        PrismObject<F> focus = focusWrapper.getObject();
        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            if (assignment.getTargetRef() != null &&
                    UserType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this);
                dto.setSimpleView(true);
                dto.setEditable(false);
                list.add(dto);
            }
        }

        Collections.sort(list);

        return list;
    }

  protected List<AssignmentType> getPolicyRulesList(List<AssignmentType> assignments, UserDtoStatus status){
        List<AssignmentType> list = new ArrayList<>();
        for (AssignmentType assignment : assignments) {
            if (AssignmentsUtil.isPolicyRuleAssignment(assignment)) {
                //TODO set status
                list.add(assignment);
            }
        }
        return list;
    }

    @Override
    protected void prepareObjectForAdd(PrismObject<F> focus) throws SchemaException {
        super.prepareObjectForAdd(focus);
        F focusType = focus.asObjectable();
        // handle added accounts

        List<ShadowType> shadowsToAdd = prepareShadowObject(getFocusShadows());
        for (ShadowType shadowToAdd : shadowsToAdd) {
            addDefaultKindAndIntent(shadowToAdd.asPrismObject());
            ObjectReferenceType linkRef = new ObjectReferenceType();
            linkRef.asReferenceValue().setObject(shadowToAdd.asPrismObject());
            focusType.getLinkRef().add(linkRef);
        }

//        List<OrgType> orgsToAdd = prepareSubobject(getParentOrgs());
//        if (!orgsToAdd.isEmpty()){
//            focusType.getParentOrg().addAll(orgsToAdd);
//        }

    }

    @Override
    protected void prepareObjectDeltaForModify(ObjectDelta<F> focusDelta) throws SchemaException {
        super.prepareObjectDeltaForModify(focusDelta);
        // handle accounts
        PrismObjectDefinition<F> objectDefinition = getObjectDefinition();
        PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(FocusType.F_LINK_REF);
        ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
        if (!refDelta.isEmpty()) {
            focusDelta.addModification(refDelta);
        }

//        refDef = objectDefinition.findReferenceDefinition(FocusType.F_PARENT_ORG_REF);
//        refDelta = prepareUserOrgsDeltaForModify(refDef);
//        if (!refDelta.isEmpty()) {
//            focusDelta.addModification(refDelta);
//        }
    }

    protected PrismObjectDefinition<F> getObjectDefinition() {
        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        return registry
                .findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
    }

    protected ContainerDelta handleAssignmentDeltas(ObjectDelta<F> focusDelta,
            List<AssignmentEditorDto> assignments, PrismContainerDefinition def,
                                                    boolean isDelegation) throws SchemaException {
        ContainerDelta assDelta = getPrismContext().deltaFactory().container().create(ItemPath.EMPTY_PATH, def.getItemName(), def);

        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue newValue = assDto.getNewValue(getPrismContext());

            switch (assDto.getStatus()) {
                case ADD:
                    newValue.applyDefinition(def, false);
                    assDelta.addValueToAdd(newValue.clone());
                    break;
                case DELETE:
                    PrismContainerValue oldValue = assDto.getOldValue();
                    if (isDelegation){
                        oldValue.applyDefinition(def, false);
                    } else {
                        oldValue.applyDefinition(def);
                    }
                    assDelta.addValueToDelete(oldValue.clone());
                    break;
                case MODIFY:
                    if (!assDto.isModified(getPrismContext())) {
                        LOGGER.trace("Assignment '{}' not modified.", new Object[] { assDto.getName() });
                        continue;
                    }

                    handleModifyAssignmentDelta(assDto, def, newValue, focusDelta);
                    break;
                default:
                    warn(getString("pageAdminUser.message.illegalAssignmentState", assDto.getStatus()));
            }
        }

        if (!assDelta.isEmpty()) {
            assDelta = focusDelta.addModification(assDelta);
        }

        return assDelta;
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
            PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<F> focusDelta)
                    throws SchemaException {
        LOGGER.debug("Handling modified assignment '{}', computing delta.",
                new Object[] { assDto.getName() });

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

        for (ItemDelta delta : deltas) {
            ItemPath deltaPath = delta.getPath().rest();
            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

            delta.setParentPath(WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
            delta.applyDefinition(deltaDef);

            focusDelta.addModification(delta);
        }
    }

    @Override
    protected boolean executeForceDelete(PrismObjectWrapper<F> userWrapper, Task task, ModelExecuteOptions options,
            OperationResult parentResult) {
        if (isForce()) {
            OperationResult result = parentResult.createSubresult("Force delete operation");

            try {
                ObjectDelta<F> forceDeleteDelta = getForceDeleteDelta(userWrapper);
                forceDeleteDelta.revive(getPrismContext());

                if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
                    getModelService().executeChanges(WebComponentUtil.createDeltaCollection(forceDeleteDelta),
                            options, task, result);
                }
            } catch (Exception ex) {
                result.recordFatalError(getString("PageAdminFocus.message.executeForceDelete.fatalError"));
                LoggingUtils.logUnexpectedException(LOGGER, "Failed to execute delete operation with force", ex);
                return false;
            }

            result.recomputeStatus();
            result.recordSuccessIfUnknown();
            return true;
        }
        return false;
    }

    private ObjectDelta<F> getForceDeleteDelta(PrismObjectWrapper<F> focusWrapper) throws SchemaException {

        List<ShadowWrapper> accounts = getFocusShadows();
        List<ReferenceDelta> refDeltas = new ArrayList<>();
        ObjectDelta<F> forceDeleteDelta = null;
        for (ShadowWrapper account : accounts) {
//            if (!accDto.isLoadedOK()) {
//                continue;
//            }
            if (account.getProjectionStatus() == UserDtoStatus.DELETE) {
//                ObjectWrapperOld accWrapper = accDto.getObjectOld();
                ReferenceDelta refDelta = getPrismContext().deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF,
                        focusWrapper.getObject().getDefinition(), account.getObject());
                refDeltas.add(refDelta);
            } else if (account.getProjectionStatus() == UserDtoStatus.UNLINK) {
//                ObjectWrapperOld accWrapper = accDto.getObjectOld();
                ReferenceDelta refDelta = getPrismContext().deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF,
                        focusWrapper.getObject().getDefinition(), account.getObject().getOid());
                refDeltas.add(refDelta);
            }
        }
        if (!refDeltas.isEmpty()) {
            forceDeleteDelta = getPrismContext().deltaFactory().object()
                    .createModifyDelta(focusWrapper.getObject().getOid(), refDeltas,
                    getCompileTimeClass());
        }
        PrismContainerDefinition def = focusWrapper.getObject().findContainer(UserType.F_ASSIGNMENT)
                .getDefinition();
        if (forceDeleteDelta == null) {
            forceDeleteDelta = getPrismContext().deltaFactory().object().createEmptyModifyDelta(getCompileTimeClass(),
                    focusWrapper.getObject().getOid());
        }
        return forceDeleteDelta;
    }

//    private <P extends ObjectType> List<P> prepareSubobject(List<FocusSubwrapperDto<P>> projections) throws SchemaException{
//        List<P> projectionsToAdd = new ArrayList<>();
//        for (FocusSubwrapperDto<P> projection : projections) {
//            if (!projection.isLoadedOK()) {
//                continue;
//            }
//
//            if (UserDtoStatus.MODIFY.equals(projection.getStatus())) {
//                // this is legal e.g. when child org is being create (one assignment comes pre-created)
//                // TODO do we need more specific checks here?
//                continue;
//            }
//
//            if (!UserDtoStatus.ADD.equals(projection.getStatus())) {
//                warn(getString("pageAdminFocus.message.illegalAccountState", projection.getStatus()));
//                continue;
//            }
//
//            ObjectWrapperOld<P> projectionWrapper = projection.getObjectOld();
//            ObjectDelta<P> delta = projectionWrapper.getObjectDelta();
//            PrismObject<P> proj = delta.getObjectToAdd();
//            WebComponentUtil.encryptCredentials(proj, true, getMidpointApplication());
//
//            projectionsToAdd.add(proj.asObjectable());
//        }
//        return projectionsToAdd;
//    }

    private List<ShadowType> prepareShadowObject(List<ShadowWrapper> projections) throws SchemaException{
        List<ShadowType> projectionsToAdd = new ArrayList<>();
        for (ShadowWrapper projection : projections) {
//            if (!projection.isLoadedOK()) {
//                continue;
//            }
            if (UserDtoStatus.MODIFY.equals(projection.getProjectionStatus())) {
                // this is legal e.g. when child org is being create (one assignment comes pre-created)
                // TODO do we need more specific checks here?
                continue;
            }

            if (!UserDtoStatus.ADD.equals(projection.getProjectionStatus())) {
                warn(getString("pageAdminFocus.message.illegalAccountState", projection.getStatus()));
                continue;
            }

            ObjectDelta<ShadowType> delta = projection.getObjectDelta();
            PrismObject<ShadowType> proj = delta.getObjectToAdd();
            WebComponentUtil.encryptCredentials(proj, true, getMidpointApplication());

            projectionsToAdd.add(proj.asObjectable());
        }
        return projectionsToAdd;
    }


    @Override
    protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
        return getShadowModifyDeltas(result);
    }

    private List<ObjectDelta<? extends ObjectType>> getShadowModifyDeltas(OperationResult result) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

        List<ShadowWrapper> accounts = getFocusShadows();
        for (ShadowWrapper account : accounts) {
//            if (!account.isLoadedOK()) {
//                continue;
//            }
            try {
                ObjectDelta<ShadowType> delta = account.getObjectDelta();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account delta computed from {} as:\n{}",
                            new Object[] { account, delta.debugDump(3) });
                }

                if (!UserDtoStatus.MODIFY.equals(account.getProjectionStatus())) {
                    continue;
                }

                if (delta == null || delta.isEmpty()) {
//                        && (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty())) {
                    continue;
                }

//                if (accountWrapper.getOldDelta() != null) {
//                    delta = ObjectDeltaCollectionsUtil.summarize(delta, accountWrapper.getOldDelta());
//                }


                WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Modifying account:\n{}", new Object[] { delta.debugDump(3) });
                }

                deltas.add(delta);

            } catch (Exception ex) {
                result.recordFatalError(getString("PageAdminFocus.message.getShadowModifyDeltas.fatalError"), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute account delta", ex);
            }
        }

        return deltas;
    }

    /**
     * remove this method after model is updated - it has to remove resource
     * from accountConstruction
     */
    @Deprecated
    private void removeResourceFromAccConstruction(AssignmentType assignment) {
        ConstructionType accConstruction = assignment.getConstruction();
        if (accConstruction == null || accConstruction.getResourceRef() == null || accConstruction.getResourceRef().asReferenceValue().getObject() == null) {
            return;
        }

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(assignment.getConstruction().getResourceRef().getOid());
        ref.setType(ResourceType.COMPLEX_TYPE);
        assignment.getConstruction().setResourceRef(ref);
    }

    private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
            throws SchemaException {
        ReferenceDelta refDelta = getPrismContext().deltaFactory().reference().create(refDef);

        List<ShadowWrapper> accounts = getFocusShadows();
        for (ShadowWrapper accountWrapper : accounts) {
//            if (accDto.isLoadedOK()) {
//                ObjectWrapperOld accountWrapper = accDto.getObjectOld();
                accountWrapper.revive(getPrismContext());
                ObjectDelta delta = accountWrapper.getObjectDelta();
                PrismReferenceValue refValue = getPrismContext().itemFactory().createReferenceValue(null, OriginType.USER_ACTION, null);

                PrismObject<ShadowType> account;
                switch (accountWrapper.getProjectionStatus()) {
                    case ADD:
                        account = delta.getObjectToAdd();
                        addDefaultKindAndIntent(account);
                        WebComponentUtil.encryptCredentials(account, true, getMidpointApplication());
                        refValue.setObject(account);
                        refDelta.addValueToAdd(refValue);
                        break;
                    case DELETE:
                        account = accountWrapper.getObject();
                        refValue.setObject(account);
                        refDelta.addValueToDelete(refValue);
                        break;
                    case MODIFY:
                        // nothing to do, account modifications were applied
                        // before
                        continue;
                    case UNLINK:
                        refValue.setOid(delta.getOid());
                        refValue.setTargetType(ShadowType.COMPLEX_TYPE);
                        refDelta.addValueToDelete(refValue);
                        break;
                    default:
                        warn(getString("pageAdminFocus.message.illegalAccountState", accountWrapper.getProjectionStatus()));
                }
//            }
        }

        return refDelta;
    }

    private void addDefaultKindAndIntent(PrismObject<ShadowType> account) {
        if (account.asObjectable().getKind() == null) {
            account.asObjectable().setKind(ShadowKindType.ACCOUNT);
        }
        if (account.asObjectable().getIntent() == null) {
            account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
        }
    }

    public List<AssignmentInfoDto> showAllAssignmentsPerformed(AjaxRequestTarget ajaxRequestTarget) {
        LOGGER.debug("Recompute user assignments");
        Task task = createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
        ObjectDelta<F> delta;
        Set<AssignmentInfoDto> assignmentInfoDtoSet = new TreeSet<>();

        try {
            reviveModels();

            PrismObjectWrapper<F> focusWrapper = getObjectWrapper();
            delta = focusWrapper.getObjectDelta();
//            if (focusWrapper.getOldDelta() != null) {
//                delta = ObjectDeltaCollectionsUtil.summarize(focusWrapper.getOldDelta(), delta);
//            }

            switch (focusWrapper.getStatus()) {
                case ADDED:
                    PrismObject<F> focus = delta.getObjectToAdd();
                    prepareObjectForAdd(focus);
                    getPrismContext().adopt(focus, getCompileTimeClass());

                    LOGGER.trace("Delta before add focus:\n{}", delta.debugDumpLazily(3));
                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());
                    } else {
                        result.recordSuccess();
                    }
                    break;
                case NOT_CHANGED:
                    prepareObjectDeltaForModify(delta);
                    LOGGER.trace("Delta before modify user:\n{}", delta.debugDumpLazily(3));

                    List<ObjectDelta<? extends ObjectType>> accountDeltas = getShadowModifyDeltas(result);
                    if (!delta.isEmpty()) {
                        delta.revive(getPrismContext());
                    }
                    for (ObjectDelta accDelta : accountDeltas) {
                        if (!accDelta.isEmpty()) {
                            accDelta.revive(getPrismContext());
                        }
                    }
                    break;
                default:
                    error(getString("pageAdminFocus.message.unsupportedState", focusWrapper.getStatus()));
            }

            ModelContext<UserType> modelContext;
            try {
                ModelExecuteOptions options = ModelExecuteOptions.createEvaluateAllAssignmentRelationsOnRecompute();
                modelContext = getModelInteractionService().previewChanges(Collections.singleton(delta), options, task, result);
            } catch (NoFocusNameSchemaException e) {
                info(getString("pageAdminFocus.message.noUserName"));
                ajaxRequestTarget.add(getFeedbackPanel());
                return null;
            }

            DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
            Collection<? extends EvaluatedAssignment<?>> evaluatedAssignments = null;
            if (evaluatedAssignmentTriple != null) {
                evaluatedAssignments = evaluatedAssignmentTriple.getNonNegativeValues();
            }
            if (evaluatedAssignments == null || evaluatedAssignments.isEmpty()) {
                info(getString("pageAdminFocus.message.noAssignmentsAvailable"));
                ajaxRequestTarget.add(getFeedbackPanel());
                return null;
            }

            for (EvaluatedAssignment<?> evaluatedAssignment : evaluatedAssignments) {
                if (!evaluatedAssignment.isValid()) {
                    continue;
                }
                // roles and orgs
                DeltaSetTriple<? extends EvaluatedAssignmentTarget> targetsTriple = evaluatedAssignment.getRoles();
                Collection<? extends EvaluatedAssignmentTarget> targets = targetsTriple.getNonNegativeValues();
                for (EvaluatedAssignmentTarget target : targets) {
                    if (target.getTarget() != null && ArchetypeType.class.equals(target.getTarget().getCompileTimeClass())){
                        continue;
                    }
                    if (target.appliesToFocusWithAnyRelation(getRelationRegistry())) {
                        assignmentInfoDtoSet.add(createAssignmentsPreviewDto(target, task, result));
                    }
                }

                // all resources
                DeltaSetTriple<EvaluatedConstruction> evaluatedConstructionsTriple = evaluatedAssignment
                        .getEvaluatedConstructions(task, result);
                Collection<EvaluatedConstruction> evaluatedConstructions = evaluatedConstructionsTriple
                        .getNonNegativeValues();
                for (EvaluatedConstruction construction : evaluatedConstructions) {
                    if (!construction.isWeak()) {
                        assignmentInfoDtoSet.add(createAssignmentsPreviewDto(construction));
                    }
                }
            }

            return new ArrayList<>(assignmentInfoDtoSet);

        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not create assignments preview.", e);
            error("Could not create assignments preview. Reason: " + e);
            ajaxRequestTarget.add(getFeedbackPanel());
        }
        return null;
    }

    private AssignmentInfoDto createAssignmentsPreviewDto(EvaluatedAssignmentTarget evaluatedAbstractRole,
            Task task, OperationResult result) {
        return createAssignmentsPreviewDto(evaluatedAbstractRole.getTarget(), evaluatedAbstractRole.isDirectlyAssigned(),
            evaluatedAbstractRole.getAssignmentPath(), evaluatedAbstractRole.getAssignment(), task, result);
    }

    protected AssignmentInfoDto createAssignmentsPreviewDto(ObjectReferenceType reference, Task task, OperationResult result) {
        PrismObject<? extends FocusType> targetObject = WebModelServiceUtils.resolveReferenceNoFetch(reference,
                PageAdminFocus.this, task, result);
        return createAssignmentsPreviewDto(targetObject, true, null, null, task, result);
    }

    protected AssignmentInfoDto createDelegableAssignmentsPreviewDto(AssignmentType assignment, Task task, OperationResult result) {
        if (assignment.getTargetRef() != null) {
            if (RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())
                    || OrgType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())
                    || ServiceType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                PrismObject<AbstractRoleType> targetObject = WebModelServiceUtils.resolveReferenceNoFetch(assignment.getTargetRef(),
                        PageAdminFocus.this, task, result);
                Boolean isDelegable = false;
                if (targetObject != null) {
                    isDelegable = targetObject.asObjectable().isDelegable();
                }
                if (Boolean.TRUE.equals(isDelegable)) {
                    return createAssignmentsPreviewDto(targetObject, true, null, assignment, task, result);
                }
            }
        }
        return null;
    }

    private AssignmentInfoDto createAssignmentsPreviewDto(PrismObject<? extends AssignmentHolderType> targetObject,
            boolean isDirectlyAssigned, AssignmentPath assignmentPath, AssignmentType assignment,
            Task task, OperationResult result) {
        AssignmentInfoDto dto = new AssignmentInfoDto();
        dto.setTargetOid(targetObject.getOid());
        dto.setTargetName(getNameToDisplay(targetObject));
        dto.setTargetDescription(targetObject.asObjectable().getDescription());
        dto.setTargetClass(targetObject.getCompileTimeClass());
        dto.setTargetType(WebComponentUtil.classToQName(getPrismContext(), targetObject.getCompileTimeClass()));
        dto.setDirect(isDirectlyAssigned);
        dto.setAssignmentParent(assignmentPath);
        if (assignment != null) {
            if (assignment.getTenantRef() != null) {
                dto.setTenantName(WebModelServiceUtils.resolveReferenceName(assignment.getTenantRef(), PageAdminFocus.this));
                dto.setTenantRef(assignment.getTenantRef());
            }
            if (assignment.getOrgRef() != null) {
                dto.setOrgRefName(WebModelServiceUtils.resolveReferenceName(assignment.getOrgRef(), PageAdminFocus.this));
                dto.setOrgRef(assignment.getOrgRef());
            }
            if (assignment.getTargetRef() != null){
                dto.setRelation(assignment.getTargetRef().getRelation());
            }
        }
        return dto;
    }

    private String getNameToDisplay(PrismObject<? extends AssignmentHolderType> target) {
        if (target.canRepresent(AbstractRoleType.class)) {
            String n = PolyString.getOrig(((AbstractRoleType)target.asObjectable()).getDisplayName());
            if (StringUtils.isNotBlank(n)) {
                return n;
            }
        }
        return PolyString.getOrig(target.asObjectable().getName());
    }

    private AssignmentInfoDto createAssignmentsPreviewDto(EvaluatedConstruction evaluatedConstruction) {
        AssignmentInfoDto dto = new AssignmentInfoDto();
        PrismObject<ResourceType> resource = evaluatedConstruction.getResource();
        dto.setTargetOid(resource.getOid());
        dto.setTargetName(PolyString.getOrig(resource.asObjectable().getName()));
        dto.setTargetDescription(resource.asObjectable().getDescription());
        dto.setTargetClass(resource.getCompileTimeClass());
        dto.setDirect(evaluatedConstruction.isDirectlyAssigned());
        dto.setAssignmentParent(evaluatedConstruction.getAssignmentPath());
        dto.setKind(evaluatedConstruction.getKind());
        dto.setIntent(evaluatedConstruction.getIntent());
        return dto;
    }

    @Override
    protected void performAdditionalValidation(PrismObject<F> object,
            Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) throws SchemaException {

        if (object != null && object.asObjectable() != null) {
            for (AssignmentType assignment : object.asObjectable().getAssignment()) {
                for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
                    if (errors == null) {
                        errors = validator.validateAssignment(assignment);
                    } else {
                        errors.addAll(validator.validateAssignment(assignment));
                    }
                }
            }
        }

    }

    protected boolean isFocusHistoryPage(){
        return false;
    }

    protected boolean isSelfProfile(){
        return false;
    }
}
