/*
 * Copyright (c) 2021 and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import java.util.Optional;
import com.google.common.collect.Lists;

public class DirectoryScanningInfoManager implements ConnectorInfoManager {

    private static final Trace LOGGER = TraceManager.getTrace(DirectoryScanningInfoManager.class);

    private final ConnectorInfoManagerFactory factory;

    private final Map<URI, ConnectorInfoManager> uriToManager = new ConcurrentHashMap<>();
    private final List<ConnectorInfoManager> managers  = Lists.newCopyOnWriteArrayList();
    private final FileAlterationMonitor monitor;
    private final FileAlterationListener listener = new Listener();
    private final ConnectorFactoryConnIdImpl ucfFactory;

    private static final long DEFAULT_POLL_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    public DirectoryScanningInfoManager(ConnectorFactoryConnIdImpl factory, long pollInterval) {
        this.factory = factory.connectorInfoManagerFactory;
        this.ucfFactory = factory;
        this.monitor = new FileAlterationMonitor(pollInterval);
    }

    public DirectoryScanningInfoManager(ConnectorFactoryConnIdImpl connIdFactory) {
        this(connIdFactory, DEFAULT_POLL_INTERVAL);
    }

    @Override
    public ConnectorInfo findConnectorInfo(ConnectorKey key) {
        for (ConnectorInfoManager manager : managers) {
            ConnectorInfo maybe = manager.findConnectorInfo(key);
            if (maybe != null) {
                return maybe;
            }
        }
        return null;
    }

    @Override
    public List<ConnectorInfo> getConnectorInfos() {
        List<ConnectorInfo> ret = new ArrayList<>();
        for (ConnectorInfoManager manager : managers) {
            ret.addAll(manager.getConnectorInfos());
        }
        return ret;
    }

    private List<ConnectorInfo> registerConnector(File bundle) {
        if (ConnectorFactoryConnIdImpl.isThisJarFileBundle(bundle)) {
            return registerConnector(bundle.toURI());
        }
        return List.of();
    }

    List<ConnectorInfo> registerConnector(URI bundle) {
        if (isLoaded(bundle)) {
            return List.of();
        }
        Optional<ConnectorInfoManager> maybeConnector = connectorFromURL(bundle);
        if (maybeConnector.isPresent()) {
            ConnectorInfoManager connector = maybeConnector.get();
            uriToManager.put(bundle, connector);
            managers.add(connector);
            return connector.getConnectorInfos();
        }
        return List.of();
    }


    URI findConnectorUri(ConnectorKey manager) {
        return uriToManager.entrySet().stream().filter(entry -> entry.getValue().findConnectorInfo(manager) != null)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private boolean isLoaded(URI maybeConnector) {
        return uriToManager.containsKey(maybeConnector);
    }

    Optional<ConnectorInfoManager> connectorFromURL(URI bundleUrl) {
        try {
            /*
             * We can not reuse one instance of ConnectorInfoManager, since it is immutable.
             * We need to create it per instance, since connector info construction is hidden
             * behind private methods.
             *
             * Fortunately ConnectorInfoManager does not hold any additional functionality
             * besides initialization of basic connector info and lookup in locally loaded connector
             * infos, thus we can have separate instance per bundle uri.
             *
             */
            ConnectorInfoManager localManager = factory.getLocalManager(bundleUrl.toURL());
            List<ConnectorInfo> connectorInfos = localManager.getConnectorInfos();
            if (connectorInfos == null || connectorInfos.isEmpty()) {
                LOGGER.error("Strange error happened. ConnId is not accepting bundle {}. But no error is indicated.", bundleUrl);
                return Optional.empty();
            } else {
                LOGGER.trace("Found {} compatible connectors in bundle {}", connectorInfos.size(), bundleUrl);
                return Optional.of(localManager);
            }
        } catch (Exception ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Error instantiating ICF bundle using URL '{}': {}", bundleUrl, ex.getMessage(), ex);
            } else {
                LOGGER.error("Error instantiating ICF bundle using URL '{}': {}", bundleUrl, ex.getMessage());
            }
            return Optional.empty();
        }
    }

    public void uriAdded(URI u) {
        registerConnector(u);
    }

    public void watchDirectory(File directory) {
        FileAlterationObserver observer = new FileAlterationObserver(directory);
        observer.addListener(listener);
        monitor.addObserver(observer);
    }

    public void start() {
        try {
            monitor.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void shutdown() {
        try {
            monitor.stop();
        } catch (Exception e) {
            LOGGER.warn("Exception raised during shutdown:_", e);
        }
    }

    class Listener extends FileAlterationListenerAdaptor {

        @Override
        public void onFileChange(File file) {
            if (isLoaded(file.toURI())) {
                LOGGER.warn("Loaded connector bundle {} was modified, System may become unstable.", file);
            } else {
                onFileCreate(file);
            }

        }

        @Override
        public void onFileDelete(File file) {
            if (isLoaded(file.toURI())) {
                LOGGER.error("Deleted loaded connector: {}, System may become unstable.", file);
            }
        }

        @Override
        public void onFileCreate(File file) {
            if (!registerConnector(file).isEmpty()) {
                ucfFactory.notifyConnectorAdded();
            }
        }

    }
}
