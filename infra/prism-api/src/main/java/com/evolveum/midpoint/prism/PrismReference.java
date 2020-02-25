/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import java.util.Collection;

import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import org.jetbrains.annotations.NotNull;

/**
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 *
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 *
 * @author semancik
 *
 */
public interface PrismReference extends Item<PrismReferenceValue,PrismReferenceDefinition> {
//    public PrismReference(QName name) {
//        super(name);
//    }
//
//    PrismReference(QName name, PrismReferenceDefinition definition, PrismContext prismContext) {
//        super(name, definition, prismContext);
//    }

    @Override
    Referencable getRealValue();

    @NotNull
    @Override
    Collection<Referencable> getRealValues();

    boolean merge(PrismReferenceValue value);

    String getOid();

    PolyString getTargetName();

    PrismReferenceValue findValueByOid(String oid);

    @Override
    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    @Override
    ReferenceDelta createDelta();

    @Override
    ReferenceDelta createDelta(ItemPath path);

    @Override
    PrismReference clone();

    @Override
    PrismReference createImmutableClone();

    @Override
    PrismReference cloneComplex(CloneStrategy strategy);

    @Override
    String toString();

    @Override
    String debugDump(int indent);

}
