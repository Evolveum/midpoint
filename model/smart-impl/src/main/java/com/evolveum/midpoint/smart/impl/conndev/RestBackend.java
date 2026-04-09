package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.smart.impl.mappings.ConnDevJsonMapper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RestBackend extends ConnectorDevelopmentBackend {

    private static final long SLEEP_TIME = 5 * 1000L;
    protected static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;


    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentBackend.class);
    private static final int MAX_SCRAPE_ITERATIONS = 2;

    public RestBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public ConnDevApplicationInfoType discoverBasicInformation() {
        try(var job = client().postJob("digester/{sessionId}/metadata")) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ConnDevApplicationInfoType();

                var jsonInfo = o.get("infoMetadata");
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
        try(var job = client().postJob("digester/{sessionId}/auth")) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), json -> {
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
        request.set("applicationName", JSON_FACTORY.textNode(
                developmentObject().getApplication().getApplicationName().getOrig()));
        request.set("applicationVersion", JSON_FACTORY.textNode(
                Objects.requireNonNullElse(developmentObject().getApplication().getVersion(), "latest")));
        request.set("llmGeneratedSearchQuery", JSON_FACTORY.booleanNode(false));
        try(var jobSpec = client().postJob("discovery/{sessionId}/discovery", request)) {
            return jobSpec.waitAndProcess(SLEEP_TIME, canRun(), result -> {
                var results = jobSpec.getResult().get("candidateLinksEnriched");

                var map = new HashMap<String, ConnDevDocumentationSourceType>();
                for (var link : results) {
                    var discovered = new ConnDevDocumentationSourceType();
                    discovered.setName(ConnDevJsonMapper.toText(link.get("title")));
                    discovered.setUri(ConnDevJsonMapper.toText(link.get("href")));
                    discovered.setDescription(ConnDevJsonMapper.toText(link.get("body")));
                    map.put(discovered.getUri(), discovered);
                }
                var ret = new ArrayList<ConnDevDocumentationSourceType>(map.values());

                for (var jsonText : result.get("candidateLinks")) {
                    var href = ConnDevJsonMapper.toText(jsonText);
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
            case AUTHENTICATION_CUSTOMIZATION -> generateAuthorizationScript(input, classification);
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

    private ConnDevArtifactType generateAuthorizationScript(ConnDevGenerateArtifactDefinitionType input, ConnectorDevelopmentArtifacts.KnownArtifactType classification) {
        if (hasAuthenticationQuirks()) {
            // FIXME: Here should be LLM call
            return classification.create().content("""
                    authentication {
                        // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                        // how to write authentication part of the script.
                    }
                    """);
        }
        return null;
    }

    private boolean hasAuthenticationQuirks() {
        return developmentObject().getConnector().getAuth().stream()
                .anyMatch(auth -> auth.getQuirks() != null && !auth.getQuirks().isBlank());
    }

    @Override
    public ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType input) {
        var artifactSpec = input.getArtifact();
        var objectClass = artifactSpec.getObjectClass();
        var classification = ConnectorDevelopmentArtifacts.classify(artifactSpec);
        var content = switch (classification) {
            case NATIVE_SCHEMA_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "native-schema", "native schema script");
            case CONNID_SCHEMA_DEFINITION -> generateObjectClassScript(artifactSpec,
                    "connid", "ConnID mapping script");
            case SEARCH_ALL_DEFINITION -> generateSearchAll(artifactSpec, input.getEndpoint());
            case CREATE -> generateObjectClassScript(artifactSpec, "create", "Create script");
            case UPDATE ->  generateObjectClassScript(artifactSpec, "update", "Update script");
            case DELETE -> generateObjectClassScript(artifactSpec, "delete", "Delete script");
            case RELATIONSHIP_SCHEMA_DEFINITION -> generateRelation(artifactSpec, input.getRelation());
            default -> throw new IllegalStateException("Unexpected script type: " + classification);
        };
        content = content.replace("${objectClass}", objectClass);
        return artifactSpec.content(content);

    }

    private String generateRelation(ConnDevArtifactType artifactSpec, List<ConnDevRelationInfoType> relation) {
        // FIXME: Upload new relations to server
        try {
            client().synchronizationClient().put("digester/{sessionId}/relations", () -> {
                var relationJsons = ConnDevJsonMapper.mapRelationsToJson(developmentObject().getConnector().getRelation());
                return EntityBuilder.create()
                        .setContentType(ContentType.APPLICATION_JSON)
                        .setText(relationJsons.toPrettyString()).build();
            });
        } catch (Exception e) {
            throw new SystemException("Couldn't generate relation for " + artifactSpec + ". Unable to synchronize relationships", e);
        }

        try(var job = client().postJob("codegen/{sessionId}/relations/" + artifactSpec.getObjectClass())) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), json -> json.get("code").asText());
        } catch (Exception e) {
            throw new SystemException("Couldn't generate relation for objectClass " + artifactSpec.getObjectClass(), e);
        }

    }



    private String generateSearchAll(ConnDevArtifactType artifactSpec, List<ConnDevHttpEndpointType> endpoints) {
        // TODO: In future when endpoints are editable ensure synchronization of endpoints
        return generateObjectClassScript(artifactSpec, "search/" + ConnDevJsonMapper.toServiceIntent(artifactSpec.getIntent()), "search script");
    }

    private String generateObjectClassScript(ConnDevArtifactType artifactSpec, String endpointSuffix, String scriptDescription) {
        try(var job = client().postJob("codegen/{sessionId}/classes/"+ artifactSpec.getObjectClass() + "/" + endpointSuffix)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), json -> json.get("code").asText());
        } catch (Exception e) {
            throw new SystemException("Couldn't generate " + scriptDescription + " for objectClass " + artifactSpec.getObjectClass(), e);
        }
    }

    @Override
    protected void restoreSession(ServiceClient.RestorationClient client) throws IOException {
        ensureDocumentationIsUploaded(client);
        restoreObjectClasses(client);
        restoreRelations(client);
        restoreEndpoints(client);
        restoreAttributes(client);
        restoreCodegenArtifacts(client);
    }

    private String sessionId() {
        return developmentObject().getOid();
    }

    protected ServiceClient client() {
        return beans.client(sessionId(), this::restoreSession, this::synchronizeSession, result);
    }


    @Override
    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated) {
        try(var job = client().postJob("digester/{sessionId}/classes")) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevBasicObjectClassInfoType>();
                var jsonClasses = o.get("objectClasses");
                for (var jsonClass : jsonClasses) {
                    var objClass = ConnDevJsonMapper.mapObjectClassFromJson(jsonClass);
                    if (objClass.isRelevant() || includeUnrelated) {
                        ret.add(objClass);
                    }
                }
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass) {
        try(var job = client().postJob("digester/{sessionId}/classes/" + objectClass + "/endpoints")) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevHttpEndpointType>();
                var jsonClasses = o.get("endpoints");
                for (var jsonClass : jsonClasses) {
                    ret.add(ConnDevJsonMapper.mapEndpointFromJson(jsonClass));
                }
                return ret;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass) {
        try(var job = client().postJob("digester/{sessionId}/classes/" + objectClass + "/attributes")) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevAttributeInfoType>();
                var jsonAttributes = (ObjectNode) o.get("attributes");
                for (var entry : jsonAttributes.properties()) {
                    ret.add(ConnDevJsonMapper.mapAttributeFromJson(entry.getKey(), entry.getValue()));
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
        var byScrapper = developmentObject().getDocumentationSource();

        var documentations = new ArrayList<ProcessedDocumentation>();
        if (!byScrapper.isEmpty()) {
            downloadUsingScrapper(byScrapper, documentations);
        }

        if (!documentations.isEmpty()) {
            var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                    .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                    .addRealValues(documentations.stream().map(ProcessedDocumentation::toBean).toList())
                    .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
            beans.modelService.executeChanges(List.of(delta), null, task, result);
        }
    }

    private void downloadUsingScrapper(Collection<ConnDevDocumentationSourceType> byScrapper, Collection<ProcessedDocumentation> documentations) {
        var request = scrapperRequest(byScrapper);
        try(var job = client().postJob("scrape/{sessionId}/scrape", request)) {
            var scrapped = job.waitAndProcess(SLEEP_TIME, canRun(), json -> {
                var ret = new ArrayList<ProcessedDocumentation>();

                var savedDocs = json.get("savedDocumentations");

                if (savedDocs != null && savedDocs.isArray()) {
                    for (var doc : savedDocs) {
                        var docId = doc.get("docId").asText();
                        var processed = new ProcessedDocumentation(docId, docId);
                        processed.write(doc.toString());
                        ret.add(processed);
                    }
                }
                return ret;
            });
            documentations.addAll(scrapped);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private ObjectNode scrapperRequest(Collection<ConnDevDocumentationSourceType> byScrapper) {
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

    private boolean isOpenApi(ConnDevDocumentationSourceType doc) {
        return false;
        //var uri = doc.getUri();
        //return uri.endsWith(".json") || uri.endsWith(".yml") || uri.endsWith(".yaml");
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered) {
        try {
            try(var job = client().postJob("digester/{sessionId}/relations")) {
                return job.waitAndProcess(SLEEP_TIME, canRun(), json -> {
                    var ret = new ArrayList<ConnDevRelationInfoType>();
                    var jsonRelations = json.get("relations");
                    for (var object : jsonRelations) {
                        var relation = ConnDevJsonMapper.mapRelationFromJson(object, discovered);
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


}
