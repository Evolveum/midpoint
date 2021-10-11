/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;

/**
 * @author katka
 */
public class PrismReferenceWrapperImpl<R extends Referencable>
        extends ItemWrapperImpl<PrismReferenceValue, PrismReference, PrismReferenceDefinition, PrismReferenceValueWrapperImpl<R>>
        implements PrismReferenceWrapper<R> {

    private ObjectFilter filter;

    public PrismReferenceWrapperImpl(PrismContainerValueWrapper<?> parent, PrismReference item, ItemStatus status) {
        super(parent, item, status);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public QName getTargetTypeName() {
        return getItemDefinition().getTargetTypeName();
    }

    @Override
    public QName getCompositeObjectElementName() {
        return getItemDefinition().getCompositeObjectElementName();
    }

    @Override
    public boolean isComposite() {
        return getItemDefinition().isComposite();
    }

    @Override
    public PrismReferenceDefinition clone() {
        return getItemDefinition().clone();
    }

    @NotNull
    @Override
    public PrismReference instantiate() {
        return getItemDefinition().instantiate();
    }

    @NotNull
    @Override
    public PrismReference instantiate(QName name) {
        return getItemDefinition().instantiate(name);
    }

    @Override
    public ObjectFilter getFilter() {
        return filter;
    }

    @Override
    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    @Override
    public List<QName> getTargetTypes() {
        return WebComponentUtil.createSupportedTargetTypeList(getTargetTypeName());
    }

    @Override
    protected boolean isEmpty() {
        if (super.isEmpty()) return true;

        List<PrismReferenceValue> pVals = getItem().getValues();
        boolean allEmpty = true;
        for (PrismReferenceValue pVal : pVals) {
            if (!pVal.isEmpty()) {
                allEmpty = false;
                break;
            }
        }

        return allEmpty;
    }

    @Override
    public boolean isImmutable() {
        // TODO
        return false;
    }

    @Override
    public void freeze() {
        // TODO
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        // TODO
        return false;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        // TODO
    }
}
