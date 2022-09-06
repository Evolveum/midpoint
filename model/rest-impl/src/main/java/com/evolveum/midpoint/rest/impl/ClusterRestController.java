/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.authentication.api.config.NodeAuthenticationToken;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.util.ClusterServiceConsts;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.schema.DefinitionProcessingOption;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.TerminateSessionEventType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchedulerInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * REST service used for inter-cluster communication.
 * <p>
 * These methods are NOT to be called generally by clients.
 * They are to be called internally by midPoint running on other cluster nodes.
 * <p>
 * So the usual form of authentication will be CLUSTER (a.k.a. node authentication).
 * However, for diagnostic purposes we might allow also administrator access sometimes in the future.
 */
@RestController
@RequestMapping({ "/ws/cluster", "/rest/cluster", "/api/cluster" })
public class ClusterRestController extends AbstractRestController {

    public static final String CLASS_DOT = ClusterRestController.class.getName() + ".";

    private static final String OPERATION_EXECUTE_CLUSTER_CACHE_INVALIDATION_EVENT = CLASS_DOT + "executeClusterCacheInvalidationEvent";
    private static final String OPERATION_EXECUTE_CLUSTER_TERMINATE_SESSION_EVENT = CLASS_DOT + "executeClusterTerminateSessionEvent";
    private static final String OPERATION_GET_LOCAL_SCHEDULER_INFORMATION = CLASS_DOT + "getLocalSchedulerInformation";
    private static final String OPERATION_STOP_LOCAL_SCHEDULER = CLASS_DOT + "stopLocalScheduler";
    private static final String OPERATION_START_LOCAL_SCHEDULER = CLASS_DOT + "startLocalScheduler";
    private static final String OPERATION_STOP_LOCAL_TASK = CLASS_DOT + "stopLocalTask";

    private static final String OPERATION_GET_REPORT_FILE = CLASS_DOT + "getReportFile";
    private static final String OPERATION_DELETE_REPORT_FILE = CLASS_DOT + "deleteReportFile";

    private static final String OPERATION_GET_TASK = CLASS_DOT + "getTask";

    private static final String EXPORT_DIR = "export/";

    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private GuiProfiledPrincipalManager focusProfileService;
    @Autowired private CacheDispatcher cacheDispatcher;

    public ClusterRestController() {
        // nothing to do
    }

    @PostMapping(ClusterServiceConsts.EVENT_INVALIDATION)
    public ResponseEntity<?> executeClusterCacheInvalidationEvent() {
        return executeClusterCacheInvalidationEvent(null, null);
    }

    @PostMapping(ClusterServiceConsts.EVENT_INVALIDATION + "{type}")
    public ResponseEntity<?> executeClusterCacheInvalidationEvent(
            @PathVariable("type") String type) {
        return executeClusterCacheInvalidationEvent(type, null);
    }

    @PostMapping(ClusterServiceConsts.EVENT_INVALIDATION + "{type}/{oid}")
    public ResponseEntity<?> executeClusterCacheInvalidationEvent(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_EXECUTE_CLUSTER_CACHE_INVALIDATION_EVENT);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();

            Class<? extends ObjectType> clazz = type != null ? ObjectTypes.getClassFromRestType(type) : null;

            // clusterwide is false: we got this from another node so we don't need to redistribute it
            cacheDispatcher.dispatchInvalidation(clazz, oid, false, new CacheInvalidationContext(true, null));

            result.recordSuccess();
            response = createResponse(HttpStatus.OK, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        finishRequest(task, result);
        return response;
    }

    @PostMapping(ClusterServiceConsts.EVENT_TERMINATE_SESSION)
    public ResponseEntity<?> executeClusterTerminateSessionEvent(
            @RequestBody TerminateSessionEventType event) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_EXECUTE_CLUSTER_TERMINATE_SESSION_EVENT);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();

            focusProfileService.terminateLocalSessions(TerminateSessionEvent.fromEventType(event));

            result.recordSuccess();
            response = createResponse(HttpStatus.OK, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        finishRequest(task, result);
        return response;
    }

    @GetMapping(ClusterServiceConsts.EVENT_LIST_USER_SESSION)
    public ResponseEntity<?> listUserSession() {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_GET_LOCAL_SCHEDULER_INFORMATION);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            List<UserSessionManagementType> principals = focusProfileService.getLocalLoggedInPrincipals();

            UserSessionManagementListType list = new UserSessionManagementListType();
            list.getSession().addAll(principals);

            response = createResponse(HttpStatus.OK, list, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @GetMapping(TaskConstants.GET_LOCAL_SCHEDULER_INFORMATION_REST_PATH)
    public ResponseEntity<?> getLocalSchedulerInformation() {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_GET_LOCAL_SCHEDULER_INFORMATION);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            SchedulerInformationType schedulerInformation = taskManager.getLocalSchedulerInformation(result);
            response = createResponse(HttpStatus.OK, schedulerInformation, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @PostMapping(TaskConstants.STOP_LOCAL_SCHEDULER_REST_PATH)
    public ResponseEntity<?> stopLocalScheduler() {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_STOP_LOCAL_SCHEDULER);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            taskManager.stopLocalScheduler(result);
            response = createResponse(HttpStatus.OK, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @PostMapping(TaskConstants.START_LOCAL_SCHEDULER_REST_PATH)
    public ResponseEntity<?> startLocalScheduler() {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_START_LOCAL_SCHEDULER);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            taskManager.startLocalScheduler(result);
            response = createResponse(HttpStatus.OK, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @PostMapping(TaskConstants.STOP_LOCAL_TASK_REST_PATH_PREFIX + "{oid}" + TaskConstants.STOP_LOCAL_TASK_REST_PATH_SUFFIX)
    public ResponseEntity<?> stopLocalTask(@PathVariable("oid") String oid) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_STOP_LOCAL_TASK);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            taskManager.stopLocalTaskRunInStandardWay(oid, result);
            response = createResponse(HttpStatus.OK, result);
        } catch (Throwable t) {
            response = handleException(result, t);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @GetMapping(
            value = ModelPublicConstants.CLUSTER_REPORT_FILE_PATH,
            produces = { "application/octet-stream", MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getReportFile(
            @RequestParam(ModelPublicConstants.CLUSTER_REPORT_FILE_FILENAME_PARAMETER) String fileName) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_GET_REPORT_FILE);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            FileResolution resolution = resolveFile(fileName);
            if (resolution.status == null) {
                // we are only interested in the content, not in its type nor length
                // TODO how to test this?
                response = ResponseEntity.ok()
                        // Explicitly needed, because Spring would change content type to "*"
                        // even if application/octet-stream is in produces section
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(FileUtils.readFileToByteArray(resolution.file));
            } else {
                response = ResponseEntity.status(resolution.status).build();
            }
            result.computeStatus();
        } catch (Throwable t) {
            response = handleException(null, t); // we don't return the operation result
        }
        finishRequest(task, result);
        return response;
    }

    @DeleteMapping(ModelPublicConstants.CLUSTER_REPORT_FILE_PATH)
    public ResponseEntity<?> deleteReportFile(
            @RequestParam(ModelPublicConstants.CLUSTER_REPORT_FILE_FILENAME_PARAMETER) String fileName) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_DELETE_REPORT_FILE);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            FileResolution resolution = resolveFile(fileName);
            if (resolution.status == null) {
                if (!resolution.file.delete()) {
                    logger.warn("Couldn't delete report output file {}", resolution.file);
                }
                response = ResponseEntity.ok().build();
            } else {
                response = ResponseEntity.status(resolution.status).build();
            }
            result.computeStatus();
        } catch (Throwable t) {
            response = handleException(null, t); // we don't return the operation result
        }
        finishRequest(task, result);
        return response;
    }

    @GetMapping(value = TaskConstants.GET_TASK_REST_PATH + "{oid}")
    public ResponseEntity<?> getTask(@PathVariable("oid") String oid, @RequestParam(value = "include", required = false) List<String> include) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, OPERATION_GET_TASK);

        ResponseEntity<?> response;
        try {
            checkNodeAuthentication();
            Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.fromRestOptions(null, include, null, null, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            PrismObject<TaskType> taskType = taskManager.getObject(TaskType.class, oid, options, result);
            response = ResponseEntity.ok(taskType);
            result.computeStatus();
        } catch (Throwable t) {
            response = handleException(null, t); // we don't return the operation result
        }
        finishRequest(task, result);
        return response;
    }

    static class FileResolution {
        File file;
        HttpStatus status;
    }

    private FileResolution resolveFile(String fileName) {
        FileResolution rv = new FileResolution();
        if (forbiddenFileName(fileName)) {
            logger.warn("File name '{}' is forbidden", fileName);
            rv.status = HttpStatus.FORBIDDEN;
            return rv;
        }

        String[] parts = fileName.split("/");
        Path directory;
        if (parts.length == 1) {
            directory = reportDirectory("export");
        } else if (parts.length == 2) {
            directory = reportDirectory(parts[0]);
            fileName = parts[1];
        } else {
            logger.warn("Report output file '{}' is a nested file", fileName);
            rv.status = HttpStatus.FORBIDDEN;
            return rv;
        }
        rv.file = directory.resolve(fileName).toFile();
        if (!rv.file.exists()) {
            logger.warn("Report output file '{}' does not exist", rv.file);
            rv.status = HttpStatus.NOT_FOUND;
        } else if (rv.file.isDirectory()) {
            logger.warn("Report output file '{}' is a directory", rv.file);
            rv.status = HttpStatus.FORBIDDEN;
        }
        return rv;
    }

    /**
     * @return trace directory if selector is "trace, import directory if selector is "import", export directory
     * otherwise
     */
    private Path reportDirectory(String selector) {

        if ("trace".equals(selector)) {
            return Paths.get(midpointConfiguration.getMidpointHome(), "trace");
        }
        String configKey = "import".equals(selector) ? "importFolder" : "exportFolder";
        String directory = midpointConfiguration.getConfiguration(MidpointConfiguration.WEB_APP_CONFIGURATION).getString(configKey);
        if (directory == null || directory.isEmpty()) {
            directory = EXPORT_DIR;
        }
        if (directory.startsWith("/")) {
            return Paths.get(directory);
        }
        return Paths.get(midpointConfiguration.getMidpointHome(), directory);
    }

    private boolean forbiddenFileName(String fileName) {
        return fileName.contains("/../");
    }

    private void checkNodeAuthentication() throws SecurityViolationException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof NodeAuthenticationToken)) {
            throw new SecurityViolationException("Node authentication is expected but not present");
        }
        // TODO consider allowing administrator access here as well
    }
}
