/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.rest.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.evolveum.midpoint.mcp.api.McpPublicErrorMessages;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedQuerySpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditExplainRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditSearchRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpException;
import com.evolveum.midpoint.mcp.api.MidpointMcpObjectView;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchResult;
import com.evolveum.midpoint.mcp.api.MidpointMcpService;
import com.evolveum.midpoint.mcp.api.MidpointMcpTypeSchemaView;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServlet;

/**
 * Registers a read-only MCP server over HTTP (streamable transport) at {@code /ws/mcp}.
 * Cursor and other modern MCP HTTP clients POST JSON-RPC to this URL; the legacy SSE split
 * ({@code /sse} + {@code /message}) does not match that pattern and would return 404 for {@code POST /ws/mcp}.
 */
@Configuration(proxyBeanMethods = false)
@RequestMapping("/ws/mcp")
public class MidpointMcpRestConfiguration extends AbstractRestController {

    /**
     * Key under which we store the Spring Security authentication in the MCP transport context.
     * <p>
     * The MCP streamable transport may resume tool handling on different threads than the servlet thread,
     * and Spring Security's ThreadLocal context can be lost there. Restoring it avoids NPEs in authorization.
     */
    private static final String SECURITY_AUTH_CONTEXT_KEY = "midpoint_security_auth";

    private static final String SCHEMA_EXPLAIN_OBJECT = """
            {
              "type": "object",
              "properties": {
                "type": {
                  "type": "string",
                  "enum": [
                    "users", "roles", "orgs", "archetypes", "connectors",
                    "resources", "services", "shadows", "tasks", "nodes", "cases"
                  ],
                  "description": "REST collection / object kind (same role as objectType in REST docs). For cases, response includes workflow fields (work items, decision history, child cases) when returnAttributes is omitted."
                },
                "oid": { "type": "string", "description": "midPoint object OID" },
                "returnAttributes": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Optional dot paths (same names as midpoint_describe_object_type_schema). Omit for curated explain defaults per type (richer than search). To request every schema path, pass returnAttributes as a one-element array where the element is the single-character string asterisk (very large response)."
                },
                "fetch": {
                  "type": "boolean",
                  "description": "Shadows only; for other types, true is rejected (400). VERY EXPENSIVE when true: performs a live connector read. Use only when you must verify current resource state; omitted or false uses the repository shadow (default, fast). Typical case—resolving a user linkRef to a shadow—should use fetch=false or omit fetch. May fail if the resource is unavailable. Not part of MQL."
                }
              },
              "required": [ "type", "oid" ]
            }""";

    private static final String SCHEMA_SEARCH_OBJECTS = """
            {
              "type": "object",
              "properties": {
                "type": {
                  "type": "string",
                  "enum": [
                    "users", "roles", "orgs", "archetypes", "connectors",
                    "resources", "services", "shadows", "tasks", "nodes", "cases"
                  ],
                  "description": "REST collection / object kind"
                },
                "query": {
                  "type": "string",
                  "description": "Optional simple search. For most types: name prefix. For cases: substring match on case name, description, or work item names (OR). Use only one of query, advancedQuery, or mql."
                },
                "advancedQuery": {
                  "type": "object",
                    "description": "Structured filters translated server-side to MQL (preferred advanced mode). Paths are dot notation from midpoint_describe_object_type_schema (shadow resource reference: path resourceRef, op eq, value = target OID). Mutually exclusive with query and mql.",
                  "properties": {
                    "combine": {
                      "type": "string",
                      "enum": [ "and", "or" ],
                      "description": "How to combine filters; default and"
                    },
                    "filters": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "path": { "type": "string" },
                          "op": {
                            "type": "string",
                            "enum": [
                              "eq", "neq", "startsWith", "contains",
                              "gt", "gte", "lt", "lte", "exists", "in"
                            ]
                          },
                          "value": {
                            "description": "Scalar or array (for in). Omit for exists."
                          }
                        },
                        "required": [ "path", "op" ]
                      }
                    },
                    "orderBy": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "path": { "type": "string" },
                          "direction": { "type": "string", "enum": [ "asc", "desc" ] }
                        },
                        "required": [ "path" ]
                      }
                    },
                    "paging": {
                      "type": "object",
                      "properties": {
                        "offset": { "type": "integer" },
                        "limit": { "type": "integer" }
                      }
                    }
                  }
                },
                "mql": {
                  "type": "string",
                  "description": "Raw midPoint Query Language filter (expert). Mutually exclusive with query and advancedQuery."
                },
                "limit": { "type": "integer", "description": "Page size (default 20, max 100). Overridden by advancedQuery.paging.limit when set." },
                "offset": { "type": "integer", "description": "Overridden by advancedQuery.paging.offset when set." },
                "returnAttributes": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "Same path vocabulary as midpoint_explain_object; when omitted, search uses lighter defaults than explain (no extension.*, no heavy containers). [*] still expands to full schema (huge)."
                },
                "fetch": {
                  "type": "boolean",
                  "description": "Shadows only with searchMode repository (default). VERY EXPENSIVE when true: one live connector read per search hit (omit or false for repository snapshot only). Use only when explicitly needed; for linkRef-style lookups prefer fetch=false. Ignored when searchMode is resource (already live). Not part of MQL."
                },
                "searchMode": {
                  "type": "string",
                  "enum": [ "repository", "resource" ],
                  "description": "Shadows only: repository (default) searches stored shadows; resource searches the connector-backed resource. Not part of MQL."
                },
                "resourceOid": {
                  "type": "string",
                  "description": "Shadows only. With searchMode resource: required resource OID. With searchMode repository: optional; combine with shadowKind or objectClass to scope the query, or use expandResourceObjectTypes=true (requires resourceOid) to OR every schemaHandling object type on that resource."
                },
                "shadowKind": {
                  "type": "string",
                  "description": "Shadows only. ShadowKindType e.g. account, entitlement. With searchMode resource: required unless objectClass is set. With searchMode repository: optional top-level scope merged (AND) into the query together with resourceOid. Mutually exclusive with objectClass."
                },
                "shadowIntent": {
                  "type": "string",
                  "description": "Shadows only, optional with shadowKind (not with objectClass alone). Refined intent for repository or resource scope."
                },
                "objectClass": {
                  "type": "string",
                  "description": "Shadows only. Object class QName as {namespaceURI}localPart or namespaceURI#localPart. Same repository/resource scope rules as shadowKind. Mutually exclusive with shadowKind."
                },
                "expandResourceObjectTypes": {
                  "type": "boolean",
                  "description": "Shadows only, searchMode repository: when true with resourceOid, runs one repository search per schemaHandling object type (kind/intent) on that resource, merges and deduplicates by OID, sorts by name, then applies offset/limit. ANDs each branch with query/mql/advancedQuery. Loads up to 500 hits per type before paging (capped). totalCount is the sum of per-branch counts. Mutually exclusive with shadowKind, shadowIntent, and objectClass. Not supported for searchMode resource."
                }
              },
              "required": [ "type" ]
            }""";

    private static final String SCHEMA_DESCRIBE_OBJECT_TYPE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "type": {
                  "type": "string",
                  "enum": [
                    "users", "roles", "orgs", "archetypes", "connectors",
                    "resources", "services", "shadows", "tasks", "nodes", "cases"
                  ],
                  "description": "REST collection / object kind"
                },
                "maxDepth": {
                  "type": "integer",
                  "minimum": 0,
                  "description": "Recommended for agents: omit this field (server default 2). Max dot-segment depth; use 0 only for full flatten (large JSON)."
                }
              },
              "required": [ "type" ]
            }""";

    private static final String SCHEMA_SEARCH_AUDIT = """
            {
              "type": "object",
              "properties": {
                "from": {
                  "type": "string",
                  "description": "Inclusive lower time bound (ISO-8601). If omitted with to, defaults to 24h before to; if both omitted, defaults to 24h ago (UTC)."
                },
                "to": {
                  "type": "string",
                  "description": "Inclusive upper time bound (ISO-8601). If omitted with from, defaults to now; if both omitted, defaults to now (UTC)."
                },
                "query": {
                  "type": "string",
                  "description": "Simple text search (OR across message, task identifier, UUID as OID). Mutually exclusive with advancedQuery."
                },
                "advancedQuery": {
                  "type": "object",
                  "description": "Structured filters on MCP audit paths (not object schema). Mutually exclusive with query. Paths: timestamp, eventType, eventStage, outcome, initiator.oid|name, target.oid|name|type, channel, task.oid|name, node, message, delta (exists only), result, sessionIdentifier, requestIdentifier, customColumn.<name>. Default order: timestamp desc. Default limit 100, max 500.",
                  "properties": {
                    "combine": { "type": "string", "enum": [ "and", "or" ], "description": "Default and" },
                    "filters": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "path": { "type": "string" },
                          "op": {
                            "type": "string",
                            "enum": [
                              "eq", "neq", "startsWith", "contains",
                              "gt", "gte", "lt", "lte", "exists", "in"
                            ]
                          },
                          "value": { "description": "Scalar or array (for in). Omit for exists." }
                        },
                        "required": [ "path", "op" ]
                      }
                    },
                    "orderBy": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "path": { "type": "string" },
                          "direction": { "type": "string", "enum": [ "asc", "desc" ] }
                        },
                        "required": [ "path" ]
                      }
                    },
                    "paging": {
                      "type": "object",
                      "properties": {
                        "offset": { "type": "integer" },
                        "limit": { "type": "integer" }
                      }
                    }
                  }
                }
              }
            }""";

    private static final String SCHEMA_EXPLAIN_AUDIT = """
            {
              "type": "object",
              "properties": {
                "id": {
                  "type": "string",
                  "description": "Audit eventIdentifier (preferred), or numeric repository repoId as decimal string"
                },
                "includeDelta": {
                  "type": "boolean",
                  "description": "Include normalized delta summary (default false; can be large)"
                },
                "includeResult": {
                  "type": "boolean",
                  "description": "Include stored result text (default false; may be truncated)"
                }
              },
              "required": [ "id" ]
            }""";

    @Autowired private MidpointMcpService midpointMcpService;

    @Bean
    public JacksonMcpJsonMapper midpointMcpJsonMapper() {
        return new JacksonMcpJsonMapper(new ObjectMapper());
    }

    /**
     * Path suffix used by the MCP Java streamable servlet to match {@code requestURI} (must match the servlet mount
     * so e.g. {@code /midpoint/ws/mcp} ends with this value).
     */
    @Bean
    public HttpServletStreamableServerTransportProvider midpointMcpTransportProvider(
            JacksonMcpJsonMapper midpointMcpJsonMapper,
            @Value("${midpoint.mcp.endpoint:/ws/mcp}") String mcpEndpoint) {

        // Capture authentication on the servlet thread, so we can restore it later during async tool execution.
        McpTransportContextExtractor<jakarta.servlet.http.HttpServletRequest> extractor = (req) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return McpTransportContext.create(Map.of(SECURITY_AUTH_CONTEXT_KEY, authentication));
        };

        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(midpointMcpJsonMapper)
                .mcpEndpoint(mcpEndpoint)
                .contextExtractor(extractor)
                .build();
    }

    @Bean
    public McpSyncServer midpointMcpSyncServer(
            HttpServletStreamableServerTransportProvider transport,
            JacksonMcpJsonMapper jsonMapper) {

        return McpServer.sync(transport)
                .serverInfo("midPoint MCP", "1.0")
                .instructions("Read-only midPoint MCP: HTTP entry at /ws/mcp requires the same authorization-rest-3#all as "
                        + "other /ws/** REST endpoints (granular REST rights alone are not enough to reach MCP). "
                        + "Each tool then enforces its own actions (e.g. get/search object, audit read, model read by type for schema). "
                        + "search/explain return a values map keyed by dot paths (same vocabulary as midpoint_describe_object_type_schema). "
                        + "Optional returnAttributes selects fields; omit for curated defaults per REST type (search uses lighter defaults than explain); "
                        + "[\"*\"] expands to full schema (huge). "
                        + "Types: users, roles, orgs, archetypes, connectors, resources, services, shadows, tasks, nodes, cases. "
                        + "Schema tool needs authorization-model-3#read for the requested object type; omit maxDepth (default 2) or use 0 for unlimited. "
                        + "Shadows: static ShadowType only in schema tool. "
                        + "Shadow fetch=true is VERY EXPENSIVE (live connector); use only when explicitly required. "
                        + "Resolving a user linkRef to a shadow should use repository data (omit fetch or fetch=false). "
                        + "Audit: midpoint_search_audit and midpoint_explain_audit_record use a dedicated audit path vocabulary "
                        + "(not midpoint_describe_object_type_schema); require audit read authorization; time window always applied.")
                .jsonMapper(jsonMapper)
                .tools(
                        toolExplainObject(jsonMapper),
                        toolSearchObjects(jsonMapper),
                        toolDescribeObjectTypeSchema(jsonMapper),
                        toolSearchAudit(jsonMapper),
                        toolExplainAuditRecord(jsonMapper))
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServlet> midpointMcpServlet(
            HttpServletStreamableServerTransportProvider transport) {
        ServletRegistrationBean<HttpServlet> registration =
                new ServletRegistrationBean<>(transport, "/ws/mcp", "/ws/mcp/*");
        registration.setName("midpointMcpServlet");
        registration.setAsyncSupported(true);
        return registration;
    }

    private McpServerFeatures.SyncToolSpecification toolExplainObject(JacksonMcpJsonMapper jsonMapper) {
        String description = "Explain one object: optional returnAttributes (dot paths); default is a curated field set. "
                + "Cases: default includes workflow summary (work items, decision history, child case list, explanation text). "
                + "Shadows: fetch=true is VERY EXPENSIVE (live connector read); use only when you must see current resource state. "
                + "For typical linkRef resolution from a user to a shadow, omit fetch or use fetch=false (repository shadow). "
                + "Response includes source and fetched. fetch is not MQL.";
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("midpoint_explain_object")
                .title(description)
                .description(description)
                .inputSchema(jsonMapper, SCHEMA_EXPLAIN_OBJECT)
                .build();

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, request) -> withRestoredSecurityContext(exchange, () -> handleExplainObject(jsonMapper, request));

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private McpServerFeatures.SyncToolSpecification toolSearchObjects(JacksonMcpJsonMapper jsonMapper) {
        String description = "Search by REST type: simple name prefix (query), structured advancedQuery (MQL translation), or raw mql; paging; response includes usedQueryMode and translatedMql for advanced/mql. "
                + "Shadows: midPoint requires each shadow search to be scoped by resource plus kind or objectClass (or expandResourceObjectTypes on repository). Resource-only filters are rejected with code shadow_query_not_type_scoped and a hint. "
                + "Repository (default): optional resourceOid + shadowKind/objectClass merged into the filter; or resourceOid + expandResourceObjectTypes to search all defined object types. "
                + "Resource searchMode: live connector search (resourceOid + shadowKind or objectClass). "
                + "With repository searchMode, fetch=true is VERY EXPENSIVE (one live read per hit); use only when explicitly needed—omit fetch or use fetch=false for linkRef-style shadow lookups. "
                + "Advanced shadow filters: path resourceRef with op eq and target OID (not resourceRef.oid). fetch, searchMode, resourceOid, shadowKind, shadowIntent, objectClass, expandResourceObjectTypes are request parameters, not MQL.";
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("midpoint_search_objects")
                .title(description)
                .description(description)
                .inputSchema(jsonMapper, SCHEMA_SEARCH_OBJECTS)
                .build();

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, request) -> withRestoredSecurityContext(exchange, () -> handleSearchObjects(jsonMapper, request));

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private McpServerFeatures.SyncToolSpecification toolDescribeObjectTypeSchema(JacksonMcpJsonMapper jsonMapper) {
        String description = "Return flattened attribute paths for a REST collection type (schema registry, includes "
                + "extension definitions). Recommended: omit maxDepth (default 2 for agents); maxDepth 0 = unlimited. "
                + "Requires authorization-model-3#read for the requested object type. Does not read the repository. For shadows, only static ShadowType "
                + "(no connector-specific attributes). Large maxDepth can produce large JSON.";
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("midpoint_describe_object_type_schema")
                .title(description)
                .description(description)
                .inputSchema(jsonMapper, SCHEMA_DESCRIBE_OBJECT_TYPE_SCHEMA)
                .build();

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, request) -> withRestoredSecurityContext(exchange, () -> handleDescribeObjectTypeSchema(jsonMapper, request));

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private McpServerFeatures.SyncToolSpecification toolSearchAudit(JacksonMcpJsonMapper jsonMapper) {
        String description = "Search audit records: optional ISO-8601 from/to (default last 24h UTC), simple query OR structured "
                + "advancedQuery on MCP audit paths (timestamp, eventType, eventStage, outcome, initiator.*, target.*, channel, "
                + "task.oid|task.name, node, message, customColumn.<name>, …). Default paging limit 100, max 500; default sort "
                + "timestamp desc. Response includes effectiveWindow, totalCount, translatedQuery (advanced). Requires audit read.";
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("midpoint_search_audit")
                .title(description)
                .description(description)
                .inputSchema(jsonMapper, SCHEMA_SEARCH_AUDIT)
                .build();
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, request) -> withRestoredSecurityContext(exchange, () -> handleSearchAudit(jsonMapper, request));
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private McpServerFeatures.SyncToolSpecification toolExplainAuditRecord(JacksonMcpJsonMapper jsonMapper) {
        String description = "Load one audit record by eventIdentifier (or numeric repo id) and return normalized fields plus "
                + "explanation; optional includeDelta/includeResult for bounded extra detail. When includeDelta is true, each "
                + "operation includes attributeChanges: [{path, modificationType, oldValue, newValue}] (values redacted for "
                + "password/credentials paths; per-delta cap with attributeChangesTruncated). Requires audit read.";
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("midpoint_explain_audit_record")
                .title(description)
                .description(description)
                .inputSchema(jsonMapper, SCHEMA_EXPLAIN_AUDIT)
                .build();
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                (exchange, request) -> withRestoredSecurityContext(exchange, () -> handleExplainAuditRecord(jsonMapper, request));
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(handler)
                .build();
    }

    private McpSchema.CallToolResult handleExplainObject(JacksonMcpJsonMapper jsonMapper, McpSchema.CallToolRequest request) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "mcpTool");
        try {
            Map<String, Object> args = request.arguments();
            String objectType = stringArg(args, "type");
            String oid = stringArg(args, "oid");
            if (StringUtils.isBlank(objectType)) {
                return toolError(jsonMapper, 400, "type is required");
            }
            if (StringUtils.isBlank(oid)) {
                return toolError(jsonMapper, 400, "oid is required");
            }
            List<String> returnAttributes = stringListArg(args, "returnAttributes");
            MidpointMcpObjectView view =
                    midpointMcpService.explainObject(
                            objectType, oid, returnAttributes, booleanArg(args, "fetch"), task, result);
            return toolSuccess(jsonMapper, view);
        } catch (MidpointMcpException e) {
            return toolError(jsonMapper, e);
        } catch (Exception e) {
            logger.error("MCP explain tool failed: {}", e.toString(), e);
            return toolError(jsonMapper, 500, McpPublicErrorMessages.UNEXPECTED_TOOL_FAILURE);
        } finally {
            finishAuxiliaryRestOperation(task, result);
        }
    }

    private McpSchema.CallToolResult withRestoredSecurityContext(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            java.util.function.Supplier<McpSchema.CallToolResult> action) {

        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        Object captured = exchange.transportContext().get(SECURITY_AUTH_CONTEXT_KEY);
        Authentication capturedAuth = captured instanceof Authentication a ? a : null;

        if (capturedAuth != null) {
            SecurityContextHolder.getContext().setAuthentication(capturedAuth);
        }
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private McpSchema.CallToolResult handleSearchObjects(JacksonMcpJsonMapper jsonMapper, McpSchema.CallToolRequest request) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "mcpTool");
        try {
            Map<String, Object> args = request.arguments();
            String objectType = stringArg(args, "type");
            if (StringUtils.isBlank(objectType)) {
                return toolError(jsonMapper, 400, "type is required");
            }
            MidpointMcpSearchRequest searchRequest = new MidpointMcpSearchRequest();
            searchRequest.setType(objectType);
            searchRequest.setQuery(stringArg(args, "query"));
            searchRequest.setMql(stringArg(args, "mql"));
            searchRequest.setLimit(integerArg(args, "limit"));
            searchRequest.setOffset(integerArg(args, "offset"));
            searchRequest.setReturnAttributes(stringListArg(args, "returnAttributes"));
            searchRequest.setFetch(booleanArg(args, "fetch"));
            searchRequest.setSearchMode(stringArg(args, "searchMode"));
            searchRequest.setResourceOid(stringArg(args, "resourceOid"));
            searchRequest.setShadowKind(stringArg(args, "shadowKind"));
            searchRequest.setShadowIntent(stringArg(args, "shadowIntent"));
            searchRequest.setObjectClass(stringArg(args, "objectClass"));
            searchRequest.setExpandResourceObjectTypes(booleanArg(args, "expandResourceObjectTypes"));

            Object advancedRaw = args != null ? args.get("advancedQuery") : null;
            if (advancedRaw != null) {
                if (!(advancedRaw instanceof Map<?, ?>)) {
                    return toolError(jsonMapper, 400, "advancedQuery must be a JSON object");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> advancedMap = (Map<String, Object>) advancedRaw;
                try {
                    searchRequest.setAdvancedQuery(
                            jsonMapper.getObjectMapper().convertValue(advancedMap, MidpointMcpAdvancedQuerySpec.class));
                } catch (IllegalArgumentException e) {
                    return toolError(jsonMapper, 400, "advancedQuery is invalid: " + e.getMessage());
                }
            }

            MidpointMcpSearchResult searchResult = midpointMcpService.searchObjects(searchRequest, task, result);
            return toolSuccess(jsonMapper, searchResult);
        } catch (MidpointMcpException e) {
            return toolError(jsonMapper, e);
        } catch (Exception e) {
            logger.error("MCP search tool failed: {}", e.toString(), e);
            return toolError(jsonMapper, 500, McpPublicErrorMessages.UNEXPECTED_TOOL_FAILURE);
        } finally {
            finishAuxiliaryRestOperation(task, result);
        }
    }

    private McpSchema.CallToolResult handleDescribeObjectTypeSchema(JacksonMcpJsonMapper jsonMapper, McpSchema.CallToolRequest request) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "mcpTool");
        try {
            Map<String, Object> args = request.arguments();
            String objectType = stringArg(args, "type");
            if (StringUtils.isBlank(objectType)) {
                return toolError(jsonMapper, 400, "type is required");
            }
            Integer maxDepth = integerArg(args, "maxDepth");
            MidpointMcpTypeSchemaView view =
                    midpointMcpService.describeObjectTypeSchema(objectType, maxDepth, task, result);
            return toolSuccess(jsonMapper, view);
        } catch (MidpointMcpException e) {
            return toolError(jsonMapper, e);
        } catch (Exception e) {
            logger.error("MCP describe object type schema tool failed: {}", e.toString(), e);
            return toolError(jsonMapper, 500, McpPublicErrorMessages.UNEXPECTED_TOOL_FAILURE);
        } finally {
            finishAuxiliaryRestOperation(task, result);
        }
    }

    private McpSchema.CallToolResult handleSearchAudit(JacksonMcpJsonMapper jsonMapper, McpSchema.CallToolRequest request) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "mcpTool");
        try {
            Map<String, Object> args = request.arguments();
            MidpointMcpAuditSearchRequest searchRequest = new MidpointMcpAuditSearchRequest();
            searchRequest.setFrom(stringArg(args, "from"));
            searchRequest.setTo(stringArg(args, "to"));
            searchRequest.setQuery(stringArg(args, "query"));
            Object advancedRaw = args != null ? args.get("advancedQuery") : null;
            if (advancedRaw != null) {
                if (!(advancedRaw instanceof Map<?, ?>)) {
                    return toolError(jsonMapper, 400, "advancedQuery must be a JSON object");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> advancedMap = (Map<String, Object>) advancedRaw;
                try {
                    searchRequest.setAdvancedQuery(
                            jsonMapper.getObjectMapper().convertValue(advancedMap, MidpointMcpAdvancedQuerySpec.class));
                } catch (IllegalArgumentException e) {
                    return toolError(jsonMapper, 400, "advancedQuery is invalid: " + e.getMessage());
                }
            }
            var searchResult = midpointMcpService.searchAudit(searchRequest, task, result);
            return toolSuccess(jsonMapper, searchResult);
        } catch (MidpointMcpException e) {
            return toolError(jsonMapper, e);
        } catch (Exception e) {
            logger.error("MCP audit search tool failed: {}", e.toString(), e);
            return toolError(jsonMapper, 500, McpPublicErrorMessages.UNEXPECTED_TOOL_FAILURE);
        } finally {
            finishAuxiliaryRestOperation(task, result);
        }
    }

    private McpSchema.CallToolResult handleExplainAuditRecord(JacksonMcpJsonMapper jsonMapper, McpSchema.CallToolRequest request) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "mcpTool");
        try {
            Map<String, Object> args = request.arguments();
            MidpointMcpAuditExplainRequest explainRequest = new MidpointMcpAuditExplainRequest();
            explainRequest.setId(stringArg(args, "id"));
            explainRequest.setIncludeDelta(booleanArg(args, "includeDelta"));
            explainRequest.setIncludeResult(booleanArg(args, "includeResult"));
            var explainResult = midpointMcpService.explainAuditRecord(explainRequest, task, result);
            return toolSuccess(jsonMapper, explainResult);
        } catch (MidpointMcpException e) {
            return toolError(jsonMapper, e);
        } catch (Exception e) {
            logger.error("MCP audit explain tool failed: {}", e.toString(), e);
            return toolError(jsonMapper, 500, McpPublicErrorMessages.UNEXPECTED_TOOL_FAILURE);
        } finally {
            finishAuxiliaryRestOperation(task, result);
        }
    }

    private static McpSchema.CallToolResult toolSuccess(JacksonMcpJsonMapper jsonMapper, Object value) {
        try {
            String text = jsonMapper.getObjectMapper().writeValueAsString(value);
            return McpSchema.CallToolResult.builder()
                    .addTextContent(text)
                    .isError(false)
                    .build();
        } catch (IOException e) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(McpPublicErrorMessages.SERIALIZATION_FAILED)
                    .isError(true)
                    .build();
        }
    }

    private static McpSchema.CallToolResult toolError(JacksonMcpJsonMapper jsonMapper, MidpointMcpException e) {
        return toolError(jsonMapper, e.getStatus(), e.getMessage(), e.getCode(), e.getHint());
    }

    private static McpSchema.CallToolResult toolError(JacksonMcpJsonMapper jsonMapper, int status, String message) {
        return toolError(jsonMapper, status, message, null, null);
    }

    private static McpSchema.CallToolResult toolError(
            JacksonMcpJsonMapper jsonMapper, int status, String message, String code, String hint) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status);
            body.put("message", message != null ? message : "");
            if (StringUtils.isNotBlank(code)) {
                body.put("code", code.trim());
            }
            if (StringUtils.isNotBlank(hint)) {
                body.put("hint", hint.trim());
            }
            String text = jsonMapper.getObjectMapper().writeValueAsString(body);
            return McpSchema.CallToolResult.builder()
                    .addTextContent(text)
                    .isError(true)
                    .build();
        } catch (IOException ex) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(message)
                    .isError(true)
                    .build();
        }
    }

    private static String stringArg(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private static List<String> stringListArg(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
            return out.isEmpty() ? null : out;
        }
        return null;
    }

    private static Boolean booleanArg(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static Integer integerArg(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
