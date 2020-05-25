/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.rest.impl;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;

@RestController
@RequestMapping({ "/ws/schema", "/rest/schema", "/api/schema" })
public class ExtensionSchemaRestController extends AbstractRestController {

    @Autowired private PrismContext prismContext;
    @Autowired private SecurityEnforcer securityEnforcer;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> listSchemas() {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "listSchemas");

        ResponseEntity<?> response;
        try {
            securityEnforcer.authorize(ModelAuthorizationAction.GET_EXTENSION_SCHEMA.getUrl(), null, AuthorizationParameters.EMPTY, null, task, result);

            SchemaRegistry registry = prismContext.getSchemaRegistry();
            Collection<SchemaDescription> descriptions = registry.getSchemaDescriptions();

            List<String> names = new ArrayList<>();

            for (SchemaDescription description : descriptions) {
                String name = computeName(description);
                if (name != null) {
                    names.add(name);
                }
            }

            String output = StringUtils.join(names, "\n");
            response = ResponseEntity.ok(output);
        } catch (Exception ex) {
            // we avoid RestServiceUtil.handleException because we cannot serialize OperationResultType into text/plain
            LoggingUtils.logUnexpectedException(logger, "Got exception while servicing REST request: {}", ex, result.getOperation());
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getMessage()); // TODO handle this somehow better
        }

        finishRequest();
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

        String midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
        java.nio.file.Path homePath = Paths.get(midpointHome, "schema");
        java.nio.file.Path relative = homePath.relativize(file.toPath());

        return relative.toString();
    }

    @GetMapping(value = "/{name}",
            produces = { MediaType.TEXT_XML_VALUE, MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity<?> getSchema(
            @PathVariable("name") String name) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "getSchema");

        ResponseEntity<?> response;
        try {
            securityEnforcer.authorize(ModelAuthorizationAction.GET_EXTENSION_SCHEMA.getUrl(),
                    null, AuthorizationParameters.EMPTY, null, task, result);

            if (name == null) {
                response = ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                        .body("Name not defined");
            } else if (!name.toLowerCase().endsWith(".xsd") && name.length() > 4) {
                response = ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN)
                        .body("Name must be an xsd schema (.xsd extension expected)");
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
                    response = ResponseEntity.ok(new File(description.getPath()));
                } else {
                    response = ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("Unknown name");
                }
            }
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        finishRequest();
        return response;
    }
}
