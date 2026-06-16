package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.Function;

public class ConnectorManifestWriter {

    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;
    private final ObjectNode application;
    private final ObjectNode connector;
    private ObjectNode root;

    public ConnectorManifestWriter(ConnectorDevelopmentType development) {

        root = FACTORY.objectNode();
        this.application = FACTORY.objectNode();
        this.connector = FACTORY.objectNode();
        root.set("application", application);
        root.set("connector", connector);

        writeApplication(development.getApplication());
        writeConnector(development.getConnector());
    }



    private void writeConnector(ConnDevConnectorType connector) {
        var schemas = FACTORY.arrayNode();
        var authorization = FACTORY.arrayNode();
        var operations = FACTORY.arrayNode();

        writeScript(authorization, connector.getAuthenticationScript());
        writeScript(operations, connector.getTestOperation());

        for (var objClass : connector.getObjectClass()) {
            writeScript(schemas, objClass.getNativeSchemaScript());
            writeScript(schemas, objClass.getConnidSchemaScript());
            writeScript(operations, objClass.getSearchAllOperation());
            writeScript(operations, objClass.getSearchIdOperation());
            writeScript(operations, objClass.getSearchFilterOperation());
            writeScript(operations, objClass.getCreateScript());
            writeScript(operations, objClass.getUpdateScript());
            writeScript(operations, objClass.getDeleteScript());
        }

        for (var relation : connector.getRelation()) {
            writeScript(schemas, relation.getSchemaScript());
        }

        this.connector.set("schema", schemas);
        if (!authorization.isEmpty()) {
            this.connector.set("authorization", authorization);
        }
        this.connector.set("operation", operations);
    }

    private void writeScript(ArrayNode array, ConnDevArtifactType artifact) {
        if (artifact == null) {
            return;
        }
        var json = FACTORY.objectNode();
        writeTextProperty(json, "script", resourcePath(artifact.getFilename()));
        // FIXME: Write classifcation properties
        writeTextProperty(json, "objectClass", artifact.getObjectClass());
        writeTextProperty(json, "operation", artifact.getOperation(), ConnDevOperationType::value);
        writeTextProperty(json, "intent", artifact.getIntent(), ConnDevScriptIntentType::value);
        if (!json.isEmpty()) {
            array.add(json);
        }
    }

    private String resourcePath(String filename) {
        return filename != null ? "/" + filename : null;
    }

    private void writeApplication(ConnDevApplicationInfoType app) {
        if (app == null) {
            return;
        }
        writeTextProperty(application, "name", app.getApplicationName());
        writeTextProperty(application, "description", app.getDescription());
    }


    private <T> void  writeTextProperty(ObjectNode target, String property, T value, Function<T, String> converter) {
        if (value != null) {
            writeTextProperty(target, property, converter.apply(value));
        }
    }

        private void writeTextProperty(ObjectNode target, String property, String value) {
        if (value != null) {
            target.set(property, FACTORY.textNode(value));
        }
    }

    private void writeTextProperty(ObjectNode target, String property, PolyStringType value) {
        if (value != null) {
            target.set(property, FACTORY.textNode(value.getOrig()));
        }
    }

    public String serialize() {
        return root.toPrettyString();

    }
}
