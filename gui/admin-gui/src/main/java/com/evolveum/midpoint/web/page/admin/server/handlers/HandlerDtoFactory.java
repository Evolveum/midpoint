/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.*;

/**
 * @author mederly
 */
public class HandlerDtoFactory {
    public static HandlerDtoFactory instance() {
        return new HandlerDtoFactory();        // TODO
    }

    public HandlerDto createDtoForTask(PageBase pageBase, Task opTask, OperationResult thisOpResult) {
        return new LiveSyncHandlerDto();
//        if (taskDto.isLiveSync()) {
//            return new LiveSyncHandlerDto(pageBase, opTask, thisOpResult);
//        } else if (taskDto.isImportAccounts()) {
//            return new ResourceRelatedHandlerDto(taskDto, pageBase, opTask, thisOpResult);
//        } else if (taskDto.isReconciliation()) {
//            return new ResourceRelatedHandlerDto(taskDto, pageBase, opTask, thisOpResult);
//        } else if (taskDto.isTriggerScanner() || taskDto.isFocusValidityScanner()) {
//            return new ScannerHandlerDto(taskDto);
//        } else if (taskDto.isBulkAction()) {
//            return new ScriptExecutionHandlerDto(taskDto);
//        } else if (taskDto.isDelete()) {
//            return new DeleteHandlerDto(taskDto);
//        } else if (taskDto.isRecomputation()) {
//            return new RecomputeHandlerDto(taskDto);
//        } else if (taskDto.isExecuteChanges()) {
//            return new ExecuteChangesHandlerDto(taskDto);
//        } else if (taskDto.isShadowIntegrityCheck()) {
//            return new GenericHandlerDto(taskDto, Arrays.asList(
//                    extensionItem(MODEL_EXTENSION_OBJECT_QUERY, QueryType.class),
//                    extensionItem(MODEL_EXTENSION_DIAGNOSE, String.class),
//                    extensionItem(MODEL_EXTENSION_FIX, String.class),
//                    extensionItem(MODEL_EXTENSION_DRY_RUN, Boolean.class),
//                    extensionItem(MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER, String.class),
//                    extensionItem(MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY, Boolean.class)), pageBase);
//        } else if (taskDto.isCleanup()) {
//            return new GenericHandlerDto(taskDto, Collections.singletonList(
//                    extensionItem(MODEL_EXTENSION_CLEANUP_POLICIES, CleanupPoliciesType.class)), pageBase);
//        } else if (taskDto.isNoOp()) {
//            return new GenericHandlerDto(taskDto, Arrays.asList(
//                    extensionItem(SchemaConstants.NOOP_STEPS_QNAME, Integer.class),
//                    extensionItem(SchemaConstants.NOOP_DELAY_QNAME, Integer.class)), pageBase);
//        } else if (taskDto.isReportCreate()) {
//            return new ReportCreateHandlerDto(taskDto);
//        } else if (taskDto.isJdbcPing()) {
//            return new GenericHandlerDto(Arrays.asList(
//                    extensionItem(SchemaConstants.JDBC_PING_TESTS_QNAME, Integer.class),
//                    extensionItem(SchemaConstants.JDBC_PING_INTERVAL_QNAME, Integer.class),
//                    extensionItem(SchemaConstants.JDBC_PING_TEST_QUERY_QNAME, String.class),
//                    extensionItem(SchemaConstants.JDBC_PING_JDBC_URL_QNAME, String.class),
//                    extensionItem(SchemaConstants.JDBC_PING_JDBC_USERNAME_QNAME, String.class),
//                    //item(SchemaConstants.JDBC_PING_JDBC_PASSWORD_QNAME, String.class),
//                    extensionItem(SchemaConstants.JDBC_PING_LOG_ON_INFO_LEVEL_QNAME, Boolean.class)
//            ), pageBase);
//        } else {
//            return new HandlerDto(taskDto);
//        }
    }
}
