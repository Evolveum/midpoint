package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import java.util.Optional;
import com.google.common.collect.Lists;

public class CompositeConnectorInfoManager implements ConnectorInfoManager {


    private static final Trace LOGGER = TraceManager.getTrace(CompositeConnectorInfoManager.class);

    private final ConnectorInfoManagerFactory factory;

    private final Map<URI, ConnectorInfoManager> uriToManager = new ConcurrentHashMap<>();
    private final List<ConnectorInfoManager> managers  = Lists.newCopyOnWriteArrayList();

    public CompositeConnectorInfoManager(ConnectorInfoManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public ConnectorInfo findConnectorInfo(ConnectorKey key) {
        for (ConnectorInfoManager manager :managers) {
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

    public void fileAdded(File maybeConnector) {
        registerConnector(maybeConnector);
    }


    private void registerConnector(File bundle) {
        if (ConnectorFactoryConnIdImpl.isThisJarFileBundle(bundle)) {
            registerConnector(bundle.toURI());
        }
    }

    private void registerConnector(URI bundle) {
        if (isLoaded(bundle)) {
            return;
        }
        Optional<ConnectorInfoManager> maybeConnector = connectorFromURL(bundle);
        if (maybeConnector.isPresent()) {
            ConnectorInfoManager connector = maybeConnector.get();
            uriToManager.put(bundle, connector);
            managers.add(connector);
        }
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
}
