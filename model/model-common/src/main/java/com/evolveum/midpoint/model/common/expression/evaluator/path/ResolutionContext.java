/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.path;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.DefinitionResolver;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Context of (further) resolution.
 */
abstract class ResolutionContext {

    /**
     * Converts resolution context into output triple (if there's nothing more to resolve).
     */
    abstract <V extends PrismValue> PrismValueDeltaSetTriple<V> createOutputTriple()
            throws SchemaException;

    /**
     * Is the context of container type i.e. can we step into it?
     */
    abstract boolean isContainer();

    /**
     * Resolve next step of the container-type context.
     */
    abstract ResolutionContext stepInto(ItemName step, DefinitionResolver<?, ?> defResolver) throws SchemaException;

    /**
     * Is the context of "structured property" type i.e. can we try to find the path in it?
     */
    abstract boolean isStructuredProperty();

    /**
     * Resolve the last mile in structured property.
     */
    abstract ResolutionContext resolveStructuredProperty(
            ItemPath pathToResolve, PrismPropertyDefinition<?> outputDefinition, PrismContext prismContext);

    /**
     * Is the context null so there's nothing to resolve?
     */
    abstract boolean isNull();
}
