/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Default {@code returnAttributes} when the client omits the parameter.
 * Search uses lightweight defaults; explain uses a richer curated set per REST type.
 * Paths use the same dot notation as {@code midpoint_describe_object_type_schema}.
 */
final class MidpointMcpDefaultAttributes {

    static final String EXTENSION_STAR = "extension.*";
    static final String STAR = "*";

    private static final List<String> COMMON = List.of(
            "oid",
            "type",
            "name",
            "displayName",
            "description",
            "lifecycleState",
            "archetypeRef",
            "summary");

    /** Curated defaults for {@code midpoint_explain_object} (and explicit-path validation vocabulary). */
    private static final Map<String, List<String>> EXPLAIN_BY_REST_TYPE = Map.ofEntries(
            Map.entry(
                    "users",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "parentOrgRef",
                            "tenantRef",
                            "roleMembershipRef",
                            "linkRef",
                            "assignment",
                            "lifecycleState",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "activation.validFrom",
                            "activation.validTo",
                            "activation.validityStatus",
                            "activation.lockoutStatus",
                            "fullName",
                            "givenName",
                            "familyName",
                            "employeeNumber",
                            "personalNumber",
                            "emailAddress",
                            "telephoneNumber",
                            "organization",
                            "organizationalUnit",
                            "costCenter",
                            "locality",
                            "preferredLanguage",
                            "locale",
                            "timezone",
                            EXTENSION_STAR)),
            Map.entry(
                    "roles",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "lifecycleState",
                            "assignment",
                            "inducement",
                            "roleMembershipRef",
                            "memberCount",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "requestable",
                            EXTENSION_STAR)),
            Map.entry(
                    "orgs",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "parentOrgRef",
                            "tenantRef",
                            "lifecycleState",
                            "memberCount",
                            "childOrgCount",
                            "managerRef",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "identifier",
                            EXTENSION_STAR)),
            Map.entry(
                    "archetypes",
                    List.of(
                            "name",
                            "description",
                            "lifecycleState",
                            "archetypeType",
                            "superArchetypeRef",
                            EXTENSION_STAR)),
            Map.entry(
                    "connectors",
                    List.of(
                            "name",
                            "description",
                            "connectorType",
                            "framework",
                            "bundle",
                            "version",
                            "connectorHostRef",
                            EXTENSION_STAR)),
            Map.entry(
                    "resources",
                    List.of(
                            "name",
                            "description",
                            "lifecycleState",
                            "connectorRef",
                            "resourceType",
                            "configured",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "capabilitiesSummary",
                            "kindIntentSummary",
                            EXTENSION_STAR)),
            Map.entry(
                    "services",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "lifecycleState",
                            "ownerRef",
                            "managerRef",
                            "memberCount",
                            "serviceType",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            EXTENSION_STAR)),
            Map.entry(
                    "shadows",
                    List.of(
                            "name",
                            "description",
                            "resourceRef",
                            "kind",
                            "intent",
                            "objectClass",
                            "primaryIdentifier",
                            "ownerRef",
                            "exists",
                            "dead",
                            "synchronizationSituation",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            EXTENSION_STAR)),
            Map.entry(
                    "tasks",
                    List.of(
                            "name",
                            "description",
                            "lifecycleState",
                            "category",
                            "handlerUri",
                            "executionState",
                            "schedulingState",
                            "resultStatus",
                            "ownerRef",
                            "nodeRef",
                            "nextRun",
                            "lastRunStart",
                            "lastRunFinish",
                            "progress",
                            EXTENSION_STAR)),
            Map.entry(
                    "nodes",
                    List.of(
                            "name",
                            "description",
                            "hostname",
                            "status",
                            "lastCheckIn",
                            "taskCount",
                            "currentTaskCount",
                            EXTENSION_STAR)),
            Map.entry(
                    "cases",
                    List.of(
                            "state",
                            "outcome",
                            "closeTimestamp",
                            "stageNumber",
                            "parentRef",
                            "objectRef",
                            "targetRef",
                            "requestorRef",
                            "workItemCount",
                            "activeWorkItemCount",
                            "currentStep",
                            EXTENSION_STAR)));

    /**
     * Lightweight defaults for {@code midpoint_search_objects}: no extension.*, no heavy containers,
     * no virtual counts that trigger extra model queries.
     */
    private static final Map<String, List<String>> SEARCH_BY_REST_TYPE = Map.ofEntries(
            Map.entry(
                    "users",
                    List.of(
                            "subtype",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "activation.validFrom",
                            "activation.validTo",
                            "activation.validityStatus",
                            "activation.lockoutStatus",
                            "fullName",
                            "givenName",
                            "familyName",
                            "employeeNumber",
                            "personalNumber",
                            "emailAddress",
                            "telephoneNumber",
                            "organization",
                            "organizationalUnit",
                            "costCenter",
                            "locality",
                            "preferredLanguage",
                            "locale",
                            "timezone")),
            Map.entry(
                    "roles",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "lifecycleState",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "requestable")),
            Map.entry(
                    "orgs",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "parentOrgRef",
                            "tenantRef",
                            "lifecycleState",
                            "managerRef",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus",
                            "identifier")),
            Map.entry(
                    "archetypes",
                    List.of(
                            "name",
                            "description",
                            "lifecycleState",
                            "archetypeType",
                            "superArchetypeRef")),
            Map.entry(
                    "connectors",
                    List.of(
                            "name",
                            "description",
                            "connectorType",
                            "framework",
                            "bundle",
                            "version",
                            "connectorHostRef")),
            Map.entry(
                    "resources",
                    List.of(
                            "name",
                            "description",
                            "lifecycleState",
                            "connectorRef",
                            "resourceType",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus")),
            Map.entry(
                    "services",
                    List.of(
                            "name",
                            "description",
                            "subtype",
                            "archetypeRef",
                            "lifecycleState",
                            "ownerRef",
                            "managerRef",
                            "serviceType",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus")),
            Map.entry(
                    "shadows",
                    List.of(
                            "name",
                            "description",
                            "resourceRef",
                            "kind",
                            "intent",
                            "objectClass",
                            "primaryIdentifier",
                            "ownerRef",
                            "exists",
                            "dead",
                            "synchronizationSituation",
                            "activation.administrativeStatus",
                            "activation.effectiveStatus")),
            Map.entry(
                    "tasks",
                    List.of(
                            "name",
                            "description",
                            "lifecycleState",
                            "category",
                            "handlerUri",
                            "executionState",
                            "schedulingState",
                            "resultStatus",
                            "ownerRef",
                            "nodeRef",
                            "nextRun",
                            "lastRunStart",
                            "lastRunFinish",
                            "progress")),
            Map.entry(
                    "nodes",
                    List.of(
                            "name",
                            "description",
                            "hostname",
                            "status",
                            "lastCheckIn",
                            "taskCount",
                            "currentTaskCount")),
            Map.entry(
                    "cases",
                    List.of(
                            "state",
                            "outcome",
                            "closeTimestamp",
                            "stageNumber",
                            "parentRef",
                            "objectRef",
                            "targetRef",
                            "requestorRef",
                            "workItemCount",
                            "activeWorkItemCount",
                            "currentStep")));

    /** Virtual paths not produced by {@link MidpointMcpSchemaFlattener} but valid for explicit {@code returnAttributes}. */
    static final Set<String> VIRTUAL_PATHS = Set.of(
            "oid",
            "type",
            "summary",
            "memberCount",
            "childOrgCount",
            "capabilitiesSummary",
            "kindIntentSummary",
            "configured",
            "workItemCount",
            "activeWorkItemCount",
            "currentStep",
            "request",
            "workItems",
            "decisionHistory",
            "childCases",
            "explanation");

    private MidpointMcpDefaultAttributes() {}

    static List<String> searchDefaultsForRestType(String restType) {
        return mergeWithCommon(restType, SEARCH_BY_REST_TYPE);
    }

    static List<String> explainDefaultsForRestType(String restType) {
        return mergeWithCommon(restType, EXPLAIN_BY_REST_TYPE);
    }

    private static List<String> mergeWithCommon(String restType, Map<String, List<String>> byType) {
        String key = restType.trim().toLowerCase(Locale.ROOT);
        List<String> specific = byType.get(key);
        if (specific == null) {
            throw new IllegalArgumentException("unsupported type '" + restType + "'");
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if ("cases".equals(key)) {
            merged.addAll(
                    List.of("oid", "type", "name", "description", "lifecycleState", "archetypeRef", "summary"));
        } else {
            merged.addAll(COMMON);
        }
        merged.addAll(specific);
        return new ArrayList<>(merged);
    }

    /** Default {@code returnAttributes} for {@code midpoint_explain_object} on {@code cases} (search uses {@link #searchDefaultsForRestType}). */
    static List<String> explainDefaultsForCases() {
        LinkedHashSet<String> merged = new LinkedHashSet<>(explainDefaultsForRestType("cases"));
        merged.add("request");
        merged.add("workItems");
        merged.add("decisionHistory");
        merged.add("childCases");
        merged.add("explanation");
        return new ArrayList<>(merged);
    }
}
