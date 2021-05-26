package com.evolveum.midpoint.model.impl.schema.transform;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismItemAccessDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.deleg.ItemDefinitionDelegator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.google.common.base.Preconditions;

/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

public abstract class TransformableItemDefinition<I extends Item<?,?>,D extends ItemDefinition<I>> implements ItemDefinitionDelegator<I>, PrismItemAccessDefinition.Mutable {

    private static final long serialVersionUID = 1L;

    private D delegate;

    protected TransformableItemDefinition(D delegate) {
        if (delegate instanceof TransformableItemDefinition) {
            // CopyOf constructor

            @SuppressWarnings("unchecked")
            TransformableItemDefinition<?, D> copyOf = (TransformableItemDefinition<?,D>) delegate;
            this.allowAdd = copyOf.allowAdd;
            this.allowRead = copyOf.allowRead;
            this.allowModify = copyOf.allowModify;
            this.template = copyOf.template;
            this.minOccurs = copyOf.minOccurs;
            this.maxOccurs = copyOf.maxOccurs;
            this.processing = copyOf.processing;
            this.valueEnumerationRef = copyOf.valueEnumerationRef;
            this.delegate = copyOf.delegate();
        } else {
            this.delegate = delegate;
        }

    }

    private boolean allowAdd = true;
    private boolean allowRead = true;
    private boolean allowModify = true;

    private ObjectTemplateItemDefinitionType template;
    private Integer minOccurs;
    private Integer maxOccurs;
    private ItemProcessing processing;
    private PrismReferenceValue valueEnumerationRef;

    @Override
    public D delegate() {
        return delegate;
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


    @Override
    public int getMinOccurs() {
        return preferLocal(minOccurs, delegate().getMinOccurs());
    }

    @Override
    public int getMaxOccurs() {
        return preferLocal(maxOccurs, delegate.getMaxOccurs());
    }

    @Override
    public ItemProcessing getProcessing() {
        return preferLocal(processing, delegate.getProcessing());
    }

    @SuppressWarnings("unchecked")
    protected <ID extends ItemDefinition<?>> ID apply(ID originalItem) {
        if (delegate == null) {
            delegate = (D) originalItem;
        }
        return (ID) publicView();
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

    protected abstract D publicView();

    @Override
    public ItemDefinition<I> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void freeze() {
        // Intentional Noop for now

    }

    @Override
    public boolean isImmutable() {
        // Intentional Noop for now
        return false;
    }

    @Override
    public void revive(PrismContext prismContext) {
        delegate.revive(prismContext);
    }

    public static boolean isMutableAccess(ItemDefinition<?> definition) {
        return definition instanceof TransformableItemDefinition<?, ?>;
    }

    public static TransformableItemDefinition<?, ?> access(ItemDefinition<?> itemDef) {
        Preconditions.checkArgument(itemDef instanceof TransformableItemDefinition<?, ?>, "Definition must be %s", TransformableItemDefinition.class.getName());
        return (TransformableItemDefinition<?, ?>) itemDef;
    }

    public void applyTemplate(ObjectTemplateItemDefinitionType templateItemDefType) {
        this.template = templateItemDefType;
    }

    @Override
    public String getDisplayName() {
        return preferLocal(template.getDisplayName(), delegate().getDisplayName());
    }

    @Override
    public String getHelp() {
        return preferLocal(template.getHelp(), delegate().getHelp());
    }

    @Override
    public Integer getDisplayOrder() {
        return preferLocal(template.getDisplayOrder(), delegate().getDisplayOrder());
    }

    @Override
    public boolean isEmphasized() {
        return preferLocal(template.isEmphasized(), delegate().isEmphasized());
    }

    @Override
    public boolean isDeprecated() {
        return preferLocal(template.isDeprecated(), delegate().isDeprecated());
    }

    @Override
    public boolean isExperimental() {
        return preferLocal(template.isExperimental(), delegate().isExperimental());
    }

    private <T> T preferLocal(T fromTemplate, T fromDelegate) {
        return fromTemplate != null ? fromTemplate : fromDelegate;
    }

    public void setMinOccurs(Integer value) {
        this.minOccurs = value;
    }

    public void setMaxOccurs(Integer value) {
        this.maxOccurs = value;
    }

    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRVal) {
        this.valueEnumerationRef = valueEnumerationRVal;
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return preferLocal(valueEnumerationRef, delegate().getValueEnumerationRef());
    }

    public void setProcessing(ItemProcessing itemProcessing) {
        this.processing = itemProcessing;
    }

    static void apply(ItemDefinition<?> overriden, ItemDefinition<?> originalItem) {
        if (overriden instanceof TransformableItemDefinition) {
            ((TransformableItemDefinition) overriden).apply(originalItem);
        }
    }

    @Override
    public String toString() {
        return "Transformable:" + delegate.toString();
    }

}
