/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author katka
 */
public abstract class ItemWrapperFactoryImpl<IW extends ItemWrapper, PV extends PrismValue, I extends Item, VW extends PrismValueWrapper>
        implements ItemWrapperFactory<IW, VW, PV> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemWrapperFactoryImpl.class);

    @Autowired private GuiComponentRegistryImpl registry;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private ModelService modelService;
    @Autowired private TaskManager taskManager;

    @SuppressWarnings("unchecked")
    @Override
    public IW createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        ItemName name = def.getItemName();

        I childItem = (I) parent.getNewValue().findItem(name);
        ItemStatus status = getStatus(childItem);

        if (skipCreateWrapper(def, status, context, childItem == null || CollectionUtils.isEmpty(childItem.getValues()))) {
            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
            return null;
        }

        if (childItem == null) {
            childItem = (I) parent.getNewValue().findOrCreateItem(name);
        }

        return createWrapper(parent, childItem, status, context);
    }

    ItemStatus getStatus(I childItem) {
        if (childItem == null) {
            return ItemStatus.ADDED;
        }

        return ItemStatus.NOT_CHANGED;
    }

    public abstract void registerWrapperPanel(IW wrapper);

    @SuppressWarnings("unchecked")
    public IW createWrapper(PrismContainerValueWrapper<?> parent, Item childItem, ItemStatus status, WrapperContext context) throws SchemaException {
        ItemDefinition def = childItem.getDefinition();
        if (skipCreateWrapper(def, status, context, CollectionUtils.isEmpty(childItem.getValues()))) {
            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
            if (parent != null && parent.getNewValue() != null) {
                parent.getNewValue().remove(childItem);
            }
            return null;
        }

        IW itemWrapper = createWrapperInternal(parent, (I) childItem, status, context);
        itemWrapper.setMetadata(context.isMetadata());
        itemWrapper.setProcessProvenanceMetadata(context.isProcessMetadataFor(itemWrapper.getPath()));

        registerWrapperPanel(itemWrapper);

        List<VW> valueWrappers = createValuesWrapper(itemWrapper, (I) childItem, context);
        itemWrapper.getValues().addAll(valueWrappers);
        itemWrapper.setShowEmpty(context.isShowEmpty(), false);

        boolean readOnly = determineReadOnly(itemWrapper, context);
        itemWrapper.setReadOnly(readOnly);

        boolean showInVirtualContainer = determineShowInVirtualContainer(itemWrapper, context);
        itemWrapper.setShowInVirtualContainer(showInVirtualContainer);

        setupWrapper(itemWrapper);

        return itemWrapper;

    }

    boolean skipCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        if (def == null) {
            return true;
        }
        if (QNameUtil.match(FocusType.F_LINK_REF, def.getItemName()) || QNameUtil.match(FocusType.F_PERSONA_REF, def.getItemName())) {
            LOGGER.trace("Skip creating wrapper for {}, it is not supported", def);
            return true;
        }

        if (ItemProcessing.IGNORE == def.getProcessing()) {
            LOGGER.trace("Skip creating wrapper for {}, because item processing is set to IGNORE.", def);
            return true;
        }

        if (def.isExperimental() && !WebModelServiceUtils.isEnableExperimentalFeature(getModelInteractionService(), context.getTask(), context.getResult())) {
            if (!(def instanceof PrismContainerDefinition)) {
                LOGGER.trace("Skipping creating wrapper for {}, because experimental GUI features are turned off.", def);
                return true;
            }
        }

        if (ItemStatus.ADDED == status && def.isDeprecated()) {
            LOGGER.trace("Skipping creating wrapper for {}, because item is deprecated and doesn't contain any value.", def);
            return true;
        }

        if (ItemStatus.ADDED == context.getObjectStatus() && !def.canAdd()) {
            LOGGER.trace("Skipping creating wrapper for {}, because ADD operation is not supported.", def);
            return true;
        }

        if (ItemStatus.NOT_CHANGED == context.getObjectStatus()) {
            if (!def.canRead()) {
                LOGGER.trace("Skipping creating wrapper for {}, because read operation is not supported.", def);
                return true;
            }

        }

        if (ObjectType.F_LENS_CONTEXT.equivalent(def.getItemName())) {
            LOGGER.trace("Skipping creating wrapper for lensContext.");
            return true;
        }

        if (ItemStatus.ADDED == status && TaskType.F_SUBTASK_REF.equivalent(def.getItemName())) {
            LOGGER.trace("Skipping creating wrapper for new subtaskRef, this is not supported. Only wrapper for existing subtaskRef should be created");
            return true;
        }

        return !canCreateWrapper(def, status, context, isEmptyValue);
    }

    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        if (def.isOperational() && !context.isCreateOperational()) {
            LOGGER.trace("Skipping creating wrapper for {}, because it is operational.", def.getItemName());
            return false;
        }

        return true;
    }

    protected abstract void setupWrapper(IW wrapper);

    protected List<VW> createValuesWrapper(IW itemWrapper, I item, WrapperContext context) throws SchemaException {
        List<VW> pvWrappers = new ArrayList<>();

        List<PV> values = getValues(item);
        if (values.isEmpty()) {
            if (shouldCreateEmptyValue(item, context)) {
                PV prismValue = createNewValue(item);
                VW valueWrapper = createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
                setupMetadata(itemWrapper, valueWrapper, context);
                pvWrappers.add(valueWrapper);
            }
            return pvWrappers;
        }

        for (PV pcv : values) {
            if (canCreateValueWrapper(pcv)) {
                VW valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
                setupMetadata(itemWrapper, valueWrapper, context);
                pvWrappers.add(valueWrapper);
            }
        }

        return pvWrappers;

    }

    protected <VW extends PrismValueWrapper> void setupMetadata(IW itemWrapper, VW valueWrapper, WrapperContext ctx) throws SchemaException {
        if (itemWrapper.isMetadata()) {
            return;
        }
        PrismValue oldValue = valueWrapper.getNewValue();
        PrismContainer<ValueMetadataType> metadataContainer = oldValue.getValueMetadataAsContainer();

        if (canContainLegacyMetadata(oldValue)) {
            PrismContainer<MetadataType> oldMetadata = ((PrismContainerValue) oldValue).findContainer(ObjectType.F_METADATA);
            if (oldMetadata != null && oldMetadata.getValue() != null) {
                PrismContainerValue<ValueMetadataType> newMetadataValue = metadataContainer.createNewValue();
                transformStorageMetadata(newMetadataValue, oldMetadata);
                transformProcessMetadata(newMetadataValue, oldMetadata);
            }
        }

        ValueMetadataWrapperFactoryImpl valueMetadataWrapperFactory = new ValueMetadataWrapperFactoryImpl(getRegistry());
        PrismContainerWrapper<ValueMetadataType> valueMetadataWrapper = valueMetadataWrapperFactory.createWrapper(null, metadataContainer, ItemStatus.NOT_CHANGED, ctx);
        if (valueMetadataWrapper != null) {
            valueWrapper.setValueMetadata(new ValueMetadataWrapperImpl(valueMetadataWrapper));
        }
    }

    private <T, PV extends PrismValue> boolean canContainLegacyMetadata(PV value) {
        if (value instanceof PrismObjectValue) {
            return true;
        }

        if (!(value instanceof PrismContainerValue)) {
            return false;
        }

        PrismContainerDefinition containerDef = ((PrismContainerValue) value).getDefinition();
        if (containerDef == null || containerDef.isRuntimeSchema()) {
            return false;
        }

        T realValue = value.getRealValue();
        if (realValue == null) {
            return false;
        }

        if (PasswordType.class.isAssignableFrom(realValue.getClass())) {
            return true;
        }

        if (AssignmentType.class.isAssignableFrom(realValue.getClass())) {
            return true;
        }

        return false;
    }

    private void transformStorageMetadata(PrismContainerValue<ValueMetadataType> metadataValue, PrismContainer<MetadataType> oldMetadata) throws SchemaException {

        MetadataType oldMetadataType = oldMetadata.getRealValue();
        StorageMetadataType storageMetadataType = new StorageMetadataType(prismContext);
        storageMetadataType.setCreateChannel(oldMetadataType.getCreateChannel());
        storageMetadataType.setCreateTaskRef(oldMetadataType.getCreateTaskRef());
        storageMetadataType.setCreateTimestamp(oldMetadataType.getCreateTimestamp());
        storageMetadataType.setCreatorRef(oldMetadataType.getCreatorRef());
        storageMetadataType.setModifierRef(oldMetadataType.getModifierRef());
        storageMetadataType.setModifyChannel(oldMetadataType.getModifyChannel());
        storageMetadataType.setModifyTaskRef(oldMetadataType.getModifyTaskRef());
        storageMetadataType.setModifyTimestamp(oldMetadataType.getModifyTimestamp());

        if (!storageMetadataType.asPrismContainerValue().isEmpty()) {
            PrismContainer<StorageMetadataType> storagetMetadata = metadataValue.findOrCreateContainer(ValueMetadataType.F_STORAGE);
            storagetMetadata.setRealValue(storageMetadataType);
        }



    }

    private void transformProcessMetadata(PrismContainerValue<ValueMetadataType> metadataValue, PrismContainer<MetadataType> oldContainer) throws SchemaException {
        MetadataType oldMetadata = oldContainer.getRealValue();
        ProcessMetadataType processMetadataType = new ProcessMetadataType(prismContext);
        processMetadataType.setCertificationFinishedTimestamp(oldMetadata.getCertificationFinishedTimestamp());
        processMetadataType.setCertificationOutcome(oldMetadata.getCertificationOutcome());
        processMetadataType.setCreateApprovalTimestamp(oldMetadata.getCreateApprovalTimestamp());
        processMetadataType.setModifyApprovalTimestamp(oldMetadata.getModifyApprovalTimestamp());
        processMetadataType.setRequestorComment(oldMetadata.getRequestorComment());
        processMetadataType.setRequestorRef(oldMetadata.getRequestorRef());
        processMetadataType.setRequestTimestamp(oldMetadata.getRequestTimestamp());

        for (ObjectReferenceType ref : oldMetadata.getCertifierRef()){
            processMetadataType.getCertifierRef().add(ref.clone());
        }

        for (String comment : oldMetadata.getCertifierComment()) {
            processMetadataType.getCertifierComment().add(comment);
        }

        for (ObjectReferenceType ref : oldMetadata.getCreateApproverRef()){
            processMetadataType.getCreateApproverRef().add(ref.clone());
        }

        for (String comment : oldMetadata.getCreateApprovalComment()){
            processMetadataType.getCreateApprovalComment().add(comment);
        }

        for (ObjectReferenceType ref : oldMetadata.getModifyApproverRef()){
            processMetadataType.getModifyApproverRef().add(ref.clone());
        }

        for (String comment : oldMetadata.getModifyApprovalComment()){
            processMetadataType.getModifyApprovalComment().add(comment);
        }

        if (!processMetadataType.asPrismContainerValue().isEmpty()) {
            PrismContainer<ProcessMetadataType> processMetadata = metadataValue.findOrCreateContainer(ValueMetadataType.F_PROCESS);
            processMetadata.setRealValue(processMetadataType);
        }
    }

    protected List<PV> getValues(I item) {
        return item.getValues();
    }

    private boolean determineReadOnly(IW itemWrapper, WrapperContext context) {

        Boolean readOnly = context.getReadOnly();
        if (readOnly != null) {
            LOGGER.trace("Setting {} as readonly because context said so.", itemWrapper);
            return readOnly;
        }

        ItemStatus objectStatus = context.getObjectStatus();

        if (ItemStatus.NOT_CHANGED == objectStatus) {
            if (!itemWrapper.canModify()) {
                LOGGER.trace("Setting {} as readonly because authZ said so.", itemWrapper);
                return true;
            }
        }

        return false;
    }

    private boolean determineShowInVirtualContainer(IW itemWrapper, WrapperContext context) {
        Collection<VirtualContainersSpecificationType> virtualContainers = context.getVirtualContainers();

        if (virtualContainers == null) {
            return false;
        }

        for (VirtualContainersSpecificationType virtualContainer : virtualContainers) {
            for (VirtualContainerItemSpecificationType item : virtualContainer.getItem()) {
                ItemPathType itemPathType = item.getPath();
                if (itemPathType == null) {
                    LOGGER.error("Bad virtual item specification, missing path. Skipping virtual item settings for {}", itemWrapper);
                    continue;
                }
                ItemPath itemPath = itemPathType.getItemPath();
                if (itemPath.equivalent(itemWrapper.getPath())) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean canCreateValueWrapper(PV pcv) {
        return true;
    }

    protected abstract PV createNewValue(I item) throws SchemaException;

    protected abstract IW createWrapperInternal(PrismContainerValueWrapper<?> parent, I childContainer, ItemStatus status, WrapperContext wrapperContext);

    protected boolean shouldCreateEmptyValue(I item, WrapperContext context) {
//        if (!item.getDefinition().isEmphasized()) {
//            return false;
//        }

        if (!context.isCreateIfEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * @return the registry
     */
    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    /**
     * @return the prismContext
     */
    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ModelService getModelService() {
        return modelService;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }
}
