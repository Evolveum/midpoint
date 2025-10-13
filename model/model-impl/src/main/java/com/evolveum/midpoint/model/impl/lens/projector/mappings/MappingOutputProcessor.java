/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface MappingOutputProcessor<V extends PrismValue> {

    /**
     * @return if true is returned then the default processing will take place
     *         after the processor is finished. If false then the default processing
     *         will be skipped.
     */
    boolean process(ItemPath mappingOutputPath, MappingOutputStruct<V> outputStruct) throws SchemaException, ExpressionEvaluationException;

}
