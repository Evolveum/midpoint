/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
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

    private static final String MIDPOINT_HOME = System.getProperty("midpoint.home");

    @Autowired
    private PrismContext prismContext;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response listSchemas() {
        SchemaRegistry registry = prismContext.getSchemaRegistry();
        Collection<SchemaDescription> descriptions = registry.getSchemaDescriptions();

        List<String> names = new ArrayList<>();

        for (SchemaDescription description : descriptions) {
            String name = computeName(MIDPOINT_HOME, description);
            if (name == null) {
                continue;
            }

            names.add(name);
        }

        String result = StringUtils.join(names, "\n");

        return Response.ok(result).build();
    }

    private String computeName(String midpointHome, SchemaDescription description) {
        String path = description.getPath();
        if (path == null) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        File home = new File(midpointHome, "/schema");
        java.nio.file.Path homePath = home.toPath();

        java.nio.file.Path filePath = file.toPath();

        java.nio.file.Path relative = homePath.relativize(filePath);

        return relative.toString();
    }

    @GET
    @Path("/{name}")
    @Produces({MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
    public Response getSchema(@PathParam("name") String name) {
        if (name == null) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("Name not defined").build();
        }

        if (!name.toLowerCase().endsWith("\\.xsd")) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("Name must be and xsd schema (.xsd extension expected)").build();
        }

        SchemaRegistry registry = prismContext.getSchemaRegistry();
        Collection<SchemaDescription> descriptions = registry.getSchemaDescriptions();

        SchemaDescription description = null;
        for (SchemaDescription desc : descriptions) {
            String descName = computeName(MIDPOINT_HOME, desc);
            if (descName == null || !descName.equals(name)) {
                continue;
            }

            description = desc;
            break;
        }

        if (description == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("Unknown name").build();
        }

        try {
            String xsd = FileUtils.readFileToString(new File(description.getPath()));

            return Response.ok(xsd).build();
        } catch (IOException ex) {
            LOGGER.error("Couldn't load schema file for " + name, ex);

            return Response.serverError().type(MediaType.TEXT_PLAIN_TYPE).entity(ex.getMessage()).build();
        }
    }
}
