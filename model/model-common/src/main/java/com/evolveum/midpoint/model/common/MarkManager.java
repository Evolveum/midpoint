/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectables;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.config.GlobalPolicyRuleConfigItem;
import com.evolveum.midpoint.schema.util.MarkTypeUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Manages {@link MarkType} objects.
 *
 * Currently does not provide its own cache, but relies on the repository cache instead.
 * (Assumes that the global caching of {@link MarkType} objects and queries is enabled.)
 * In the future we may consider adding caching capabilities here, if needed.
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // temporary
@Component
public class MarkManager {

    private static final Trace LOGGER = TraceManager.getTrace(MarkManager.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;

    /** Gets a mark by OID. */
    public @NotNull MarkType getMark(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (!cacheRepositoryService.supportsMarks()) {
            throw new UnsupportedOperationException("The repository does not support mark objects");
        }
        var options = GetOperationOptionsBuilder.create()
                .readOnly()
                .build();
        return cacheRepositoryService
                .getObject(MarkType.class, oid, options, result)
                .asObjectable();
    }

    /** Gets the mark by OID (if exists). */
    public @Nullable MarkType getMarkIfExists(String oid, OperationResult result)
            throws SchemaException {
        if (!cacheRepositoryService.supportsMarks()) {
            return null;
        }
        var options = GetOperationOptionsBuilder.create()
                .allowNotFound()
                .readOnly()
                .build();
        try {
            return cacheRepositoryService
                    .getObject(MarkType.class, oid, options, result)
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets a mark by URI. Note that we currently have no repo constraint disallowing multiple marks with the same URI,
     * but check that as a configuration exception. This should be perhaps changed and enforced on DB level.
     */
    public @Nullable MarkType getMarkByUri(@NotNull String uri, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (!cacheRepositoryService.supportsMarks()) {
            return null;
        }
        var marks = cacheRepositoryService.searchObjects(
                MarkType.class,
                prismContext.queryFor(MarkType.class)
                        .item(MarkType.F_URI).eq(uri)
                        .build(),
                readOnly(),
                result);
        LOGGER.trace("Mark(s) by URI '{}': {}", uri, marks);
        return asObjectable(
                MiscUtil.extractSingleton(
                        marks,
                        () -> new ConfigurationException("Multiple marks with URI of '" + uri + "': " + marks)));
    }

    public @NotNull Collection<MarkType> getAllMarks(OperationResult result) {
        if (!cacheRepositoryService.supportsMarks()) {
            return List.of();
        }
        try {
            return asObjectables(
                    cacheRepositoryService.searchObjects(
                            MarkType.class, null, readOnly(), result));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when retrieving all marks");
        }
    }

    public @NotNull Collection<MarkType> getAllEventMarks(OperationResult result) {
        return getAllMarks(result).stream()
                .filter(mark -> ObjectTypeUtil.hasArchetypeRef(mark, SystemObjectsType.ARCHETYPE_EVENT_MARK.value()))
                .collect(Collectors.toList());
    }

    /**
     * Collects all (global) policy rules from all marks. Adding the `markRef` in case it does not include a reference to the
     * current mark.
     *
     * [EP:M:PRC] DONE, the returned values have correct origin
     */
    public @NotNull Collection<GlobalRuleWithId> getAllEnabledMarkPolicyRules(Task task, OperationResult result) {
        List<GlobalRuleWithId> rules = new ArrayList<>();
        for (MarkType mark : getAllMarks(result)) {
            if (isEnabled(mark, task)) {
                for (GlobalPolicyRuleType rule : mark.getPolicyRule()) {
                    // [EP:M:PRC] DONE Origin is safe, as the rules are obtained right from the repository.
                    GlobalPolicyRuleConfigItem ruleCI = GlobalPolicyRuleConfigItem.embedded(rule);
                    if (!Referencable.getOids(rule.getMarkRef()).contains(mark.getOid())) {
                        var ruleClone = rule.clone();
                        ruleClone.getMarkRef().add(
                                ObjectTypeUtil.createObjectRef(mark));
                        ruleCI = GlobalPolicyRuleConfigItem.of(ruleClone, ruleCI.origin());
                    }
                    rules.add(
                            GlobalRuleWithId.of(ruleCI, mark.getOid()));
                }
            }
        }
        return rules;
    }

    private boolean isEnabled(@NotNull MarkType mark, @NotNull Task task) {
        if (!task.canSee(mark)) {
            return false;
        }
        if (!ObjectTypeUtil.hasArchetypeRef(mark, SystemObjectsType.ARCHETYPE_EVENT_MARK.value())) {
            // Marks other than "event marks" are visible regardless of the simulation settings.
            // Event marks are currently supported only in simulation mode.
            return true;
        }
        SimulationTransaction simulationTransaction = task.getSimulationTransaction();
        if (simulationTransaction != null) {
            return simulationTransaction.getSimulationResult().isEventMarkEnabled(mark);
        }
        if (task.isExecutionFullyPersistent()) {
            return false; // Event marks are currently used only for simulations
        }
        // We have no simulation [result] definition, so no custom inclusion/exclusion of event marks.
        // Hence, the default setting has to be applied.
        return MarkTypeUtil.isEnabledByDefault(mark);
    }

    /** Temporary */
    public Map<String, MarkType> resolveMarkNames(Collection<String> markOids, OperationResult result) {
        Map<String, MarkType> map = new HashMap<>();
        for (String markOid : markOids) {
            MarkType mark = null;
            try {
                mark = getMarkIfExists(markOid, result);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve mark {} (ignoring)", e, markOid);
            }
            map.put(markOid, mark);
        }
        return map;
    }
}
