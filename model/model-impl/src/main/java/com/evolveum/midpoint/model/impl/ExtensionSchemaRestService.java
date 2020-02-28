/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@Service
@Path("/schema")
@Consumes(MediaType.WILDCARD)
public class ExtensionSchemaRestService {

    private static final Trace LOGGER = TraceManager.getTrace(ExtensionSchemaRestService.class);

    @Autowired private PrismContext prismContext;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private SecurityHelper securityHelper;

    public static final String CLASS_DOT = ExtensionSchemaRestService.class.getName() + ".";
    private static final String OPERATION_LIST_SCHEMAS = CLASS_DOT +  "listSchemas";
    private static final String OPERATION_GET_SCHEMA = CLASS_DOT +  "getSchema";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response listSchemas(@Context MessageContext mc) {

        Task task = RestServiceUtil.initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_LIST_SCHEMAS);

        Response response;
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
            response = Response.ok(output).build();
        } catch (Exception ex) {
            // we avoid RestServiceUtil.handleException because we cannot serialize OperationResultType into text/plain
            LoggingUtils.logUnexpectedException(LOGGER, "Got exception while servicing REST request: {}", ex, result.getOperation());
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();              // TODO handle this somehow better
        }

        RestServiceUtil.finishRequest(task, securityHelper);
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

    @GET
    @Path("/{name}")
    @Produces({MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
    public Response getSchema(@PathParam("name") String name, @Context MessageContext mc) {

        Task task = RestServiceUtil.initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_GET_SCHEMA);

        Response response;
        try {
            securityEnforcer.authorize(ModelAuthorizationAction.GET_EXTENSION_SCHEMA.getUrl(), null, AuthorizationParameters.EMPTY, null, task, result);

            if (name == null) {
                response = Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE)
                        .entity("Name not defined").build();
            } else if (!name.toLowerCase().endsWith(".xsd") && name.length() > 4) {
                response = Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE)
                        .entity("Name must be an xsd schema (.xsd extension expected)").build();
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
                    response = Response.ok(new File(description.getPath())).build();
                } else {
                    response = Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN_TYPE)
                            .entity("Unknown name").build();
                }
            }
        } catch (Exception ex) {
            result.computeStatus();
            response = RestServiceUtil.handleException(result, ex);
        }

        RestServiceUtil.finishRequest(task, securityHelper);
        return response;
    }
}
