/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

public interface MidpointMcpService {

    /**
     * Read-only explain view for a single object. {@code objectType} is the REST collection name
     * ({@code users}, {@code roles}, {@code orgs}, {@code archetypes}, {@code connectors},
     * {@code resources}, {@code services}, {@code shadows}, {@code tasks}, {@code nodes}, {@code cases}).
     *
     * @param returnAttributes optional dot paths (same vocabulary as {@link #describeObjectTypeSchema});
     *                         {@code null} or empty uses curated explain defaults per type (richer than search defaults);
     *                         {@code ["*"]} returns all schema paths plus virtual fields (large JSON).
     * @param fetch shadows only: {@code true} triggers a live connector read (very expensive; use only when explicitly
     *                          needed). {@code null} or {@code false} uses the repository shadow (default)—preferred when
     *                          resolving e.g. a user's {@code linkRef} to a shadow. May fail if the resource is down.
     *                          For non-shadow types, {@code fetch=true} is rejected with {@code 400} ({@code bad_request}).
     */
    MidpointMcpObjectView explainObject(
            String objectType,
            String oid,
            List<String> returnAttributes,
            Boolean fetch,
            Task task,
            OperationResult result);

    /**
     * Search objects of the given REST collection. Use {@link MidpointMcpSearchRequest} for:
     * <ul>
     *     <li>simple {@code query} — for most types: prefix match on {@code name}; for {@code cases}: substring match on
     *         case name, description, or work item names (OR)</li>
     *     <li>{@code advancedQuery} — structured filters translated to MQL</li>
     *     <li>{@code mql} — raw MQL filter (expert)</li>
     * </ul>
     * At most one of those three may be set per request.
     *
     * @param request {@link MidpointMcpSearchRequest#getReturnAttributes()} same path vocabulary as {@link #explainObject};
     *                when omitted, search uses lighter defaults than explain (fewer fields, no extension.* by default).
     */
    MidpointMcpSearchResult searchObjects(MidpointMcpSearchRequest request, Task task, OperationResult result);

    /**
     * Returns a flattened attribute-path schema for the given REST collection type (including extension definitions
     * from the schema registry). Requires {@code authorization-model-3#read} for the corresponding object type.
     * Does not load repository objects.
     * For {@code shadows}, only the static {@code ShadowType} definition is returned (no resource-specific attributes).
     * <p>
     * Callers should pass {@code null} for {@code maxDepth} (default {@code 2}) unless a deeper or full flatten is
     * required; {@code 0} means unlimited and can be very large.
     *
     * @param maxDepth {@code null} uses default {@code 2}; {@code 0} means unlimited path depth; {@code >= 1} caps
     *                 dot-segment depth of emitted paths.
     */
    MidpointMcpTypeSchemaView describeObjectTypeSchema(
            String objectType, Integer maxDepth, Task task, OperationResult result);

    /**
     * Search audit records (separate from {@link #searchObjects}). Enforces midPoint audit read authorization on the model layer.
     * <p>
     * At most one of {@link MidpointMcpAuditSearchRequest#getQuery()} or {@link MidpointMcpAuditSearchRequest#getAdvancedQuery()}
     * may be set. A time window is always applied (explicit {@code from}/{@code to} or default last 24 hours in UTC).
     */
    MidpointMcpAuditSearchResult searchAudit(MidpointMcpAuditSearchRequest request, Task task, OperationResult result);

    /**
     * Load one audit record by {@code eventIdentifier} or numeric repository id and return a normalized explanation.
     */
    MidpointMcpAuditExplainResult explainAuditRecord(MidpointMcpAuditExplainRequest request, Task task, OperationResult result);
}
