/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
