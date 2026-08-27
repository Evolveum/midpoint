package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.IOException;
import java.util.List;

public class OfflineBackend extends ConnectorDevelopmentBackend {

    public OfflineBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    protected List<ProcessedDocumentation> synchronizeDocumentation(List<DevShadowDocument> documentation) {
        // The offline backend has no generation service to process documentation, so there is nothing to sync.
        return List.of();
    }


    @Override
    public ConnDevApplicationInfoType discoverBasicInformation(boolean skipCache) {
        // FIXME: Other backends will have HTTP calls here
        return new ConnDevApplicationInfoType()
                .applicationName("Dummy Connector")
                .description("Dummy Backend Application")
                .version("2.0")
                .integrationType(ConnDevIntegrationType.REST);

    }

    @Override
    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(boolean skipCache) {
        return List.of(
                new ConnDevAuthInfoType()
                        .name("Basic Authorization")
                        .type(ConnDevHttpAuthTypeType.BASIC)
                        .quirks(""),
                new ConnDevAuthInfoType()
                        .name("API Key Authorization")
                        .type(ConnDevHttpAuthTypeType.API_KEY)
                        .quirks("Username is `apiKey` and password is API Token."),
                new ConnDevAuthInfoType()
                        .name("OAuth2 Client Credentials")
                        .type(ConnDevHttpAuthTypeType.OAUTH2_CLIENT_CREDENTIALS)
                        .quirks("")
                );
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation(boolean skipCache) {
        return List.of(
                new  ConnDevDocumentationSourceType()
                        .name("Dummy Project - OpenAPI specification")
                        .description("OpenAPI documentation for Dummy Project")
                        .contentType("application/yaml")
                        .uri("https://community.openproject.org/api/v3/spec.yml"),
                new  ConnDevDocumentationSourceType()
                        .name("Dummy Project - API Docuemntation")
                        .description("OpenAPI documentation for Dummy Project")
                        .contentType("application/html")
                        .uri("https://community.openproject.org/api/v3")
        );
    }

    @Override
    public ConnDevArtifactType generateArtifact(ConnDevGenerateArtifactDefinitionType input, boolean skipCache) {
        var artifactSpec = input.getArtifact();
        var ret = artifactSpec.clone();
        if (artifactSpec.getObjectClass() != null) {
            return generateObjectClassArtifact(input, skipCache);
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
                            endpoint("/users/me")
                        }
                        """);
            default -> throw new IllegalStateException("Unexpected value: " + artifactSpec.getIntent());
        };
    }

    @Override
    public ConnDevArtifactType generateObjectClassArtifact(ConnDevGenerateArtifactDefinitionType input, boolean skipCache) {
        var artifactSpec = input.getArtifact();
        var objectClass = artifactSpec.getObjectClass();
        var classification = ConnectorDevelopmentArtifacts.classify(artifactSpec);
        var content = switch (classification) {
            case NATIVE_SCHEMA_DEFINITION -> """
                    objectClass("${objectClass}") {
                        // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                        attribute("id") {
                          jsonType "string"
                          description "Primary Identifier of User"
                          updatable true
                          creatable true
                       }
                       attribute("name") {
                          jsonType "string"
                          description "Login Name of the user"
                          updatable true
                          creatable true

                       }
                       attribute("givenName") {
                          jsonType "string"
                          description "Given Name of the user"
                          updatable true
                          creatable true

                       }
                       attribute("familyName") {
                          jsonType "string"
                          description "Family Name"
                          updatable true
                          creatable true
                       }
                    }
                    """;
            case CONNID_SCHEMA_DEFINITION -> """
                    objectClass("${objectClass}") {
                        // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                        connIdAttribute("UID","id")
                        connIdAttribute("NAME","name")
                    }
                    """;
            case SEARCH_ALL_DEFINITION -> """
                    objectClass("${objectClass}") {
                        search {
                            // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                            // how to write test connection part of the script.
                        }
                    }
                    """;
            case RELATIONSHIP_SCHEMA_DEFINITION -> """
                        relationship("${objectClass}") {
                            // See https://docs.evolveum.com/connectors/scimrest-framework/ for documentation
                            // how to write test connection part of the script.
                        }
                    """;
            case CREATE -> """
                    /*
                    objectClass("${objectClass}") {
                        create {
                            endpoint("users") {
                                httpOperation POST
                            }
                        }
                    }
                    */
                    """;
            case UPDATE -> """
                    /*
                    objectClass("${objectClass}") {
                        update {
                            endpoint(PUT, "users/{id}") {
                                pathParameter "id", attribute("id")
                                absolute true
                            }
                        }
                    }
                    */
                    """;
            case DELETE -> """
                    /*
                    objectClass("${objectClass}") {
                        delete {
                            endpoint(DELETE, "users/{id}") // Short form
                        }
                    }
                    */
                    """;
            default -> throw new IllegalStateException("Unexpected script type: " + classification);
        };
        content = content.replace("${objectClass}", objectClass);
        return artifactSpec.content(content);

    }

    @Override
    public List<ConnDevBasicObjectClassInfoType> discoverObjectClassesUsingDocumentation(List<ConnDevBasicObjectClassInfoType> connectorDiscovered, boolean includeUnrelated, boolean skipCache) {
        return List.of(
                new ConnDevBasicObjectClassInfoType()
                        .name("User")
                        .description("User represents account on the system")
                        ._abstract(false).embedded(false).relevant(true),
                new ConnDevBasicObjectClassInfoType()
                        .name("Group")
                        .description("Group represents group on the system")
                        ._abstract(false).embedded(false).relevant(true)
        );
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverConnectivityEndpoints(boolean skipCache) {
        return List.of(
                new ConnDevHttpEndpointType()
                        .name("Health check")
                        .operation(ConnDevHttpOperationType.GET)
                        .uri("/health"),
                new ConnDevHttpEndpointType()
                        .name("List users")
                        .operation(ConnDevHttpOperationType.GET)
                        .uri("/api/v1/users")
        );
    }

    @Override
    public List<ConnDevHttpEndpointType> discoverObjectClassEndpoints(String objectClass, boolean skipCache) {
        return switch (objectClass) {
            case "User" -> List.of(
                    new ConnDevHttpEndpointType().name("Get User")
                            .operation(ConnDevHttpOperationType.GET)
                            .uri("/user/{id}")
                            .suggestedUse(ConnDevHttpEndpointIntentType.GET_BY_ID)
                    ,
                    new ConnDevHttpEndpointType().name("Get Users")
                            .operation(ConnDevHttpOperationType.GET)
                            .uri("/user/{id}")
                            .suggestedUse(ConnDevHttpEndpointIntentType.GET_ALL)
                            .suggestedUse(ConnDevHttpEndpointIntentType.SEARCH),
                    new ConnDevHttpEndpointType().name("Create User")
                            .operation(ConnDevHttpOperationType.GET)
                            .uri("/user")
                            .suggestedUse(ConnDevHttpEndpointIntentType.CREATE),
                    new ConnDevHttpEndpointType().name("Update User")
                            .operation(ConnDevHttpOperationType.PUT)
                            .uri("/user/{id}")
                            .suggestedUse(ConnDevHttpEndpointIntentType.UPDATE),
                    new ConnDevHttpEndpointType().name("Delete User")
                            .operation(ConnDevHttpOperationType.DELETE)
                            .uri("/user/{id}")
                            .suggestedUse(ConnDevHttpEndpointIntentType.DELETE)
                    );
            case "Group" -> List.of();
            default -> List.of();
        };
    }

    @Override
    public List<ConnDevAttributeInfoType> discoverObjectClassAttributes(String objectClass, boolean skipCache) {
        return switch (objectClass) {
            case "User" -> List.of(
                    new ConnDevAttributeInfoType().name("id")
                            .type("integer")
                            .format("int64")
                            .description("User's id")
                            .mandatory(true)
                            .readable(true)
                            .multivalue(false)
                            .returnedByDefault(true)
                            .connIdAttribute("UID")
                    ,
                    new ConnDevAttributeInfoType().name("name")
                            .type("string")
                            .description("The name of the user")
                            .mandatory(true)
                            .creatable(true)
                            .readable(true)
                            .multivalue(false)
                            .returnedByDefault(true)
                            .connIdAttribute("NAME")
            );
            case "Group" -> List.of(
                    new ConnDevAttributeInfoType().name("id")
                            .type("integer")
                            .format("int64")
                            .description("Group ID")
                            .mandatory(true)
                            .readable(true)
                            .multivalue(false)
                            .returnedByDefault(true)
                            .connIdAttribute("UID"),
                    new ConnDevAttributeInfoType().name("name")
                            .type("string")
                            .description("The name of the group")
                            .mandatory(true)
                            .creatable(true)
                            .readable(true)
                            .multivalue(false)
                            .returnedByDefault(true)
                            .connIdAttribute("NAME")
            );
            default -> List.of();
        };
    }

    @Override
    public void processDocumentation(boolean skipCache) {
        // NOOP
    }

    @Override
    protected void restoreSession(ServiceClient.RestorationClient client) throws IOException {
        // NOOP - offline backend has no remote session to restore
    }

    @Override
    public List<ConnDevRelationInfoType> discoverRelationsUsingObjectClasses(List<ConnDevBasicObjectClassInfoType> discovered, boolean skipCache) {
        return List.of(new ConnDevRelationInfoType()
                    .name("UserGroupMembership")
                    .shortDescription("User's group membership")
                    .subject("User")
                    .subjectAttribute("memberOf")
                    .object("Group")
                    .objectAttribute("members"),
                new ConnDevRelationInfoType()
                    .name("GroupOwnership")
                    .subject("User")
                    .subjectAttribute("ownerOf")
                    .object("Group")
                    .objectAttribute("owner")
        );
    }

}
