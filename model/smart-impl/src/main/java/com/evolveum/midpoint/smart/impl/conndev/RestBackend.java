package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.smart.impl.mappings.ConnDevJsonMapper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class RestBackend extends ConnectorDevelopmentBackend {

    protected static final JsonNodeFactory JSON_FACTORY = JsonNodeFactory.instance;

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentBackend.class);
    private static final int MAX_SCRAPE_ITERATIONS = 2;

    public RestBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public ConnDevApplicationInfoType discoverBasicInformation(boolean skipCache) {
        try(var job = client().postJob("digester/{sessionId}/metadata", null, skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ConnDevApplicationInfoType();

                var jsonInfo = o.path("infoMetadata");
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
                if (jsonInfo.get("apiVersion") != null) {
                    ret.apiVersion(jsonInfo.get("apiVersion").asText());
                }
                ret.integrationType(integrationTypeOf(jsonInfo));
                var baseApiEndpoint = baseApiEndpointOf(jsonInfo);
                if (baseApiEndpoint != null) {
                    ret.baseApiEndpoint(baseApiEndpoint);
                }
                // FIXME: Add dynamic
                return ret;
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't discover basic application information", e);
        }
    }

    /**
     * The digester reports the supported API styles in {@code apiType} (e.g. {@code ["rest","scim"]}).
     * When an application supports both, REST is preferred (the historical default); pure-SCIM
     * applications get SCIM. Missing/unknown values keep the REST default.
     */
    private static ConnDevIntegrationType integrationTypeOf(JsonNode jsonInfo) {
        var apiType = jsonInfo.path("apiType");
        if (apiType.isArray()) {
            for (var type : apiType) {
                if ("rest".equalsIgnoreCase(type.asText())) {
                    return ConnDevIntegrationType.REST;
                }
            }
            for (var type : apiType) {
                if ("scim".equalsIgnoreCase(type.asText())) {
                    return ConnDevIntegrationType.SCIM;
                }
            }
        }
        return ConnDevIntegrationType.REST;
    }

    /**
     * The base API endpoint moved from the top level of {@code infoMetadata} into the per-style
     * availability blocks ({@code restAvailability}/{@code scimAvailability}); the old top-level
     * location is still read as a fallback for older digesters.
     */
    private static String baseApiEndpointOf(JsonNode jsonInfo) {
        for (var container : List.of(
                jsonInfo.path("restAvailability"), jsonInfo.path("scimAvailability"), jsonInfo)) {
            var uri = container.path("baseApiEndpoint").path(0).path("uri");
            if (uri.isTextual() && !uri.asText().isBlank()) {
                return uri.asText();
            }
        }
        return null;
    }

    private ProcessedDocumentation selectBestDocumentation(List<ProcessedDocumentation> processedDocumentation) {
        // FIXME: Select documentation based on classification
        for (var doc : processedDocumentation) {
            if ("application/json".equals(doc.contentType()) || "application/yaml".equals(doc.contentType())) {
                return doc;
            }
        }
        if (processedDocumentation.isEmpty()) {
            throw new SystemException("No processed documentation is available to select from");
        }
        return processedDocumentation.get(0);
    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache) {
        try(var job = client().postJob("digester/{sessionId}/auth", null, skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), json -> {
                var ret = new ArrayList<ConnDevAuthInfoType>();
                for (var jsonAuth : json.get("auth")) {
                    var auth = SupportedAuthorization.fromAiType(jsonAuth.get("type").asText());
                    if (auth != null) {
                        auth.setName(jsonAuth.get("name").asText());
                        auth.quirks(jsonAuth.get("quirks").asText());
                        auth.setRecommended(true);
                        ret.add(auth);
                    }
                }
                return ret;
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't discover authorization information", e);
        }
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache) {

        ObjectNode request = JSON_FACTORY.objectNode();
        request.set("applicationName", JSON_FACTORY.textNode(
                developmentObject().getApplication().getApplicationName().getOrig()));
        request.set("applicationVersion", JSON_FACTORY.textNode(
                Objects.requireNonNullElse(developmentObject().getApplication().getVersion(), "latest")));
        request.set("llmGeneratedSearchQuery", JSON_FACTORY.booleanNode(false));
        try(var jobSpec = client().postJob("discovery/{sessionId}/discovery", request, null, skipCache)) {
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
        } catch (IOException e) {
            throw new SystemException("Couldn't discover candidate links", e);
        }
    }

    @Override
    protected void restoreSession(ServiceClient.RestorationClient client) throws IOException {
        restoreMetadata(client);
        ensureDocumentationIsUploaded(client);
        restoreObjectClasses(client);
        restoreRelations(client);
        restoreEndpoints(client);
        restoreAttributes(client);
        restoreCodegenArtifacts(client);
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache) {
        try (var job = client().postJob("digester/{sessionId}/connectivity-endpoint", null, skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevHttpEndpointType>();
                var jsonEndpoints = o.get("endpoints");
                for (var jsonEndpoint : jsonEndpoints) {
                    ret.add(ConnDevJsonMapper.mapEndpointFromJson(jsonEndpoint));
                }
                if (ret.isEmpty()) {
                    var jsonErrors = o.get("errors");
                    if (jsonErrors != null && !jsonErrors.isEmpty()) {
                        throw new SystemException("Connectivity endpoint discovery failed with errors: " + jsonErrors);
                    }
                }
                return ret;
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't discover connectivity endpoints", e);
        }
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass, boolean skipCache) {
        try(var job = client().postJob("digester/{sessionId}/classes/" + objectClass + "/endpoints", apiType(), skipCache)) {
            return job.waitAndProcess(SLEEP_TIME, canRun(), o -> {
                var ret = new ArrayList<ConnDevHttpEndpointType>();
                var jsonClasses = o.get("endpoints");
                for (var jsonClass : jsonClasses) {
                    ret.add(ConnDevJsonMapper.mapEndpointFromJson(jsonClass));
                }
                return ret;
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't discover endpoints for object class " + objectClass, e);
        }
    }


    @Override
    public void processDocumentation(boolean skipCache) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            PolicyViolationException, ObjectAlreadyExistsException, SubscriptionComplianceException {
        ConnDevDocumentationSourceType openApi = null;
        var byScrapper = developmentObject().getDocumentationSource();

        var documentations = new ArrayList<ProcessedDocumentation>();
        if (!byScrapper.isEmpty()) {
            downloadUsingScrapper(byScrapper, documentations, skipCache);
        }

        if (!documentations.isEmpty()) {
            var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                    .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                    .addRealValues(documentations.stream().map(ProcessedDocumentation::toBean).toList())
                    .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
            beans.modelService.executeChanges(List.of(delta), null, task, result);
        }
    }

    private void downloadUsingScrapper(Collection<ConnDevDocumentationSourceType> byScrapper, Collection<ProcessedDocumentation> documentations, boolean skipCache) {
        var request = scrapperRequest(byScrapper);
        try(var job = client().postJob("scrape/{sessionId}/scrape", request, null, skipCache)) {
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
            throw new SystemException("Couldn't scrape documentation", e);
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
            throw new SystemException("Couldn't download documentation from " + openApi.getUri(), e);
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
    public List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered, boolean skipCache) {
        try {
            try(var job = client().postJob("digester/{sessionId}/relations", null, skipCache)) {
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
        } catch (IOException e) {
            throw new SystemException("Couldn't discover relations between object classes", e);
        }
    }


}
