/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

/**
 * (Traditional) data mapping.
 */
public class MappingImpl<V extends PrismValue, D extends ItemDefinition> extends AbstractMappingImpl<V, D, MappingType> {

    MappingImpl(MappingBuilder<V, D> builder) {
        super(builder);
    }

    private MappingImpl(MappingImpl<V, D> prototype) {
        super(prototype);
    }

    ValueMetadataComputer createValueMetadataComputer(OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ValueMetadataProcessingSpec processingSpec = createProcessingSpec(result);
        if (processingSpec.isEmpty()) {
            return null;
        } else {
            return new MappingValueMetadataComputerImpl(processingSpec, this);
        }
    }

    private ValueMetadataProcessingSpec createProcessingSpec(OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ValueMetadataProcessingSpec processingSpec = new ValueMetadataProcessingSpec();
        ModelContext<?> lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        if (lensContext != null) {
            ObjectTemplateType focusTemplate = lensContext.getFocusTemplate();
            if (focusTemplate != null) {
                processingSpec.addFromObjectTemplate(focusTemplate, parser.getOutputPath(),
                        objectResolver, getMappingContextDescription(), task, result);
            }
        }
        processingSpec.addMappings(mappingBean.getMetadataMapping());
        return processingSpec;
    }

    @Override
    public MappingImpl<V, D> clone() {
        return new MappingImpl<>(this);
    }
}
