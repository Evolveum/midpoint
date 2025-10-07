/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.ExecutedDeltaPostProcessor;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;

import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.impl.binding.AbstractPlainStructured;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.CommonException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * @author katka
 */
public abstract class ItemWrapperImpl<I extends Item<?, ?>, VW extends PrismValueWrapper>
        implements ItemWrapper<I, VW>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ItemWrapperImpl.class);

    private final PrismContainerValueWrapper<?> parent;

    private final ItemStatus status;

    private final List<VW> values = new ArrayList<>();

    private final I oldItem;
    private final I newItem;

    private String displayName;

    private String helpText;

    private boolean column;

//    private boolean stripe;

    private boolean showEmpty;

    private boolean showInVirtualContainer;

    private boolean isMetadata;
    private boolean showMetadataDetails;

    private boolean processProvenanceMetadata;

    //consider
    private boolean readOnly;
    private UserInterfaceElementVisibilityType visibleOverwrite;
    private Integer displayOrder;
    private boolean validated;

    public ItemWrapperImpl(PrismContainerValueWrapper<?> parent, I item, ItemStatus status) {
        Validate.notNull(item, "Item must not be null.");
        Validate.notNull(status, "Item status must not be null.");

        this.parent = parent;
        this.newItem = item;
        this.oldItem = (I) item.clone();
        this.status = status;
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {
        LOGGER.trace("Start computing delta for {}", newItem);

        if (isOperational()) {
            return null;
        }
        return computeDeltaInternal();
    }

    //TODO this is not good. if getDetla is overriden, this is never called.
    // however, this is needed for special cases, such as authentication behavior
    // think about better solution
    protected  <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> computeDeltaInternal() throws SchemaException {
        D delta;
        if (parent != null && ValueStatus.ADDED == parent.getStatus()) {
            delta = (D) createEmptyDelta(getItemName());
        } else {
            delta = (D) createEmptyDelta(getPath());
        }

        for (VW value : values) {
            addValueToDelta(value, delta);
        }

        if (delta.isEmpty()) {
            LOGGER.trace("There is no delta for {}", newItem);
            return null;
        }

        LOGGER.trace("Returning delta {}", delta);
        return MiscUtil.createCollection(delta);
    }

    protected  <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> void addValueToDelta(VW value, D delta)
            throws SchemaException {
        value.addToDelta(delta);
    }

    @Override
    public String getDisplayName() {
        if (displayName == null) {
            displayName = getLocalizedDisplayName();
        }

        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getHelp() {
        if (helpText == null) {
            helpText = getLocalizedHelpText();
        }
        return helpText;
    }

    public void setHelp(String help) {
        this.helpText = help;
    }

    private String getLocalizedHelpText() {
        Class<?> containerClass = null;
        PrismContainerValue<?> val = newItem.getParent();
        if (val != null && val.getDefinition() != null
                && !val.getDefinition().isRuntimeSchema()
                && val.getRealClass() != null) {
            containerClass = val.getRealClass();
        }

        return WebPrismUtil.getHelpText(getItemDefinition(), containerClass);
    }

    @Override
    public boolean isExperimental() {
        return getItemDefinition().isExperimental();
    }

    @Override
    public String getDeprecatedSince() {
        return getItemDefinition().getDeprecatedSince();
    }

    @Override
    public boolean isDeprecated() {
        return getItemDefinition().isDeprecated();
    }

    @Override
    public boolean isOptionalCleanup() {
        return getItemDefinition().isOptionalCleanup();
    }

    @Override
    public boolean isRemoved() {
        return getItemDefinition().isRemoved();
    }

    @Override
    public String getRemovedSince() {
        return getItemDefinition().getRemovedSince();
    }

    @Override
    public ItemStatus getStatus() {
        return status;
    }

    @Override
    public I getItem() {
        return newItem;
    }

    @Override
    public void setColumn(boolean column) {
        this.column = column;
    }

    @Override
    public boolean isColumn() {
        return column;
    }

    @Override
    public PrismContainerValueWrapper<?> getParent() {
        return parent;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public ItemPath getPath() {
        return newItem.getPath();
    }

    @Override
    public ExpressionType getFormComponentValidator() {
        FormItemValidationType formItemValidation = getItemDefinition().getAnnotation(ItemRefinedDefinitionType.F_VALIDATION);
        if (formItemValidation == null) {
            return null;
        }

        List<FormItemServerValidationType> serverValidators = formItemValidation.getServer();
        if (CollectionUtils.isNotEmpty(serverValidators)) {
            return serverValidators.iterator().next().getExpression();
        }

        return null;
    }

    <ID extends ItemDefinition<I>> ID getItemDefinition() {
        //noinspection unchecked
        return (ID) newItem.getDefinition();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append(getDisplayName())
                .append(" (").append(getItemName()).append(", ")
                .append(status).append(", ");
        if (isReadOnly()) {
            sb.append("readonly)");
        } else {
            sb.append("writable)");
        }
        sb.append("\n");

        if (!values.isEmpty()) {
            for (VW value : values) {
                sb.append(value.debugDump(indent + 1)).append("\n");
            }
        } else {
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("NO VALUES \n");
        }
        return sb.toString();

    }

    private String getLocalizedDisplayName() {
        return WebPrismUtil.getLocalizedDisplayName(newItem);
    }

    @Override
    public ItemStatus findObjectStatus() {
        if (parent == null) {
            return status;
        }

        ItemWrapper parentWrapper = parent.getParent();

        PrismObjectWrapper<?> objectWrapper = findObjectWrapper(parentWrapper);
        if (objectWrapper == null) {
            return status;
        }

        return objectWrapper.getStatus();
    }

    @Override
    public <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper() {
        if (this instanceof PrismObjectWrapper) {
            return (OW) this;
        }

        if (parent == null) {
            return null;
        }

        ItemWrapper parentWrapper = parent.getParent();

        return findObjectWrapper(parentWrapper);

    }

    private <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper(ItemWrapper parent) {
        if (parent != null) {
            if (parent instanceof PrismObjectWrapper) {
                return (OW) parent;
            }
            if (parent.getParent() != null) {
                return findObjectWrapper(parent.getParent().getParent());
            }
        }
        return null;

    }

    @Override
    public List<VW> getValues() {
        return values;
    }

    @Override
    public VW getValue() throws SchemaException {
        if (CollectionUtils.isEmpty(getValues())) {
            return null;
        }

        if (isMultiValue()) {
            throw new SchemaException("Attempt to get single value from multi-value property.");
        }

        return getValues().iterator().next();
    }

    @Override
    public boolean checkRequired() {
        return newItem.getDefinition().isMandatory();
    }

    @Override
    public boolean isShowEmpty() {
        return showEmpty;
    }

    @Override
    public void setShowEmpty(boolean isShowEmpty, boolean recursive) {
        this.showEmpty = isShowEmpty;
    }

    @Override
    public boolean isShowInVirtualContainer() {
        return showInVirtualContainer;
    }

    @Override
    public void setShowInVirtualContainer(boolean showInVirtualContainer) {
        this.showInVirtualContainer = showInVirtualContainer;
    }

    @Override
    public void setVisibleOverwrite(UserInterfaceElementVisibilityType visibleOverwrite) {
        this.visibleOverwrite = visibleOverwrite;
    }

    @Override
    public boolean isEmpty() {
        return newItem.isEmpty();
    }

    ItemStatus getItemStatus() {
        return status;
    }

    @NotNull
    @Override
    public ItemName getItemName() {
        return getItemDefinition().getItemName();
    }

    @Override
    public int getMinOccurs() {
        return getItemDefinition().getMinOccurs();
    }

    @Override
    public int getMaxOccurs() {
        return getItemDefinition().getMaxOccurs();
    }

    @Override
    public boolean isOperational() {
        return getItemDefinition().isOperational();
    }

    @Override
    public boolean isInherited() {
        return getItemDefinition().isInherited();
    }

    @Override
    public boolean isDynamic() {
        return getItemDefinition().isDynamic();
    }

    @Override
    public boolean canRead() {
        return getItemDefinition().canRead();
    }

    @Override
    public boolean canModify() {
        return getItemDefinition().canModify();
    }

    @Override
    public boolean canAdd() {
        return getItemDefinition().canAdd();
    }

    @Override
    public QName getSubstitutionHead() {
        return getItemDefinition().getSubstitutionHead();
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return getItemDefinition().isHeterogeneousListItem();
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return getItemDefinition().getValueEnumerationRef();
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        return getItemDefinition().isValidFor(elementQName, clazz, caseInsensitive);
    }

    @NotNull
    @Override
    public I instantiate() throws SchemaException {
        return getItemDefinition().instantiate();
    }

    @NotNull
    @Override
    public I instantiate(QName name) throws SchemaException {
        return getItemDefinition().instantiate();
    }

    @Override
    public <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return getItemDefinition().findItemDefinition(path, clazz);
    }

    @Override
    public @NotNull ItemDelta createEmptyDelta(ItemPath path) {
        return getItemDefinition().createEmptyDelta(path);
    }

    @Override
    public @NotNull ItemDefinition<I> clone() {
        return getItemDefinition().clone();
    }

    @Override
    public ItemDefinition<I> deepClone(@NotNull DeepCloneOperation operation) {
        return getItemDefinition().deepClone(operation);
    }

    @Override
    public void revive(PrismContext prismContext) {
        getItemDefinition().revive(prismContext);
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        //TODO implement for wrappers
        getItemDefinition().debugDumpShortToString(sb);
    }

    @Override
    public ItemDefinitionMutator mutator() {
        return getItemDefinition().mutator();
    }

    @Override
    public @NotNull QName getTypeName() {
        return getItemDefinition().getTypeName();
    }

    @Override
    public boolean isRuntimeSchema() {
        return getItemDefinition().isRuntimeSchema();
    }

    @Override
    @Deprecated
    public boolean isIgnored() {
        return getItemDefinition().isIgnored();
    }

    @Override
    public ItemProcessing getProcessing() {
        return getItemDefinition().getProcessing();
    }

    @Override
    public boolean isAbstract() {
        return getItemDefinition().isAbstract();
    }

    @Override
    public String getPlannedRemoval() {
        return getItemDefinition().getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return getItemDefinition().isElaborate();
    }

    @Override
    public DisplayHint getDisplayHint() {
        return getItemDefinition().getDisplayHint();
    }

    @Override
    public @Nullable List<QName> getNaturalKeyConstituents() {
        return getItemDefinition().getNaturalKeyConstituents();
    }

    @Override
    public @Nullable String getMergerIdentifier() {
        return getItemDefinition().getMergerIdentifier();
    }

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return getItemDefinition().getNaturalKeyInstance();
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return getItemDefinition().getMergerInstance(strategy, originMarker);
    }

    @Override
    public boolean isAlwaysUseForEquals() {
        return getItemDefinition().isAlwaysUseForEquals();
    }

    @Override
    public boolean isEmphasized() {
        return getItemDefinition().isEmphasized();
    }

    @Override
    public Integer getDisplayOrder() {
        if (displayOrder == null) {
            displayOrder = getItemDefinition().getDisplayOrder();
        }
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public String getDocumentation() {
        return getItemDefinition().getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return getItemDefinition().getDocumentationPreview();
    }

    @Override
    public Class<?> getTypeClass() {
        return getItemDefinition().getTypeClass();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return getItemDefinition().getAnnotation(qname);
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return getItemDefinition().getAnnotations();
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return getItemDefinition().getSchemaMigrations();
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return getItemDefinition().getDiagrams();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

//    @Override
//    public boolean isStripe() {
//        return stripe;
//    }
//
//    @Override
//    public void setStripe(boolean stripe) {
//        this.stripe = stripe;
//    }

    public I getOldItem() {
        return oldItem;
    }

    @Override
    public boolean isIndexOnly() {
        return false;   // todo
    }

    @Override
    public UserInterfaceElementVisibilityType getVisibleOverwrite() {
        return visibleOverwrite;
    }

    @Override
    public boolean isVisible(PrismContainerValueWrapper<?> parent, ItemVisibilityHandler visibilityHandler) {

        if (!isVisibleByVisibilityHandler(visibilityHandler)) {
            return false;
        }

        if (!parent.isVirtual() && isShowInVirtualContainer()) {
            return false;
        }

        ItemStatus objectStatus = findObjectStatus();

        switch (objectStatus) {
            case NOT_CHANGED:
                return isVisibleForModify(parent.isShowEmpty());
            case ADDED:
                return isVisibleForAdd(parent.isShowEmpty());
            case DELETED:
                return false;
        }

        return false;
    }

    protected boolean isVisibleByVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
        if (visibilityHandler != null) {
            ItemVisibility visible = visibilityHandler.isVisible(this);
            if (visible != null) {
                switch (visible) {
                    case HIDDEN:
                        return false;
                    default:
                        // automatic, go on ...
                }
            }
        }

        return true;

    }

    private boolean isVisibleForModify(boolean parentShowEmpty) {
        if (parentShowEmpty) {
            return true;
        }

        return isEmphasized() || !isEmpty() || isMandatory();
    }

    private boolean isVisibleForAdd(boolean parentShowEmpty) {
        if (parentShowEmpty) {
            return true;
        }

        return isEmphasized() || !isEmpty() || isMandatory();
    }

    @Override
    public void remove(VW valueWrapper, ModelServiceLocator locator) throws SchemaException {
        removeValue(valueWrapper);
        int count = countUsableValues(values);

        if (count == 0 && !hasEmptyPlaceholder(values)) {
            PrismValue emptyValue = createNewEmptyValue(locator);
            Class<?> type = getTypeClass();
            if (emptyValue instanceof PrismPropertyValue ppv && type != null && AbstractPlainStructured.class.isAssignableFrom(type)) {
                // see MID-9564 and also PrismValueWrapperImpl.addToDelta
                // we need to set real value to something in case of AbstractPlainStructured since placeholder is needed
                // for most panels to work.
                // When computing delta this has to be later taken into account.
                try {
                    Object realValue = type.getConstructor().newInstance();
                    ppv.setValue(realValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            add(emptyValue, locator);
        }
    }

    @Override
    public void removeAll(ModelServiceLocator locator) throws SchemaException {
        for (VW value : new ArrayList<>(values)) {
            removeValue(value);
        }

        if (!hasEmptyPlaceholder(values)) {
            add(createNewEmptyValue(locator), locator);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void removeValue(VW valueWrapper) {
        Item rawItem = getItem(); // using not parameterized version to make compiler happy
        switch (valueWrapper.getStatus()) {
            case ADDED:
            case MODIFIED:
                values.remove(valueWrapper);
                rawItem.remove(valueWrapper.getOldValue());
                rawItem.remove(valueWrapper.getNewValue());
                break;
            case NOT_CHANGED:
                removeNotChangedStatusValue(valueWrapper, rawItem);
                break;
        }
    }

    protected void removeNotChangedStatusValue(VW valueWrapper, Item rawItem) {
        rawItem.remove(valueWrapper.getNewValue());
        valueWrapper.setStatus(ValueStatus.DELETED);
    }

    protected abstract <PV extends PrismValue> PV createNewEmptyValue(ModelServiceLocator locator);

    @Override
    public <PV extends PrismValue> void add(PV newValue, ModelServiceLocator locator) throws SchemaException {
        //noinspection unchecked,rawtypes
        ((Item) getItem()).addIgnoringEquivalents(newValue);
        VW newItemValue = WebPrismUtil.createNewValueWrapper(this, newValue, locator);
        values.add(newItemValue);
    }

    @Override
    public <PV extends PrismValue> void addIgnoringEquivalents(PV newValue, ModelServiceLocator locator) throws SchemaException {
        //noinspection unchecked,rawtypes
        ((Item) getItem()).addIgnoringEquivalents(newValue);
        VW newItemValue = WebPrismUtil.createNewValueWrapper(this, newValue, locator);
        values.add(newItemValue);
    }

    private int countUsableValues(List<VW> values) {
        int count = 0;
        for (VW value : values) {
            if (ValueStatus.DELETED.equals(value.getStatus())) {
                continue;
            }
            if (ValueStatus.ADDED.equals(value.getStatus())) {
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean hasEmptyPlaceholder(List<VW> values) {
        for (VW value : values) {
            if (ValueStatus.ADDED.equals(value.getStatus())) {//&& !value.hasValueChanged()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isMetadata() {
        return isMetadata;
    }

    @Override
    public void setMetadata(boolean metadata) {
        isMetadata = metadata;
    }

    @Override
    public void setShowMetadataDetails(boolean showMetadataDetails) {
        this.showMetadataDetails = showMetadataDetails;
    }

    @Override
    public boolean isShowMetadataDetails() {
        return showMetadataDetails;
    }

    @Override
    public boolean isProcessProvenanceMetadata() {
        return processProvenanceMetadata;
    }

    @Override
    public void setProcessProvenanceMetadata(boolean processProvenanceMetadata) {
        this.processProvenanceMetadata = processProvenanceMetadata;
    }

    @Override
    public Optional<ComplexTypeDefinition> structuredType() {
        return getItemDefinition().structuredType();
    }

    @Override
    public <C extends Containerable> PrismContainerValueWrapper<C> getParentContainerValue(Class<? extends C> parentClass) {
        PrismContainerValueWrapper<?> parent = getParent();
        if (parent == null) {
            return null;
        }
        return parent.getParentContainerValue(parentClass);
    }

    @Override
    public boolean isValidated() {
        return validated;
    }

    @Override
    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    @Override
    public Collection<ExecutedDeltaPostProcessor> getPreconditionDeltas(
            ModelServiceLocator serviceLocator, OperationResult result) throws CommonException {
        Collection<ExecutedDeltaPostProcessor> processors = new ArrayList<>();
        for (VW value : getValues()) {
            Collection<ExecutedDeltaPostProcessor> processor = value.getPreconditionDeltas(serviceLocator, result);
            if (processor == null || processor.isEmpty()) {
                continue;
            }
            processors.addAll(processor);
        }
        return processors;
    }

    @Override
    public String toString() {
        return "\"" + getItemName() + "\"->" + super.toString();
    }
}
