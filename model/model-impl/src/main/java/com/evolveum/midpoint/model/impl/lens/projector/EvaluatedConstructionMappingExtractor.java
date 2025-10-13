/*
 * Copyright (c) 2014-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedResourceObjectConstructionImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

@FunctionalInterface
public interface EvaluatedConstructionMappingExtractor<V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> {

    Collection<? extends PrismValueDeltaSetTripleProducer<?, ?>> getMappings(EvaluatedResourceObjectConstructionImpl<AH, ?> evaluatedConstruction);

}
