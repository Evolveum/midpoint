/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin;

import java.util.*;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.wrapper.AssignmentWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AssignmentValueWrapperImpl;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;

import com.evolveum.midpoint.web.component.prism.ValueStatus;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

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

    public PageAdminFocus(final PrismObject<F> unitToEdit, boolean isNewObject) {
        initialize(unitToEdit, isNewObject);
    }

    public PageAdminFocus(final PrismObject<F> unitToEdit, boolean isNewObject, boolean isReadonly) {
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

        delegatedToMeModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

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

    private List<ShadowWrapper> loadShadowWrappers(boolean noFetch) {
        LOGGER.trace("Loading shadow wrapper");
        long start = System.currentTimeMillis();
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
            if (reference == null || (reference.getOid() == null && reference.getTargetType() == null)) {
                LOGGER.trace("Skiping reference for shadow with null oid");
                continue; // default value
            }
            long shadowTimestampBefore = System.currentTimeMillis();
            OperationResult subResult = task.getResult().createMinorSubresult(OPERATION_LOAD_SHADOW);
            PrismObject<ShadowType> projection = getPrismObjectForWrapper(ShadowType.class, reference.getOid(),
                    noFetch, task, subResult, createLoadOptionForShadowWrapper());

            long shadowTimestampAfter = System.currentTimeMillis();
            LOGGER.trace("Got shadow: {} in {}", projection, shadowTimestampAfter - shadowTimestampBefore);
            if (projection == null) {
//                showResult(subResult, "pageAdminFocus.message.couldntLoadShadowProjection");
                LOGGER.error("Couldn't load shadow projection");
                continue;
            }

            long timestampWrapperStart = System.currentTimeMillis();
            try {

                ShadowWrapper wrapper = loadShadowWrapper(projection, task, subResult);
                wrapper.setLoadWithNoFetch(noFetch);

                if (wrapper != null) {
                    list.add(wrapper);
                } else {
                    showResult(subResult, "pageAdminFocus.message.shadowWrapperIsNull");
                    LOGGER.error("ShadowWrapper is null");
                }

                //TODO catch Exception/Runtim,eException, Throwable
            } catch (SchemaException e) {
                showResult(subResult, "pageAdminFocus.message.couldntCreateShadowWrapper");
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create shadow wrapper", e);
            }
            long timestampWrapperEnd = System.currentTimeMillis();
            LOGGER.trace("Load wrapper in {}", timestampWrapperEnd - timestampWrapperStart);
        }
        long end = System.currentTimeMillis();
        LOGGER.trace("Load projctions in {}", end - start);
        return list;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createLoadOptionForShadowWrapper() {
        return getSchemaHelper().getOperationOptionsBuilder()
                .item(ShadowType.F_RESOURCE_REF).resolve().readOnly()
                .build();
    }

    public ShadowWrapper loadShadowWrapper(PrismObject<ShadowType> projection, Task task, OperationResult result) throws SchemaException {
        PrismObjectWrapperFactory<ShadowType> factory = getRegistry().getObjectWrapperFactory(projection.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(false);
        ShadowWrapper wrapper = (ShadowWrapper) factory.createObjectWrapper(projection, ItemStatus.NOT_CHANGED, context);
        wrapper.setProjectionStatus(UserDtoStatus.MODIFY);
        return wrapper;
    }

    public void loadFullShadow(PrismObjectValueWrapper<ShadowType> shadowWrapperValue, AjaxRequestTarget target) {
        LOGGER.trace("Loading full shadow");
        long start = System.currentTimeMillis();
        if (shadowWrapperValue.getRealValue() == null) {
            error(getString("pageAdminFocus.message.couldntCreateShadowWrapper"));
            LOGGER.error("Couldn't create shadow wrapper, because RealValue is null in " + shadowWrapperValue);
            return;
        }
        String oid = shadowWrapperValue.getRealValue().getOid();
        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
        OperationResult result = task.getResult();
        long loadStart = System.currentTimeMillis();
        PrismObject<ShadowType> projection = getPrismObjectForWrapper(ShadowType.class, oid, false, task,
                result, createLoadOptionForShadowWrapper());

        long loadEnd = System.currentTimeMillis();
        LOGGER.trace("Load projection in {} ms", loadEnd - loadStart);
        if (projection == null) {
            result.recordFatalError(getString("PageAdminFocus.message.loadFullShadow.fatalError", shadowWrapperValue.getRealValue()));
            showResult(result);
            target.add(getFeedbackPanel());
            return;
        }

        long wrapperStart = System.currentTimeMillis();
        ShadowWrapper shadowWrapperNew;
        try {
            shadowWrapperNew = loadShadowWrapper(projection, task, result);

            if (shadowWrapperNew == null) {
                error(getString("pageAdminFocus.message.shadowWrapperIsNull"));
                LOGGER.error("ShadowWrapper is null");
                return;
            }

            shadowWrapperValue.clearItems();
            shadowWrapperValue.addItems((Collection) shadowWrapperNew.getValue().getItems());
            ((ShadowWrapper) shadowWrapperValue.getParent()).setLoadWithNoFetch(false);
        } catch (SchemaException e) {
            error(getString("pageAdminFocus.message.couldntCreateShadowWrapper"));
            LOGGER.error("Couldn't create shadow wrapper", e);
        }
        long wrapperEnd = System.currentTimeMillis();
        LOGGER.trace("Wrapper loaded in {} ms", wrapperEnd - wrapperStart);
        long end = System.currentTimeMillis();
        LOGGER.trace("Got full shadow in {} ms", end - start);
    }

    private <S extends ObjectType> PrismObject<S> getPrismObjectForWrapper(Class<S> type, String oid, boolean noFetch,
            Task task, OperationResult subResult, Collection<SelectorOptions<GetOperationOptions>> loadOptions) {
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
            LOGGER.trace("Loaded projection {} ({}):\n{}", oid, loadOptions, projection == null ? null : projection.debugDump());
        }

        return projection;
    }

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
                    if (isDelegation) {
                        oldValue.applyDefinition(def, false);
                    } else {
                        oldValue.applyDefinition(def);
                    }
                    assDelta.addValueToDelete(oldValue.clone());
                    break;
                case MODIFY:
                    if (!assDto.isModified(getPrismContext())) {
                        LOGGER.trace("Assignment '{}' not modified.", assDto.getName());
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
        LOGGER.debug("Handling modified assignment '{}', computing delta.", assDto.getName());

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue, EquivalenceStrategy.IGNORE_METADATA);

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
                    getModelService().executeChanges(MiscUtil.createCollection(forceDeleteDelta),
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
            if (account.getProjectionStatus() == UserDtoStatus.DELETE) {
                ReferenceDelta refDelta = getPrismContext().deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF,
                        focusWrapper.getObject().getDefinition(), account.getObject());
                refDeltas.add(refDelta);
            } else if (account.getProjectionStatus() == UserDtoStatus.UNLINK) {
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

    private List<ShadowType> prepareShadowObject(List<ShadowWrapper> projections) throws SchemaException {
        List<ShadowType> projectionsToAdd = new ArrayList<>();
        for (ShadowWrapper projection : projections) {
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
            try {
                ObjectDelta<ShadowType> delta = account.getObjectDelta();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account delta computed from {} as:\n{}",
                            account, delta.debugDump(3));
                }

                if (!UserDtoStatus.MODIFY.equals(account.getProjectionStatus())) {
                    continue;
                }

                if (delta == null || delta.isEmpty()) {
                    continue;
                }

                WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Modifying account:\n{}", delta.debugDump(3));
                }

                deltas.add(delta);

            } catch (Exception ex) {
                result.recordFatalError(getString("PageAdminFocus.message.getShadowModifyDeltas.fatalError"), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute account delta", ex);
            }
        }

        return deltas;
    }

    private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
            throws SchemaException {
        ReferenceDelta refDelta = getPrismContext().deltaFactory().reference().create(refDef);

        List<ShadowWrapper> accounts = getFocusShadows();
        for (ShadowWrapper accountWrapper : accounts) {
            accountWrapper.revive(getPrismContext());
            ObjectDelta delta = accountWrapper.getObjectDelta();
            PrismReferenceValue refValue = getPrismContext().itemFactory().createReferenceValue(null, OriginType.USER_ACTION, null);

            PrismObject<ShadowType> account;
            switch (accountWrapper.getProjectionStatus()) {
                case ADD:
                    account = delta.getObjectToAdd();
                    if (skipAddShadow(account.asObjectable().getResourceRef(), accounts)) {
                        break;
                    }
                    addDefaultKindAndIntent(account);
                    WebComponentUtil.encryptCredentials(account, true, getMidpointApplication());
                    refValue.setObject(account);
                    refDelta.addValueToAdd(refValue);
                    break;
                case DELETE:
                    account = accountWrapper.getObject();
                    if (skipDeleteShadow(account.asObjectable().getResourceRef(), accounts)) {
                        break;
                    }
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

    private boolean skipAddShadow(ObjectReferenceType resourceRef, List<ShadowWrapper> accounts) {
        if (resourceRef == null) {
            return false;
        }
        String actualresourceOid = resourceRef.getOid();
        if (actualresourceOid == null) {
            return false;
        }
        for (ShadowWrapper account : accounts) {
            if (account.getProjectionStatus().equals(UserDtoStatus.DELETE)
                    && account.getObject().asObjectable().getResourceRef() != null
                    && account.getObject().asObjectable().getResourceRef().getOid() != null
                    && account.getObject().asObjectable().getResourceRef().getOid().equals(actualresourceOid)) {
                return true;
            }
        }
        return false;
    }

    private boolean skipDeleteShadow(ObjectReferenceType resourceRef, List<ShadowWrapper> accounts) throws SchemaException {
        if (resourceRef == null) {
            return false;
        }
        String actualresourceOid = resourceRef.getOid();
        if (actualresourceOid == null) {
            return false;
        }
        for (ShadowWrapper account : accounts) {
            if (account.getProjectionStatus().equals(UserDtoStatus.ADD)
                    && account.getObjectDelta().getObjectToAdd().asObjectable().getResourceRef() != null
                    && account.getObjectDelta().getObjectToAdd().asObjectable().getResourceRef().getOid() != null
                    && account.getObjectDelta().getObjectToAdd().asObjectable().getResourceRef().getOid().equals(actualresourceOid)) {
                return true;
            }
        }
        return false;
    }

    private void addDefaultKindAndIntent(PrismObject<ShadowType> account) {
        if (account.asObjectable().getKind() == null) {
            account.asObjectable().setKind(ShadowKindType.ACCOUNT);
        }
        if (account.asObjectable().getIntent() == null) {
            account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
        }
    }

    public List<AssignmentValueWrapper> showAllAssignmentsPerformed(IModel<PrismContainerWrapper<AssignmentType>> parent) {
        LOGGER.debug("Recompute user assignments");
        Task task = createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
        ObjectDelta<F> delta;
        Set<AssignmentValueWrapper> assignmentValueWrapperSet = new TreeSet<>();

        try {
            reviveModels();

            PrismObjectWrapper<F> focusWrapper = getObjectWrapper();
            delta = focusWrapper.getObjectDelta();

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
                ModelExecuteOptions options = executeOptions().evaluateAllAssignmentRelationsOnRecompute();
                modelContext = getModelInteractionService().previewChanges(Collections.singleton(delta), options, task, result);
            } catch (NoFocusNameSchemaException e) {
                info(getString("pageAdminFocus.message.noUserName"));
                return null;
            }

            Collection<? extends EvaluatedAssignment<?>> evaluatedAssignments = modelContext.getNonNegativeEvaluatedAssignments();
            if (evaluatedAssignments.isEmpty()) {
                info(getString("pageAdminFocus.message.noAssignmentsAvailable"));
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
                    if (target.getTarget() != null && ArchetypeType.class.equals(target.getTarget().getCompileTimeClass())) {
                        continue;
                    }
                    if (target.appliesToFocusWithAnyRelation(getRelationRegistry())) {
                        AssignmentType assignmentType = target.getAssignment();
                        assignmentType.setDescription(target.getTarget().asObjectable().getDescription());
                        assignmentType.getTargetRef().setTargetName(new PolyStringType(target.getTarget().getName()));
                        assignmentType.getTargetRef().setType(target.getTarget().getComplexTypeDefinition().getTypeName());
                        ValueStatus status = evaluatedAssignment.getAssignmentType(true) == null ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED;
                        AssignmentValueWrapper assignmentValueWrapper = WebPrismUtil.createNewValueWrapper(parent.getObject(),
                                assignmentType.asPrismContainerValue(), status, this);
                        assignmentValueWrapper.setDirectAssignment(target.isDirectlyAssigned());
                        assignmentValueWrapper.setAssignmentParent(target.getAssignmentPath());
                        assignmentValueWrapperSet.add(assignmentValueWrapper);
                    }
                }

                // all resources
                DeltaSetTriple<EvaluatedResourceObjectConstruction> evaluatedConstructionsTriple = evaluatedAssignment
                        .getEvaluatedConstructions(task, result);
                Collection<EvaluatedResourceObjectConstruction> evaluatedConstructions = evaluatedConstructionsTriple
                        .getNonNegativeValues();
                for (EvaluatedResourceObjectConstruction construction : evaluatedConstructions) {
                    if (!construction.isWeak()) {
                        PrismContainerDefinition<AssignmentType> assignmentDef = getPrismContext().getSchemaRegistry()
                                .findContainerDefinitionByCompileTimeClass(AssignmentType.class);
                        AssignmentType assignmentType = assignmentDef.instantiate().createNewValue().asContainerable();
                        ObjectReferenceType targetRef = new ObjectReferenceType();
                        targetRef.setOid(construction.getResource().getOid());
                        targetRef.setType(ResourceType.COMPLEX_TYPE);
                        targetRef.setTargetName(new PolyStringType(construction.getResource().getName()));
                        assignmentType.setTargetRef(targetRef);
                        ConstructionType constructionType = new ConstructionType();
                        constructionType.setKind(construction.getKind());
                        constructionType.setIntent(construction.getIntent());
                        assignmentType.setConstruction(constructionType);
                        assignmentType.setDescription(construction.getResource().asObjectable().getDescription());
                        ValueStatus status = evaluatedAssignment.getAssignmentType(true) == null ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED;
                        AssignmentValueWrapper assignmentValueWrapper = WebPrismUtil.createNewValueWrapper(parent.getObject(),
                                assignmentType.asPrismContainerValue(), status, this);
                        assignmentValueWrapper.setDirectAssignment(construction.isDirectlyAssigned());
                        assignmentValueWrapper.setAssignmentParent(construction.getAssignmentPath());
                        assignmentValueWrapperSet.add(assignmentValueWrapper);
                    }
                }
            }

            return new ArrayList<>(assignmentValueWrapperSet);

        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not create assignments preview.", e);
            error("Could not create assignments preview. Reason: " + e);
        }
        return null;
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
                    return createAssignmentsPreviewDto(targetObject, true, null, assignment);
                }
            }
        }
        return null;
    }

    private AssignmentInfoDto createAssignmentsPreviewDto(PrismObject<? extends AssignmentHolderType> targetObject,
            boolean isDirectlyAssigned, AssignmentPath assignmentPath, AssignmentType assignment) {
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
            if (assignment.getTargetRef() != null) {
                dto.setRelation(assignment.getTargetRef().getRelation());
            }
        }
        return dto;
    }

    private String getNameToDisplay(PrismObject<? extends AssignmentHolderType> target) {
        if (target.canRepresent(AbstractRoleType.class)) {
            String n = PolyString.getOrig(((AbstractRoleType) target.asObjectable()).getDisplayName());
            if (StringUtils.isNotBlank(n)) {
                return n;
            }
        }
        return PolyString.getOrig(target.asObjectable().getName());
    }

    @Override
    protected void performAdditionalValidation(PrismObject<F> object,
            Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) {

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

    protected boolean isFocusHistoryPage() {
        return false;
    }

    protected boolean isSelfProfile() {
        return false;
    }

    public boolean isLoggedInFocusPage() {
        return getObjectWrapper() != null && getObjectWrapper().getObject() != null &&
                org.apache.commons.lang3.StringUtils.isNotEmpty(getObjectWrapper().getObject().asObjectable().getOid()) &&
                getObjectWrapper().getObject().asObjectable().getOid().equals(WebModelServiceUtils.getLoggedInFocusOid());
    }
}
