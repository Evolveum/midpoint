/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemServerValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperImpl<PV extends PrismValue, I extends Item<PV, ID>, ID extends ItemDefinition<I>, VW extends PrismValueWrapper> implements ItemWrapper<PV, I, ID, VW>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(ItemWrapperImpl.class);

    private PrismContainerValueWrapper<?> parent;

    private ItemStatus status = null;

    private String displayName;

    private List<VW> values = new ArrayList<>();

    private I oldItem;
    private I newItem;

    private boolean column;

    private boolean stripe;

    private boolean showEmpty;

    private boolean showInVirtualContainer;


    //consider
    private boolean readOnly;

    public ItemWrapperImpl(@Nullable PrismContainerValueWrapper<?> parent, I item, ItemStatus status) {
        Validate.notNull(item, "Item must not be null.");
        Validate.notNull(status, "Item status must not be null.");

        this.parent = parent;
        this.newItem = item;
        this.oldItem = (I) item.clone();
        this.status = status;

    }

    protected <D extends ItemDelta<PV, ID>, O extends ObjectType> D getItemDelta(Class<O> objectClass, Class<D> deltaClass) throws SchemaException {
        D delta = (D) createEmptyDelta(null);
        for (VW value : values) {
            value.addToDelta(delta);
        }

        if (delta.isEmpty()) {
            return null;
        }
        return delta;
    }

    @Override
    public <D extends ItemDelta<PV, ID>> Collection<D> getDelta() throws SchemaException {
        LOGGER.trace("Start computing delta for {}", newItem);

        if (isOperational()) {
            return null;
        }

        D delta = null;
        if (parent != null && ValueStatus.ADDED == parent.getStatus()) {
            delta = (D) createEmptyDelta(getItemName());
        } else {
            delta = (D) createEmptyDelta(getPath());
        }

        for (VW value : values) {
            value.addToDelta(delta);
        }

        if (delta.isEmpty()) {
            LOGGER.trace("There is no delta for {}", newItem);
            return null;
        }

        LOGGER.trace("Returning delta {}", delta);
        return MiscUtil.createCollection(delta);
    }


    @Override
    public <D extends ItemDelta<PV, ID>> void applyDelta(D delta) throws SchemaException {
        if (delta == null) {
            return;
        }

        LOGGER.trace("Applying {} to {}", delta, newItem);
        delta.applyTo(newItem);
    }

    @Override
    public String getDisplayName() {
        if (displayName == null) {
            displayName = getLocalizedDisplayName();
        }

        return displayName;
    }



    @Override
    public String getHelp() {
        return WebPrismUtil.getHelpText(getItemDefinition());
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
    public boolean isMandatory() {
        return getItemDefinition().isMandatory();
    }

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

    public PrismContainerValueWrapper<?> getParent() {
        return parent;
    }

    @Override
    public boolean isMultiValue() {
        return getItemDefinition().isMultiValue();
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

    ID getItemDefinition() {
        return newItem.getDefinition();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append(toString());
        sb.append("Original definition: ").append(newItem.getDefinition()).append("\n");
        sb.append("Display nam: ").append(displayName).append("\n");
        sb.append("Item status: ").append(status).append("\n");
        sb.append("Read only: ").append(isReadOnly()).append("\n");
        sb.append("New item: \n").append(newItem).append("\n");
        sb.append("Old item: \n").append(oldItem).append("\n");
        sb.append("Values: \n");
        for (VW value : values) {
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append(value.debugDump());
        }
        return sb.toString();

    }

    private String getLocalizedDisplayName() {
        Validate.notNull(newItem, "Item must not be null.");

        String displayName = newItem.getDisplayName();
        if (!StringUtils.isEmpty(displayName)) {
            return localizeName(displayName, displayName);
        }

        QName name = newItem.getElementName();
        if (name != null) {
            displayName = name.getLocalPart();

            PrismContainerValue<?> val = newItem.getParent();
            if (val != null && val.getDefinition() != null
                    && val.getDefinition().isRuntimeSchema()) {
                return localizeName(displayName, displayName);
            }

            if (val != null && val.getTypeName() != null) {
                if (val.getRealClass() != null) {
                    displayName = val.getRealClass().getSimpleName() + "." + displayName;
                } else {
                    displayName = val.getTypeName().getLocalPart() + "." + displayName;
                }
            }
        } else {
            displayName = newItem.getDefinition().getTypeName().getLocalPart();
        }

        return localizeName(displayName, name.getLocalPart());
    }

    private String localizeName(String nameKey, String defaultString) {
        Validate.notNull(nameKey, "Null localization key");
        return ColumnUtils.createStringResource(nameKey, defaultString).getString();
    }


    @Override
    public <O extends ObjectType> ItemStatus findObjectStatus() {
        if (parent == null) {
            return status;
        }

        ItemWrapper parentWrapper = parent.getParent();

        PrismObjectWrapper<O> objectWrapper = findObjectWrapper(parentWrapper);
        if (objectWrapper == null) {
            return status;
        }

        return objectWrapper.getStatus();
    }

    @Override
    public <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper() {
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
            throw new SchemaException("Attempt to get sngle value from multi-value property.");
        }

        return getValues().iterator().next();
    }

    @Override
    public boolean checkRequired(PageBase pageBase) {
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
    public boolean isVisible(PrismContainerValueWrapper parentContainer, ItemVisibilityHandler visibilityHandler) {

        if (!isVisibleByVisibilityHandler(visibilityHandler)) {
            return false;
        }

        if (!parentContainer.isVirtual() && showInVirtualContainer) {
            return false;
        }

        ID def = getItemDefinition();
        switch (findObjectStatus()) {
            case NOT_CHANGED:
                return isVisibleForModify(parentContainer.isShowEmpty(), def);
            case ADDED:
                return isVisibleForAdd(parentContainer.isShowEmpty(), def);
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
                    case VISIBLE:
                        return true;
                    case HIDDEN:
                        return false;
                    default:
                        // automatic, go on ...
                }
            }
        }

        return true;

    }

    private boolean isVisibleForModify(boolean parentShowEmpty, ID def) {
        if (parentShowEmpty) {
            return def.canRead();
        }

        return def.canRead() && (def.isEmphasized() || !isEmpty());
    }

    private boolean isVisibleForAdd(boolean parentShowEmpty, ID def) {
        if (parentShowEmpty) {
            return def.canAdd();
        }

        return def.isEmphasized() && def.canAdd();
    }

    protected boolean isEmpty() {
        if (newItem.isEmpty()) {
            return true;
        }

        return false;
    }

    ItemStatus getItemStatus() {
        return status;
    }


    @Override
    public ItemName getItemName() {
        return getItemDefinition().getItemName();
    }

    @Override
    public String getNamespace() {
        return getItemDefinition().getNamespace();
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
    public boolean isSingleValue() {
        return getItemDefinition().isSingleValue();
    }

    @Override
    public boolean isOptional() {
        return getItemDefinition().isOptional();
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
    public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
        return getItemDefinition().isValidFor(elementQName, clazz);
    }

    @Override
    public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
        return getItemDefinition().isValidFor(elementQName, clazz, caseInsensitive);
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
        getItemDefinition().adoptElementDefinitionFrom(otherDef);
    }

    @Override
    public I instantiate() throws SchemaException {
        return getItemDefinition().instantiate();
    }

    @Override
    public I instantiate(QName name) throws SchemaException {
        return getItemDefinition().instantiate();
    }

    @Override
    public <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
        return getItemDefinition().findItemDefinition(path, clazz);
    }

    @Override
    public ItemDelta createEmptyDelta(ItemPath path) {
        return getItemDefinition().createEmptyDelta(path);
    }

    @Override
    public ItemDefinition<I> clone() {
        return getItemDefinition().clone();
    }

    @Override
    public ItemDefinition<I> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return getItemDefinition().deepClone(ultraDeep, postCloneAction);
    }

    @Override
    public ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath,
            Consumer<ItemDefinition> postCloneAction) {
        return getItemDefinition().deepClone(ctdMap, onThisPath, postCloneAction);
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
    public boolean canBeDefinitionOf(I item) {
        return getItemDefinition().canBeDefinitionOf(item);
    }

    @Override
    public boolean canBeDefinitionOf(PrismValue pvalue) {
        return getItemDefinition().canBeDefinitionOf(pvalue);
    }

    @Override
    public MutableItemDefinition<I> toMutable() {
        return getItemDefinition().toMutable();
    }

    @Override
    public QName getTypeName() {
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
    public boolean isEmphasized() {
        return getItemDefinition().isEmphasized();
    }

    @Override
    public Integer getDisplayOrder() {
        return getItemDefinition().getDisplayOrder();
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
    public PrismContext getPrismContext() {
        return getItemDefinition().getPrismContext();
    }

    @Override
    public Class getTypeClassIfKnown() {
        return getItemDefinition().getTypeClassIfKnown();
    }

    @Override
    public Class getTypeClass() {
        return getItemDefinition().getTypeClass();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return getItemDefinition().getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        getItemDefinition().setAnnotation(qname, value);
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return getItemDefinition().getSchemaMigrations();
    }

    @Override
    public void accept(Visitor visitor) {
        getItemDefinition().accept(visitor);
    }


    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isStripe() {
        return stripe;
    }

    @Override
    public void setStripe(boolean stripe) {
        this.stripe = stripe;
    }

    protected I getOldItem() {
        return oldItem;
    }

    @Override
    public boolean isIndexOnly() {
        return false;   // todo
    }
}
