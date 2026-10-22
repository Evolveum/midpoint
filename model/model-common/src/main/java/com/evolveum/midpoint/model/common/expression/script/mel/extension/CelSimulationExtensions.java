/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.script.mel.value.*;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.ListType;
import dev.cel.common.types.NullableType;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.NullabilityProperties;

import java.util.List;

/**
 * Extensions for CEL compiler and runtime providing safe access to processed simulation data.
 *
 * Exposes simulation metrics, processed item deltas, and related assignments through typed CEL values.
 */
public class CelSimulationExtensions extends AbstractMidPointCelExtensions {

    private static final String PREFIX = "simulation";

    private final MidpointFunctions midpointFunctions;

    public CelSimulationExtensions(MidpointFunctions midpointFunctions) {
        this.midpointFunctions = midpointFunctions;
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(
                // simulation.metrics(object, showEventMarks, showExplicitMetrics)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                PREFIX + ".metrics",
                                CelOverloadDecl.newGlobalOverload(
                                        PREFIX + "-metrics",
                                        "Returns metrics related to a processed simulation object.",
                                        ListType.create(SimulationMetricCelValue.CEL_TYPE),
                                        ContainerValueCelValue.CEL_TYPE,
                                        NullableType.create(SimpleType.BOOL),
                                        NullableType.create(SimpleType.BOOL))),
                        CelFunctionBinding.from(
                                PREFIX + "-metrics",
                                List.of(ContainerValueCelValue.class, Object.class, Object.class),
                                this::metrics,
                                NullabilityProperties.NULLABLE)),
                // simulation.itemDeltas(object, pathsToInclude, pathsToExclude, includeOperationalItems)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                PREFIX + ".itemDeltas",
                                CelOverloadDecl.newGlobalOverload(
                                        PREFIX + "-itemDeltas",
                                        "Returns item deltas related to a processed simulation object.",
                                        ListType.create(SimulationItemDeltaCelValue.CEL_TYPE),
                                        ContainerValueCelValue.CEL_TYPE,
                                        NullableType.create(SimpleType.ANY),
                                        NullableType.create(SimpleType.ANY),
                                        NullableType.create(SimpleType.BOOL))),
                        CelFunctionBinding.from(
                                PREFIX + "-itemDeltas",
                                List.of(
                                        ContainerValueCelValue.class,
                                        Object.class,
                                        Object.class,
                                        Object.class),
                                this::itemDeltas,
                                NullabilityProperties.NULLABLE)),
                // itemDelta.relatedAssignment()
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "relatedAssignment",
                                CelOverloadDecl.newMemberOverload(
                                        PREFIX + "-itemDelta-relatedAssignment",
                                        "Returns the assignment related to a processed item delta.",
                                        NullableType.create(ContainerValueCelValue.CEL_TYPE),
                                        SimulationItemDeltaCelValue.CEL_TYPE)),
                        CelFunctionBinding.from(
                                PREFIX + "-itemDelta-relatedAssignment",
                                SimulationItemDeltaCelValue.class,
                                CelSimulationExtensions::relatedAssignment,
                                NullabilityProperties.NULLABLE)),
                // itemDelta.relatedAssignment(value)
                new Function(
                        CelFunctionDecl.newFunctionDeclaration(
                                "relatedAssignment",
                                CelOverloadDecl.newMemberOverload(
                                        PREFIX + "-itemDelta-relatedAssignment-value",
                                        "Returns the assignment related to a processed item delta or value.",
                                        NullableType.create(ContainerValueCelValue.CEL_TYPE),
                                        SimulationItemDeltaCelValue.CEL_TYPE,
                                        NullableType.create(PrismCelValue.CEL_TYPE))),
                        CelFunctionBinding.from(
                                PREFIX + "-itemDelta-relatedAssignment-value",
                                SimulationItemDeltaCelValue.class,
                                PrismCelValue.class,
                                CelSimulationExtensions::relatedAssignment,
                                NullabilityProperties.NULLABLE)));
    }

    private Object metrics(Object[] args) {
        try {
            var object = processedObject(args[0]);
            Boolean showEventMarks = (Boolean) toJava(args[1]);
            Boolean showExplicitMetrics = (Boolean) toJava(args[2]);
            return midpointFunctions.parseSimulationProcessedObject(object)
                    .getMetrics(showEventMarks, showExplicitMetrics)
                    .stream()
                    .map(SimulationMetricCelValue::create)
                    .toList();
        } catch (Exception e) {
            throw createException(e);
        }
    }

    private Object itemDeltas(Object[] args) {
        try {
            var object = processedObject(args[0]);
            Object pathsToInclude = toJava(args[1]);
            Object pathsToExclude = toJava(args[2]);
            Boolean includeOperationalItems = (Boolean) toJava(args[3]);
            return midpointFunctions.parseSimulationProcessedObject(object)
                    .getItemDeltas(pathsToInclude, pathsToExclude, includeOperationalItems)
                    .stream()
                    .map(SimulationItemDeltaCelValue::create)
                    .toList();
        } catch (Exception e) {
            throw createException(e);
        }
    }

    private static SimulationResultProcessedObjectType processedObject(Object value) {
        Object javaValue = toJava(value);
        if (javaValue instanceof SimulationResultProcessedObjectType processedObject) {
            return processedObject;
        }
        if (value instanceof ContainerValueCelValue<?> containerValue
                && containerValue.getContainerValue().asContainerable() instanceof SimulationResultProcessedObjectType processedObject) {
            return processedObject;
        }
        throw createException("Expected a simulation result processed object, got " + javaValue);
    }

    private static ContainerValueCelValue<?> relatedAssignment(SimulationItemDeltaCelValue itemDelta) {
        return wrapAssignment(itemDelta.getItemDelta().getRelatedAssignment());
    }

    private static ContainerValueCelValue<?> relatedAssignment(
            SimulationItemDeltaCelValue itemDelta, PrismCelValue prismCelValue) {
        AssignmentType fromDelta = itemDelta.getItemDelta().getRelatedAssignment();
        if (fromDelta != null) {
            return wrapAssignment(fromDelta);
        }
        if (prismCelValue.getJavaValue() instanceof PrismContainerValue<?> pcv) {
            return ContainerValueCelValue.create(pcv);
        }
        return null;
    }

    private static ContainerValueCelValue<?> wrapAssignment(AssignmentType assignment) {
        return assignment != null
                ? ContainerValueCelValue.create((PrismContainerValue<?>) assignment.asPrismContainerValue())
                : null;
    }

    private record Library(CelSimulationExtensions version0) implements CelExtensionLibrary<CelSimulationExtensions> {

            private Library(MidpointFunctions midpointFunctions) {
                this(new CelSimulationExtensions(midpointFunctions));
            }

            @Override
            public String name() {
                return MidPointConstants.MEL_EXTENSION_SIMULATION_NAME;
            }

            @Override
            public ImmutableSet<CelSimulationExtensions> versions() {
                return ImmutableSet.of(version0);
            }
        }

    public static CelExtensionLibrary<CelSimulationExtensions> library(MidpointFunctions midpointFunctions) {
        return new Library(midpointFunctions);
    }

    @Override
    public int version() {
        return 0;
    }
}
