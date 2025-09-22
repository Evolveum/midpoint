package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;

import com.evolveum.midpoint.repo.api.RepositoryService;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ServiceClient;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.exception.SystemException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

@Component
public class ConnDevBeans {

    private static ConnDevBeans instance;

    @Autowired public ModelService modelService;
    @Autowired public RepositoryService repositoryService;
    @Autowired public ConnectorInstallationService connectorService;
    @Autowired public ProvisioningService provisioningService;
    @Autowired public SystemObjectCache systemObjectCache;
    @Autowired public MidpointConfiguration configuration;
    private CloseableHttpClient client;

    @PostConstruct
    public void init() {
        instance = this;
        SSLContext trustAllContext = null;
        try {
            trustAllContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();
            var timeout = 5000;
            var defaultConfig = ConnectionConfig.custom()
                    .setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .setSocketTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();

            client = HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setDefaultConnectionConfig(defaultConfig)
                            .setTlsSocketStrategy(new DefaultClientTlsStrategy(trustAllContext))
                            .build()).build();
        } catch (Exception e) {
            throw new RuntimeException("Problem initializing client", e);
        }
    }

    @PreDestroy
    void preDestroy() {
        instance = null;
    }

    public static ConnDevBeans get() {
        return instance;
    }

    public String getServiceUrl(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                return systemConfiguration.getSmartIntegration().getConnectorGenerationUrl();
            }
        } catch (SchemaException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return null;
    }

    public String getFrameworkUrl(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                return systemConfiguration.getSmartIntegration().getConnectorFrameworkUrl();
            }
        } catch (SchemaException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return null;
    }

    public ServiceClient client(OperationResult result) {
        var apiBase = getServiceUrl(result);
        if (apiBase == null) {
            throw new SystemException("Connector Generation Service  not configured.");
        }
        return new ServiceClient(apiBase, client);
    }

    public boolean isOffline() {
        return getServiceUrl(new OperationResult("isOffline")) == null;
    }

    public void downloadFile(URL url, FileOutputStream target) throws IOException {
        if ("file".equals(url.getProtocol())) {
            try (var input = url.openStream()) {
                var readableByteChannel = Channels.newChannel(input);
                var fileChannel = target.getChannel();
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
        } else {
            client.execute(new HttpGet(url.toString()), response -> {
                if (response.getCode() == HttpStatus.SC_OK) {
                    var entity = response.getEntity();
                    if (entity != null) {
                        try (InputStream input = entity.getContent()) {
                            var readableByteChannel = Channels.newChannel(input);
                            var fileChannel = target.getChannel();
                            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                        }
                    }
                } else {
                    throw new IOException("Can not download file " + url.toString());
                }
                return response;
            });
        }
    }

    public String getMidpointHome() {
        return configuration.getMidpointHome();
    }
}
