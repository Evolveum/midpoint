/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.ArrayType;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Consumer;

public class ValueMetadataWrapperImpl implements PrismContainerWrapper<Containerable> {

    private PrismContainerWrapper<Containerable> metadataValueWrapper;

    public ValueMetadataWrapperImpl(PrismContainerWrapper<Containerable> metadataValueWrapper) {
        this.metadataValueWrapper = metadataValueWrapper;
    }

    @Override
    public @NotNull QName getTypeName() {
        return metadataValueWrapper.getTypeName();
    }

    @Override
    public boolean isRuntimeSchema() {
        return metadataValueWrapper.isRuntimeSchema();
    }

    @Override
    public boolean isIgnored() {
        return metadataValueWrapper.isIgnored();
    }

    @Override
    public ItemProcessing getProcessing() {
        return metadataValueWrapper.getProcessing();
    }

    @Override
    public boolean isAbstract() {
        return metadataValueWrapper.isAbstract();
    }

    @Override
    public boolean isDeprecated() {
        return metadataValueWrapper.isDeprecated();
    }

    @Override
    public boolean isExperimental() {
        return metadataValueWrapper.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return metadataValueWrapper.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return metadataValueWrapper.isElaborate();
    }

    @Override
    public String getDeprecatedSince() {
        return metadataValueWrapper.getDeprecatedSince();
    }

    @Override
    public boolean isEmphasized() {
        return metadataValueWrapper.isEmphasized();
    }

    @Override
    public String getDisplayName() {
        return metadataValueWrapper.getDisplayName();
//        if (getDefinition() == null) {
//            return "MetadataMock";
//        }
//        return getDefinition().getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return metadataValueWrapper.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return metadataValueWrapper.getHelp();
    }

    @Override
    public String getDocumentation() {
        return metadataValueWrapper.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return metadataValueWrapper.getDocumentationPreview();
    }

    @Override
    public Class getTypeClassIfKnown() {
        return metadataValueWrapper.getTypeClassIfKnown();
    }

    @Override
    public boolean isExpanded() {
        return metadataValueWrapper.isExpanded();
    }

    @Override
    public ItemStatus getStatus() {
        return metadataValueWrapper.getStatus();
    }

    @Override
    public Class<Containerable> getCompileTimeClass() {
        return metadataValueWrapper.getCompileTimeClass();
    }

    @Override
    public ComplexTypeDefinition getComplexTypeDefinition() {
        return metadataValueWrapper.getComplexTypeDefinition();
    }

    @Override
    public String getDefaultNamespace() {
        return metadataValueWrapper.getDefaultNamespace();
    }

    @Override
    public List<String> getIgnoredNamespaces() {
        return metadataValueWrapper.getIgnoredNamespaces();
    }

    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        return metadataValueWrapper.getDefinitions();
    }

    @Override
    public boolean isCompletelyDefined() {
        return metadataValueWrapper.isCompletelyDefined();
    }

    @Override
    public List<PrismPropertyDefinition> getPropertyDefinitions() {
        return metadataValueWrapper.getPropertyDefinitions();
    }

    @Override
    public @NotNull ItemName getItemName() {
        return metadataValueWrapper.getItemName();
    }

    @Override
    public String getNamespace() {
        return metadataValueWrapper.getNamespace();
    }

    @Override
    public int getMinOccurs() {
        return metadataValueWrapper.getMinOccurs();
    }

    @Override
    public int getMaxOccurs() {
        return metadataValueWrapper.getMaxOccurs();
    }

    @Override
    public boolean isMandatory() {
        return metadataValueWrapper.isMandatory();
    }

    @Override
    public boolean isOptional() {
        return metadataValueWrapper.isOptional();
    }

    @Override
    public boolean isOperational() {
        return metadataValueWrapper.isOperational();
    }

    @Override
    public boolean isIndexOnly() {
        return metadataValueWrapper.isIndexOnly();
    }

    @Override
    public boolean isInherited() {
        return metadataValueWrapper.isInherited();
    }

    @Override
    public boolean isDynamic() {
        return metadataValueWrapper.isDynamic();
    }

    @Override
    public boolean canRead() {
        return metadataValueWrapper.canRead();
    }

    @Override
    public boolean canModify() {
        return metadataValueWrapper.canModify();
    }

    @Override
    public boolean canAdd() {
        return metadataValueWrapper.canAdd();
    }

    @Override
    public QName getSubstitutionHead() {
        return metadataValueWrapper.getSubstitutionHead();
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return metadataValueWrapper.isHeterogeneousListItem();
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return metadataValueWrapper.getValueEnumerationRef();
    }

    @Override
    public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
        return metadataValueWrapper.isValidFor(elementQName, clazz);
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
        return metadataValueWrapper.isValidFor(elementQName, clazz, caseInsensitive);
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
        metadataValueWrapper.adoptElementDefinitionFrom(otherDef);
    }

    @NotNull
    @Override
    public PrismContainer<Containerable> instantiate() throws SchemaException {
        return metadataValueWrapper.instantiate();
    }

    @NotNull
    @Override
    public PrismContainer<Containerable> instantiate(QName name) throws SchemaException {
        return metadataValueWrapper.instantiate(name);
    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive) {
        return metadataValueWrapper.findLocalItemDefinition(name, clazz, caseInsensitive);
    }

    @Override
    public <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return metadataValueWrapper.findItemDefinition(path, clazz);
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest, @NotNull Class<ID> clazz) {
        return metadataValueWrapper.findNamedItemDefinition(firstName, rest, clazz);
    }

    @Override
    public ContainerDelta<Containerable> createEmptyDelta(ItemPath path) {
        return metadataValueWrapper.createEmptyDelta(path);
    }

    @Override
    public @NotNull PrismContainerDefinition<Containerable> clone() {
        return metadataValueWrapper.clone();
    }

    @Override
    public ItemDefinition<PrismContainer<Containerable>> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return metadataValueWrapper.deepClone(ultraDeep, postCloneAction);
    }

    @Override
    public ItemDefinition<PrismContainer<Containerable>> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return metadataValueWrapper.deepClone(ctdMap, onThisPath, postCloneAction);
    }

    @Override
    public void revive(PrismContext prismContext) {
        metadataValueWrapper.revive(prismContext);
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        metadataValueWrapper.debugDumpShortToString(sb);
    }

    @Override
    public boolean canBeDefinitionOf(PrismContainer<Containerable> item) {
        return metadataValueWrapper.canBeDefinitionOf(item);
    }

    @Override
    public boolean canBeDefinitionOf(PrismValue pvalue) {
        return metadataValueWrapper.canBeDefinitionOf(pvalue);
    }

    @Override
    public PrismContainerDefinition<Containerable> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        return metadataValueWrapper.cloneWithReplacedDefinition(itemName, newDefinition);
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
        metadataValueWrapper.replaceDefinition(itemName, newDefinition);
    }

    @Override
    public PrismContainerValue<Containerable> createValue() {
        return metadataValueWrapper.createValue();
    }

    @Override
    public boolean isEmpty() {
        return metadataValueWrapper.isEmpty();
    }

    @Override
    public boolean canRepresent(@NotNull QName type) {
        return metadataValueWrapper.canRepresent(type);
    }

    @Override
    public MutablePrismContainerDefinition<Containerable> toMutable() {
        return metadataValueWrapper.toMutable();
    }

    @Override
    public Class<Containerable> getTypeClass() {
        return metadataValueWrapper.getTypeClass();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return metadataValueWrapper.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        metadataValueWrapper.setAnnotation(qname, value);
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return metadataValueWrapper.getSchemaMigrations();
    }

    @Override
    public void remove(PrismContainerValueWrapper<Containerable> valueWrapper, ModelServiceLocator locator) throws SchemaException {
        throw new UnsupportedOperationException("Remove value not supported");
    }

    @Override
    public void removeAll(ModelServiceLocator locator) throws SchemaException {
        throw new UnsupportedOperationException("Remove all not supported");
    }

    @Override
    public <PV extends PrismValue> void add(PV newValueWrapper, ModelServiceLocator locator) throws SchemaException {
        throw new UnsupportedOperationException("Add value not supported");
    }

    @Override
    public void setVirtual(boolean virtual) {
        metadataValueWrapper.setVirtual(virtual);
    }

    @Override
    public void setExpanded(boolean expanded) {
        metadataValueWrapper.setExpanded(expanded);
    }

    @Override
    public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
        return metadataValueWrapper.findContainer(path);
    }

    @Override
    public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException {
        return metadataValueWrapper.findProperty(propertyPath);
    }

    @Override
    public <R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException {
        return metadataValueWrapper.findReference(path);
    }

    @Override
    public <T extends Containerable> PrismContainerValueWrapper<T> findContainerValue(ItemPath path) throws SchemaException {
        return metadataValueWrapper.findContainerValue(path);
    }

    @Override
    public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
        return metadataValueWrapper.findItem(path, type);
    }

    @Override
    public ItemPath getPath() {
        return metadataValueWrapper.getPath();
    }

    @Override
    public boolean isReadOnly() {
        return metadataValueWrapper.isReadOnly();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        metadataValueWrapper.setReadOnly(readOnly);
    }

    @Override
    public ExpressionType getFormComponentValidator() {
        return metadataValueWrapper.getFormComponentValidator();
    }

    @Override
    public List<PrismContainerValueWrapper<Containerable>> getValues() {
        return metadataValueWrapper.getValues();
    }

    @Override
    public PrismContainerValueWrapper<Containerable> getValue() throws SchemaException {
        return metadataValueWrapper.getValue();
    }

    @Override
    public boolean isStripe() {
        return metadataValueWrapper.isStripe();
    }

    @Override
    public void setStripe(boolean stripe) {
        metadataValueWrapper.setShowMetadataDetails(stripe);
    }

    @Override
    public PrismContainer<Containerable> getItem() {
        return metadataValueWrapper.getItem();
    }

    @Override
    public boolean isColumn() {
        return metadataValueWrapper.isColumn();
    }

    @Override
    public void setColumn(boolean column) {
        metadataValueWrapper.setColumn(column);
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {
        return metadataValueWrapper.getDelta();
    }

    @Override
    public ItemStatus findObjectStatus() {
        return metadataValueWrapper.findObjectStatus();
    }

    @Override
    public <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper() {
        throw new UnsupportedOperationException("Find obejct wrapper not supported");
    }

    @Override
    public boolean isShowEmpty() {
        return metadataValueWrapper.isShowEmpty();
    }

    @Override
    public void setShowEmpty(boolean isShowEmpty, boolean recursive) {
        metadataValueWrapper.setShowEmpty(isShowEmpty, recursive);
    }

    @Override
    public boolean isShowInVirtualContainer() {
        return metadataValueWrapper.isShowInVirtualContainer();
    }

    @Override
    public void setShowInVirtualContainer(boolean showInVirtualContainer) {
        metadataValueWrapper.setShowInVirtualContainer(showInVirtualContainer);
    }

    @Override
    public boolean isVirtual() {
        return metadataValueWrapper.isVirtual();
    }

    @Override
    public boolean isMetadata() {
        return metadataValueWrapper.isMetadata();
    }

    @Override
    public void setMetadata(boolean metadata) {
        metadataValueWrapper.setMetadata(metadata);
    }

    @Override
    public void setShowMetadataDetails(boolean showMetadataDetails) {
        metadataValueWrapper.setShowMetadataDetails(showMetadataDetails);
    }

    @Override
    public boolean isShowMetadataDetails() {
        return metadataValueWrapper.isShowMetadataDetails();
    }

    @Override
    public String debugDump(int indent) {
        return metadataValueWrapper.debugDump(indent);
    }

    @Override
    public void setVisibleOverwrite(UserInterfaceElementVisibilityType visible) {
        metadataValueWrapper.setVisibleOverwrite(visible);
    }

    @Override
    public UserInterfaceElementVisibilityType getVisibleOverwrite() {
        return metadataValueWrapper.getVisibleOverwrite();
    }

    @Override
    public boolean isVisible(PrismContainerValueWrapper<?> parentContainer, ItemVisibilityHandler visibilityHandler) {
        return metadataValueWrapper.isVisible(parentContainer, visibilityHandler);
    }

    @Override
    public boolean checkRequired(PageBase pageBase) {
        return metadataValueWrapper.checkRequired(pageBase);
    }

    @Override
    public PrismContainerValueWrapper<?> getParent() {
        return metadataValueWrapper.getParent();
    }

    @Override
    public boolean isImmutable() {
        return metadataValueWrapper.isImmutable();
    }

    @Override
    public void freeze() {
        metadataValueWrapper.freeze();
    }

    @Override
    public PrismContext getPrismContext() {
        return metadataValueWrapper.getPrismContext();
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        return metadataValueWrapper.accept(visitor, visitation);
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        metadataValueWrapper.accept(visitor);
    }

    public <T extends Containerable> List<PrismContainerDefinition<Containerable>> getChildContainers() throws SchemaException {
        List<PrismContainerValueWrapper<Containerable>> containerValues = getValues();
        if (CollectionUtils.isEmpty(containerValues)) {
            return Collections.EMPTY_LIST;
        }

        List<PrismContainerDefinition<Containerable>> childContainers = new ArrayList<>();
        for (PrismContainerValueWrapper<Containerable> metadataValue : getValues()) {
            for (PrismContainerWrapper<Containerable> child : metadataValue.getContainers()) {
                if (child.isEmpty()) {
                    continue;
                }
                if (!containainChild(childContainers, child)) {
                    childContainers.add(child);
                }
            }
        }
//        PrismContainerValueWrapper<Containerable> containerValue = containerValues.iterator().next();
//        for (PrismContainerDefinition<Containerable> containerDef : containerValue.getContainers()) {
//            //do not allow to add already existing singel value container
//            childContainers.add(containerDef);
//        }
        return childContainers;
    }

    private boolean containainChild(List<PrismContainerDefinition<Containerable>> containers, PrismContainerWrapper<Containerable> child) {
        return containers.stream().anyMatch(ch -> QNameUtil.match(ch.getTypeName(), child.getTypeName()));
    }
}
