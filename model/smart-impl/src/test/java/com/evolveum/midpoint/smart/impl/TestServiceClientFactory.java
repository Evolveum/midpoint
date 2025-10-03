package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;

public class TestServiceClientFactory implements ServiceClientFactory {

    private ServiceClient client;

    // Used by @TestBean as a factory method.
    public static TestServiceClientFactory create() {
        return new TestServiceClientFactory();
    }

    @Override
    public ServiceClient getServiceClient(OperationResult parentResult) {
        return this.client;
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
