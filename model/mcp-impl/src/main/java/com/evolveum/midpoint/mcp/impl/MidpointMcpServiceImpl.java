/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedOrderBySpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedPagingSpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAdvancedQuerySpec;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditEffectiveWindow;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditExplainRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditExplainResult;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditRecordSummary;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditSearchRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpAuditSearchResult;
import com.evolveum.midpoint.mcp.api.MidpointMcpException;
import com.evolveum.midpoint.mcp.api.MidpointMcpObjectView;
import com.evolveum.midpoint.mcp.api.MidpointMcpSchemaAttribute;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchItem;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchRequest;
import com.evolveum.midpoint.mcp.api.MidpointMcpSearchResult;
import com.evolveum.midpoint.mcp.api.McpPublicErrorMessages;
import com.evolveum.midpoint.mcp.api.MidpointMcpService;
import com.evolveum.midpoint.mcp.api.MidpointMcpTypeSchemaView;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceOperationCoordinates;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthorizationAction;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MidpointMcpServiceImpl implements MidpointMcpService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int AUDIT_DEFAULT_LIMIT = 100;
    private static final int AUDIT_MAX_LIMIT = 500;
    /** Max shadows loaded per schema branch when expandResourceObjectTypes is used (before merge, dedupe, and paging). */
    private static final int EXPAND_PER_BRANCH_MAX = 500;

    private static final Set<String> MCP_OBJECT_REST_TYPES = Set.of(
            "users",
            "roles",
            "orgs",
            "archetypes",
            "connectors",
            "resources",
            "services",
            "shadows",
            "tasks",
            "nodes",
            "cases");

    @Autowired private ModelService modelService;
    @Autowired private ModelAuditService modelAuditService;
    @Autowired private OrgStructFunctions orgStructFunctions;
    @Autowired private PrismContext prismContext;
    @Autowired private SecurityEnforcer securityEnforcer;

    @Override
    public MidpointMcpObjectView explainObject(
            String objectType,
            String oid,
            List<String> returnAttributes,
            Boolean fetch,
            Task task,
            OperationResult result) {
        return translateExceptions(() -> {
            Class<? extends ObjectType> clazz = resolveMcpObjectClass(objectType);
            if (Boolean.TRUE.equals(fetch) && !ShadowType.class.equals(clazz)) {
                throw new IllegalArgumentException("fetch is supported only for type shadows");
            }
            authorizeRest(RestAuthorizationAction.GET_OBJECT, task, result);
            String rest = ObjectTypes.getRestTypeFromClass(clazz);
            boolean shadow = ShadowType.class.equals(clazz);
            boolean caseType = CaseType.class.equals(clazz);
            boolean liveFetch = Boolean.TRUE.equals(fetch);
            Collection<SelectorOptions<GetOperationOptions>> getOpts;
            String source = null;
            Boolean fetchedFlag = null;
            if (shadow) {
                if (liveFetch) {
                    getOpts = shadowLiveGetOptions();
                    source = "resource";
                    fetchedFlag = true;
                } else {
                    getOpts = GetOperationOptions.noFetch();
                    source = "repository";
                    fetchedFlag = false;
                }
            } else {
                getOpts = null;
            }

            PrismObject<? extends ObjectType> object = modelService.getObject(clazz, oid, getOpts, task, result);
            PrismObjectDefinition<?> def =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
            List<String> paths =
                    CaseType.class.equals(clazz) && (returnAttributes == null || returnAttributes.isEmpty())
                            ? resolvePathsOrBadRequest(
                                    MidpointMcpDefaultAttributes.explainDefaultsForCases(), rest, def, false)
                            : resolvePathsOrBadRequest(returnAttributes, rest, def, false);
            MidpointMcpAttributeProjector projector =
                    new MidpointMcpAttributeProjector(prismContext, modelService, orgStructFunctions);
            ShadowType shadowBean = shadow ? (ShadowType) object.asObjectable() : null;
            String summary =
                    shadow
                            ? MidpointMcpShadowProjector.explainShadowSummary(
                                    shadowBean, source, Boolean.TRUE.equals(fetchedFlag))
                            : MidpointMcpAttributeProjector.explainSummary(object.asObjectable(), rest);
            var values = projector.project(object, rest, paths, summary, task, result, true);
            if (caseType) {
                @SuppressWarnings("unchecked")
                PrismObject<CaseType> caseObject = (PrismObject<CaseType>) object;
                MidpointMcpCaseProjector.enrichExplain(
                        values, caseObject, prismContext, modelService, task, result);
            }
            if (shadow) {
                MidpointMcpShadowProjector.putShadowPayload(values, shadowBean);
            }
            if (shadow && liveFetch) {
                OperationResult sub = result.createMinorSubresult(MidpointMcpServiceImpl.class.getName() + ".liveVsRepository");
                try {
                    PrismObject<ShadowType> repoShadow =
                            modelService.getObject(ShadowType.class, oid, GetOperationOptions.noFetch(), task, sub);
                    String note = liveVsRepositoryNote(shadowBean, repoShadow.asObjectable());
                    if (note != null) {
                        values.put("summary.liveVsRepository", note);
                    }
                } catch (Exception e) {
                    // best-effort comparison only
                } finally {
                    sub.close();
                }
            }
            MidpointMcpObjectView view = new MidpointMcpObjectView();
            if (shadow) {
                view.setSource(source);
                view.setFetched(fetchedFlag);
            }
            view.setValues(values);
            return view;
        });
    }

    @Override
    public MidpointMcpSearchResult searchObjects(MidpointMcpSearchRequest request, Task task, OperationResult result) {
        return translateExceptions(() -> {
            if (request == null) {
                throw new IllegalArgumentException("request is required");
            }
            validateSearchModes(request);
            validateShadowOnlyParameters(request);
            Class<? extends ObjectType> clazz = resolveMcpObjectClass(request.getType());
            if (ShadowType.class.equals(clazz)) {
                return searchShadowObjects(request, task, result);
            }
            return searchObjectsForClass(clazz, request, task, result);
        });
    }

    @Override
    public MidpointMcpAuditSearchResult searchAudit(MidpointMcpAuditSearchRequest request, Task task, OperationResult result) {
        return translateExceptions(() -> {
            if (request == null) {
                throw new IllegalArgumentException("request is required");
            }
            validateAuditSearchModes(request);
            if (!modelAuditService.supportsRetrieval()) {
                throw new IllegalArgumentException("Audit retrieval is not enabled for this deployment");
            }
            Instant now = Instant.now();
            Instant fromInstant;
            Instant toInstant;
            if (StringUtils.isNotBlank(request.getFrom()) || StringUtils.isNotBlank(request.getTo())) {
                if (StringUtils.isNotBlank(request.getFrom()) && StringUtils.isNotBlank(request.getTo())) {
                    fromInstant = parseInstantBound(request.getFrom(), "from");
                    toInstant = parseInstantBound(request.getTo(), "to");
                } else if (StringUtils.isNotBlank(request.getFrom())) {
                    fromInstant = parseInstantBound(request.getFrom(), "from");
                    toInstant = now;
                } else {
                    toInstant = parseInstantBound(request.getTo(), "to");
                    fromInstant = toInstant.minus(24, ChronoUnit.HOURS);
                }
            } else {
                fromInstant = now.minus(24, ChronoUnit.HOURS);
                toInstant = now;
            }
            if (fromInstant.isAfter(toInstant)) {
                throw new IllegalArgumentException("from must be before or equal to to");
            }

            String usedMode;
            ObjectFilter contentFilter = null;
            String translatedQuery = null;
            if (StringUtils.isNotBlank(request.getQuery())) {
                usedMode = "simple";
                ObjectQuery sq = MidpointMcpAuditFilterBuilder.buildSimpleTextQuery(prismContext, request.getQuery());
                contentFilter = sq != null ? sq.getFilter() : null;
            } else if (request.getAdvancedQuery() != null) {
                usedMode = "advancedQuery";
                contentFilter = MidpointMcpAuditFilterBuilder.buildAdvancedFilter(prismContext, request.getAdvancedQuery());
                translatedQuery = MidpointMcpAuditFilterBuilder.describeAdvancedFilter(contentFilter);
            } else {
                usedMode = "simple";
            }

            ObjectFilter timeFilter = auditTimestampBetweenFilter(fromInstant, toInstant);
            ObjectFilter merged = ObjectQueryUtil.filterAndImmutable(contentFilter, timeFilter);
            ObjectQuery objectQuery = prismContext.queryFactory().createQuery(merged);

            MidpointMcpAdvancedQuerySpec aq = request.getAdvancedQuery();
            int limit = normalizeAuditLimit(aq != null && aq.getPaging() != null ? aq.getPaging().getLimit() : null);
            int offset = normalizeAuditOffset(aq != null && aq.getPaging() != null ? aq.getPaging().getOffset() : null);
            MidpointMcpAuditSchema.validateOrderByPaths(aq != null ? aq.getOrderBy() : null);

            ObjectPaging paging = prismContext.queryFactory().createPaging(offset, limit);
            if (aq == null || aq.getOrderBy() == null || aq.getOrderBy().isEmpty()) {
                paging.addOrderingInstruction(AuditEventRecordType.F_TIMESTAMP, OrderDirection.DESCENDING);
            } else {
                for (MidpointMcpAdvancedOrderBySpec ob : aq.getOrderBy()) {
                    if (ob == null || StringUtils.isBlank(ob.getPath())) {
                        continue;
                    }
                    paging.addOrderingInstruction(
                            MidpointMcpAuditSchema.orderByItemPath(ob.getPath().trim()),
                            parseOrderDirection(ob.getDirection()));
                }
            }
            objectQuery.setPaging(paging);

            SearchResultList<AuditEventRecordType> found;
            int totalCount;
            try {
                found = modelAuditService.searchObjects(objectQuery, null, task, result);

                ObjectQuery countQuery = objectQuery.clone();
                countQuery.setPaging(null);
                totalCount = modelAuditService.countObjects(countQuery, null, task, result);
            } catch (RuntimeException e) {
                throw new MidpointMcpException(
                        "audit_query_failed",
                        400,
                        "Audit query could not be executed with the provided filters/time window.",
                        "Try narrowing from/to, reducing limit, or using advancedQuery (e.g., target.oid).",
                        e);
            }

            MidpointMcpAuditSearchResult out = new MidpointMcpAuditSearchResult();
            out.setUsedQueryMode(usedMode);
            out.setEffectiveWindow(new MidpointMcpAuditEffectiveWindow(
                    java.time.format.DateTimeFormatter.ISO_INSTANT.format(fromInstant),
                    java.time.format.DateTimeFormatter.ISO_INSTANT.format(toInstant)));
            out.setTotalCount(totalCount);
            out.setCount(found.size());
            out.setTranslatedQuery(translatedQuery);
            out.setLimit(limit);
            out.setOffset(offset);
            List<MidpointMcpAuditRecordSummary> records = new ArrayList<>();
            for (AuditEventRecordType r : found) {
                records.add(MidpointMcpAuditNormalizer.toSummary(r));
            }
            out.setRecords(records);
            return out;
        });
    }

    @Override
    public MidpointMcpAuditExplainResult explainAuditRecord(MidpointMcpAuditExplainRequest request, Task task, OperationResult result) {
        return translateExceptions(() -> {
            if (request == null || StringUtils.isBlank(request.getId())) {
                throw new IllegalArgumentException("id is required");
            }
            if (!modelAuditService.supportsRetrieval()) {
                throw new IllegalArgumentException("Audit retrieval is not enabled for this deployment");
            }
            String id = request.getId().trim();
            AuditEventRecordType record = loadAuditRecordById(id, task, result);
            if (record == null) {
                throw new MidpointMcpException("not_found", 404, McpPublicErrorMessages.NOT_FOUND);
            }
            boolean delta = Boolean.TRUE.equals(request.getIncludeDelta());
            boolean res = Boolean.TRUE.equals(request.getIncludeResult());
            return MidpointMcpAuditNormalizer.toExplain(record, delta, res, prismContext);
        });
    }

    private AuditEventRecordType loadAuditRecordById(String id, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
                    CommunicationException, ConfigurationException {
        ObjectQuery byEventId = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_EVENT_IDENTIFIER)
                .eq(id)
                .build();
        SearchResultList<AuditEventRecordType> list = modelAuditService.searchObjects(byEventId, null, task, result);
        if (!list.isEmpty()) {
            if (list.size() > 1) {
                throw new IllegalArgumentException("Multiple audit records share event identifier " + id);
            }
            return list.get(0);
        }
        if (id.matches("^\\d+$")) {
            try {
                long repoId = Long.parseLong(id);
                ObjectQuery byRepo = prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_REPO_ID)
                        .eq(repoId)
                        .build();
                SearchResultList<AuditEventRecordType> byRepoList =
                        modelAuditService.searchObjects(byRepo, null, task, result);
                if (!byRepoList.isEmpty()) {
                    if (byRepoList.size() > 1) {
                        throw new IllegalArgumentException("Multiple audit records share repo id " + id);
                    }
                    return byRepoList.get(0);
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return null;
    }

    private ObjectFilter auditTimestampBetweenFilter(Instant from, Instant to) {
        XMLGregorianCalendar cFrom = MiscUtil.asXMLGregorianCalendar(from.toEpochMilli());
        XMLGregorianCalendar cTo = MiscUtil.asXMLGregorianCalendar(to.toEpochMilli());
        ObjectFilter ge = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .ge(cFrom)
                .build()
                .getFilter();
        ObjectFilter le = prismContext.queryFor(AuditEventRecordType.class)
                .item(AuditEventRecordType.F_TIMESTAMP)
                .le(cTo)
                .build()
                .getFilter();
        return prismContext.queryFactory().createAnd(ge, le);
    }

    private static Instant parseInstantBound(String raw, String label) {
        String s = raw.trim();
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            try {
                XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(s);
                return cal.toGregorianCalendar().toInstant();
            } catch (DatatypeConfigurationException ex) {
                throw new IllegalArgumentException("Invalid " + label + " datetime: " + raw);
            }
        }
    }

    private static void validateAuditSearchModes(MidpointMcpAuditSearchRequest request) {
        boolean hasQuery = StringUtils.isNotBlank(request.getQuery());
        boolean hasAdvanced = request.getAdvancedQuery() != null;
        if (hasQuery && hasAdvanced) {
            throw new IllegalArgumentException("Use only one of: query or advancedQuery");
        }
    }

    private int normalizeAuditLimit(Integer limit) {
        if (limit == null) {
            return AUDIT_DEFAULT_LIMIT;
        }
        if (limit < 1) {
            throw new MidpointMcpException("bad_request", 400, "limit must be greater than 0");
        }
        return Math.min(limit, AUDIT_MAX_LIMIT);
    }

    private int normalizeAuditOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new MidpointMcpException("bad_request", 400, "offset must be 0 or greater");
        }
        return offset;
    }

    private static void validateShadowOnlyParameters(MidpointMcpSearchRequest request) {
        String n = request.getType() != null ? request.getType().trim().toLowerCase(Locale.ROOT) : "";
        boolean isShadow = "shadows".equals(n);
        if (Boolean.TRUE.equals(request.getFetch()) && !isShadow) {
            throw new IllegalArgumentException("fetch is supported only for type shadows");
        }
        if (StringUtils.isNotBlank(request.getSearchMode()) && !isShadow) {
            throw new IllegalArgumentException("searchMode is supported only for type shadows");
        }
        if (!isShadow) {
            if (StringUtils.isNotBlank(request.getResourceOid())
                    || StringUtils.isNotBlank(request.getShadowKind())
                    || StringUtils.isNotBlank(request.getShadowIntent())
                    || StringUtils.isNotBlank(request.getObjectClass())
                    || Boolean.TRUE.equals(request.getExpandResourceObjectTypes())) {
                throw new IllegalArgumentException(
                        "resourceOid, shadowKind, shadowIntent, objectClass, and expandResourceObjectTypes are supported only for type shadows");
            }
        }
    }

    private static String normalizeSearchMode(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "repository";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!"repository".equals(s) && !"resource".equals(s)) {
            throw new IllegalArgumentException("searchMode must be repository or resource");
        }
        return s;
    }

    private static void validateShadowResourceSearch(MidpointMcpSearchRequest request, String mode) {
        boolean expand = Boolean.TRUE.equals(request.getExpandResourceObjectTypes());
        boolean hasRoi = StringUtils.isNotBlank(request.getResourceOid());
        boolean hasKind = StringUtils.isNotBlank(request.getShadowKind());
        boolean hasOc = StringUtils.isNotBlank(request.getObjectClass());
        boolean hasIntentOnly = StringUtils.isNotBlank(request.getShadowIntent());

        if (expand && !"repository".equals(mode)) {
            throw new IllegalArgumentException("expandResourceObjectTypes is only allowed for searchMode repository (default)");
        }
        if (expand && !hasRoi) {
            throw new IllegalArgumentException("expandResourceObjectTypes requires resourceOid");
        }
        if (expand && (hasKind || hasOc || hasIntentOnly)) {
            throw new IllegalArgumentException(
                    "expandResourceObjectTypes cannot be combined with shadowKind, shadowIntent, or objectClass");
        }
        if (hasIntentOnly && !hasKind && !hasOc) {
            throw new IllegalArgumentException("shadowIntent requires shadowKind or objectClass");
        }
        if (hasKind && hasOc) {
            throw new IllegalArgumentException("Use either shadowKind or objectClass, not both");
        }
        if ("resource".equals(mode)) {
            if (expand) {
                throw new IllegalArgumentException("expandResourceObjectTypes is not supported for searchMode resource");
            }
            if (StringUtils.isBlank(request.getResourceOid())) {
                throw new IllegalArgumentException("resourceOid is required when searchMode is resource");
            }
            if (!hasKind && !hasOc) {
                throw new IllegalArgumentException("For searchMode resource, set shadowKind or objectClass");
            }
        } else {
            // repository (default): top-level resourceOid must be paired with a type scope or expand
            if (hasRoi && !expand && !hasKind && !hasOc) {
                throw new MidpointMcpException(
                        "shadow_repository_scope_incomplete",
                        400,
                        "With searchMode repository, resourceOid must be used together with shadowKind or objectClass, "
                                + "or set expandResourceObjectTypes=true. Otherwise omit resourceOid and express resource "
                                + "plus kind/objectClass only in mql/advancedQuery.",
                        shadowSearchCoordinatesHint());
            }
        }
    }

    private MidpointMcpSearchResult searchShadowObjects(MidpointMcpSearchRequest request, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        authorizeRest(RestAuthorizationAction.SEARCH_OBJECTS, task, result);
        String rest = "shadows";
        PrismObjectDefinition<ShadowType> def =
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        List<String> paths = resolvePathsOrBadRequest(request.getReturnAttributes(), rest, def, true);

        String mode = normalizeSearchMode(request.getSearchMode());
        validateShadowResourceSearch(request, mode);

        PagingResolution paging = resolvePaging(request);
        int normalizedLimit = normalizeLimit(paging.limit());
        int normalizedOffset = normalizeOffset(paging.offset());

        String usedMode;
        String translatedMql = null;
        String simpleQuery = null;
        ObjectQuery objectQuery;

        if (StringUtils.isNotBlank(request.getQuery())) {
            usedMode = "simple";
            simpleQuery = request.getQuery().trim();
            objectQuery = createSimpleSearchQuery(ShadowType.class, simpleQuery, normalizedLimit, normalizedOffset);
        } else if (request.getAdvancedQuery() != null) {
            usedMode = "advancedQuery";
            Map<String, MidpointMcpSchemaAttribute> schema =
                    MidpointMcpAdvancedQueryTranslator.schemaMapForValidation(def);
            MidpointMcpAdvancedQuerySpec aq = request.getAdvancedQuery();
            validateOrderByPaths(aq.getOrderBy(), schema, def);
            String mqlFilter = MidpointMcpAdvancedQueryTranslator.translateToMqlFilter(aq, schema);
            translatedMql = mqlFilter.isEmpty() ? null : mqlFilter;
            objectQuery = buildObjectQueryFromMql(ShadowType.class, mqlFilter, normalizedLimit, normalizedOffset, aq.getOrderBy(), def);
        } else if (StringUtils.isNotBlank(request.getMql())) {
            usedMode = "mql";
            translatedMql = request.getMql().trim();
            objectQuery = buildObjectQueryFromMql(ShadowType.class, translatedMql, normalizedLimit, normalizedOffset, List.of(), def);
        } else {
            usedMode = "simple";
            objectQuery = createSimpleSearchQuery(ShadowType.class, null, normalizedLimit, normalizedOffset);
        }

        List<ObjectFilter> expandedBranches = null;
        if ("repository".equals(mode) && Boolean.TRUE.equals(request.getExpandResourceObjectTypes())) {
            expandedBranches = buildExpandedResourceShadowTypeBranches(request.getResourceOid().trim(), task, result);
        } else if ("repository".equals(mode)) {
            objectQuery = augmentShadowRepositoryQueryWithoutExpand(request, objectQuery, task, result);
        }

        if ("resource".equals(mode)) {
            ObjectFilter scope = buildShadowResourceScopeFilter(request);
            ObjectFilter merged = ObjectQueryUtil.filterAndImmutable(objectQuery.getFilter(), scope);
            objectQuery = objectQuery.clone();
            objectQuery.setFilter(merged);
        }

        Collection<SelectorOptions<GetOperationOptions>> searchOpts;
        if ("repository".equals(mode)) {
            searchOpts = GetOperationOptions.noFetch();
        } else {
            searchOpts = shadowResourceSearchOptions();
        }

        SearchResultList<PrismObject<ShadowType>> found;
        int totalCount;
        if (expandedBranches != null) {
            found = searchShadowsExpandedBranches(
                    objectQuery, expandedBranches, normalizedLimit, normalizedOffset, searchOpts, task, result);
            totalCount = countShadowsExpandedBranches(objectQuery, expandedBranches, searchOpts, task, result);
        } else {
            assertShadowSearchCoordinates(objectQuery);
            found = modelService.searchObjects(ShadowType.class, objectQuery, searchOpts, task, result);
            ObjectQuery countQuery = objectQuery.clone();
            countQuery.setPaging(null);
            try {
                Integer c = modelService.countObjects(ShadowType.class, countQuery, searchOpts, task, result);
                totalCount = c != null ? c : -1;
            } catch (Exception e) {
                totalCount = -1;
            }
        }

        boolean perItemResourceFetch = "repository".equals(mode) && Boolean.TRUE.equals(request.getFetch());
        if (perItemResourceFetch) {
            List<PrismObject<ShadowType>> refreshed = new ArrayList<>(found.size());
            for (PrismObject<ShadowType> s : found) {
                refreshed.add(modelService.getObject(ShadowType.class, s.getOid(), shadowLiveGetOptions(), task, result));
            }
            found = new SearchResultList<>(refreshed);
        }

        boolean fetched = "resource".equals(mode) || perItemResourceFetch;
        String source = "resource".equals(mode) ? "resource" : "repository";

        MidpointMcpSearchResult searchResult = new MidpointMcpSearchResult();
        searchResult.setType(rest);
        searchResult.setQuery(simpleQuery);
        searchResult.setUsedQueryMode(usedMode);
        searchResult.setTranslatedMql(translatedMql);
        searchResult.setLimit(normalizedLimit);
        searchResult.setOffset(normalizedOffset);
        searchResult.setTotalCount(totalCount);
        searchResult.setSource(source);
        searchResult.setFetched(fetched);
        searchResult.setSearchMode(mode);

        MidpointMcpAttributeProjector projector =
                new MidpointMcpAttributeProjector(prismContext, modelService, orgStructFunctions);
        List<MidpointMcpSearchItem> items = new ArrayList<>();
        for (PrismObject<ShadowType> object : found) {
            String summary = MidpointMcpAttributeProjector.searchSummary(object.asObjectable());
            MidpointMcpSearchItem item = new MidpointMcpSearchItem();
            var vals = projector.project(object, rest, paths, summary, task, result, false);
            MidpointMcpShadowProjector.putShadowPayload(vals, object.asObjectable());
            item.setValues(vals);
            items.add(item);
        }
        searchResult.setItems(items);
        return searchResult;
    }

    private ObjectFilter buildShadowResourceScopeFilter(MidpointMcpSearchRequest request) {
        String resourceOid = request.getResourceOid().trim();
        try {
            if (StringUtils.isNotBlank(request.getObjectClass())) {
                QName oc = MidpointMcpShadowProjector.parseObjectClass(request.getObjectClass());
                return ObjectQueryUtil.createResourceAndObjectClassFilter(resourceOid, oc);
            }
            ShadowKindType kind = MidpointMcpShadowProjector.parseShadowKindEnum(request.getShadowKind());
            if (StringUtils.isNotBlank(request.getShadowIntent())) {
                return ObjectQueryUtil.createResourceAndKindIntentFilter(
                        resourceOid, kind, request.getShadowIntent().trim());
            }
            return ObjectQueryUtil.createResourceAndKind(resourceOid, kind).getFilter();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Repository shadow search: optional top-level {@code resourceOid} + kind/objectClass merged into the query (AND).
     */
    private ObjectQuery augmentShadowRepositoryQueryWithoutExpand(
            MidpointMcpSearchRequest request, ObjectQuery objectQuery, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        ObjectQuery q = objectQuery;
        if (StringUtils.isNotBlank(request.getResourceOid())
                && (StringUtils.isNotBlank(request.getShadowKind())
                        || StringUtils.isNotBlank(request.getObjectClass()))) {
            ObjectFilter scope = buildShadowResourceScopeFilter(request);
            q = andShadowFilter(q, scope);
        }
        return q;
    }

    private static ObjectQuery andShadowFilter(ObjectQuery objectQuery, ObjectFilter extra) {
        ObjectFilter merged = ObjectQueryUtil.filterAndImmutable(objectQuery.getFilter(), extra);
        ObjectQuery clone = objectQuery.clone();
        clone.setFilter(merged);
        return clone;
    }

    /**
     * One filter per schemaHandling object type (deduped). midPoint does not accept a top-level OR for shadow
     * repository coordinates, so callers run one search per branch and merge.
     */
    private List<ObjectFilter> buildExpandedResourceShadowTypeBranches(String resourceOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        PrismObject<ResourceType> resource = modelService.getObject(
                ResourceType.class, resourceOid, GetOperationOptions.createReadOnlyCollection(), task, result);
        SchemaHandlingType handling = resource.asObjectable().getSchemaHandling();
        if (handling == null || handling.getObjectType().isEmpty()) {
            throw new MidpointMcpException(
                    "shadow_expand_no_object_types",
                    400,
                    "Resource has no schemaHandling object types to expand: " + resourceOid,
                    "Define object types on the resource or use shadowKind / objectClass instead of expandResourceObjectTypes.");
        }
        List<ObjectFilter> branches = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ResourceObjectTypeDefinitionType def : handling.getObjectType()) {
            ShadowKindType kind = def.getKind();
            if (kind == null) {
                continue;
            }
            String intent = def.getIntent();
            String key = kind.toString() + "|" + (intent != null ? intent : "");
            if (!seen.add(key)) {
                continue;
            }
            ObjectFilter branch;
            if (StringUtils.isNotBlank(intent)) {
                branch = ObjectQueryUtil.createResourceAndKindIntentFilter(resourceOid, kind, intent.trim());
            } else {
                branch = ObjectQueryUtil.createResourceAndKind(resourceOid, kind).getFilter();
            }
            branches.add(branch);
        }
        if (branches.isEmpty()) {
            throw new MidpointMcpException(
                    "shadow_expand_no_kinds",
                    400,
                    "Resource schemaHandling object types have no kind: " + resourceOid,
                    "Use shadowKind / objectClass or fix resource schema handling.");
        }
        return branches;
    }

    private SearchResultList<PrismObject<ShadowType>> searchShadowsExpandedBranches(
            ObjectQuery baseQuery,
            List<ObjectFilter> typeBranches,
            int limit,
            int offset,
            Collection<SelectorOptions<GetOperationOptions>> searchOpts,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        ObjectFilter userFilter = baseQuery != null ? baseQuery.getFilter() : null;
        Map<String, PrismObject<ShadowType>> byOid = new LinkedHashMap<>();
        for (ObjectFilter branch : typeBranches) {
            ObjectFilter merged = ObjectQueryUtil.filterAndImmutable(userFilter, branch);
            assertShadowSearchCoordinates(prismContext.queryFactory().createQuery(merged));
            ObjectQuery branchQuery = prismContext.queryFactory().createQuery(merged);
            branchQuery.setPaging(prismContext.queryFactory().createPaging(0, EXPAND_PER_BRANCH_MAX));
            SearchResultList<PrismObject<ShadowType>> part =
                    modelService.searchObjects(ShadowType.class, branchQuery, searchOpts, task, result);
            for (PrismObject<ShadowType> s : part) {
                byOid.putIfAbsent(s.getOid(), s);
            }
        }
        List<PrismObject<ShadowType>> sorted = new ArrayList<>(byOid.values());
        sorted.sort(Comparator.comparing(
                o -> o.getName() != null && o.getName().getOrig() != null ? o.getName().getOrig() : "",
                String.CASE_INSENSITIVE_ORDER));
        int from = Math.min(offset, sorted.size());
        int to = Math.min(offset + limit, sorted.size());
        return new SearchResultList<>(sorted.subList(from, to));
    }

    private int countShadowsExpandedBranches(
            ObjectQuery baseQuery,
            List<ObjectFilter> typeBranches,
            Collection<SelectorOptions<GetOperationOptions>> searchOpts,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        ObjectFilter userFilter = baseQuery != null ? baseQuery.getFilter() : null;
        int sum = 0;
        for (ObjectFilter branch : typeBranches) {
            ObjectFilter merged = ObjectQueryUtil.filterAndImmutable(userFilter, branch);
            assertShadowSearchCoordinates(prismContext.queryFactory().createQuery(merged));
            ObjectQuery countQuery = prismContext.queryFactory().createQuery(merged);
            Integer c = modelService.countObjects(ShadowType.class, countQuery, searchOpts, task, result);
            sum += c != null ? c : 0;
        }
        return sum;
    }

    private void assertShadowSearchCoordinates(ObjectQuery objectQuery) throws SchemaException {
        ObjectFilter filter = objectQuery != null ? objectQuery.getFilter() : null;
        try {
            ResourceOperationCoordinates roc = ObjectQueryUtil.getOperationCoordinates(filter);
            roc.checkNotResourceScoped();
        } catch (SchemaException e) {
            throw new MidpointMcpException(
                    "shadow_query_invalid_coordinates",
                    400,
                    e.getMessage(),
                    shadowSearchCoordinatesHint(),
                    e);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("cannot be applied to the whole resource")) {
                throw new MidpointMcpException(
                        "shadow_query_not_type_scoped",
                        400,
                        "Shadow search must constrain kind or objectClass (midPoint does not allow resource-wide shadow search).",
                        shadowSearchCoordinatesHint(),
                        e);
            }
            throw e;
        }
    }

    private static String shadowSearchCoordinatesHint() {
        return "Examples (MQL): resourceRef matches (oid = \"<resource-uuid>\") and kind = \"ACCOUNT\"; "
                + "or add top-level resourceOid + shadowKind; or resourceOid + expandResourceObjectTypes=true "
                + "to OR all schemaHandling object types. Advanced filters: path resourceRef with op eq and OID value "
                + "(not resourceRef.oid).";
    }

    private List<SelectorOptions<GetOperationOptions>> shadowLiveGetOptions() {
        List<SelectorOptions<GetOperationOptions>> opts = new ArrayList<>(3);
        opts.add(SelectorOptions.create(new GetOperationOptions().forceRefresh(Boolean.TRUE)));
        opts.add(SelectorOptions.create(
                prismContext.toUniformPath(ShadowType.F_ATTRIBUTES), GetOperationOptions.createRetrieve()));
        opts.add(SelectorOptions.create(
                prismContext.toUniformPath(ShadowType.F_ASSOCIATIONS), GetOperationOptions.createRetrieve()));
        return opts;
    }

    private List<SelectorOptions<GetOperationOptions>> shadowResourceSearchOptions() {
        return List.of(
                SelectorOptions.create(
                        prismContext.toUniformPath(ShadowType.F_ATTRIBUTES), GetOperationOptions.createRetrieve()),
                SelectorOptions.create(
                        prismContext.toUniformPath(ShadowType.F_ASSOCIATIONS), GetOperationOptions.createRetrieve()));
    }

    private static String liveVsRepositoryNote(ShadowType live, ShadowType repo) {
        List<String> diffs = new ArrayList<>();
        if (!Objects.equals(live.isDead(), repo.isDead())) {
            diffs.add("dead: repository=" + repo.isDead() + ", live view=" + live.isDead());
        }
        if (!Objects.equals(live.isExists(), repo.isExists())) {
            diffs.add("exists: repository=" + repo.isExists() + ", live view=" + live.isExists());
        }
        if (diffs.isEmpty()) {
            return null;
        }
        return String.join("; ", diffs);
    }

    private static void validateSearchModes(MidpointMcpSearchRequest request) {
        int n = 0;
        if (StringUtils.isNotBlank(request.getQuery())) {
            n++;
        }
        if (StringUtils.isNotBlank(request.getMql())) {
            n++;
        }
        if (request.getAdvancedQuery() != null) {
            n++;
        }
        if (n > 1) {
            throw new IllegalArgumentException("Use only one of: query (simple name prefix), advancedQuery, or mql");
        }
    }

    private <O extends ObjectType> MidpointMcpSearchResult searchObjectsForClass(
            Class<O> clazz,
            MidpointMcpSearchRequest request,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
                    ConfigurationException, ExpressionEvaluationException {

        authorizeRest(RestAuthorizationAction.SEARCH_OBJECTS, task, result);
        String rest = ObjectTypes.getRestTypeFromClass(clazz);
        PrismObjectDefinition<O> def =
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        List<String> paths = resolvePathsOrBadRequest(request.getReturnAttributes(), rest, def, true);

        PagingResolution paging = resolvePaging(request);
        int normalizedLimit = normalizeLimit(paging.limit());
        int normalizedOffset = normalizeOffset(paging.offset());

        String usedMode;
        String translatedMql = null;
        String simpleQuery = null;
        ObjectQuery objectQuery;

        if (StringUtils.isNotBlank(request.getQuery())) {
            usedMode = "simple";
            simpleQuery = request.getQuery().trim();
            objectQuery = createSimpleSearchQuery(clazz, simpleQuery, normalizedLimit, normalizedOffset);
        } else if (request.getAdvancedQuery() != null) {
            usedMode = "advancedQuery";
            Map<String, MidpointMcpSchemaAttribute> schema =
                    MidpointMcpAdvancedQueryTranslator.schemaMapForValidation(def);
            MidpointMcpAdvancedQuerySpec aq = request.getAdvancedQuery();
            validateOrderByPaths(aq.getOrderBy(), schema, def);
            String mqlFilter = MidpointMcpAdvancedQueryTranslator.translateToMqlFilter(aq, schema);
            translatedMql = mqlFilter.isEmpty() ? null : mqlFilter;
            objectQuery = buildObjectQueryFromMql(clazz, mqlFilter, normalizedLimit, normalizedOffset, aq.getOrderBy(), def);
        } else if (StringUtils.isNotBlank(request.getMql())) {
            usedMode = "mql";
            translatedMql = request.getMql().trim();
            objectQuery = buildObjectQueryFromMql(clazz, translatedMql, normalizedLimit, normalizedOffset, List.of(), def);
        } else {
            usedMode = "simple";
            objectQuery = createSimpleSearchQuery(clazz, null, normalizedLimit, normalizedOffset);
        }

        SearchResultList<PrismObject<O>> found = modelService.searchObjects(clazz, objectQuery, null, task, result);

        ObjectQuery countQuery = objectQuery.clone();
        countQuery.setPaging(null);
        int totalCount = modelService.countObjects(clazz, countQuery, null, task, result);

        MidpointMcpSearchResult searchResult = new MidpointMcpSearchResult();
        searchResult.setType(rest);
        searchResult.setQuery(simpleQuery);
        searchResult.setUsedQueryMode(usedMode);
        searchResult.setTranslatedMql(translatedMql);
        searchResult.setLimit(normalizedLimit);
        searchResult.setOffset(normalizedOffset);
        searchResult.setTotalCount(totalCount);

        MidpointMcpAttributeProjector projector =
                new MidpointMcpAttributeProjector(prismContext, modelService, orgStructFunctions);
        List<MidpointMcpSearchItem> items = new ArrayList<>();
        for (PrismObject<O> object : found) {
            String summary = MidpointMcpAttributeProjector.searchSummary(object.asObjectable());
            MidpointMcpSearchItem item = new MidpointMcpSearchItem();
            item.setValues(projector.project(object, rest, paths, summary, task, result, false));
            items.add(item);
        }
        searchResult.setItems(items);
        return searchResult;
    }

    private record PagingResolution(Integer limit, Integer offset) {}

    private PagingResolution resolvePaging(MidpointMcpSearchRequest request) {
        MidpointMcpAdvancedQuerySpec aq = request.getAdvancedQuery();
        if (aq != null && aq.getPaging() != null) {
            MidpointMcpAdvancedPagingSpec p = aq.getPaging();
            Integer limit = p.getLimit() != null ? p.getLimit() : request.getLimit();
            Integer offset = p.getOffset() != null ? p.getOffset() : request.getOffset();
            return new PagingResolution(limit, offset);
        }
        return new PagingResolution(request.getLimit(), request.getOffset());
    }

    private void validateOrderByPaths(
            List<MidpointMcpAdvancedOrderBySpec> orderBy,
            Map<String, MidpointMcpSchemaAttribute> schema,
            PrismObjectDefinition<?> objectDef) {
        if (orderBy == null || orderBy.isEmpty()) {
            return;
        }
        for (MidpointMcpAdvancedOrderBySpec ob : orderBy) {
            if (ob == null || StringUtils.isBlank(ob.getPath())) {
                throw new IllegalArgumentException("orderBy.path is required for each entry");
            }
            String path = ob.getPath().trim();
            if (!schema.containsKey(path)) {
                throw new IllegalArgumentException("Unknown orderBy path '" + path + "' for this object type");
            }
            if (objectDef != null
                    && CaseType.class.equals(objectDef.getCompileTimeClass())
                    && MidpointMcpCaseProjector.isOrderByBlockedForCases(path)) {
                throw new IllegalArgumentException(
                        "orderBy.path '" + path + "' is not supported for cases; use a repository-backed Prism path");
            }
        }
    }

    private <O extends ObjectType> ObjectQuery buildObjectQueryFromMql(
            Class<O> clazz,
            String mqlFilter,
            int limit,
            int offset,
            List<MidpointMcpAdvancedOrderBySpec> orderBy,
            PrismObjectDefinition<O> objDef) {

        ObjectQuery oq;
        if (StringUtils.isBlank(mqlFilter)) {
            oq = prismContext.queryFor(clazz).build();
        } else {
            ObjectFilter filter;
            try {
                filter = prismContext.createQueryParser().parseFilter(clazz, mqlFilter);
            } catch (SchemaException e) {
                throw new MidpointMcpException("bad_request", 400, "MQL could not be parsed.", e);
            }
            oq = prismContext.queryFactory().createQuery(filter);
        }
        ObjectPaging paging = prismContext.queryFactory().createPaging(offset, limit);
        if (orderBy != null) {
            for (MidpointMcpAdvancedOrderBySpec ob : orderBy) {
                if (ob == null || StringUtils.isBlank(ob.getPath())) {
                    continue;
                }
                var itemPath = MidpointMcpAdvancedQueryTranslator.dotPathToItemPath(objDef, ob.getPath().trim());
                OrderDirection dir = parseOrderDirection(ob.getDirection());
                paging.addOrderingInstruction(itemPath, dir);
            }
        }
        oq.setPaging(paging);
        return oq;
    }

    private static OrderDirection parseOrderDirection(String direction) {
        if (StringUtils.isBlank(direction)) {
            return OrderDirection.ASCENDING;
        }
        String d = direction.trim().toLowerCase(Locale.ROOT);
        if ("desc".equals(d) || "descending".equals(d)) {
            return OrderDirection.DESCENDING;
        }
        if ("asc".equals(d) || "ascending".equals(d)) {
            return OrderDirection.ASCENDING;
        }
        throw new IllegalArgumentException("orderBy.direction must be 'asc' or 'desc'");
    }

    private List<String> resolvePathsOrBadRequest(
            List<String> returnAttributes,
            String restType,
            PrismObjectDefinition<?> def,
            boolean useSearchDefaults) {
        try {
            return MidpointMcpAttributeProjector.resolveRequestedPaths(
                    returnAttributes, restType, def, useSearchDefaults);
        } catch (IllegalArgumentException e) {
            throw new MidpointMcpException("bad_request", 400, e.getMessage(), e);
        }
    }

    @Override
    public MidpointMcpTypeSchemaView describeObjectTypeSchema(
            String objectType, Integer maxDepth, Task task, OperationResult result) {
        return translateExceptions(() -> {
            Class<? extends ObjectType> clazz = resolveMcpObjectClass(objectType);
            authorizeModelReadForType(clazz, task, result);
            PrismObjectDefinition<? extends ObjectType> objDef =
                    prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
            int depthLimit = resolveSchemaMaxDepth(maxDepth);
            MidpointMcpTypeSchemaView view = new MidpointMcpTypeSchemaView();
            view.setType(ObjectTypes.getRestTypeFromClass(clazz));
            List<MidpointMcpSchemaAttribute> attrs =
                    new ArrayList<>(MidpointMcpSchemaFlattener.flatten(objDef, depthLimit));
            if (CaseType.class.equals(clazz)) {
                Map<String, MidpointMcpSchemaAttribute> byPath = new LinkedHashMap<>();
                for (MidpointMcpSchemaAttribute a : attrs) {
                    byPath.put(a.getPath(), a);
                }
                for (MidpointMcpSchemaAttribute syn : MidpointMcpCaseSchema.syntheticDescribeAttributes()) {
                    byPath.putIfAbsent(syn.getPath(), syn);
                }
                attrs = new ArrayList<>(byPath.values());
            }
            view.setAttributes(attrs);
            return view;
        });
    }

    private static int resolveSchemaMaxDepth(Integer maxDepth) {
        if (maxDepth == null) {
            return 2;
        }
        if (maxDepth == 0) {
            return Integer.MAX_VALUE;
        }
        return maxDepth;
    }

    private Class<? extends ObjectType> resolveMcpObjectClass(String objectTypeRest) {
        if (StringUtils.isBlank(objectTypeRest)) {
            throw new IllegalArgumentException("type is required");
        }
        String normalized = objectTypeRest.trim().toLowerCase(Locale.ROOT);
        if (!MCP_OBJECT_REST_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported type '" + objectTypeRest.trim()
                    + "'; allowed: " + String.join(", ", MCP_OBJECT_REST_TYPES));
        }
        return ObjectTypes.getClassFromRestType(normalized);
    }

    private <O extends ObjectType> ObjectQuery createSimpleSearchQuery(Class<O> type, String query, int limit, int offset) {
        if (CaseType.class.equals(type)) {
            return createCaseSimpleSearchQuery(query, limit, offset);
        }
        if (StringUtils.isNotBlank(query)) {
            return prismContext.queryFor(type)
                    .item(ObjectType.F_NAME)
                    .startsWith(query)
                    .offset(offset)
                    .maxSize(limit)
                    .build();
        }

        return prismContext.queryFor(type)
                .offset(offset)
                .maxSize(limit)
                .build();
    }

    private ObjectQuery createCaseSimpleSearchQuery(String query, int limit, int offset) {
        if (StringUtils.isBlank(query)) {
            return prismContext.queryFor(CaseType.class)
                    .offset(offset)
                    .maxSize(limit)
                    .build();
        }
        String q = query.trim();
        return prismContext.queryFor(CaseType.class)
                .item(ObjectType.F_NAME)
                .contains(q)
                .or()
                .item(ObjectType.F_DESCRIPTION)
                .contains(q)
                .or()
                .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_NAME)
                .contains(q)
                .offset(offset)
                .maxSize(limit)
                .build();
    }

    private void authorizeRest(RestAuthorizationAction action, Task task, OperationResult result) {
        translateExceptions(() -> {
            securityEnforcer.authorize(action.getUri(), task, result);
            return null;
        });
    }

    private void authorizeModelReadForType(
            Class<? extends ObjectType> clazz, Task task, OperationResult result) {
        translateExceptions(() -> {
            securityEnforcer.authorize(
                    ModelAuthorizationAction.READ.getUrl(),
                    null,
                    AuthorizationParameters.Builder.buildObject(prismContext.createObject(clazz)),
                    task,
                    result);
            return null;
        });
    }

    private <T> T translateExceptions(CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (MidpointMcpException e) {
            throw e;
        } catch (ObjectNotFoundException e) {
            throw new MidpointMcpException("not_found", 404, McpPublicErrorMessages.NOT_FOUND, e);
        } catch (SecurityViolationException e) {
            throw new MidpointMcpException("forbidden", 403, McpPublicErrorMessages.ACCESS_DENIED, e);
        } catch (IllegalArgumentException e) {
            throw new MidpointMcpException("bad_request", 400, e.getMessage(), e);
        } catch (SchemaException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            throw new MidpointMcpException(
                    "internal_error", 500, McpPublicErrorMessages.INTERNAL_ERROR, e);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            throw new MidpointMcpException("bad_request", 400, "limit must be greater than 0");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new MidpointMcpException("bad_request", 400, "offset must be 0 or greater");
        }
        return offset;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {

        T get() throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
                ConfigurationException, ExpressionEvaluationException;
    }
}
