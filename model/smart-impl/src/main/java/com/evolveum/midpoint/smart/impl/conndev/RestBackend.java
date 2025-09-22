package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RestBackend extends ConnectorDevelopmentBackend {

    private static final long SLEEP_TIME = 5 * 1000L;
    private static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;


    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentBackend.class);
    private static final int MAX_SCRAPE_ITERATIONS = 2;

    public RestBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public ConnDevApplicationInfoType discoverBasicInformation() {
        var documentation = selectBestDocumentation(getProcessedDocumentation());
        try(var job = client().postDocumentationJob("digester/getInfoMetadata", documentation.asInputStream() , null)) {
            return job.waitAndProcess(SLEEP_TIME, o -> {
                var ret = new ConnDevApplicationInfoType();

                var jsonInfo = o.get("infoAboutSchema");
                if (jsonInfo.isEmpty()) {
                    // Should we re
                    return ret;
                }
                if (jsonInfo.get("name") != null) {
                    ret.applicationName((jsonInfo.get("name").asText()));
                }
                if (jsonInfo.get("applicationVersion") != null) {
                    ret.version(jsonInfo.get("applicationVersion").asText());
                }
                // FIXME for proper detection
                ret.integrationType(ConnDevIntegrationType.REST);
                if (jsonInfo.get("baseApiEndpoint") != null) {
                    ret.baseApiEndpoint(jsonInfo.get("baseApiEndpoint").get(0).get("uri").asText());
                }
                // FIXME: Add dynamic
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProcessedDocumentation selectBestDocumentation(List<ProcessedDocumentation> processedDocumentation) {
        // FIXME: Select documentation based on classification
        for (var doc : processedDocumentation) {
            if ("application/json".equals(doc.contentType()) || "application/yaml".equals(doc.contentType())) {
                return doc;
            }
        }
        return processedDocumentation.get(0);
    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation() {
        var documentation = selectBestDocumentation(getProcessedDocumentation());

        try(var job = client().postDocumentationJob("digester/getAuth", documentation.asInputStream() , null)) {
            return job.waitAndProcess(SLEEP_TIME, json -> {
                var ret = new ArrayList<ConnDevAuthInfoType>();
                for (var jsonAuth : json.get("auth")) {

                    var auth = SupportedAuthorization.fromAiType(jsonAuth.get("type").asText());
                    if (auth != null) {
                        auth.setName(jsonAuth.get("name").asText());
                        auth.quirks(jsonAuth.get("quirks").asText());
                        ret.add(auth);
                    }
                }
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation() {

        ObjectNode request = JSON_FACTORY.objectNode();
        request.set("applicationName", JSON_FACTORY.textNode(developmentObject().getApplication().getApplicationName().getOrig()));
        request.set("applicationVersion", JSON_FACTORY.textNode(developmentObject().getApplication().getVersion()));

        try(var jobSpec = client().postJob("discovery/getCandidateLinks", request)) {
            return jobSpec.waitAndProcess(SLEEP_TIME, result -> {
                var results = jobSpec.getResult().get("candidateLinksEnriched");

                var map = new HashMap<String, ConnDevDocumentationSourceType>();
                for (var link : results) {
                    var discovered = new ConnDevDocumentationSourceType();
                    discovered.setName(toString(link.get("title")));
                    discovered.setUri(toString(link.get("href")));
                    discovered.setDescription(toString(link.get("body")));
                    map.put(discovered.getUri(), discovered);
                }
                var ret = new ArrayList<ConnDevDocumentationSourceType>(map.values());

                for (var jsonText : result.get("candidateLinks")) {
                    var href = toString(jsonText);
                    if (!map.containsKey(href)) {
                        var discovered = new ConnDevDocumentationSourceType();
                        discovered.setName(href);
                        discovered.setUri(href);
                        ret.add(discovered);
                    }
                }
                return ret;
            });
        } catch (Exception e) {
            throw new SystemException("Couldn't discover candidate links", e);
        }
    }

    public ConnDevArtifactType generateArtifact(ConnDevGenerateArtifactDefinitionType input) {
        var artifactSpec = input.getArtifact();
        var ret = artifactSpec.clone();
        if (artifactSpec.getObjectClass() != null) {
            return generateObjectClassArtifact(input);
        }

        var classification = ConnectorDevelopmentArtifacts.classify(artifactSpec);
        return switch (classification) {
            case AUTHENTICATION_CUSTOMIZATION -> ret.content("""
                        authentication {
                            // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                            // how to write authentication part of the script.
                        }
                        """);
            case TEST_CONNECTION_DEFINITION -> ret.content("""
                        test {
                            // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                            // how to write test connection part of the script.
                            // Usually it is only necessary to specify endpoint here.
                            endpoint("/my_preferences")
                        }
                        """);
            default -> throw new IllegalStateException("Unexpected value: " + artifactSpec.getIntent());
        };
    }

    @Override
    public ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType input) {
        var artifactSpec = input.getArtifact();
        var objectClass = artifactSpec.getObjectClass();
        var classification = ConnectorDevelopmentArtifacts.classify(artifactSpec);
        var content = switch (classification) {
            case NATIVE_SCHEMA_DEFINITION -> generateNativeSchema(artifactSpec);
            case CONNID_SCHEMA_DEFINITION -> generateConnIdSchema(artifactSpec);
            case SEARCH_ALL_DEFINITION -> generateSearchAll(artifactSpec, input.getEndpoint());
            case RELATIONSHIP_SCHEMA_DEFINITION -> genereateRelation(artifactSpec, input.getRelation());
            default -> throw new IllegalStateException("Unexpected script type: " + classification);
        };
        content = content.replace("${objectClass}", objectClass);
        return artifactSpec.content(content);

    }

    private String genereateRelation(ConnDevArtifactType artifactSpec, List<ConnDevRelationInfoType> relation) {
        var request = MultipartEntityBuilder.create();
        var documentation = selectBestDocumentation(getProcessedDocumentation());
        try {
            request.addBinaryBody("documentation", documentation.asInputStream(), ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");
            request.addTextBody("relation",  toJsonRelations(relation).toPrettyString());
        } catch (FileNotFoundException e) {
            throw new SystemException("Couldn't open documentation file", e);
        }
        try(var job = client().postEntityJob("codegen/getRelation" , request.build())) {
            return job.waitAndProcess(SLEEP_TIME, json -> json.get("code").asText());
        } catch (Exception e) {
            throw new SystemException("Couldn't generate relation for objectClass " + artifactSpec.getObjectClass(), e);
        }

    }



    private String generateSearchAll(ConnDevArtifactType artifactSpec, List<ConnDevHttpEndpointType> endpoints) {
        var attributes = connectorObjectClass(artifactSpec.getObjectClass()).getAttribute();
        var request = MultipartEntityBuilder.create();
        var documentation = selectBestDocumentation(getProcessedDocumentation());
        try {
            request.addBinaryBody("documentation", documentation.asInputStream(), ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");
            request.addTextBody("attributes", toJsonAttributes(attributes).toPrettyString());
            request.addTextBody("endpoints",  toJsonEndpoints(endpoints).toPrettyString());
        } catch (FileNotFoundException e) {
            throw new SystemException("Couldn't open documentation file", e);
        }
        try(var job = client().postEntityJob("codegen/getSearch", artifactSpec.getObjectClass(), request.build())) {
            return job.waitAndProcess(SLEEP_TIME, json -> json.get("code").asText());
        } catch (Exception e) {
            throw new SystemException("Couldn't generate native schema for objectClass " + artifactSpec.getObjectClass(), e);
        }
    }

    private String generateConnIdSchema(ConnDevArtifactType artifactSpec) {
        var attributes = connectorObjectClass(artifactSpec.getObjectClass()).getAttribute();
        var attributesPair = new BasicNameValuePair("attributes", toJsonAttributes(attributes).toPrettyString());
        var request = new UrlEncodedFormEntity(List.of(attributesPair));

        try(var job = client().postEntityJob("codegen/getConnID", artifactSpec.getObjectClass(), request)) {
            return job.waitAndProcess(SLEEP_TIME, json -> json.get("code").asText());
        } catch (Exception e) {
            throw new SystemException("Couldn't generate native schema for objectClass " + artifactSpec.getObjectClass(), e);
        }
    }

    private ServiceClient client() {
        return beans.client(result);
    }

    private String generateNativeSchema(ConnDevArtifactType artifactSpec) {
        var attributes = connectorObjectClass(artifactSpec.getObjectClass()).getAttribute();
        var attributesPair = new BasicNameValuePair("attributes", toJsonAttributes(attributes).toPrettyString());
        var request = new UrlEncodedFormEntity(List.of(attributesPair));

        try(var job = client().postEntityJob("codegen/getNativeSchema", artifactSpec.getObjectClass(), request)) {
            return job.waitAndProcess(SLEEP_TIME, json -> {
                return json.get("code").asText();
            });
        } catch (Exception e) {
            throw new SystemException("Couldn't generate native schema for objectClass " + artifactSpec.getObjectClass(), e);
        }
    }

    @Override
    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated) {
        var documentation = selectBestDocumentation(getProcessedDocumentation());

        try(var job = client().postDocumentationJob("digester/getObjectClass", documentation.asInputStream() , null)) {
            return job.waitAndProcess(SLEEP_TIME, o -> {
                var ret = new ArrayList<ConnDevBasicObjectClassInfoType>();
                var jsonClasses = o.get("objectClasses");
                for (var jsonClass : jsonClasses) {
                    var objClass = new ConnDevBasicObjectClassInfoType();
                    var relevant = toBoolean(jsonClass.get("relevant"));
                    objClass.setName(jsonClass.get("name").asText());
                    objClass.setRelevant(relevant);
                    objClass.setAbstract(toBoolean(jsonClass.get("abstract")));
                    objClass.setEmbedded(toBoolean(jsonClass.get("embedded")));
                    objClass.setDescription(jsonClass.get("description").asText());
                    if (relevant || includeUnrelated) {
                        ret.add(objClass);
                    }
                }
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Boolean toBoolean(JsonNode relevant) {
        if (relevant == null) {
            return null;
        }
        if (relevant.isTextual()) {
            return Boolean.parseBoolean(relevant.asText());
        }
        return relevant.asBoolean();
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass) {
        var documentation = selectBestDocumentation(getProcessedDocumentation());
        try(var job = client().postDocumentationObjectClassJob("digester/getEndpoints", objectClass , documentation.asInputStream() , null)) {
            return job.waitAndProcess(SLEEP_TIME, o -> {
                var ret = new ArrayList<ConnDevHttpEndpointType>();
                var jsonClasses = o.get("endpoints");
                for (var jsonClass : jsonClasses) {
                    var endpoint = new ConnDevHttpEndpointType();

                    endpoint.setName(toString(jsonClass.get("description")));
                    endpoint.setUri(toString(jsonClass.get("path")));
                    endpoint.setOperation(toOperation(jsonClass.get("method")));

                    endpoint.setRequestContentType(toString(jsonClass.get("requestContentType")));
                    endpoint.setResponseContentType(toString(jsonClass.get("responseContentType")));

                    if (jsonClass.get("suggestedUse") != null) {
                        for (var use : jsonClass.get("suggestedUse")) {
                            var suggestedUse = toSuggestedUse(use);
                            if (suggestedUse != null) {
                                endpoint.suggestedUse(suggestedUse);
                            }
                        }
                    }
                    ret.add(endpoint);
                }
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConnDevHttpEndpointIntentType toSuggestedUse(JsonNode use) {
        if (use == null || use.isNull()) {
            return null;
        }
        return Arrays.stream(ConnDevHttpEndpointIntentType.values())
                .filter(v -> v.value().equals(use.asText()))
                .findFirst().orElse(null);
    }

    private String toString(JsonNode jsonNode) {
        return jsonNode == null || jsonNode.isNull() ? null : jsonNode.asText();
    }

    private ConnDevHttpOperationType toOperation(JsonNode method) {
        if (method == null || method.isNull()) {
            return null;
        }
        return switch (method.asText().toUpperCase()) {
            case "GET" -> ConnDevHttpOperationType.GET;
            case "POST" -> ConnDevHttpOperationType.POST;
            case "PUT" -> ConnDevHttpOperationType.PUT;
            case "DELETE" -> ConnDevHttpOperationType.DELETE;
            default -> null;
        };
    }

    @Override
    public List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass) {
        var documentation = selectBestDocumentation(getProcessedDocumentation());
        try(var job = client().postDocumentationObjectClassJob("digester/getObjectClassSchema", objectClass, documentation.asInputStream() , null)) {
            return job.waitAndProcess(SLEEP_TIME, o -> {
                var ret = new ArrayList<ConnDevAttributeInfoType>();
                var jsonAttributes = (ObjectNode) o.get("attributes");
                for (var entry : jsonAttributes.properties()) {
                    var jsonAttr = entry.getValue();
                    var attr = new ConnDevAttributeInfoType();
                    attr.setName(entry.getKey());
                    attr.setType(jsonAttr.get("type").asText());
                    attr.setFormat(jsonAttr.get("format").asText());
                    attr.setMandatory(toBoolean(jsonAttr.get("mandatory")));
                    attr.setUpdatable(toBoolean(jsonAttr.get("updatable")));
                    attr.setCreatable(toBoolean(jsonAttr.get("creatable")));
                    attr.setReadable(toBoolean(jsonAttr.get("readable")));
                    attr.setMultivalue(toBoolean(jsonAttr.get("multivalue")));
                    attr.setReturnedByDefault(toBoolean(jsonAttr.get("returnedByDefault")));

                    ret.add(attr);
                }
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processDocumentation() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        ConnDevDocumentationSourceType openApi = null;
        var byMidpoint = new ArrayList<ConnDevDocumentationSourceType>();
        var byScrapper = new ArrayList<ConnDevDocumentationSourceType>();
        for (var doc : developmentObject().getDocumentationSource()) {
            if (isOpenApi(doc)) {
                // Workaround since midPoint does not
                openApi = doc;
                byMidpoint.add(doc);
            } else {
                byScrapper.add(doc);
            }
        }

        var documentations = new ArrayList<ProcessedDocumentation>();
        if (openApi != null) {
            documentations.add(downloadAndCache(openApi));
        }
        if (!byScrapper.isEmpty()) {
            downloadUsingScrapper(byScrapper, documentations);
        }

        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                .addRealValues(documentations.stream().map(ProcessedDocumentation::toBean).toList())
                .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
    }

    private void downloadUsingScrapper(ArrayList<ConnDevDocumentationSourceType> byScrapper, ArrayList<ProcessedDocumentation> documentations) {
        var request = scrapperRequest(byScrapper);
        try(var job = client().postJob("scrape/getRelevantDocumentation", request)) {
            var scrapped = job.waitAndProcess(SLEEP_TIME, json -> {
                var ret = new ArrayList<ProcessedDocumentation>();
                var jsonUris = json.get("relevantDocuments");

                var jsonFulltexts = json.get("relevantDocumentsFulltext");

                for (int i = 0; i < jsonUris.size(); i++) {
                    try {
                        var uri = toString(jsonUris.get(i));
                        var fulltext = toString(jsonFulltexts.get(i));
                        var processed = new ProcessedDocumentation(UUID.randomUUID().toString(), uri);
                        processed.write(fulltext);
                        ret.add(processed);
                    } catch (Exception e) {
                        // Skipping documentation
                        // FIXME: we should log this
                    }
                }
                return ret;
            });
            documentations.addAll(scrapped);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private ObjectNode scrapperRequest(ArrayList<ConnDevDocumentationSourceType> byScrapper) {
        var ret = JSON_FACTORY.objectNode();
        var starterLinks = JSON_FACTORY.arrayNode();
        var trustedDomains = new HashSet<String>();


        for (var doc : byScrapper) {
            try {
                var uri = new URL(doc.getUri());
                starterLinks.add(doc.getUri());
                trustedDomains.add(uri.getHost());
            } catch (Exception e) {
                // SHould not happen.
            }
        }

        ret.set("starterLinks", starterLinks);
        ret.set("applicationName",  JSON_FACTORY.textNode(developmentObject().getApplication().getApplicationName().getOrig()));
        ret.set("applicationVersion", JSON_FACTORY.textNode("latest"));

        var trustedDomainsJson = JSON_FACTORY.arrayNode();
        trustedDomains.forEach(d -> trustedDomainsJson.add(JSON_FACTORY.textNode(d)));
        ret.set("trustedDomains", trustedDomainsJson );

        ret.set("maxScraperIterations", JSON_FACTORY.numberNode(MAX_SCRAPE_ITERATIONS));
        ret.set("runParts", JSON_FACTORY.textNode("all"));
        ret.set("scraperUrlSelectMethod", JSON_FACTORY.textNode("current-except"));
        ret.set("returnFulltext", JSON_FACTORY.booleanNode(true));
        return ret;
    }

    protected ProcessedDocumentation downloadAndCache(ConnDevDocumentationSourceType openApi) {
        var documentation = new ProcessedDocumentation(UUID.randomUUID().toString(), openApi.getUri());

        try {
            var url = new URL(openApi.getUri());
            beans.downloadFile(url, documentation.asOutputStream());
            return documentation;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected List<ProcessedDocumentation> getProcessedDocumentation() {
        return developmentObject().getProcessedDocumentation().stream()
                .map(ProcessedDocumentation::new).toList();

    }

    private boolean isOpenApi(ConnDevDocumentationSourceType doc) {
        var uri = doc.getUri();
        return uri.endsWith(".json") || uri.endsWith(".yml") || uri.endsWith(".yaml");
    }

    static ObjectNode toJsonAttributes(List<ConnDevAttributeInfoType> attributes) {
        var ret = JSON_FACTORY.objectNode();
        var jsonAttributes = JSON_FACTORY.objectNode();
        ret.set("attributes", jsonAttributes);
        for (var attr : attributes) {
            var jsonAttr = JSON_FACTORY.objectNode();
            jsonAttr.set("type", JSON_FACTORY.textNode(attr.getType()));
            jsonAttr.set("format", JSON_FACTORY.textNode(attr.getFormat()));
            jsonAttr.set("mandatory", JSON_FACTORY.booleanNode(attr.isMandatory()));
            jsonAttr.set("updatable", JSON_FACTORY.booleanNode(attr.isUpdatable()));
            jsonAttr.set("readable", JSON_FACTORY.booleanNode(attr.isReadable()));
            jsonAttr.set("multivalue", JSON_FACTORY.booleanNode(attr.isMultivalue()));
            jsonAttr.set("returnedByDefault", JSON_FACTORY.booleanNode(attr.isReturnedByDefault()));

            jsonAttributes.set(attr.getName(), jsonAttr);
        }
        return ret;
    }

    public static ObjectNode toJsonEndpoints(List<ConnDevHttpEndpointType> endpoints) {
        var ret = JSON_FACTORY.objectNode();
        var jsonEndpoints = JSON_FACTORY.arrayNode();
        ret.set("endpoints", jsonEndpoints);
        for (var endpoint : endpoints) {
            var jsonEndpoint = JSON_FACTORY.objectNode();
            jsonEndpoint.set("path", JSON_FACTORY.textNode(endpoint.getUri()));
            jsonEndpoint.set("description", JSON_FACTORY.textNode(endpoint.getName()));
            jsonEndpoint.set("responseContentType", JSON_FACTORY.textNode(endpoint.getResponseContentType()));
            jsonEndpoint.set("requestContentType", JSON_FACTORY.textNode(endpoint.getRequestContentType()));
            jsonEndpoint.set("method", JSON_FACTORY.textNode(toValue(endpoint.getOperation())));
            jsonEndpoints.add(jsonEndpoint);
        }
        return ret;
    }

    public static ObjectNode toJsonObjectClasses(List<? extends ConnDevBasicObjectClassInfoType> classes) {
        var  ret = JSON_FACTORY.objectNode();
        var jsonObjectClasses = JSON_FACTORY.arrayNode();
        ret.set("objectClasses", jsonObjectClasses);
        for (var classInfo : classes) {
            var object = JSON_FACTORY.objectNode();
            object.set("name", JSON_FACTORY.textNode(classInfo.getName()));
            object.set("description", JSON_FACTORY.textNode(classInfo.getDescription()));
            // Somehow relevant needs to be string
            object.set("relevant", JSON_FACTORY.textNode(Boolean.toString(classInfo.isRelevant())));
            object.set("abstract", JSON_FACTORY.booleanNode(classInfo.isAbstract()));
            object.set("embedded", JSON_FACTORY.booleanNode(classInfo.isEmbedded()));
            jsonObjectClasses.add(object);
        }
        return ret;
    }

    private ObjectNode toJsonRelations(List<ConnDevRelationInfoType> relation) {
        var ret = JSON_FACTORY.objectNode();
        var jsonRelations = JSON_FACTORY.arrayNode();
        ret.set("relations", jsonRelations);
        for (var relationInfo : relation) {
            var object = JSON_FACTORY.objectNode();
            object.set("name", JSON_FACTORY.textNode(relationInfo.getName()));
            object.set("subject", JSON_FACTORY.textNode(relationInfo.getSubject()));
            object.set("subjectAttribute",  JSON_FACTORY.textNode(relationInfo.getSubjectAttribute()));
            object.set("object",  JSON_FACTORY.textNode(relationInfo.getObject()));
            object.set("objectAttribute",  JSON_FACTORY.textNode(relationInfo.getObjectAttribute()));
            jsonRelations.add(object);
        }
        return ret;
    }

    private static String toValue(ConnDevHttpOperationType operation) {
        return operation != null ? operation.value() : null;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered) {

        var request = MultipartEntityBuilder.create();
        var documentation = selectBestDocumentation(getProcessedDocumentation());
        try {
            request.addBinaryBody("documentation", documentation.asInputStream(), ContentType.create("application/yaml", StandardCharsets.UTF_8), "spec.yml");
            request.addTextBody("relevantObjectClasses", toJsonObjectClasses(discovered).toPrettyString());

            try(var job = client().postEntityJob("digester/getRelations",request.build())) {
                return job.waitAndProcess(SLEEP_TIME, json -> {
                    var ret = new ArrayList<ConnDevRelationInfoType>();
                    var jsonRelations = json.get("relations");
                    for (var object : jsonRelations) {
                        var relation = toRelation(object, discovered);
                        if (relation != null) {
                            ret.add(relation);
                        }
                    }
                    return ret;
                });
            }
        } catch (Exception e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    private ConnDevRelationInfoType toRelation(JsonNode object, List<ConnDevBasicObjectClassInfoType> discovered) {
        var ret = new ConnDevRelationInfoType();
        ret.setName(toString(object.get("name")));
        ret.setObject(toString(object.get("object")));
        ret.setObjectAttribute(toString(object.get("objectAttribute")));
        ret.setSubject(toString(object.get("subject")));
        ret.setSubjectAttribute(toString(object.get("subjectAttribute")));
        ret.setShortDescription(toString(object.get("shortDescription")));


        ret.setObject(normalize(ret.getObject(), discovered));
        ret.setSubject(normalize(ret.getSubject(), discovered));

        if (ret.getSubject() == null) {
            // Ignore relations without subject.
            return null;
        }

        // Add name and relation
        if (ret.getName() == null) {
            var base = ret.getSubject()
                    + "_" + ret.getSubjectAttribute()
                    + "_" + ret.getObject()
                    + "_" + ret.getObjectAttribute();
            ret.setName(base);

        }
        if (ret.getShortDescription() == null) {
            var base = "Relation between "
                    + ret.getSubject()
                    + "/" + ret.getSubjectAttribute()
                    + " - " + ret.getObject()
                    + "/" + ret.getObjectAttribute();
            ret.setShortDescription(base);
        }
        return ret;
    }

    private String normalize(String llmName, List<ConnDevBasicObjectClassInfoType> discovered) {
        if (llmName == null) {
            return null;
        }
        return discovered.stream().map(ConnDevBasicObjectClassInfoType::getName)
                .filter(llmName::equalsIgnoreCase).findFirst().orElse(null);

    }

}
