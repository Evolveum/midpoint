package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.model.api.util.ResourceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ScimRestConfigurationProperties;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDocumentationSourceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedDocumentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScimBackend extends RestBackend {

    private static final Trace LOGGER = TraceManager.getTrace(ScimBackend.class);

    private static final List<String> SCIM_OBJECT_CLASSES = List.of("conndev_ScimSchema", "conndev_ScimResource");

    public ScimBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache) {
        return super.discoverDocumentation(skipCache);
    }

    @Override
    public void ensureDocumentationIsProcessed() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        super.ensureDocumentationIsProcessed();

        refreshScimDocumentation();
    }

    private void refreshScimDocumentation() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        var testingResourceOid = getTestingResourceOid();

        if (!isScimDiscoveryConfigured(testingResourceOid)) {
            LOGGER.info("Testing resource {} is not configured for SCIM discovery (missing scimBaseUrl or developmentMode), skipping", testingResourceOid);
            return;
        }

        LOGGER.info("Starting SCIM schema discovery from shadows on testing resource {}", testingResourceOid);
        ResourceUtils.deleteSchema(testingResourceOid, beans.modelService, task, result);
        beans.provisioningService.testResource(testingResourceOid, task, result);

        var newScimDocs = SCIM_OBJECT_CLASSES
                .stream()
                .flatMap(objectClass -> loadShadowsAsDocumentation(testingResourceOid, objectClass).stream())
                .map(ProcessedDocumentation::toBean)
                .toList();

        var mergedDocs = new ArrayList<>(
                developmentObject()
                        .getProcessedDocumentation()
                        .stream()
                        .filter(d -> SCIM_OBJECT_CLASSES.stream().noneMatch(c -> d.getUri().startsWith(c)))
                        .map(ProcessedDocumentationType::clone)
                        .toList()
        );
        mergedDocs.addAll(newScimDocs);

        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                .replaceRealValues(mergedDocs)
                .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
        reload();
        ensureDocumentationIsUploaded(client().synchronizationClient());
    }

    private List<ProcessedDocumentation> loadShadowsAsDocumentation(String resourceOid, String objectClassLocalName) {
        try {
            var objectClass = new QName(SchemaConstants.NS_RI, objectClassLocalName);
            var query = PrismContext.get().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
                    .build();

            var shadows = beans.provisioningService.searchObjects(ShadowType.class, query, null, task, result);
            if (shadows.isEmpty()) {
                LOGGER.warn("No shadows found for object class {} on resource {}", objectClassLocalName, resourceOid);
                return List.of();
            }

            var serializer = PrismContext.get().jsonSerializer();
            var mapper = new ObjectMapper();
            var docs = new ArrayList<ProcessedDocumentation>();
            for (var shadow : shadows) {
                var attrs = shadow.findContainer(ShadowType.F_ATTRIBUTES);
                if (attrs != null && !attrs.isEmpty()) {
                    var json = serializer.serialize(attrs.getValue());
                    var parsedAttrs = mapper.readTree(json).get("attributes");
                    var idNode = parsedAttrs.get("id");
                    var name = idNode != null
                            ? idNode.asText()
                            : shadow.getOid();
                    var doc = new ProcessedDocumentation(UUID.randomUUID().toString(), objectClassLocalName + "_" + name);
                    doc.write(parsedAttrs.toString());
                    docs.add(doc);
                }
            }
            return docs;
        } catch (Exception e) {
            throw new SystemException("Could not load shadow documentation for " + objectClassLocalName, e);
        }
    }

    private String getTestingResourceOid() {
        var testing = developmentObject().getTesting();
        if (testing == null || testing.getTestingResource() == null) {
            throw new SystemException("No testing resource configured");
        }
        return testing.getTestingResource().getOid();
    }

    private boolean isScimDiscoveryConfigured(String testingResourceOid) {
        try {
            var resource = beans.modelService.getObject(ResourceType.class, testingResourceOid, null, task, result).asObjectable();
            var connectorConfig = ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);

            var scimBaseUrl = resource.asPrismObject().findProperty(ItemPath.create(connectorConfig, ScimRestConfigurationProperties.SCIM_BASE_URL));
            var developmentMode = resource.asPrismObject().findProperty(ItemPath.create(connectorConfig, ScimRestConfigurationProperties.DEVELOPMENT_MODE));

            var hasScimBaseUrl = scimBaseUrl != null && scimBaseUrl.getRealValue() != null
                    && !((String) scimBaseUrl.getRealValue()).isBlank();
            var hasDevelopmentMode = developmentMode != null && Boolean.TRUE.equals(developmentMode.getRealValue());

            return hasScimBaseUrl && hasDevelopmentMode;
        } catch (Exception e) {
            LOGGER.warn("Could not check configuration on testing resource {}: {}", testingResourceOid, e.getMessage());
            return false;
        }
    }
}
