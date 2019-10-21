/*
 * Copyright (c) 2014-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.Construction;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

@FunctionalInterface
public interface MappingExtractor<V extends PrismValue, D extends ItemDefinition, F extends FocusType> {

    Collection<? extends PrismValueDeltaSetTripleProducer<V,D>> getMappings(Construction<F> construction);


}
