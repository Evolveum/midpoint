/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.item;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class DummyReferenceImpl extends DummyItem<PrismReferenceValue,PrismReferenceDefinition, PrismReference> implements PrismReference {

    private static final long serialVersionUID = 1L;

    public DummyReferenceImpl(PrismReference realReference, @NotNull ItemPath path) {
        super(realReference, path);
    }

    public Referencable getRealValue() {
        return delegate().getRealValue();
    }

    @NotNull
    public Collection<Referencable> getRealValues() {
        return delegate().getRealValues();
    }

    public boolean merge(PrismReferenceValue value) {
        return delegate().merge(value);
    }

    public String getOid() {
        return delegate().getOid();
    }

    public PolyString getTargetName() {
        return delegate().getTargetName();
    }

    public PrismReferenceValue findValueByOid(String oid) {
        return delegate().findValueByOid(oid);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return delegate().findPartial(path);
    }

    public ReferenceDelta createDelta() {
        return delegate().createDelta();
    }

    public ReferenceDelta createDelta(ItemPath path) {
        return delegate().createDelta(path);
    }

    public PrismReference clone() {
        return delegate().clone();
    }

    @Override
    public PrismReference createImmutableClone() {
        return delegate().createImmutableClone();
    }

    public PrismReference cloneComplex(CloneStrategy strategy) {
        return delegate().cloneComplex(strategy);
    }

    public PrismReferenceValue getValue() {
        return delegate().getValue();
    }

    @Override
    public String getHelp() {
        return delegate().getHelp();
    }

}
