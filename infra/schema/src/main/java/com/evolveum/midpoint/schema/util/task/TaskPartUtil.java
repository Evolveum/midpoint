/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPartParametersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericParametersContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartParametersSpecificationType;

public class TaskPartUtil {

    @NotNull
    public static List<PartParameters> getPartParametersCollection(TaskPartParametersSpecificationType parameters) throws SchemaException {
        List<PartParameters> values = new ArrayList<>();
        if (parameters == null) {
            return values;
        }
        addTypedParameters(values, parameters.getReconciliation());
        addTypedParameters(values, parameters.getLiveSynchronization());
        addTypedParameters(values, parameters.getSynchronization());
        addUntypedParameters(values, parameters.getGeneric());
        return values;
    }

    private static void addUntypedParameters(List<PartParameters> values, GenericParametersContainerType genericContainer) {
        if (genericContainer == null) {
            return;
        }
        SchemaRegistry schemaRegistry = PrismContext.get().getSchemaRegistry();
        PrismContainerValue<?> pcv = genericContainer.asPrismContainerValue();
        for (Item<?, ?> item : pcv.getItems()) {
            ItemDefinition<?> definition = item.getDefinition();
            if (definition != null &&
                    schemaRegistry.isAssignableFromGeneral(AbstractPartParametersType.COMPLEX_TYPE, definition.getTypeName())) {
                for (PrismValue value : item.getValues()) {
                    if (value instanceof PrismContainerValue<?>) {
                        values.add(new UntypedPartParameters((PrismContainerValue<?>) value));
                    }
                }
            }
        }
    }

    private static void addTypedParameters(List<PartParameters> values, AbstractPartParametersType realValue) {
        if (realValue != null) {
            values.add(new TypedPartParameters(realValue));
        }
    }

    public static Collection<QName> getPartParametersTypesFound(TaskPartParametersSpecificationType parameters)
            throws SchemaException {
        List<PartParameters> partParametersCollection = getPartParametersCollection(parameters);
        return partParametersCollection.stream()
                .map(PartParameters::getTypeName)
                .collect(Collectors.toSet());
    }

    public abstract static class PartParameters {
        @NotNull abstract QName getTypeName();
    }

    public static class TypedPartParameters extends PartParameters {
        @NotNull private final AbstractPartParametersType typedParameters;

        TypedPartParameters(@NotNull AbstractPartParametersType typedParameters) {
            this.typedParameters = typedParameters;
        }

        public @NotNull AbstractPartParametersType getTypedParameters() {
            return typedParameters;
        }

        @Override
        @NotNull QName getTypeName() {
            return PrismContext.get().getSchemaRegistry()
                    .findComplexTypeDefinitionByCompileTimeClass(typedParameters.getClass())
                    .getTypeName();
        }

        @Override
        public String toString() {
            return typedParameters.toString();
        }
    }

    public static class UntypedPartParameters extends PartParameters {
        @NotNull private final PrismContainerValue<?> untypedParameters;

        UntypedPartParameters(@NotNull PrismContainerValue<?> untypedParameters) {
            this.untypedParameters = untypedParameters;
        }

        public @NotNull PrismContainerValue<?> getUntypedParameters() {
            return untypedParameters;
        }

        @Override
        @NotNull QName getTypeName() {
            return untypedParameters.getDefinition().getTypeName();
        }

        @Override
        public String toString() {
            return untypedParameters.toString();
        }
    }
}
