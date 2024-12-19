/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.util.exception.CommonException;

import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FocusDetailsModels<F extends FocusType> extends AssignmentHolderDetailsModel<F> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusDetailsModels.class);

    private static final String DOT_CLASS = FocusDetailsModels.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";

    private LoadableDetachableModel<List<ShadowWrapper>> projectionModel;
    private final LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;
    private boolean isSelfProfile = false;
    private boolean history;

    private static final String PANEL_TYPE_BASIC = "basic";
    private static final String PANEL_TYPE_ASSIGNMENTS = "assignments";
    private static final String PANEL_TYPE_ACTIVATION = "activation";
    private static final String PANEL_TYPE_PASSWORD = "password";
    private static final String PANEL_TYPE_DELEGATED_TO_ME = "delegatedToMe";
    private static final String PANEL_TYPE_MODIFICATION_TARGET = "modificationTarget";
    public FocusDetailsModels(LoadableDetachableModel<PrismObject<F>> prismObjectModel, PageBase serviceLocator) {
        this(prismObjectModel, false, serviceLocator);
    }

    public FocusDetailsModels(LoadableDetachableModel<PrismObject<F>> prismObjectModel, boolean history, PageBase serviceLocator) {
        super(prismObjectModel, serviceLocator);

        projectionModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ShadowWrapper> load() {
                return loadShadowWrappers();
            }
        };

        executeOptionsModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ExecuteChangeOptionsDto load() {
                return ExecuteChangeOptionsDto.createFromSystemConfiguration();
            }
        };
        this.history = history;
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {

//        if (getPatternDeltas() == null || getPatternDeltas().isEmpty()) {
//            GuiObjectDetailsPageType guiObjectDetailsPageType = super.loadDetailsPageConfiguration().clone();
//            hiddeAssignmentTargetPanel(guiObjectDetailsPageType);
//            return guiObjectDetailsPageType;
//        }

        if (history) {
            GuiObjectDetailsPageType guiObjectDetailsPageType = super.loadDetailsPageConfiguration().clone();
            List<ContainerPanelConfigurationType> containerPanelConfigurationTypeList = guiObjectDetailsPageType.getPanel();
            hiddeSpecificPanel(containerPanelConfigurationTypeList, visiblePanelIdentifierList());

            return guiObjectDetailsPageType;
        } else if (isSelfProfile) {
            GuiObjectDetailsPageType detailsPage = getModelServiceLocator().getCompiledGuiProfile().getSelfProfilePage();
            if (detailsPage != null) {
                return detailsPage;
            }
        }

        return super.loadDetailsPageConfiguration();
    }

//    private void hiddeAssignmentTargetPanel(GuiObjectDetailsPageType guiObjectDetailsPageType) {
//        List<ContainerPanelConfigurationType> containerPanelConfigurationTypeList = guiObjectDetailsPageType.getPanel();
//
//        for (ContainerPanelConfigurationType containerPanelConfigurationType : containerPanelConfigurationTypeList) {
//            String identifier = containerPanelConfigurationType
//                    .getIdentifier();
//
//            if (identifier == null) {
//                continue;
//            }
//
//            if (identifier.equals(PANEL_TYPE_MODIFICATION_TARGET)) {
//                containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
//            }
//        }
//
//        if (history) {
//            hiddeSpecificPanel(containerPanelConfigurationTypeList, visiblePanelIdentifierList());
//        }
//    }

    private void hiddeSpecificPanel(List<ContainerPanelConfigurationType> item, List<String> visiblePanelIdentifierList) {
        for (ContainerPanelConfigurationType containerPanelConfigurationType : item) {
            String identifier = containerPanelConfigurationType
                    .getIdentifier();

            if (identifier == null) {
                continue;
            }

            if (!visiblePanelIdentifierList.contains(identifier)) {
                containerPanelConfigurationType.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
            }
        }
    }

    @Override
    protected boolean isReadonly() {
        return history;
    }

    public boolean isHistoricalObject() {
        return history;
    }

    private List<String> visiblePanelIdentifierList() {
        ArrayList<String> visiblePanelIdentifier = new ArrayList<>();
        visiblePanelIdentifier.add(PANEL_TYPE_BASIC);
        visiblePanelIdentifier.add(PANEL_TYPE_DELEGATED_TO_ME);
        visiblePanelIdentifier.add(PANEL_TYPE_PASSWORD);
        visiblePanelIdentifier.add(PANEL_TYPE_ACTIVATION);
        visiblePanelIdentifier.add(PANEL_TYPE_ASSIGNMENTS);

        return visiblePanelIdentifier;
    }

    private List<ShadowWrapper> loadShadowWrappers() {
        LOGGER.trace("Loading shadow wrapper");
        long start = System.currentTimeMillis();
        List<ShadowWrapper> list = new ArrayList<>();

        PrismObjectWrapper<F> focusWrapper = getObjectWrapperModel().getObject();
        PrismObject<F> focus = focusWrapper.getObject();
        PrismReference prismReference = focus.findReference(UserType.F_LINK_REF);
        if (prismReference == null || prismReference.isEmpty()) {
            return new ArrayList<>();
        }
        List<PrismReferenceValue> references = prismReference.getValues();

        Task task = getModelServiceLocator().createSimpleTask(OPERATION_LOAD_SHADOW);
        for (PrismReferenceValue reference : references) {
            if (reference == null || (reference.getOid() == null && reference.getTargetType() == null)) {
                LOGGER.trace("Skiping reference for shadow with null oid");
                continue; // default value
            }
            long shadowTimestampBefore = System.currentTimeMillis();
            OperationResult subResult = task.getResult().createMinorSubresult(OPERATION_LOAD_SHADOW);
            PrismObject<ShadowType> projection = getPrismObjectForShadowWrapper(reference.getOid(),
                    true, task, subResult, createLoadOptionForShadowWrapper());

            long shadowTimestampAfter = System.currentTimeMillis();
            LOGGER.trace("Got shadow: {} in {}", projection, shadowTimestampAfter - shadowTimestampBefore);
            if (projection == null) {
                LOGGER.error("Couldn't load shadow projection");
                continue;
            }

            long timestampWrapperStart = System.currentTimeMillis();
            try {

                ShadowWrapper wrapper = loadShadowWrapper(projection, true, task, subResult);
                wrapper.setLoadWithNoFetch(true);
                wrapper.setRelation(reference.getRelation());
                list.add(wrapper);

            } catch (Throwable e) {
                getPageBase().showResult(subResult, "pageAdminFocus.message.couldntCreateShadowWrapper");
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
        return getPageBase().getOperationOptionsBuilder()
                .resolveNames()
                .build();
    }

    @NotNull
    public ShadowWrapper loadShadowWrapper(PrismObject<ShadowType> projection, boolean noFetch, Task task, OperationResult result) throws SchemaException {
        PrismObjectWrapperFactory<ShadowType> factory = getPageBase().findObjectWrapperFactory(projection.getDefinition());
        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(noFetch ? false : true);
        ShadowWrapper wrapper = (ShadowWrapper) factory.createObjectWrapper(projection, ItemStatus.NOT_CHANGED, context);
        wrapper.setProjectionStatus(UserDtoStatus.MODIFY);
        return wrapper;
    }

    public void setSelfProfile(boolean selfProfile) {
        isSelfProfile = selfProfile;
    }

    public boolean isSelfProfile() {
        return isSelfProfile;
    }

    private PrismObject<ShadowType> getPrismObjectForShadowWrapper(String oid, boolean noFetch,
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

        PrismObject<ShadowType> projection = WebModelServiceUtils.loadObject(ShadowType.class, oid, loadOptions, getPageBase(), task, subResult);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Loaded projection {} ({}):\n{}", oid, loadOptions, projection == null ? null : projection.debugDump());
        }

        return projection;
    }

    public LoadableDetachableModel<List<ShadowWrapper>> getProjectionModel() {
        return projectionModel;
    }

    @Override
    protected void prepareObjectForAdd(PrismObject<F> focus) throws CommonException {
        super.prepareObjectForAdd(focus);
        F focusType = focus.asObjectable();
        // handle added accounts

        List<ShadowType> shadowsToAdd = prepareShadowObject(projectionModel.getObject());
        for (ShadowType shadowToAdd : shadowsToAdd) {
            addDefaultKindAndIntent(shadowToAdd.asPrismObject());
            ObjectReferenceType linkRef = new ObjectReferenceType();
            linkRef.asReferenceValue().setObject(shadowToAdd.asPrismObject());
            focusType.getLinkRef().add(linkRef);
        }
    }

    private void addDefaultKindAndIntent(PrismObject<ShadowType> account) {
        if (account.asObjectable().getKind() == null) {
            account.asObjectable().setKind(ShadowKindType.ACCOUNT);
        }
        if (account.asObjectable().getIntent() == null) {
            account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
        }
    }

    private List<ShadowType> prepareShadowObject(List<ShadowWrapper> projections) throws CommonException {
        List<ShadowType> projectionsToAdd = new ArrayList<>();
        for (ShadowWrapper projection : projections) {
            if (UserDtoStatus.MODIFY.equals(projection.getProjectionStatus())) {
                // this is legal e.g. when child org is being create (one assignment comes pre-created)
                // TODO do we need more specific checks here?
                continue;
            }

            if (!UserDtoStatus.ADD.equals(projection.getProjectionStatus())) {
                getPageBase().warn(getPageBase().getString("pageAdminFocus.message.illegalAccountState", projection.getStatus()));
                continue;
            }

            ObjectDelta<ShadowType> delta = projection.getObjectDelta();
            PrismObject<ShadowType> proj = delta.getObjectToAdd();
            WebComponentUtil.encryptCredentials(proj, true, getPageBase());

            projectionsToAdd.add(proj.asObjectable());
        }
        return projectionsToAdd;
    }

    @Override
    protected void prepareObjectDeltaForModify(ObjectDelta<F> focusDelta) throws CommonException {
        super.prepareObjectDeltaForModify(focusDelta);
        // handle accounts
        PrismObjectDefinition<F> objectDefinition = getObjectDefinition();
        PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(FocusType.F_LINK_REF);
        ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
        if (!refDelta.isEmpty()) {
            focusDelta.addModification(refDelta);
        }
    }

    protected PrismObjectDefinition<F> getObjectDefinition() {
        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        return registry
                .findObjectDefinitionByCompileTimeClass(getObjectWrapperModel().getObject().getCompileTimeClass());
    }

    private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
            throws CommonException {
        ReferenceDelta refDelta = getPrismContext().deltaFactory().reference().create(refDef);

        List<ShadowWrapper> accounts = projectionModel.getObject();
        for (ShadowWrapper accountWrapper : accounts) {
            accountWrapper.revive(getPrismContext());
            ObjectDelta delta = accountWrapper.getObjectDelta();
            PrismReferenceValue refValue = getPrismContext().itemFactory().createReferenceValue(null, OriginType.USER_ACTION, null);
            refValue.setRelation(accountWrapper.getRelation());

            PrismObject<ShadowType> account;
            switch (accountWrapper.getProjectionStatus()) {
                case ADD:
                    account = delta.getObjectToAdd();
                    if (skipAddShadow(account.asObjectable().getResourceRef(), accounts)) {
                        break;
                    }
                    addDefaultKindAndIntent(account);
                    WebComponentUtil.encryptCredentials(account, true, getPageBase());
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
                    getPageBase().warn(getPageBase().getString("pageAdminFocus.message.illegalAccountState", accountWrapper.getProjectionStatus()));
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

    private boolean skipDeleteShadow(ObjectReferenceType resourceRef, List<ShadowWrapper> accounts) throws CommonException {
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

    @Override
    protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

        List<ShadowWrapper> accounts = projectionModel.getObject();
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

                WebComponentUtil.encryptCredentials(delta, true, getPageBase());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Modifying account:\n{}", delta.debugDump(3));
                }

                deltas.add(delta);

            } catch (Exception ex) {
                result.recordFatalError(getPageBase().getString("PageAdminFocus.message.getShadowModifyDeltas.fatalError"), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute account delta", ex);
            }
        }

        return deltas;
    }

    public LoadableModel<ExecuteChangeOptionsDto> getExecuteOptionsModel() {
        return this.executeOptionsModel;
    }
}
