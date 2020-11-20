/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingTransformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransformationMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import javax.xml.namespace.QName;

/**
 * Mapping that manages transformation metadata.
 */
@Component
public class TransformationBuiltinMapping extends BaseBuiltinMetadataMapping {

    private static final Trace LOGGER = TraceManager.getTrace(TransformationBuiltinMapping.class);

    private static final ItemPath PATH = ItemPath.create(ValueMetadataType.F_TRANSFORMATION);

    @SuppressWarnings("unused")
    TransformationBuiltinMapping() {
        super(PATH);
    }

    /**
     * Transformation merges the acquisitions into single yield.
     */
    @Override
    public void applyForTransformation(@NotNull TransformationalMetadataComputation computation) {
        List<PrismValue> input = computation.getInputValues();

        LOGGER.trace("Computing transformation metadata during value transformation. Input values:\n{}",
                lazy(() -> dumpInput(input)));

        MappingTransformationType mappingTransformation = new MappingTransformationType(prismContext)
                .mappingSpecification(computation.getMappingSpecification());

        List<QName> sourceNames = computation.getSourceNames();
        if (computation.getInputValues().size() < sourceNames.size()) {
            throw new IllegalStateException("Couldn't compute transformational metadata: there are less values ("
                    + computation.getInputValues().size() + ") than sources (" + sourceNames.size() + ") in "
                    + computation.getContextDescription());
        }

        for (int i = 0; i < sourceNames.size(); i++) {
            MappingSourceType source = new MappingSourceType(prismContext);

            QName sourceName = sourceNames.get(i);
            if (sourceName != null) {
                source.setName(sourceName.getLocalPart());
            }

            PrismValue inputValue = computation.getInputValues().get(i);
            if (inputValue != null) {
                PrismValue inputValueClone = inputValue.clone();
                markNotTransient(inputValueClone);
                source.setValue(new RawType(inputValueClone, null, prismContext));
            }

            mappingTransformation.getSource().add(source);
        }

        TransformationMetadataType transformation = new TransformationMetadataType(prismContext)
                .mappingTransformation(mappingTransformation);

        LOGGER.trace("Output: transformation:\n{}", lazy(() -> transformation.asPrismContainerValue().debugDump()));

        computation.getOutputMetadataValueBean().setTransformation(transformation);
    }

    /**
     * The value can contain transient values e.g. because during its computation the transformation metadata was
     * marked as transient. But there can be situations when transformation metadata for the current value are
     * non-transient. Then we could want to store them completely, i.e. with transformation metadata of source values.
     * So we must mark them as not transient.
     */
    private void markNotTransient(PrismValue rootValue) {
        rootValue.accept(visitable -> {
            if (visitable instanceof PrismValue) {
                PrismValue value = (PrismValue) visitable;
                value.setTransient(false);
                value.getValueMetadataAsContainer().valuesStream()
                        .forEach(this::markNotTransient);
            }
        });
    }

    /**
     * During consolidation we simply take (any) value.
     */
    @Override
    public void applyForConsolidation(@NotNull ConsolidationMetadataComputation computation) {
        TransformationMetadataType transformation = selectTransformation(computation);
        if (transformation != null) {
            computation.getOutputMetadataValueBean().setTransformation(transformation.clone());
        }
    }

    private TransformationMetadataType selectTransformation(ConsolidationMetadataComputation computation) {
        for (ValueMetadataType nonNegativeValue : computation.getNonNegativeValues()) {
            if (nonNegativeValue.getTransformation() != null) {
                return nonNegativeValue.getTransformation();
            }
        }
        for (ValueMetadataType existingValue : computation.getExistingValues()) {
            if (existingValue.getTransformation() != null) {
                return existingValue.getTransformation();
            }
        }
        return null;
    }
}
