/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;

/**
 * @author katka
 *
 */
public interface PrismReferenceWrapper<R extends Referencable> extends ItemWrapper<PrismReferenceValue, PrismReference, PrismReferenceDefinition, PrismReferenceValueWrapperImpl<R>>, PrismReferenceDefinition {

    ObjectFilter getFilter();
    void setFilter(ObjectFilter filter);

    List<QName> getTargetTypes();



}
