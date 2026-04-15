package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestServiceClientFactory implements ServiceClientFactory {

    private ServiceClient client;

    // Used by @TestBean as a factory method.
    public static TestServiceClientFactory create() {
        return new TestServiceClientFactory();
    }

    @Override
    public ServiceClient getServiceClient(OperationResult parentResult) throws SchemaException, ConfigurationException {
        // If a mock client was configured by the test, use it.
        if (this.client != null) {
            return this.client;
        }

        // If tests are running against a real Smart service (URL override present),
        // transparently fall back to the default HTTP client using the override.
        if (DefaultServiceClientImpl.hasServiceUrlOverride()) {
            // Passing null makes DefaultServiceClientImpl use the URL from the system property override.
            return new DefaultServiceClientImpl(null);
        }

        // No mock configured and no URL override -> misconfiguration for tests.
        throw new ConfigurationException("TestServiceClientFactory has no mock client configured and no service URL override is set");
    }

    public void answerWithClient(ServiceClient client) {
        this.client = client;
    }

    /**
     * Configure provided factory mock to return specified client.
     *
     * When {@code TestServiceClientFactory} is used with the
     * {@link org.springframework.test.context.bean.override.convention.TestBean}, then it may be necessary to use the
     * {@link ServiceClientFactory} interface as the type of annotated field in order to correctly override the factory
     * in Spring's application context. That also means, you would need to manually cast it everytime you want to use
     * it. This method does it for you.
     * @param clientFactoryMock the instance of the factory mock you want to configure.
     * @param client the client you want to return from the factory mock.
     */
    public static void mockServiceClient(ServiceClientFactory clientFactoryMock, ServiceClient client) {
        ((TestServiceClientFactory) clientFactoryMock).answerWithClient(client);
    }

}
