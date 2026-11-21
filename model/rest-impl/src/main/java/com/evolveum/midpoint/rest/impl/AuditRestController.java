/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.rest.impl;

import static com.evolveum.midpoint.security.api.RestAuthorizationAction.SEARCH_AUDIT_RECORDS;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthorizationAction;
import com.evolveum.midpoint.security.api.RestHandlerMethod;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * Read-only access to the audit log (audit event records).
 *
 * The endpoint is meant for automated clients (integrations, monitoring, AI agents), so it is deliberately
 * restrictive; see {@link AuditSearchGuard} for the limits applied to queries and {@link AuditRecordRedactor}
 * for what data is returned.
 *
 * Note about authorizations: the data are read via {@link ModelAuditService}, which requires
 * {@link ModelAuthorizationAction#AUDIT_READ} (`authorization-model-3#auditRead`). To avoid the need of having
 * `rest-3#all` authorization to use this endpoint, there is a specific REST-level authorization
 * {@link RestAuthorizationAction#SEARCH_AUDIT_RECORDS} (`authorization-rest-3#searchAuditRecords`).
 * So, a user calling this endpoint needs just the above two authorizations.
 */
@RestController
@RequestMapping({ "/ws/rest/audit", "/rest/model/audit", "/api/model/audit" })
public class AuditRestController extends AbstractRestController implements InitializingBean {

    private static final String OP_SEARCH_AUDIT_RECORDS = "searchAuditRecords";

    @Autowired private ModelAuditService modelAuditService;
    @Autowired private MidpointConfiguration midpointConfiguration;

    private AuditSearchGuard.Limits limits = AuditSearchGuard.Limits.DEFAULT;

    @Override
    public void afterPropertiesSet() {
        limits = AuditSearchGuard.Limits.fromConfiguration(
                midpointConfiguration.getConfiguration(MidpointConfiguration.WEB_APP_CONFIGURATION));
        logger.debug("Audit REST search limits: {}", limits);
    }

    /**
     * Searches for audit event records.
     *
     * @param include Additional parts of the records to return: `delta`, `delta/executionResult`, `payload`.
     * @param returnTotalCount If true, the total count of matching records is returned in the `count` attribute.
     * @param queryType The query; paging with `maxSize` is required. See {@link AuditSearchGuard}.
     */
    @RestHandlerMethod(authorization = SEARCH_AUDIT_RECORDS)
    @PostMapping("/search")
    public ResponseEntity<?> searchAuditRecords(
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "returnTotalCount", required = false) Boolean returnTotalCount,
            @RequestBody QueryType queryType) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, OP_SEARCH_AUDIT_RECORDS);

        ResponseEntity<?> response;
        try {
            if (!modelAuditService.supportsRetrieval()) {
                result.recordFatalError("There is no audit service supporting retrieval of audit records configured.");
                response = createErrorResponseBuilder(HttpStatus.NOT_IMPLEMENTED, result);
            } else {
                response = searchAuditRecordsInternal(include, Boolean.TRUE.equals(returnTotalCount), queryType, task, result);
            }
        } catch (AuditSearchGuard.RejectedException e) {
            logger.debug("Audit search request rejected: {}", e.getMessage());
            result.recordFatalError(e.getMessage(), e);
            response = createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.close();
        finishRequest(task, result);
        return response;
    }

    private ResponseEntity<?> searchAuditRecordsInternal(
            List<String> include, boolean returnTotalCount, QueryType queryType, Task task, OperationResult result)
            throws Exception {

        AuditRecordRedactor.Includes includes = AuditRecordRedactor.Includes.parse(include);

        ObjectQuery query;
        try {
            query = prismContext.getQueryConverter().createObjectQuery(AuditEventRecordType.class, queryType);
        } catch (SchemaException | RuntimeException e) {
            // The query comes from the client, so a failure to convert it is a client error. Besides SchemaException,
            // the query language parser throws runtime exceptions for some invalid input (an unknown enum value,
            // an undeclared namespace prefix, a reference target filter without the target type).
            logger.debug("Audit search query could not be converted", e);
            throw new AuditSearchGuard.RejectedException("Invalid query: "
                    + (e.getMessage() != null ? e.getMessage() : "the query could not be parsed ("
                    + e.getClass().getSimpleName() + ")"), e);
        }

        AuditSearchGuard guard = new AuditSearchGuard(limits, prismContext.queryFactory());
        boolean anchored = guard.checkAndNormalize(query);
        if (returnTotalCount && !anchored) {
            throw new AuditSearchGuard.RejectedException(
                    "Returning the total count requires a filter with an indexed condition (e.g. on 'timestamp').");
        }

        SearchResultList<AuditEventRecordType> records =
                modelAuditService.searchObjects(query, includes.toGetOperationOptions(), task, result);

        int removed = new AuditRecordRedactor(prismContext).redact(records, includes);
        if (removed > 0) {
            OperationResult redactionResult = result.createSubresult(OP_SEARCH_AUDIT_RECORDS + ".redaction");
            redactionResult.recordWarning("Content of " + removed + " delta(s) was not returned, "
                    + "because protected data in it could not be safely removed.");
            redactionResult.close();
        }

        AuditEventRecordListType list = new AuditEventRecordListType();
        list.getObject().addAll(records);

        if (returnTotalCount) {
            ObjectQuery countQuery = query.clone();
            countQuery.setPaging(null);
            list.setCount(modelAuditService.countObjects(countQuery, null, task, result));
        }

        return createResponse(HttpStatus.OK, list, result, true);
    }
}
