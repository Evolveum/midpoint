/*
 * Copyright (C) 2021-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.impl.key.NaturalKeyDefinitionImpl;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.ItemDefinitionDelegator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.List;

public abstract class TransformableItemDefinition<I extends Item<?,?>,D extends ItemDefinition<I>>
        extends TransformableDefinition
        implements ItemDefinitionDelegator<I>, PrismItemAccessDefinition.Mutable, PartiallyMutableItemDefinition<I> {

    @Serial private static final long serialVersionUID = 1L;

    private DelegatedItem<D> delegate;

    private boolean allowAdd = true;
    private boolean allowRead = true;
    private boolean allowModify = true;

    private String displayName;

    private String help;
    private Integer displayOrder;
    private DisplayHint displayHint;
    private Boolean emphasized;
    private Boolean deprecated;
    private Boolean experimental;
    private Boolean alwaysUseForEquals;

    private Integer minOccurs;
    private Integer maxOccurs;
    private ItemProcessing processing;
    private PrismReferenceValue valueEnumerationRef;
    private String merger;

    private List<QName> naturalKeyConstituents;

    protected TransformableItemDefinition(D delegate) {
        super(delegate);
        if (delegate instanceof TransformableItemDefinition) {
            // CopyOf constructor

            @SuppressWarnings("unchecked")
            TransformableItemDefinition<?, D> copyOf = (TransformableItemDefinition<?,D>) delegate;
            this.allowAdd = copyOf.allowAdd;
            this.allowRead = copyOf.allowRead;
            this.allowModify = copyOf.allowModify;
            this.minOccurs = copyOf.minOccurs;
            this.maxOccurs = copyOf.maxOccurs;
            this.processing = copyOf.processing;

            this.help = copyOf.help;
            this.displayOrder = copyOf.displayOrder;
            this.displayHint = copyOf.displayHint;
            this.emphasized = copyOf.emphasized;
            this.deprecated = copyOf.deprecated;
            this.experimental = copyOf.experimental;
            this.valueEnumerationRef = copyOf.valueEnumerationRef;
            this.delegate = copyOf.delegate;
            this.alwaysUseForEquals = copyOf.alwaysUseForEquals;
            this.merger = copyOf.merger;
            this.naturalKeyConstituents = copyOf.naturalKeyConstituents;
        } else {
            this.delegate = new DelegatedItem.FullySerializable<>(delegate);
        }

    }


    @SuppressWarnings("unchecked")
    public static <ID extends ItemDefinition<?>> TransformableItemDefinition<?, ID> from(ID originalItem) {
        if (originalItem == null) {
            return null;
        }
        if (originalItem instanceof TransformableItemDefinition<?, ?>) {
            return ((TransformableItemDefinition<?,ID>) originalItem);
        }
        if (originalItem instanceof PrismPropertyDefinition<?>) {
            return (TransformableItemDefinition<?,ID>) TransformablePropertyDefinition.of((PrismPropertyDefinition<?>) originalItem);
        }
        if (originalItem instanceof PrismReferenceDefinition) {
            return (TransformableItemDefinition<?,ID>) TransformableReferenceDefinition.of((PrismReferenceDefinition) originalItem);
        }
        if (originalItem instanceof PrismObjectDefinition<?>) {
            return (TransformableItemDefinition<?,ID>) TransformableObjectDefinition.of((PrismObjectDefinition<?>) originalItem);
        }
        if (originalItem instanceof PrismContainerDefinition<?>) {
            return (TransformableItemDefinition<?,ID>) TransformableContainerDefinition.of((PrismContainerDefinition<?>) originalItem);
        }
        throw new IllegalArgumentException("Unsupported item definition type " + originalItem.getClass());
    }


    public static <ID extends ItemDefinition<?>> ID publicFrom(ID definition) {
        TransformableItemDefinition<?, ID> accessDef = from(definition);
        if (accessDef != null) {
            return accessDef.publicView();
        }
        return null;
    }

    @Override
    public D delegate() {
        return delegate.get();
    }

    @Override
    public boolean canAdd() {
        return allowAdd;
    }

    @Override
    public boolean canModify() {
        return allowModify;
    }

    @Override
    public boolean canRead() {
        return allowRead;
    }

    @Override
    public int getMinOccurs() {
        return preferLocal(minOccurs, delegate().getMinOccurs());
    }


    @Override
    public int getMaxOccurs() {
        return preferLocal(maxOccurs, delegate().getMaxOccurs());
    }


    @Override
    public ItemProcessing getProcessing() {
        return preferLocal(processing, delegate().getProcessing());
    }

    @Override
    public boolean isAlwaysUseForEquals() {
        return preferLocal(alwaysUseForEquals, delegate().isAlwaysUseForEquals());
    }

    @Override
    public void setAlwaysUseForEquals(boolean alwaysUseForEquals) {
        this.alwaysUseForEquals = alwaysUseForEquals;
    }

    @Override
    public void setCanAdd(boolean val) {
        allowAdd = val;
    }

    @Override
    public void setCanModify(boolean val) {
        allowModify = val;
    }

    @Override
    public void setCanRead(boolean val) {
        allowRead = val;
    }

    protected abstract D publicView();

    @Override
    public @NotNull ItemDefinition<I> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ItemDefinition<I> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void freeze() {
        // Intentional Noop for now

    }

    @Nullable
    @Override
    public String getMergerIdentifier() {
        return merger;
    }

    @Override
    public void setMergerIdentifier(String mergerIdentifier) {
        this.merger = mergerIdentifier;
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return PrismContext.get().itemMergerFactory().createMerger(this, strategy, originMarker);
    }

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        // todo how to create proper NaturalKey instance, implementations could be outside of prism api/impl
        return naturalKeyConstituents != null && !naturalKeyConstituents.isEmpty() ? NaturalKeyDefinitionImpl.of(naturalKeyConstituents) : null;
    }

    @Nullable
    @Override
    public List<QName> getNaturalKeyConstituents() {
        return naturalKeyConstituents;
    }

    @Override
    public void setNaturalKeyConstituents(List<QName> naturalKeyConstituents) {
        this.naturalKeyConstituents = naturalKeyConstituents;
    }

    @Override
    public boolean isImmutable() {
        // Intentional Noop for now
        return false;
    }

    @Override
    public void revive(PrismContext prismContext) {
        delegate().revive(prismContext);
    }

    public static TransformableItemDefinition<?, ?> access(ItemDefinition<?> itemDef) {
        Preconditions.checkArgument(itemDef instanceof TransformableItemDefinition<?, ?>, "Definition must be %s", TransformableItemDefinition.class.getName());
        return (TransformableItemDefinition<?, ?>) itemDef;
    }

    public void applyTemplate(ObjectTemplateItemDefinitionType apply) {
        if (apply.getDisplayName() != null) {
            this.setDisplayName(apply.getDisplayName());
        }
        if (apply.getHelp() != null) {
            this.setHelp(apply.getHelp());
        }
        if (apply.getDisplayHint() != null) {
            this.setDisplayHint(MiscSchemaUtil.toDisplayHint(apply.getDisplayHint()));
        }
        if (apply.getDisplayOrder() != null) {
            this.setDisplayOrder(apply.getDisplayOrder());
        }
        if (apply.isEmphasized() != null) {
            this.setEmphasized(apply.isEmphasized());
        }
        if (apply.isDeprecated() != null) {
            this.setDeprecated(apply.isDeprecated());
        }
        if (apply.isExperimental() != null) {
            this.setExperimental(apply.isExperimental());
        }
    }

    @Override
    public String getDisplayName() {
        return preferLocal(this.displayName, delegate().getDisplayName());
    }

    @Override
    public String getHelp() {
        return preferLocal(this.help, delegate().getHelp());
    }

    @Override
    public Integer getDisplayOrder() {
        return preferLocal(this.displayOrder, delegate().getDisplayOrder());
    }

    @Override
    public DisplayHint getDisplayHint() {
        return preferLocal(this.displayHint, delegate().getDisplayHint());
    }

    @Override
    public boolean isEmphasized() {
        return preferLocal(this.emphasized, delegate().isEmphasized()) || getDisplayHint() == DisplayHint.EMPHASIZED;
    }

    @Override
    public boolean isDeprecated() {
        return preferLocal(this.deprecated, delegate().isDeprecated());
    }

    @Override
    public boolean isExperimental() {
        return preferLocal(this.experimental, delegate().isExperimental());
    }

    private <T> T preferLocal(T fromTemplate, T fromDelegate) {
        return fromTemplate != null ? fromTemplate : fromDelegate;
    }

    @Override
    public void setMinOccurs(int value) {
        this.minOccurs = value;
    }

    @Override
    public void setMaxOccurs(int value) {
        this.maxOccurs = value;
    }

    @Override
    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRVal) {
        this.valueEnumerationRef = valueEnumerationRVal;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    @Override
    public void setHelp(String help) {
        this.help = help;
    }


    @Override
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public void setDisplayHint(DisplayHint displayHint) {
        this.displayHint = displayHint;
    }

    @Override
    public void setEmphasized(boolean emphasized) {
        this.emphasized = emphasized;
    }


    @Override
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }


    @Override
    public void setExperimental(boolean experimental) {
        this.experimental = experimental;
    }


    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return preferLocal(valueEnumerationRef, delegate().getValueEnumerationRef());
    }

    @Override
    public void setProcessing(ItemProcessing itemProcessing) {
        this.processing = itemProcessing;
    }

    static void apply(ItemDefinition<?> overridden, ItemDefinition<?> originalItem) {
        //if (overridden instanceof TransformableItemDefinition) {
        //    ((TransformableItemDefinition) overridden).apply(originalItem);
        //}
    }

    @Override
    public ItemDefinitionMutator mutator() {
        return this;
    }

    @Override
    public ItemDefinition<I> deepClone(@NotNull DeepCloneOperation operation) {
        return copy();
    }

    protected abstract TransformableItemDefinition<I,D> copy();

    // FIXME we should display overridden values here (e.g. RAM access flags)
    @Override
    public String toString() {
        return "Transformable:" + delegate().toString();
    }

    @Override
    public @NotNull I instantiate(QName name) throws SchemaException {
        var deleg = delegate().instantiate(name);
        //noinspection unchecked
        ((Item<?,ItemDefinition<?>>)deleg).setDefinition(this);
        return deleg;
    }


    ItemDefinition<?> attachTo(TransformableComplexTypeDefinition complexType) {
        var parentDelegator = complexType.delegate;
        if (parentDelegator instanceof DelegatedItem.StaticComplexType) {
            var delegateDef = parentDelegator.get().findItemDefinition(getItemName());
            // If definition is same object as definition from schema - reuse it
            if (delegateDef == delegate()) {
                    delegate = new DelegatedItem.ComplexTypeDerived<>(complexType.getTypeName(), delegate());
            }
        }
        return this;
    }

    protected void delegatedItem(DelegatedItem<D> deleg) {
        this.delegate = deleg;
    }
}
