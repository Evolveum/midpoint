/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.util;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Utility methods for extracting mapping information and building
 * UI helpers for mapping simulation results.
 */
public class MappingUtil {

    private static final Trace LOGGER = TraceManager.getTrace(MappingUtil.class);

    public enum MappingStatus {
        ADDED("badge text-bg-success opaque", "Correlation.simulation.state.added"),
        REMOVED("badge text-bg-danger opaque", "Correlation.simulation.state.removed"),
        MODIFIED("badge text-bg-info opaque", "Correlation.simulation.state.modified"),
        NOT_CHANGED("badge text-bg-secondary opaque", "Correlation.simulation.state.notChanged"),
        CHANGE_NOT_APPLIED("badge text-bg-secondary opaque", "Correlation.simulation.state.changeNotApplied"),
        FAILED("badge text-bg-danger opaque", "Correlation.simulation.state.failed");

        private final String cssClass;
        private final String translationKey;

        MappingStatus(String cssClass, String translationKey) {
            this.cssClass = cssClass;
            this.translationKey = translationKey;
        }

        public String cssClass() {
            return cssClass;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public static @NotNull Badge createSituationMappingBadge(
            @NotNull List<ObjectReferenceType> eventMakRefs,
            @NotNull PageBase pageBase) {
        Set<String> eventMarkOids = new HashSet<>();
        eventMakRefs.forEach(ref -> {
            if (ref.getOid() != null) {
                eventMarkOids.add(ref.getOid());
            }
        });

        if (eventMarkOids.contains(MARK_ITEM_VALUE_FAILED.value())) {
            String label = pageBase.getString(MappingStatus.FAILED.translationKey);
            return new Badge(MappingStatus.FAILED.cssClass(), label);
        } else if (eventMarkOids.contains(MARK_ITEM_VALUE_ADDED.value())) {
            String label = pageBase.getString(MappingStatus.ADDED.translationKey);
            return new Badge(MappingStatus.ADDED.cssClass(), label);
        } else if (eventMarkOids.contains(MARK_ITEM_VALUE_REMOVED.value())) {
            String label = pageBase.getString(MappingStatus.REMOVED.translationKey);
            return new Badge(MappingStatus.REMOVED.cssClass(), label);
        } else if (eventMarkOids.contains(MARK_ITEM_VALUE_MODIFIED.value())) {
            String label = pageBase.getString(MappingStatus.MODIFIED.translationKey);
            return new Badge(MappingStatus.MODIFIED.cssClass(), label);
        } else if (eventMarkOids.contains(MARK_ITEM_VALUE_NOT_CHANGED.value())) {
            String label = pageBase.getString(MappingStatus.NOT_CHANGED.translationKey);
            return new Badge(MappingStatus.NOT_CHANGED.cssClass(), label);
        } else if (eventMarkOids.contains(MARK_ITEM_VALUE_CHANGE_NOT_APPLIED.value())) {
            String label = pageBase.getString(MappingStatus.CHANGE_NOT_APPLIED.translationKey);
            return new Badge(MappingStatus.CHANGE_NOT_APPLIED.cssClass(), label);
        } else {
            return new Badge("badge text-bg-secondary opaque", pageBase.getString("Correlation.simulation.state.unknown"));
        }
    }

    public record MappingInfo(
            String mappingName,
            String source,
            String target,
            MappingStrengthType mappingStrength
    ) implements Serializable {
    }

    /**
     * Extract information about mapping from simulation results.
     *
     * This method considers only one (the first) mapping from whole simulation mappings hierarchy.
     * That means the following: `mappings simulation work def -> inline mappings -> take first -> inbound/outbound
     * mappings -> take first`.
     *
     * @return the extracted mapping info or empty Optional.
     */
    public static Optional<MappingInfo> extractMappingSimulationInfo(PageBase page, SimulationResultType result) {
        return findMappingWorkDefinition(page, result)
                .flatMap(workDef -> {
                    final ItemPathType attributeRef = new ItemPathType();
                    if (workDef instanceof InboundMappingsSimulationWorkDefType inboundMappingWorkDef) {
                        return Optional.ofNullable(inboundMappingWorkDef.getInlineMappings())
                                .flatMap(inlineMappings -> inlineMappings.stream().findFirst())
                                .map(inlineMapping -> {
                                    attributeRef.setItemPath(inlineMapping.getRef().getItemPath());
                                    return inlineMapping.getInbound();
                                })
                                .flatMap(inboundMappings -> inboundMappings.stream().findFirst())
                                .map(mapping -> new AttributeMapping(attributeRef, mapping, false));
                    } else if (workDef instanceof OutboundMappingsSimulationWorkDefType outboundMappingWorkDef) {
                        return Optional.ofNullable(outboundMappingWorkDef.getInlineMappings())
                                .flatMap(inlineMappings -> inlineMappings.stream().findFirst())
                                .map(inlineMapping -> {
                                    attributeRef.setItemPath(inlineMapping.getRef().getItemPath());
                                    return inlineMapping.getOutbound();
                                })
                                .flatMap(outboundMappings -> outboundMappings.stream().findFirst())
                                .map(mapping -> new AttributeMapping(attributeRef, mapping, true));
                    }
                    return Optional.empty();
                })
                .map(MappingUtil::extractMappingSimulationInfo);
    }

    private static MappingInfo extractMappingSimulationInfo(AttributeMapping attributeMapping) {
        final MappingType mapping = attributeMapping.mapping();
        final String source;
        final String target;
        if (attributeMapping.outbound()) {
            source = Optional.ofNullable(mapping.getSource())
                    .flatMap(sources -> sources.stream().findFirst())
                    .map(VariableBindingDefinitionType::getPath)
                    .map(ItemPathType::toString)
                    .orElse("");
            target = attributeMapping.attributeRef.toString();
        } else {
            source = attributeMapping.attributeRef.toString();
            target = mapping.getTarget() != null ? mapping.getTarget().getPath().toString() : "";
        }
        return new MappingInfo(mapping.getName(), source, target, mapping.getStrength()
        );
    }

    private record AttributeMapping(ItemPathType attributeRef, MappingType mapping, boolean outbound){}

    private static Optional<AbstractWorkDefinitionType> findMappingWorkDefinition(
            @NotNull PageBase page, @NotNull SimulationResultType result) {

        PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
        if (task == null) {
            LOGGER.warn("Simulation task not found for simulation result {}", result.getOid());
            return Optional.empty();
        }

        // Try inbound mappings simulation first
        PrismContainer<InboundMappingsSimulationWorkDefType> inboundContainer =
                task.findContainer(ItemPath.create(
                        TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_INBOUND_MAPPINGS_SIMULATION
                ));
        if (inboundContainer != null) {
            return Optional.of(inboundContainer.getRealValue());
        }

        // Try outbound mappings simulation
        PrismContainer<OutboundMappingsSimulationWorkDefType> outboundContainer =
                task.findContainer(ItemPath.create(
                        TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_OUTBOUND_MAPPINGS_SIMULATION
                ));
        if (outboundContainer != null) {
            return Optional.of(outboundContainer.getRealValue());
        }

        LOGGER.debug("No mapping work definition found in task {}", task.getOid());
        return Optional.empty();
    }

}
