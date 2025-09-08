/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.ExecutedDeltaPostProcessor;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.cleanupEmptyValue;

public class ObjectDetailsModels<O extends ObjectType> implements Serializable, IDetachable {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDetailsModels.class);

    private static final String DOT_CLASS = ObjectDetailsModels.class.getName() + ".";
    protected static final String OPERATION_LOAD_PARENT_ORG = DOT_CLASS + "loadParentOrgs";

    private ModelServiceLocator modelServiceLocator;
    private LoadableDetachableModel<PrismObject<O>> prismObjectModel;

    private LoadableModel<PrismObjectWrapper<O>> objectWrapperModel;
    private LoadableModel<GuiObjectDetailsPageType> detailsPageConfigurationModel;

    private LoadableDetachableModel<O> summaryModel;
    private List<ObjectDelta<? extends ObjectType>> savedDeltas = new ArrayList<>();

    private final Map<String, IModel<PrismContainerValueWrapper>> subMenuModels = new HashMap<>();

    public ObjectDetailsModels(LoadableDetachableModel<PrismObject<O>> prismObjectModel, ModelServiceLocator serviceLocator) {
        this.prismObjectModel = prismObjectModel;
        this.modelServiceLocator = serviceLocator;

        objectWrapperModel = new LoadableModel<>(false) {

            @Override
            protected PrismObjectWrapper<O> load() {
                PrismObject<O> prismObject = getPrismObject();

                if (prismObject == null) {
                    return null;
                }

                PrismObjectWrapperFactory<O> factory = modelServiceLocator.findObjectWrapperFactory(prismObject.getDefinition());
                Task task = modelServiceLocator.createSimpleTask("createWrapper");
                OperationResult result = task.getResult();
                WrapperContext ctx = createWrapperContext(task, result);
                if (isReadonly()) {
                    ctx.setReadOnly(isReadonly());
                }
                try {
                    return factory.createObjectWrapper(prismObject, isEditObject() ? ItemStatus.NOT_CHANGED : ItemStatus.ADDED, ctx);
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot create wrapper for {} \nReason: {]", e, prismObject, e.getMessage());
                    result.recordFatalError("Cannot create wrapper for " + prismObject + ", because: " + e.getMessage(), e);
                    getPageBase().showResult(result);
                    throw getPageBase().redirectBackViaRestartResponseException();
                }

            }
        };

        detailsPageConfigurationModel = new LoadableModel<>(false) {
            @Override
            protected GuiObjectDetailsPageType load() {
                return loadDetailsPageConfiguration().clone();
            }
        };

        summaryModel = new LoadableDetachableModel<O>() {

            @Override
            protected O load() {
                PrismObjectWrapper<O> wrapper = objectWrapperModel.getObject();
                if (wrapper == null) {
                    return null;
                }

                PrismObject<O> object = wrapper.getObject();
                loadParentOrgs(object);
                return object.asObjectable();
            }
        };
    }

    public WrapperContext createWrapperContext() {
        Task task = getModelServiceLocator().createSimpleTask("createWrapper");
        OperationResult result = task.getResult();
        WrapperContext context = createWrapperContext(task, result);
        return context;
    }

    protected WrapperContext createWrapperContext(Task task, OperationResult result) {
        WrapperContext ctx = new WrapperContext(task, result);
        ctx.setCreateIfEmpty(true);
        ctx.setDetailsPageTypeConfiguration(getPanelConfigurations());
        if (getPageBase() instanceof AbstractPageObjectDetails) {
            AbstractPageObjectDetails page = (AbstractPageObjectDetails) getPageBase();
            ctx.setShowedByWizard(page.isShowedByWizard());
        }
        return ctx;
    }

    public List<? extends ContainerPanelConfigurationType> getPanelConfigurations() {
        GuiObjectDetailsPageType detailsPage = detailsPageConfigurationModel.getObject();
        if (detailsPage == null) {
            return Collections.emptyList();
        }
        return detailsPage.getPanel();
    }

    private void loadParentOrgs(PrismObject<O> object) {
        Task task = getModelServiceLocator().createSimpleTask(OPERATION_LOAD_PARENT_ORG);
        OperationResult subResult = task.getResult();
        // Load parent organizations (full objects). There are used in the
        // summary panel and also in the main form.
        // Do it here explicitly instead of using resolve option to have ability
        // to better handle (ignore) errors.
        for (ObjectReferenceType parentOrgRef : object.asObjectable().getParentOrgRef()) {

            PrismObject<OrgType> parentOrg = null;
            try {

                parentOrg = getModelServiceLocator().getModelService().getObject(
                        OrgType.class, parentOrgRef.getOid(), null, task, subResult);
                LOGGER.trace("Loaded parent org with result {}", subResult.getLastSubresult());
            } catch (AuthorizationException e) {
                // This can happen if the user has permission to read parentOrgRef but it does not have
                // the permission to read target org
                // It is OK to just ignore it.
                subResult.muteLastSubresultError();
                PrismObject<? extends FocusType> taskOwner = task.getOwner(subResult);
                LOGGER.debug("User {} does not have permission to read parent org unit {} (ignoring error)", taskOwner.getName(), parentOrgRef.getOid());
            } catch (Exception ex) {
                subResult.recordWarning(getPageBase().createStringResource("PageAdminObjectDetails.message.loadParentOrgs.warning", parentOrgRef.getOid()).getString(), ex);
                LOGGER.warn("Cannot load parent org {}: {}", parentOrgRef.getOid(), ex.getMessage(), ex);
            }

            if (parentOrg != null) {
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(parentOrg, getPrismContext());
                ref.asReferenceValue().setObject(parentOrg);
                object.asObjectable().getParentOrgRef().add(ref);
            }
        }
        subResult.computeStatus();
    }

    protected PageBase getPageBase() {
        return (PageBase) getModelServiceLocator();
    }

    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        return modelServiceLocator.getCompiledGuiProfile().findObjectDetailsConfiguration(getPrismObject().getDefinition().getTypeName());
    }

    //TODO change summary panels to wrappers?
    public LoadableDetachableModel<O> getSummaryModel() {
        return summaryModel;
    }

    public boolean isEditObject() {
        return getPrismObject().getOid() != null;
    }

    protected PrismContext getPrismContext() {
        return modelServiceLocator.getPrismContext();
    }

    private Collection<SimpleValidationError> validationErrors;
    private ObjectDelta<O> delta;

    public ObjectDelta<O> getDelta() {
        return delta;
    }

    private Collection<ObjectDelta<? extends ObjectType>> collectDeltasFromObject(OperationResult result) throws CommonException {
        validationErrors = null;
        PrismObjectWrapper<O> objectWrapper = getObjectWrapperModel().getObject();
        delta = objectWrapper.getObjectDelta();

        delta.getModifications().forEach(mod -> {
            cleanupEmptyValue(mod.getValuesToAdd());
            cleanupEmptyValue(mod.getValuesToReplace());
            cleanupEmptyValue(mod.getValuesToDelete());
        });

        WebComponentUtil.encryptCredentials(delta, true, modelServiceLocator);
        switch (objectWrapper.getStatus()) {
            case ADDED:
                PrismObject<O> objectToAdd = delta.getObjectToAdd();
                prepareObjectForAdd(objectToAdd);
                getPrismContext().adopt(objectToAdd, objectWrapper.getCompileTimeClass());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Delta before add user:\n{}", delta.debugDump(3));
                }

                if (!delta.isEmpty()) {
                    delta.revive(getPrismContext());

                    final Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(delta);
                    validationErrors = performCustomValidation(objectToAdd, deltas);
                    return deltas;
                }
                break;

            case NOT_CHANGED:
                prepareObjectDeltaForModify(delta); //preparing of deltas for projections (ADD, DELETE, UNLINK)

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Delta before modify user:\n{}", delta.debugDump(3));
                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
                if (!delta.isEmpty()) {
                    delta.revive(getPrismContext());
                    deltas.add(delta);
                    validationErrors = performCustomValidation(objectWrapper.getObject(), deltas);
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
                return deltas;
            // support for add/delete containers (e.g. delete credentials)
            default:
                throw new UnsupportedOperationException("Unsupported state");
        }
        LOGGER.trace("returning from saveOrPreviewPerformed");
        return new ArrayList<>();
    }

    /**
     * Collect processor with deltas and consumer, that should be processed before basic deltas of showed object
     */
    public Collection<ExecutedDeltaPostProcessor> collectPreconditionDeltas(
            ModelServiceLocator serviceLocator, OperationResult result) throws CommonException {
        PrismObjectWrapper<O> objectWrapper = getObjectWrapperModel().getObject();
        Collection<ExecutedDeltaPostProcessor> preconditionDeltas = objectWrapper.getPreconditionDeltas(serviceLocator, result);
        return preconditionDeltas == null ? new ArrayList<>() : preconditionDeltas;
    }

    public Collection<SimpleValidationError> getValidationErrors() {
        return validationErrors;
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

//        performAdditionalValidation(object, deltas, errors);

        for (MidpointFormValidator validator : getValidators()) {
            if (errors == null) {
                errors = validator.validateObject(object, deltas);
            } else {
                errors.addAll(validator.validateObject(object, deltas));
            }
        }

        return errors;
    }

    private Collection<MidpointFormValidator> getValidators() {
        return modelServiceLocator.getFormValidatorRegistry().getValidators();
    }

    protected void prepareObjectForAdd(PrismObject<O> objectToAdd) throws CommonException {

    }

    protected void prepareObjectDeltaForModify(ObjectDelta<O> modifyDelta) throws CommonException {

    }

    protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
        return new ArrayList<>();
    }

    public void reset() {
        prismObjectModel.detach();
        objectWrapperModel.reset();
        detailsPageConfigurationModel.reset();
        summaryModel.detach();
    }

    protected ModelServiceLocator getModelServiceLocator() {
        return modelServiceLocator;
    }

    protected AdminGuiConfigurationMergeManager getAdminGuiConfigurationMergeManager() {
        return modelServiceLocator.getAdminGuiConfigurationMergeManager();
    }

    public LoadableModel<PrismObjectWrapper<O>> getObjectWrapperModel() {
        return objectWrapperModel;
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
        return getObjectWrapperModel().getObject();
    }

    protected PrismObject<O> getPrismObject() {
        if (!objectWrapperModel.isLoaded()) {
            return prismObjectModel.getObject();
        }
        return getObjectWrapper().getObject();
    }

    public void reloadPrismObjectModel() {
        PrismObject<O> oldObject = getObjectWrapper().getObjectOld();
        reset();
        reloadPrismObjectModel(oldObject);
    }

    public void reloadPrismObjectModel(@NotNull PrismObject<O> newObject) {
        prismObjectModel.detach();
        if (!savedDeltas.isEmpty()) {
            savedDeltas.forEach(delta -> {
                try {
                    if (delta.isAdd()) {
                        if (newObject.getOid() == null) {
                            newObject.getValue().mergeContent(delta.getObjectToAdd().getValue(), List.of());
                        }
                    } else {
                        ((ObjectDelta) delta).applyTo(newObject);
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't apply delta " + delta + " to object " + newObject, e);
                }
            });
        }
        prismObjectModel = new LoadableDetachableModel<>() {

            @Override
            protected PrismObject<O> load() {
                return newObject;
            }
        };
    }

    public LoadableModel<GuiObjectDetailsPageType> getObjectDetailsPageConfiguration() {
        return detailsPageConfigurationModel;
    }

    public O getObjectType() {
        PrismObject<O> object = getPrismObject();
        return object == null ? null : object.asObjectable();
    }

    protected boolean isReadonly() {
        return false;
    }

    public ItemStatus getObjectStatus() {
        return objectWrapperModel.getObject().getStatus();
    }

    public SummaryPanelSpecificationType getSummaryPanelSpecification() {
        GuiObjectDetailsPageType detailsPageConfig = detailsPageConfigurationModel.getObject();
        if (detailsPageConfig == null) {
            return null;
        }

        return detailsPageConfig.getSummaryPanel();
    }

    @Override
    public void detach() {
        prismObjectModel.detach();
        objectWrapperModel.detach();
        detailsPageConfigurationModel.detach();
        summaryModel.detach();
    }

    public Collection<ObjectDelta<? extends ObjectType>> collectDeltas(OperationResult result) throws CommonException {
        Collection<ObjectDelta<? extends ObjectType>> actualDeltas = collectDeltasFromObject(result);
        if (savedDeltas.isEmpty()) {
            return actualDeltas;
        }

        ArrayList<ObjectDelta<? extends ObjectType>> localSavedDeltas = new ArrayList<>();

        mergeDeltas(actualDeltas, localSavedDeltas);
        return localSavedDeltas;
    }

    public Collection<ObjectDelta<? extends ObjectType>> collectDeltaWithoutSavedDeltas(OperationResult result) throws CommonException {
        return collectDeltasFromObject(result);
    }

    public void saveDeltas() {
        try {
            OperationResult result = new OperationResult("collect deltas");
            Collection<ObjectDelta<? extends ObjectType>> actualDeltas = collectDeltaWithoutSavedDeltas(result);
            List<ObjectDelta<? extends ObjectType>> newSavedDeltas = new ArrayList<>();
            mergeDeltas(actualDeltas, newSavedDeltas);
            savedDeltas.clear();
            savedDeltas.addAll(newSavedDeltas);

        } catch (CommonException e) {
            LOGGER.error("Couldn't collect deltas from " + getObjectType());
        }
    }

    private void mergeDeltas(
            Collection<ObjectDelta<? extends ObjectType>> actualDeltas, List<ObjectDelta<? extends ObjectType>> retDeltas) throws SchemaException {
        for (ObjectDelta<? extends ObjectType> delta : savedDeltas) {
            Class<? extends ObjectType> type = getObjectType().getClass();
            Optional<? extends ObjectDelta> match =
                    actualDeltas.stream()
                            .filter(actualDelta -> (delta.getOid() == null && actualDelta.getObjectTypeClass().equals(type))
                                    || (actualDelta.getOid() != null && actualDelta.getOid().equals(delta.getOid())))
                            .findFirst();
            if (match.isPresent()) {
                ObjectDelta<? extends ObjectType> newDelta = delta.clone();
                if (match.get().isAdd() && match.get().getOid() == null
                        && delta.isAdd() && delta.getOid() == null) {
                    newDelta = match.get().clone();
                } else {
                    newDelta.merge(match.get());

                    if (newDelta.getOid() == null) {
                        newDelta.setOid(match.get().getOid());
                        newDelta.setChangeType(match.get().getChangeType());
                    }
                }
                if (!newDelta.isEmpty()) {
                    retDeltas.add(newDelta);
                }
                actualDeltas.remove(match.get());
            } else {
                retDeltas.add(delta);
            }
        }
        retDeltas.addAll(actualDeltas);
    }

    public void setSubPanelModel(String panelIdentifier, IModel<PrismContainerValueWrapper> valueModel) {
        if (subMenuModels.containsKey(panelIdentifier)) {
            subMenuModels.replace(panelIdentifier, valueModel);
        } else {
            subMenuModels.put(panelIdentifier, valueModel);
        }
    }

    public boolean containsModelForSubmenu(String identifier) {
        return subMenuModels.containsKey(identifier);
    }

    public IModel<PrismContainerValueWrapper> getModelForSubmenu(String identifier) {
        return subMenuModels.get(identifier);
    }
}
