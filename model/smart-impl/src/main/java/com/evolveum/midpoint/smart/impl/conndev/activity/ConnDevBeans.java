package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorExportService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;

import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationDispatcher;
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
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class ConnDevBeans {

    private static ConnDevBeans instance;

    @Autowired public ModelService modelService;
    @Autowired public RepositoryService repositoryService;
    @Autowired public ClusterwideCacheInvalidationDispatcher cacheDispatcher;
    @Autowired public ConnectorInstallationService connectorService;
    @Autowired public ConnectorExportService connectorExportService;
    @Autowired public ProvisioningService provisioningService;
    @Autowired public SystemObjectCache systemObjectCache;
    @Autowired public MidpointConfiguration configuration;
    @Autowired public Protector protector;
    @Autowired public LocalizationService localizationService;
    private CloseableHttpClient client;

    @PostConstruct
    public void init() {
        instance = this;
        SSLContext trustAllContext = null;
        try {
            trustAllContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .build();
            var timeout = 120_000;
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

    public String getSqlFrameworkUrl(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                return systemConfiguration.getSmartIntegration().getConnectorSqlFrameworkUrl();
            }
        } catch (SchemaException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return null;
    }

    public String getConnectorGeneratorApiKey(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                var apiKey = systemConfiguration.getSmartIntegration().getConnectorGeneratorApiKey();
                return apiKey != null ? protector.decryptString(apiKey) : null;
            }
        } catch (SchemaException | EncryptionException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return null;
    }

    public long getConnectorGenerationTransientErrorTimeoutMs(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            if (systemConfiguration != null && systemConfiguration.getSmartIntegration() != null) {
                var timeout = systemConfiguration.getSmartIntegration().getConnectorGenerationTransientErrorTimeout();
                if (timeout != null) {
                    return timeout.getTimeInMillis(new Date());
                }
            }
        } catch (SchemaException e) {
            throw new SystemException("Could not get system configuration.", e);
        }
        return ServiceClient.DEFAULT_TRANSIENT_ERROR_TIMEOUT_MS;
    }

    public ServiceClient client(String sessionId, ServiceClient.SessionRestoration restoration, ServiceClient.SessionRestoration synchronization, OperationResult result) {
        var apiBase = getServiceUrl(result);
        if (apiBase == null) {
            throw new SystemException("Connector Generation Service  not configured.");
        }
        return new ServiceClient(apiBase, getConnectorGeneratorApiKey(result), sessionId, restoration, synchronization, client,
                getConnectorGenerationTransientErrorTimeoutMs(result));
    }

    public boolean isOffline() {
        return getServiceUrl(new OperationResult("isOffline")) == null;
    }

    private static final String API_KEY_HEADER = "X-Gravitee-Api-Key"; // must match ServiceClient.API_KEY_HEADER

    /**
     * Calls the connector generation service's {@code /ready} endpoint, to be checked right
     * before starting an operation that depends on it - so a missing/unreachable service is
     * reported immediately, instead of only after a task has started and failed.
     */
    public ReadinessStatus checkReadiness(OperationResult result) {
        var apiBase = getServiceUrl(result);
        if (apiBase == null) {
            return new ReadinessStatus(false, translate("ConnDevBeans.readiness.notConfigured"));
        }
        var uri = (apiBase.endsWith("/") ? apiBase : apiBase + "/") + "ready";
        var request = new HttpGet(uri);
        var apiKey = getConnectorGeneratorApiKey(result);
        if (apiKey != null) {
            request.setHeader(API_KEY_HEADER, apiKey);
        }
        try {
            return client.execute(request, response -> fromResponse(response.getCode()));
        } catch (IOException e) {
            return new ReadinessStatus(false, translate("ConnDevBeans.readiness.unreachable", e.getMessage()));
        }
    }

    private ReadinessStatus fromResponse(int code) {
        if (code == HttpStatus.SC_OK) {
            return new ReadinessStatus(true, null);
        }
        if (code == HttpStatus.SC_FORBIDDEN) {
            return new ReadinessStatus(false, translate("ConnDevBeans.readiness.forbidden"));
        }
        return new ReadinessStatus(false, translate("ConnDevBeans.readiness.unreachable", "HTTP " + code));
    }

    private String translate(String key, Object... args) {
        return localizationService.translate(key, args, localizationService.getDefaultLocale(), key);
    }

    /**
     * Result of {@link #checkReadiness}, with a message ready to show the user when {@code ready}
     * is {@code false}: what is wrong, and - where it can be determined - why.
     */
    public record ReadinessStatus(boolean ready, String message) {
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
