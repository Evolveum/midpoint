/*
 * Copyright (C) 2022-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.PrimitiveTextProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaFileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaFilesType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TestSchemaRestService extends RestServiceInitializer {

    protected String ENDPOINT_ADDRESS = "http://localhost:"
            + TestMidPointSpringApplication.DEFAULT_PORT
            + System.getProperty("mp.test.rest.context.path", "/api/schema");

    @Override
    protected String getAcceptHeader() {
        return null;
    }

    @Override
    protected String getContentType() {
        return null;
    }

    @Override
    protected MidpointAbstractProvider<?> getProvider() {
        return null;
    }

    @Override
    protected WebClient prepareClient(String username, String password) {
        WebClient client = WebClient.create(ENDPOINT_ADDRESS, Arrays.asList(jsonProvider, new PrimitiveTextProvider<>()));
        ClientConfiguration clientConfig = WebClient.getConfig(client);

        clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

        client.accept(MediaType.APPLICATION_JSON_TYPE);

        createAuthorizationHeader(client, username, password);

        return client;
    }

    private WebClient prepareClient() {
        return prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
    }

    @Test
    public void test010listSchemas() {
        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        SchemaFilesType schemas = response.readEntity(SchemaFilesType.class);
        assertNotNull("Error response must contain list of schemas", schemas);
        logger.info("Returned result: {}", schemas);

        assertAndGetPiracySchemaFile(schemas);
    }

    private SchemaFileType assertAndGetPiracySchemaFile(SchemaFilesType schemas) {
        assertNotNull("Schema file list must no be null", schemas);
        assertEquals(1, schemas.getSchema().size());

        SchemaFileType schemaFile = schemas.getSchema().get(0);
        assertEquals("piracy.xsd", schemaFile.getFileName());
        assertEquals("http://midpoint.evolveum.com/xml/ns/samples/piracy", schemaFile.getNamespace());
        assertNull(schemaFile.getUsualPrefix());

        return schemaFile;
    }

    @Test
    public void test020getSchema() throws IOException {
        WebClient client = prepareClient();

        when();
        Response response = client.get();
        SchemaFilesType schemas = response.readEntity(SchemaFilesType.class);
        SchemaFileType file = assertAndGetPiracySchemaFile(schemas);

        client.path("/" + file.getFileName());
        client.accept(MediaType.TEXT_PLAIN_TYPE);
        response = client.get();

        then();
        assertStatus(response, 200);
        String content = response.readEntity(String.class);

        String expected = FileUtils.readFileToString(new File("./src/test/resources/schema/piracy.xsd"), StandardCharsets.UTF_8);

        assertEquals("Schemas doesn't match", expected, content);
    }

    @Test
    public void test025getNonExistingSchema() {
        WebClient client = prepareClient();

        when();
        client.path("/non-existing.xsd");
        client.accept(MediaType.TEXT_PLAIN_TYPE, MediaType.APPLICATION_JSON_TYPE);

        Response response = client.get();

        then();
        assertStatus(response, 404);
        OperationResultType result = response.readEntity(OperationResultType.class);
        assertNotNull("Error result must not be null", result);

        assertFailure("Unknown schema", result);
    }

    @Test
    public void test030getWrongExtensionSchema() {
        WebClient client = prepareClient();

        when();
        client.path("/wrong-extension");
        client.accept(MediaType.TEXT_PLAIN_TYPE, MediaType.APPLICATION_JSON_TYPE);

        Response response = client.get();

        then();
        assertStatus(response, 400);
        OperationResultType result = response.readEntity(OperationResultType.class);
        assertNotNull("Error result must not be null", result);

        assertFailure("Name must be an xsd schema (.xsd extension expected)", result);
    }
}
