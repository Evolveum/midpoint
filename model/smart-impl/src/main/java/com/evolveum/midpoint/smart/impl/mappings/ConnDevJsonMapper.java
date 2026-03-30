package com.evolveum.midpoint.smart.impl.mappings;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;

public class ConnDevJsonMapper {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    // ---- Object classes ----

    public static ObjectNode mapObjectClassesToJson(List<? extends ConnDevBasicObjectClassInfoType> classes) {
        var ret = JSON.objectNode();
        var jsonObjectClasses = JSON.arrayNode();
        ret.set("objectClasses", jsonObjectClasses);
        for (var classInfo : classes) {
            var object = JSON.objectNode();
            object.set("name", JSON.textNode(classInfo.getName()));
            object.set("superclass", JSON.textNode(classInfo.getSuperclass()));
            object.set("description", JSON.textNode(classInfo.getDescription()));
            // Somehow relevant needs to be string
            object.set("relevant", JSON.textNode(Boolean.toString(classInfo.isRelevant())));
            object.set("abstract", JSON.booleanNode(classInfo.isAbstract()));
            object.set("embedded", JSON.booleanNode(classInfo.isEmbedded()));
            object.set("relevantDocumentations", toJsonRelevantDocs(classInfo.getRelevantDocumentations()));
            jsonObjectClasses.add(object);
        }
        return ret;
    }

    public static ConnDevBasicObjectClassInfoType mapObjectClassFromJson(JsonNode json) {
        var objClass = new ConnDevBasicObjectClassInfoType();
        objClass.setName(json.get("name").asText());
        objClass.setRelevant(toBoolean(json.get("relevant")));
        objClass.setAbstract(toBoolean(json.get("abstract")));
        objClass.setEmbedded(toBoolean(json.get("embedded")));
        objClass.setDescription(json.get("description").asText());
        var jsonRelevantDocs = json.get("relevantDocumentations");
        if (jsonRelevantDocs != null) {
            for (var jsonDoc : jsonRelevantDocs) {
                var chunk = new ConnDevRelevantDocumentationsType();
                chunk.setDocId(jsonDoc.get("docId").asText());
                chunk.setChunkId(toText(jsonDoc.get("chunkId")));
                objClass.getRelevantDocumentations().add(chunk);
            }
        }
        return objClass;
    }

    // ---- Endpoints ----

    public static ObjectNode mapEndpointsToJson(List<ConnDevHttpEndpointType> endpoints) {
        var jsonEndpoints = JSON.arrayNode();
        for (var endpoint : endpoints) {
            var jsonEndpoint = JSON.objectNode();
            jsonEndpoint.set("path", JSON.textNode(endpoint.getUri()));
            jsonEndpoint.set("description", JSON.textNode(endpoint.getName()));
            jsonEndpoint.set("responseContentType", JSON.textNode(endpoint.getResponseContentType()));
            jsonEndpoint.set("requestContentType", JSON.textNode(endpoint.getRequestContentType()));
            jsonEndpoint.set("method", JSON.textNode(toValue(endpoint.getOperation())));
            var suggestedUseArray = JSON.arrayNode();
            for (var use : endpoint.getSuggestedUse()) {
                suggestedUseArray.add(JSON.textNode(use.value()));
            }
            jsonEndpoint.set("suggestedUse", suggestedUseArray);
            jsonEndpoint.set("relevantDocumentations", toJsonRelevantDocs(endpoint.getRelevantDocumentations()));
            jsonEndpoints.add(jsonEndpoint);
        }
        var ret = JSON.objectNode();
        ret.set("endpoints", jsonEndpoints);
        return ret;
    }

    public static ConnDevHttpEndpointType mapEndpointFromJson(JsonNode json) {
        var endpoint = new ConnDevHttpEndpointType();
        endpoint.setName(toText(json.get("description")));
        endpoint.setUri(toText(json.get("path")));
        endpoint.setOperation(toOperation(json.get("method")));
        endpoint.setRequestContentType(toText(json.get("requestContentType")));
        endpoint.setResponseContentType(toText(json.get("responseContentType")));
        if (json.get("suggestedUse") != null) {
            for (var use : json.get("suggestedUse")) {
                var suggestedUse = toSuggestedUse(use);
                if (suggestedUse != null) {
                    endpoint.suggestedUse(suggestedUse);
                }
            }
        }
        var jsonRelevantDocs = json.get("relevantDocumentations");
        if (jsonRelevantDocs != null) {
            for (var jsonDoc : jsonRelevantDocs) {
                endpoint.getRelevantDocumentations().add(new ConnDevRelevantDocumentationsType()
                        .docId(toText(jsonDoc.get("docId")))
                        .chunkId(toText(jsonDoc.get("chunkId")))
                );
            }
        }
        return endpoint;
    }

    // ---- Attributes ----

    public static ObjectNode mapAttributesToJson(List<ConnDevAttributeInfoType> attributes) {
        var jsonAttributes = JSON.objectNode();
        for (var attr : attributes) {
            var jsonAttr = JSON.objectNode();
            jsonAttr.set("type", JSON.textNode(attr.getType()));
            jsonAttr.set("format", JSON.textNode(attr.getFormat()));
            jsonAttr.set("description", JSON.textNode(attr.getDescription()));
            jsonAttr.set("mandatory", JSON.booleanNode(attr.isMandatory()));
            jsonAttr.set("updatable", JSON.booleanNode(attr.isUpdatable()));
            jsonAttr.set("creatable", JSON.booleanNode(attr.isCreatable()));
            jsonAttr.set("readable", JSON.booleanNode(attr.isReadable()));
            jsonAttr.set("multivalue", JSON.booleanNode(attr.isMultivalue()));
            jsonAttr.set("returnedByDefault", JSON.booleanNode(attr.isReturnedByDefault()));
            jsonAttr.set("scimAttribute", attr.getScimAttribute() != null ? JSON.textNode(attr.getScimAttribute()) : JSON.nullNode());
            jsonAttr.set("relevantDocumentations", toJsonRelevantDocs(attr.getRelevantDocumentations()));
            jsonAttributes.set(attr.getName(), jsonAttr);
        }
        var ret = JSON.objectNode();
        ret.set("attributes", jsonAttributes);
        return ret;
    }

    public static ConnDevAttributeInfoType mapAttributeFromJson(String name, JsonNode jsonAttr) {
        var attr = new ConnDevAttributeInfoType();
        attr.setName(name);
        attr.setType(jsonAttr.get("type").asText());
        attr.setFormat(jsonAttr.get("format").asText());
        attr.setDescription(toText(jsonAttr.get("description")));
        attr.setMandatory(toBoolean(jsonAttr.get("mandatory")));
        attr.setUpdatable(toBoolean(jsonAttr.get("updatable")));
        attr.setCreatable(toBoolean(jsonAttr.get("creatable")));
        attr.setReadable(toBoolean(jsonAttr.get("readable")));
        attr.setMultivalue(toBoolean(jsonAttr.get("multivalue")));
        attr.setReturnedByDefault(toBoolean(jsonAttr.get("returnedByDefault")));
        attr.setScimAttribute(toText(jsonAttr.get("scimAttribute")));
        var jsonRelevantDocs = jsonAttr.get("relevantDocumentations");
        if (jsonRelevantDocs != null) {
            for (var jsonDoc : jsonRelevantDocs) {
                attr.getRelevantDocumentations().add(new ConnDevRelevantDocumentationsType()
                        .docId(toText(jsonDoc.get("docId")))
                        .chunkId(toText(jsonDoc.get("chunkId")))
                );
            }
        }
        return attr;
    }

    // ---- Relations ----

    public static ObjectNode mapRelationsToJson(List<ConnDevRelationInfoType> relations) {
        var jsonRelations = JSON.arrayNode();
        for (var relationInfo : relations) {
            var object = JSON.objectNode();
            object.set("name", JSON.textNode(relationInfo.getName()));
            object.set("shortDescription", JSON.textNode(relationInfo.getShortDescription()));
            object.set("subject", JSON.textNode(relationInfo.getSubject()));
            object.set("subjectAttribute", JSON.textNode(relationInfo.getSubjectAttribute()));
            object.set("object", JSON.textNode(relationInfo.getObject()));
            object.set("objectAttribute", JSON.textNode(relationInfo.getObjectAttribute()));
            jsonRelations.add(object);
        }
        var ret = JSON.objectNode();
        ret.set("relations", jsonRelations);
        return ret;
    }

    public static ConnDevRelationInfoType mapRelationFromJson(JsonNode object, List<ConnDevBasicObjectClassInfoType> discovered) {
        var ret = new ConnDevRelationInfoType();
        ret.setName(toText(object.get("name")));
        ret.setObject(toText(object.get("object")));
        ret.setObjectAttribute(toText(object.get("objectAttribute")));
        ret.setSubject(toText(object.get("subject")));
        ret.setSubjectAttribute(toText(object.get("subjectAttribute")));
        ret.setShortDescription(toText(object.get("shortDescription")));

        ret.setObject(normalize(ret.getObject(), discovered));
        ret.setSubject(normalize(ret.getSubject(), discovered));

        if (ret.getSubject() == null) {
            return null;
        }
        if (ret.getName() == null) {
            ret.setName(ret.getSubject() + "_" + ret.getSubjectAttribute()
                    + "_" + ret.getObject() + "_" + ret.getObjectAttribute());
        }
        if (ret.getShortDescription() == null) {
            ret.setShortDescription("Relation between "
                    + ret.getSubject() + "/" + ret.getSubjectAttribute()
                    + " - " + ret.getObject() + "/" + ret.getObjectAttribute());
        }
        return ret;
    }

    // ---- Utilities ----

    public static Boolean toBoolean(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return Boolean.parseBoolean(node.asText());
        }
        return node.asBoolean();
    }

    public static String toText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    public static String toValue(ConnDevHttpOperationType operation) {
        return operation != null ? operation.value() : null;
    }

    public static String toServiceIntent(ConnDevScriptIntentType intent) {
        return intent.value();
    }

    private static ConnDevHttpOperationType toOperation(JsonNode method) {
        if (method == null || method.isNull()) {
            return null;
        }
        return switch (method.asText().toUpperCase()) {
            case "GET" -> ConnDevHttpOperationType.GET;
            case "POST" -> ConnDevHttpOperationType.POST;
            case "PUT" -> ConnDevHttpOperationType.PUT;
            case "DELETE" -> ConnDevHttpOperationType.DELETE;
            case "PATCH" -> ConnDevHttpOperationType.PATCH;
            default -> null;
        };
    }

    private static ConnDevHttpEndpointIntentType toSuggestedUse(JsonNode use) {
        if (use == null || use.isNull()) {
            return null;
        }
        return Arrays.stream(ConnDevHttpEndpointIntentType.values())
                .filter(v -> v.value().equals(use.asText()))
                .findFirst().orElse(null);
    }

    private static String normalize(String llmName, List<ConnDevBasicObjectClassInfoType> discovered) {
        if (llmName == null) {
            return null;
        }
        return discovered.stream()
                .map(ConnDevBasicObjectClassInfoType::getName)
                .filter(llmName::equalsIgnoreCase)
                .findFirst().orElse(null);
    }

    private static ArrayNode toJsonRelevantDocs(List<ConnDevRelevantDocumentationsType> docs) {
        var jsonRelevantDocs = JSON.arrayNode();
        for (var chunk : docs) {
            var jsonDoc = JSON.objectNode();
            jsonDoc.set("docId", JSON.textNode(chunk.getDocId()));
            jsonDoc.set("chunkId", JSON.textNode(chunk.getChunkId()));
            jsonRelevantDocs.add(jsonDoc);
        }
        return jsonRelevantDocs;
    }
}
