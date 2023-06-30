/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaFileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaFilesType;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@RestController
@RequestMapping({ "/ws/schema", "/rest/schema", "/api/schema" })
public class ExtensionSchemaRestController extends AbstractRestController {

    @Autowired private SecurityEnforcer securityEnforcer;

    @GetMapping
    public ResponseEntity<?> listSchemas() {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "listSchemas");

        ResponseEntity<?> response;
        try {
            securityEnforcer.authorize(
                    ModelAuthorizationAction.GET_EXTENSION_SCHEMA.getUrl(), task, result);

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            Collection<SchemaDescription> descriptions = registry.getSchemaDescriptions();

            SchemaFilesType files = new SchemaFilesType();

            for (SchemaDescription description : descriptions) {
                String name = computeName(description);

                if (name == null) {
                    continue;
                }

                files.schema(new SchemaFileType()
                        .namespace(description.getNamespace())
                        .usualPrefix(description.getUsualPrefix())
                        .fileName(name));
            }

            response = ResponseEntity.ok(files);
        } catch (Exception ex) {
            result.recordFatalError(ex);

            response = handleException(result, ex);
        }

        finishRequest(task, result);

        return response;
    }

    private String computeName(SchemaDescription description) {
        String path = description.getPath();
        if (path == null) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        return file.getName();
    }

    @GetMapping(value = "/{name}",
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity<?> getSchema(
            @PathVariable("name") String name) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "getSchema");

        ResponseEntity<?> response;
        try {
            securityEnforcer.authorize(
                    ModelAuthorizationAction.GET_EXTENSION_SCHEMA.getUrl(), task, result);

            if (name == null) {
                result.recordFatalError("Name not defined");
                response = createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
            } else if (!name.toLowerCase().endsWith(".xsd") && name.length() > 4) {
                result.recordFatalError("Name must be an xsd schema (.xsd extension expected)");
                response = createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
            } else {
                SchemaRegistry registry = prismContext.getSchemaRegistry();
                Collection<SchemaDescription> descriptions = registry.getSchemaDescriptions();

                SchemaDescription description = null;
                for (SchemaDescription desc : descriptions) {
                    String descName = computeName(desc);
                    if (descName != null && descName.equals(name)) {
                        description = desc;
                        break;
                    }
                }

                if (description != null) {
                    String content = FileUtils.readFileToString(new File(description.getPath()), StandardCharsets.UTF_8);
                    response = ResponseEntity.status(HttpStatus.OK)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(content);
                } else {
                    result.recordFatalError("Unknown schema");
                    response = createErrorResponseBuilder(HttpStatus.NOT_FOUND, result);
                }
            }
        } catch (Exception ex) {
            result.recordFatalError(ex);

            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }
}
