/*
 * Copyright (c) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

@FunctionalInterface
public interface EvaluatedConstructionMappingExtractor<V extends PrismValue, D extends ItemDefinition, AH extends AssignmentHolderType> {

    Collection<? extends PrismValueDeltaSetTripleProducer<V,D>> getMappings(EvaluatedConstructionImpl<AH> evaluatedConstruction);


}
