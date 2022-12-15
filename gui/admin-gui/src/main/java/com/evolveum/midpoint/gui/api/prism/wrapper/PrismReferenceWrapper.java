/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

/**
 * @author katka
 */
public interface PrismReferenceWrapper<R extends Referencable> extends ItemWrapper<PrismReference, PrismReferenceValueWrapperImpl<R>>, PrismReferenceDefinition {

    ObjectFilter getFilter();
    void setFilter(ObjectFilter filter);

    List<QName> getTargetTypes();

//    Set<SerializableSupplier<FilterableSearchItemWrapper>> getSpecialSearchItemFunctions();
//    void setSpecialSearchItemFunctions(Set<SerializableSupplier<FilterableSearchItemWrapper>> specialItems);

    Set<SearchItemType> getPredefinedSearchItem();
    void setPredefinedSearchItem(Set<SearchItemType> searchItems);
    boolean isOnlyForDeltaComputation();
}
