/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.util;

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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_ITEM_VALUE_CHANGE_NOT_APPLIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_ITEM_VALUE_MODIFIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_ITEM_VALUE_NOT_CHANGED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.MARK_ITEM_VALUE_REMOVED;

/**
 * Utility methods for extracting mapping information and building
 * UI helpers for mapping simulation results.
 */
public class MappingUtil {

    private static final Trace LOGGER = TraceManager.getTrace(MappingUtil.class);

    public enum MappingStatus {
        ADDED("info-badge success-light py-1", "Correlation.simulation.state.added"),
        REMOVED("info-badge danger-light py-1", "Correlation.simulation.state.removed"),
        MODIFIED("info-badge info-light py-1", "Correlation.simulation.state.modified"),
        NOT_CHANGED("info-badge secondary-light py-1", "Correlation.simulation.state.notChanged"),
        CHANGE_NOT_APPLIED("info-badge secondary-light py-1", "Correlation.simulation.state.changeNotApplied");

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

        if (eventMarkOids.contains(MARK_ITEM_VALUE_ADDED.value())) {
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
            return new Badge("info-badge secondary-light py-1", pageBase.getString("Correlation.simulation.state.unknown"));
        }
    }

    private static @Nullable MappingWorkDefinitionType findMappingWorkDefinition(
            @NotNull PageBase page, @NotNull SimulationResultType result) {

        PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
        if (task == null) {
            LOGGER.warn("Simulation task not found for simulation result {}", result.getOid());
            return null;
        }

        MappingWorkDefinitionType mappingWorkDefinition = findMappingWorkDefinition(task);
        if (mappingWorkDefinition == null) {
            LOGGER.debug("No mapping work definition found in task {}", task.getOid());
            return null;
        }
        return mappingWorkDefinition;
    }

    private static @Nullable MappingWorkDefinitionType findMappingWorkDefinition(
            @NotNull PrismObject<TaskType> task) {

        PrismContainer<MappingWorkDefinitionType> container =
                task.findContainer(ItemPath.create(
                        TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_MAPPINGS
                ));
        return container != null ? container.getRealValue() : null;
    }

    public record MappingInfo(
            String mappingName,
            String source,
            String target,
            MappingStrengthType mappingStrength
    ) implements Serializable {
    }

    //Support only one mapping
    private static @Nullable InlineMappingDefinitionType findInlineMappingDefinition(@NotNull MappingWorkDefinitionType mappingWorkDefinition) {
        List<InlineMappingDefinitionType> inlineMappings = mappingWorkDefinition.getInlineMappings();
        if (inlineMappings == null || inlineMappings.isEmpty()) {
            LOGGER.debug("No inline mapping definitions found in mapping work definition");
            return null;
        }

        InlineMappingDefinitionType inlineMappingDefinitionType = inlineMappings.get(0);
        if (inlineMappingDefinitionType == null) {
            LOGGER.debug("No inline mapping definition found in mapping work definition");
            return null;
        }
        return inlineMappingDefinitionType;
    }

    private static @Nullable MappingInfo extractMappingInfo(@NotNull InlineMappingDefinitionType inlineMappingDefinition) {
        ItemPathType ref = inlineMappingDefinition.getRef();

        List<InboundMappingType> inbound = inlineMappingDefinition.getInbound();
        if (inbound != null && !inbound.isEmpty()) {
            InboundMappingType inboundMappingType = inbound.get(0);
            return new MappingInfo(
                    inboundMappingType.getName(),
                    ref != null ? ref.getItemPath().toString() : null,
                    inboundMappingType.getTarget() != null ? inboundMappingType.getTarget().getPath().toString() : null,
                    inboundMappingType.getStrength()
            );
        }

        List<OutboundMappingType> outbound = inlineMappingDefinition.getOutbound();
        if (outbound != null && !outbound.isEmpty()) {
            OutboundMappingType outboundMappingType = outbound.get(0);
            return new MappingInfo(
                    outboundMappingType.getName(),
                    ref != null ? ref.getItemPath().toString() : null,
                    outboundMappingType.getTarget() != null ? outboundMappingType.getTarget().getPath().toString() : null,
                    outboundMappingType.getStrength()
            );
        }
        LOGGER.debug("No inbound or outbound mapping definitions found in inline mapping definition");
        return null;
    }

    public static @Nullable MappingInfo extractMappingInfo(PageBase page, SimulationResultType result) {
        MappingWorkDefinitionType mappingWorkDefinition = findMappingWorkDefinition(page, result);
        if (mappingWorkDefinition == null) {
            return null;
        }

        InlineMappingDefinitionType inlineMappingDefinition = findInlineMappingDefinition(mappingWorkDefinition);
        if (inlineMappingDefinition == null) {
            return null;
        }

        return extractMappingInfo(inlineMappingDefinition);
    }
}
