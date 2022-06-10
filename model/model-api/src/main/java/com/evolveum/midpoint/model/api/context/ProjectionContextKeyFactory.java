/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

public interface ProjectionContextKeyFactory {

    /**
     * Determines {@link ProjectionContextKey} for a given shadow as precisely as possible.
     *
     * This process is very important because precise projection keys are needed for the correct functioning of the projector,
     * namely for:
     *
     * 1. assigning resource object constructions to correct projections,
     * 2. resolving dependencies between projections,
     * 3. ensuring projection context uniqueness (w.r.t. resource/kind/intent/tag/order/gone).
     *
     * The troublesome piece is the `kind`/`intent` pair. It requires the shadow to be classified to determine it precisely.
     *
     * Therefore, this method tries to classify the shadow if it's not classified. If it does not succeed, it applies some
     * guesswork called "emergency classification":
     *
     * 1. obtains the most appropriate definition for shadow's object class (see
     * {@link ResourceSchema#findDefinitionForObjectClass(QName)}),
     * 2. as the last hope it checks {@link ResourceObjectClassDefinition#isDefaultAccountDefinition()} flag.
     */
    ProjectionContextKey createKey(@NotNull ShadowType shadow, @NotNull Task task, @NotNull OperationResult result);
}
