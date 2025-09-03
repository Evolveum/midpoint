/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.prism.schema.PrismSchemaBuildingUtil.addNewComplexTypeDefinition;
import static com.evolveum.midpoint.prism.schema.PrismSchemaBuildingUtil.addNewContainerDefinition;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICF_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME;
import static com.evolveum.midpoint.schema.processor.ConnectorSchema.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.Key;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.evolveum.midpoint.prism.PrismPropertyDefinition.PrismPropertyDefinitionMutator;
import com.evolveum.midpoint.prism.impl.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.processor.ConnectorSchema;
import com.evolveum.midpoint.schema.processor.ConnectorSchemaFactory;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;

import jakarta.annotation.PostConstruct;
import javax.net.ssl.TrustManager;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;

import org.apache.commons.configuration2.Configuration;
import org.identityconnectors.common.Version;
import org.identityconnectors.common.security.Encryptor;
import org.identityconnectors.common.security.EncryptorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorDiscoveryListener;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.UcfUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Implementation of the UCF Connector Manager API interface for ConnId framework.
 *
 * @author Radovan Semancik
 */
@Component
public class ConnectorFactoryConnIdImpl implements ConnectorFactory {

    public static final String ICFS_NAME_DISPLAY_NAME = "ConnId Name";
    public static final int ICFS_NAME_DISPLAY_ORDER = 110;
    public static final String ICFS_UID_DISPLAY_NAME = "ConnId UID";
    public static final int ICFS_UID_DISPLAY_ORDER = 100;

    static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME = "connectorPoolConfiguration";
    public static final ItemName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT =
            ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "connectorPoolConfiguration");
    public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE =
            new QName(SchemaConstants.NS_ICF_CONFIGURATION, "ConnectorPoolConfigurationType");
    protected static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_EVICTABLE_IDLE_TIME_MILLIS = "minEvictableIdleTimeMillis";
    public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_IDLE = "minIdle";
    public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_IDLE = "maxIdle";
    public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_OBJECTS = "maxObjects";
    public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_WAIT = "maxWait";
    public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_IDLE_TIME_MILLIS = "maxIdleTimeMillis";

    public static final ItemName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT =
            ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "producerBufferSize");
    public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE = DOMUtil.XSD_INT;

    static final ItemName CONNECTOR_SCHEMA_LEGACY_SCHEMA_ELEMENT =
            ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION, "legacySchema");
    public static final QName CONNECTOR_SCHEMA_LEGACY_SCHEMA_TYPE = DOMUtil.XSD_BOOLEAN;

    public static final String CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME = "timeouts";
    public static final ItemName CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT = ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION,
            CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME);
    public static final QName CONNECTOR_SCHEMA_TIMEOUTS_TYPE = new QName(SchemaConstants.NS_ICF_CONFIGURATION, "TimeoutsType");

    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME = "resultsHandlerConfiguration";
    public static final ItemName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT = ItemName.from(SchemaConstants.NS_ICF_CONFIGURATION,
            CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME);
    public static final QName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_TYPE = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
            "ResultsHandlerConfigurationType");
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_NORMALIZING_RESULTS_HANDLER = "enableNormalizingResultsHandler";
    public static final ItemName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_NORMALIZING_RESULTS_HANDLER_ITEM_NAME =
            new ItemName(SchemaConstants.NS_ICF_CONFIGURATION, CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_NORMALIZING_RESULTS_HANDLER);
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_FILTERED_RESULTS_HANDLER = "enableFilteredResultsHandler";
    public static final ItemName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_FILTERED_RESULTS_HANDLER_ITEM_NAME =
            new ItemName(SchemaConstants.NS_ICF_CONFIGURATION, CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_FILTERED_RESULTS_HANDLER);
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE = "filteredResultsHandlerInValidationMode";
    public static final ItemName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE_ITEM_NAME =
            new ItemName(SchemaConstants.NS_ICF_CONFIGURATION, CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE);
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_CASE_INSENSITIVE_HANDLER = "enableCaseInsensitiveFilter";
    public static final ItemName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_CASE_INSENSITIVE_HANDLER_ITEM_NAME =
            new ItemName(SchemaConstants.NS_ICF_CONFIGURATION, CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_CASE_INSENSITIVE_HANDLER);
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER = "enableAttributesToGetSearchResultsHandler";
    public static final ItemName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER_ITEM_NAME =
            new ItemName(SchemaConstants.NS_ICF_CONFIGURATION, CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER);

    static final Map<String, Collection<Class<? extends APIOperation>>> API_OP_MAP = new HashMap<>();

    private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = SchemaConstants.ICF_FRAMEWORK_URI + "/bundle/";
    private static final String CONNECTOR_IDENTIFIER_SEPARATOR = "/";

    public static final int ATTR_DISPLAY_ORDER_START = 120;
    public static final int ATTR_DISPLAY_ORDER_INCREMENT = 10;

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryConnIdImpl.class);

    static {
        // Forces JUL logger to be loaded by the parent classloader so we can correctly
        // adjust the log levels from the main code.
        java.util.logging.Logger.getLogger(ConnectorFactoryConnIdImpl.class.getName());
    }

    ConnectorInfoManagerFactory connectorInfoManagerFactory;
    private DirectoryScanningInfoManager localConnectorInfoManager;
    private Set<URI> bundleURIs;
    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private Protector protector;
    @Autowired private PrismContext prismContext;
    @Autowired private LocalizationService localizationService;
    private CopyOnWriteArrayList<ConnectorDiscoveryListener> listeners = new CopyOnWriteArrayList<>();

    public ConnectorFactoryConnIdImpl() {
    }

    /**
     * Initialize the ICF implementation. Look for all connector bundles, get
     * basic information about them and keep that in memory.
     */
    @PostConstruct
    public void initialize() {

        connectorInfoManagerFactory = ConnectorInfoManagerFactory.getInstance();
        localConnectorInfoManager = new DirectoryScanningInfoManager(this);

        bundleURIs = new HashSet<>();

        Configuration config = midpointConfiguration.getConfiguration(MidpointConfiguration.ICF_CONFIGURATION);

        // Is classpath scan enabled
        if (config.getBoolean("scanClasspath")) {
            // Scan class path
            bundleURIs.addAll(scanClassPathForBundles());
        }

        // Scan all provided directories
        List<Object> dirs = config.getList("scanDirectory");
        for (Object dir : dirs) {
            bundleURIs.addAll(scanAndWatchDirectory(dir.toString()));
        }

        for (URI u : bundleURIs) {
            LOGGER.debug("ICF bundle URI : {}", u);
            localConnectorInfoManager.uriAdded(u);
        }
        localConnectorInfoManager.start();
    }

    /**
     * Creates new connector instance.
     * <p>
     * It will initialize the connector by taking the XML Resource definition,
     * transforming it to the ICF configuration and applying that to the new
     * connector instance.
     */
    @Override
    public @NotNull ConnectorInstance createConnectorInstance(
            @NotNull ConnectorType connectorBean, String instanceName, String instanceDescription)
            throws ObjectNotFoundException, SchemaException {

        ConnectorInfo cinfo = getConnectorInfo(connectorBean);

        if (cinfo == null) {
            LOGGER.error("Failed to instantiate {}", ObjectTypeUtil.toShortString(connectorBean));
            LOGGER.debug("Connector key: {}, host: {}", getConnectorKey(connectorBean),
                    ObjectTypeUtil.toShortString(connectorBean));
            LOGGER.trace("Connector object: {}", ObjectTypeUtil.dump(connectorBean));
            var connectorHostRef = connectorBean.getConnectorHostRef();
            if (connectorHostRef != null) {
                var connectorHost = connectorHostRef.asReferenceValue().getObject();
                if (connectorHost == null) {
                    LOGGER.trace("Connector host ref: {}", connectorHostRef);
                } else {
                    LOGGER.trace("Connector host object:\n{}", connectorHost.debugDump(1));
                }
            }
            // TODO Is this really ObjectNotFoundException?
            throw new ObjectNotFoundException(
                    String.format(
                            "The classes (JAR) of %s were not found by the ICF framework; bundle=%s connector type=%s, version=%s",
                            ObjectTypeUtil.toShortString(connectorBean),
                            connectorBean.getConnectorBundle(),
                            connectorBean.getConnectorType(),
                            connectorBean.getConnectorVersion()));
        }

        ConnectorSchema connectorSchema;
        var storedConnectorSchema = ConnectorTypeUtil.parseConnectorSchemaIfPresent(connectorBean);
        if (storedConnectorSchema != null) {
            connectorSchema = storedConnectorSchema;
        } else {
            connectorSchema = generateConnectorConfigurationSchema(cinfo, connectorBean);
        }

        return new ConnectorInstanceConnIdImpl(
                cinfo, connectorBean, connectorSchema, instanceName, instanceDescription);
    }

    /**
     * Returns a list XML representation of the ICF connectors.
     */
    @Override
    public Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentResult)
            throws CommunicationException {
        OperationResult result = parentResult.createSubresult(ConnectorFactory.OPERATION_LIST_CONNECTORS);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryConnIdImpl.class);
        result.addParam("host", host);

        try {
            if (host == null) {
                Set<ConnectorType> connectors = listLocalConnectors();
                result.recordSuccess();
                return connectors;
            } else {
                // This is necessary as list of the remote connectors is cached locally.
                // So if any remote connector is added then it will not be discovered unless we
                // clear the cache. This may look like inefficiency but in fact the listConnectors() method is
                // used only when discovering new connectors. Normal connector operation is using connector objects
                // stored in repository.
                connectorInfoManagerFactory.clearRemoteCache();
                Set<ConnectorType> connectors = listRemoteConnectors(host);
                result.recordSuccess();
                return connectors;
            }
        } catch (Throwable icfException) {
            Throwable ex = processConnIdException(icfException, "list connectors", result);
            result.recordFatalError(ex.getMessage(), ex);
            if (ex instanceof CommunicationException communicationException) {
                throw communicationException;
            } else if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (ex instanceof Error error) {
                throw error;
            } else {
                throw new SystemException("Unexpected ICF exception: " + ex.getMessage(), ex);
            }
        }
    }

    private Set<ConnectorType> listLocalConnectors() {
        Set<ConnectorType> localConnectorTypes = new HashSet<>();

        // Fetch list of local connectors from ICF
        List<ConnectorInfo> connectorInfos = getLocalConnectorInfoManager().getConnectorInfos();

        for (ConnectorInfo connectorInfo : connectorInfos) {
            ConnectorType connectorType;
            try {
                connectorType = convertToConnectorType(connectorInfo, null);
                localConnectorTypes.add(connectorType);
            } catch (SchemaException e) {
                LOGGER.error("Schema error while initializing ICF connector {}: {}", getConnectorDesc(connectorInfo), e.getMessage(), e);
            }
        }
        return localConnectorTypes;
    }

    private Set<ConnectorType> listRemoteConnectors(ConnectorHostType host) {
        ConnectorInfoManager remoteConnectorInfoManager = getRemoteConnectorInfoManager(host);
        Set<ConnectorType> connectorTypes = new HashSet<>();
        List<ConnectorInfo> connectorInfos = remoteConnectorInfoManager.getConnectorInfos();
        for (ConnectorInfo connectorInfo : connectorInfos) {
            try {
                ConnectorType connectorType = convertToConnectorType(connectorInfo, host);
                connectorTypes.add(connectorType);
            } catch (SchemaException e) {
                LOGGER.error("Schema error while initializing ICF connector {}: {}", getConnectorDesc(connectorInfo), e.getMessage(), e);
            }
        }
        return connectorTypes;
    }

    private String getConnectorDesc(ConnectorInfo connectorInfo) {
        return connectorInfo.getConnectorKey().getConnectorName();
    }

    /**
     * Converts ICF ConnectorInfo into a midPoint XML connector representation.
     * <p>
     * TODO: schema transformation
     *
     * @param hostType host that this connector runs on or null for local connectors
     */
    private ConnectorType convertToConnectorType(ConnectorInfo cinfo, ConnectorHostType hostType) throws SchemaException {
        ConnectorType connectorType = new ConnectorType();
        ConnectorKey key = cinfo.getConnectorKey();
        UcfUtil.addConnectorNames(connectorType, "ConnId", key.getConnectorName(), key.getBundleVersion(), hostType);
        String stringID = keyToNamespaceSuffix(key);
        connectorType.setFramework(SchemaConstants.ICF_FRAMEWORK_URI);
        connectorType.setConnectorType(key.getConnectorName());
        var namespace = ICF_CONFIGURATION_NAMESPACE_PREFIX + stringID;
        connectorType.setNamespace(namespace);
        // We register global mapping of connector namespace
        GlobalDynamicNamespacePrefixMapper.registerPrefixGlobal(namespace, SchemaConstants.CONNECTOR_CONFIGURATION_PREFIX);
        connectorType.setConnectorVersion(key.getBundleVersion());
        connectorType.setConnectorBundle(key.getBundleName());
        if (hostType != null) {
            // bind using connectorHostRef and OID
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(hostType.getOid());
            ref.setType(ObjectTypes.CONNECTOR_HOST.getTypeQName());
            ref.asReferenceValue().setObject(hostType.asPrismObject());
            connectorType.setConnectorHostRef(ref);
        }

        PrismSchema connectorSchema = Objects.requireNonNull(generateConnectorConfigurationSchema(cinfo, connectorType));
        LOGGER.trace("Generated connector schema for {}: {} definitions", connectorType, connectorSchema.size());
        UcfUtil.setConnectorSchema(connectorType, connectorSchema);

        return connectorType;
    }

    /**
     * Converts ICF connector key to a simple string.
     * <p>
     * The string may be used as an OID.
     */
    private String keyToNamespaceSuffix(ConnectorKey key) {
        return key.getBundleName()
                + CONNECTOR_IDENTIFIER_SEPARATOR
                // Don't include version. It is lesser evil.
                // + key.getBundleVersion();
                // + CONNECTOR_IDENTIFIER_SEPARATOR;
                + key.getConnectorName();
    }

    @Override
    public @Nullable ConnectorSchema generateConnectorConfigurationSchema(@NotNull ConnectorType connectorBean)
            throws ObjectNotFoundException, SchemaException {
        ConnectorInfo cinfo = getConnectorInfo(connectorBean);
        if (cinfo == null) {
            throw new ObjectNotFoundException(
                    "Connector " + connectorBean + " cannot be found by ConnId framework",
                    ConnectorType.class, connectorBean.getOid());
        }
        return generateConnectorConfigurationSchema(cinfo, connectorBean);
    }

    private @Nullable ConnectorSchema generateConnectorConfigurationSchema(ConnectorInfo cinfo, ConnectorType connectorBean) throws SchemaException {

        LOGGER.trace("Generating configuration schema for {}", this);

        // Configuration properties as provided by ConnId
        ConfigurationProperties icfConfigurationProperties = cinfo.createDefaultAPIConfiguration().getConfigurationProperties();

        if (icfConfigurationProperties == null
                || icfConfigurationProperties.getPropertyNames() == null
                || icfConfigurationProperties.getPropertyNames().isEmpty()) {
            LOGGER.debug("No configuration schema for {}", this);
            return null;
        }

        var connectorSchema = ConnectorSchemaFactory.newConnectorSchema(connectorBean.getNamespace());

        // Definition of "connectorConfiguration" container - the root one; contains both generic ConnId items + specific ones
        var configurationContainerDef =
                addNewContainerDefinition(
                        connectorSchema, CONNECTOR_CONFIGURATION_LOCAL_NAME, CONNECTOR_CONFIGURATION_TYPE_LOCAL_NAME);

        // CTD for its "configurationProperties" child - these are properties specific to the connector
        var configPropertiesCtd =
                addNewComplexTypeDefinition(connectorSchema, ICF_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME);

        int displayOrder = 1;
        for (String icfPropertyName : icfConfigurationProperties.getPropertyNames()) {
            ConfigurationProperty icfProperty = icfConfigurationProperties.getProperty(icfPropertyName);

            var type = icfProperty.getType();
            boolean multivalue;
            Class<?> componentType;
            // For multi-valued configuration properties we are only interested in the component type.
            // We consider arrays to be multi-valued ... unless it is byte[] or char[]
            if (type.isArray() && !type.equals(byte[].class) && !type.equals(char[].class)) {
                multivalue = true;
                componentType = type.getComponentType();
            } else {
                multivalue = false;
                componentType = type;
            }

            var xsdTypeName = ConnIdTypeMapper.connIdTypeToXsdTypeName(
                    componentType, null, icfProperty.isConfidential(), null);
            LOGGER.trace("{}: Mapping ICF config schema property {} from {} to {} (multi: {})", this,
                    icfPropertyName, icfProperty.getType(), xsdTypeName, multivalue);
            PrismPropertyDefinitionMutator<?> propertyDefinition =
                    configPropertiesCtd.mutator().createPropertyDefinition(icfPropertyName, xsdTypeName);
            propertyDefinition.setDisplayName(icfProperty.getDisplayName(null));
            propertyDefinition.setHelp(icfProperty.getHelpMessage(null));
            propertyDefinition.setMaxOccurs(multivalue ? -1 : 1);
            if (icfProperty.isRequired() && icfProperty.getValue() == null) {
                // If ICF says that the property is required it may not be in fact really required if it also has a default value
                propertyDefinition.setMinOccurs(1);
            } else {
                propertyDefinition.setMinOccurs(0);
            }
            propertyDefinition.setDisplayOrder(displayOrder);
            displayOrder++;
        }

        // Create common ICF configuration property containers as a references to a static schema
        configurationContainerDef.mutator().createContainerDefinition(
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT,
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE, 0, 1);
        configurationContainerDef.mutator().createPropertyDefinition(
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT,
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE, 0, 1);
        configurationContainerDef.mutator().createContainerDefinition(
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT,
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_TIMEOUTS_TYPE, 0, 1);
        configurationContainerDef.mutator().createContainerDefinition(
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT,
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_TYPE, 0, 1);
        configurationContainerDef.mutator().createPropertyDefinition(
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_LEGACY_SCHEMA_ELEMENT,
                ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_LEGACY_SCHEMA_TYPE, 0, 1);

        configurationContainerDef.mutator().createContainerDefinition(
                SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME,
                configPropertiesCtd, 1, 1);

        LOGGER.debug("Generated configuration schema for {}: {} definitions", this, connectorSchema.size());
        return connectorSchema;
    }

    /**
     * Returns ICF connector info manager that manages local connectors. The
     * manager will be created if it does not exist yet.
     *
     * @return ICF connector info manager that manages local connectors
     */

    DirectoryScanningInfoManager getLocalConnectorInfoManager() {
        return localConnectorInfoManager;
    }

    /**
     * Returns ICF connector info manager that manages local connectors. The
     * manager will be created if it does not exist yet.
     *
     * @return ICF connector info manager that manages local connectors
     */
    private ConnectorInfoManager getRemoteConnectorInfoManager(ConnectorHostType hostType) {
        String hostname = hostType.getHostname();
        int port = Integer.parseInt(hostType.getPort());
        GuardedString key;
        try {
            key = new GuardedString(protector.decryptString(hostType.getSharedSecret()).toCharArray());
        } catch (EncryptionException e) {
            throw new SystemException("Shared secret decryption error: " + e.getMessage(), e);
        }
        Integer timeout = hostType.getTimeout();
        if (timeout == null) {
            timeout = 0;
        }
        boolean useSSL = false;
        if (hostType.isProtectConnection() != null) {
            useSSL = hostType.isProtectConnection();
        }
        List<TrustManager> trustManagers = protector.getTrustManagers();
        LOGGER.trace("Creating RemoteFrameworkConnectionInfo: hostname={}, port={}, key={}, useSSL={}, trustManagers={}, timeout={}",
                hostname, port, key, useSSL, trustManagers, timeout);
        RemoteFrameworkConnectionInfo remoteFramewrorkInfo = new RemoteFrameworkConnectionInfo(hostname, port, key, useSSL, trustManagers, timeout);
        return connectorInfoManagerFactory.getRemoteManager(remoteFramewrorkInfo);
    }

    /**
     * Scan class path for connector bundles
     *
     * @return Set of all bundle URL
     */
    private Set<URI> scanClassPathForBundles() {
        Set<URI> bundle = new HashSet<>();

        // scan class path for bundles
        Enumeration<URL> en = null;
        try {
            // Search all jars in classpath
            en = ConnectorFactoryConnIdImpl.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        } catch (IOException ex) {
            LOGGER.debug("Error during reading content from class path");
            // TODO return? or NPE on the while below?
        }

        // Find which one is ICF connector
        while (en.hasMoreElements()) {
            URL u = en.nextElement();

            Properties prop = new Properties();
            LOGGER.trace("Scan classloader resource: " + u.toString());
            try {
                // read content of META-INF/MANIFEST.MF
                InputStream is = u.openStream();
                // skip if unreadable
                if (is == null) {
                    continue;
                }
                // Convert to properties
                prop.load(is);
            } catch (IOException ex) {
                LOGGER.trace("Unable load: " + u + " [" + ex.getMessage() + "]");
            }
// tomcat
// toString >>> jar:file:/<ABSOLUTE_PATH_TO_TOMCAT_WEBAPPS>/midpoint/WEB-INF/lib/connector-csv-2.1-SNAPSHOT.jar!/META-INF/MANIFEST.MF
// getPath  >>>     file:/<ABSOLUTE_PATH_TO_TOMCAT_WEBAPPS>/WEB-INF/lib/connector-csv-2.1-SNAPSHOT.jar!/META-INF/MANIFEST.MF

// boot
// toString >>> jar:file:/<ABSOLUTE_PATH_TO_WAR_FOLDER>/midpoint.war!/WEB-INF/lib/connector-csv-2.1-SNAPSHOT.jar!/META-INF/MANIFEST.MF
// getPath  >>>     file:/<ABSOLUTE_PATH_TO_WAR_FOLDER>/midpoint.war!/WEB-INF/lib/connector-csv-2.1-SNAPSHOT.jar!/META-INF/MANIFEST.MF

            if (null != prop.get("ConnectorBundle-Name")) {
                LOGGER.info("Discovered ICF bundle on CLASSPATH: " + prop.get("ConnectorBundle-Name") + " version: "
                        + prop.get("ConnectorBundle-Version"));

                // hack to split MANIFEST from name
                try {
                    String upath = u.toString();
                    if (upath.split("!/").length != 3) {
                        // we're running standard war in application server
                        upath = u.getPath();
                    }

                    URL tmp = new URL(toUrl(upath.substring(0, upath.lastIndexOf("!"))));
                    if (isThisBundleCompatible(tmp)) {
                        try {
                            bundle.add(tmp.toURI());
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    } else {
                        LOGGER.warn("Skip loading ICF bundle {} due error occurred", tmp);
                    }
                } catch (MalformedURLException e) {
                    LOGGER.error("This never happened we hope. URL:" + u.getPath(), e);
                    throw new SystemException(e);
                }
            }
        }
        return bundle;
    }

    private String toUrl(String string) {
        if (string.contains(":")) {
            // We are OK, there is a protocol section
            return string;
        }
        // We assume file protocol as default
        return "file:" + string;
    }

    /**
     * Scans the directory for ConnId connector bundles and schedules it for later scanning.
     *
     * As a special case, checks if the path provided points to a single JAR file. (No watching in that case.)
     *
     * @return Collection of bundle URIs found.
     */
    private Set<URI> scanAndWatchDirectory(String path) {

        Set<URI> bundleUris = new HashSet<>();
        File dir = new File(path);

        // Test for the special case: is this path a single JAR file?
        if (isThisJarFileBundle(dir)) {
            addBundleIfEligible(bundleUris, dir);
            return bundleUris;
        }

        if (dir.isDirectory()) {
            scanDirectoryNow(bundleUris, dir);
        } else {
            LOGGER.error("Provided ConnId connector path {} is not a directory.", dir.getAbsolutePath());
        }

        // TODO is this OK even if the "dir" is not a directory?
        localConnectorInfoManager.watchDirectory(dir);

        return bundleUris;
    }

    private void scanDirectoryNow(Set<URI> bundleUris, File dir) {
        File[] dirEntries = dir.listFiles();
        if (dirEntries == null) {
            LOGGER.debug("No bundles found in directory {}", dir.getAbsolutePath());
            return;
        }

        for (File dirEntry : dirEntries) {
            if (isThisJarFileBundle(dirEntry)) {
                addBundleIfEligible(bundleUris, dirEntry);
            }
            if (dirEntry.isDirectory() && new File(dirEntry, "META-INF/MANIFEST.MF").exists()) {
                try {
                    var uri = dirEntry.toURI();
                    if (isThisBundleCompatible(uri.toURL())) {
                        bundleUris.add(uri);
                    }
                } catch (MalformedURLException e) {
                    throw new SystemException(e);
                }
            }
        }
    }

    private void addBundleIfEligible(Set<URI> bundle, File dirEntry) {
        try {
            final URI uri = dirEntry.toURI();
            if (isThisBundleCompatible(uri.toURL())) {
                bundle.add(uri);
            } else {
                LOGGER.warn("Skip loading bundle {} due error occurred", uri.toURL());
            }
        } catch (MalformedURLException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Test if bundle internal configuration and dependencies are OK
     *
     * @param bundleUrl tested bundle URL
     * @return true if OK
     */
    Boolean isThisBundleCompatible(URL bundleUrl) {
        if (null == bundleUrl) {
            return false;
        }
        try {
            ConnectorInfoManager localManager = ConnectorInfoManagerFactory.getInstance().getLocalManager(bundleUrl);
            List<ConnectorInfo> connectorInfos = localManager.getConnectorInfos();
            if (connectorInfos == null || connectorInfos.isEmpty()) {
                LOGGER.error("Strange error happened. ConnId is not accepting bundle {}. But no error is indicated.", bundleUrl);
                return false;
            } else {
                LOGGER.trace("Found {} compatible connectors in bundle {}", connectorInfos.size(), bundleUrl);
                return true;
            }
        } catch (Exception ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Error instantiating ICF bundle using URL '{}': {}", bundleUrl, ex.getMessage(), ex);
            } else {
                LOGGER.error("Error instantiating ICF bundle using URL '{}': {}", bundleUrl, ex.getMessage());
            }
            return false;
        }
    }

    /**
     * Test if provided file is connector bundle
     *
     * @param file tested file
     * @return boolean
     */
    static Boolean isThisJarFileBundle(File file) {
        // Startup tests
        if (null == file) {
            throw new IllegalArgumentException("No file is provided for bundle test.");
        }

        // Skip all processing if it is not a file
        if (!file.isFile()) {
            LOGGER.debug("This {} is not a file", file.getAbsolutePath());
            return false;
        }

        if (file.getName().endsWith(".tmp")) {
            LOGGER.debug("This {} is a temporary file", file.getAbsolutePath());
            return false;
        }

        Properties prop = new Properties();
        JarFile jar;
        // Open jar file
        try {
            jar = new JarFile(file);
        } catch (IOException ex) {
            LOGGER.debug("Unable to read jar file: " + file.getAbsolutePath() + " [" + ex.getMessage() + "]");
            return false;
        }

        // read jar file
        InputStream is;
        try {
            JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
            is = jar.getInputStream(entry);
        } catch (IOException ex) {
            LOGGER.debug("Unable to fine MANIFEST.MF in jar file: " + file.getAbsolutePath() + " [" + ex.getMessage()
                    + "]");
            return false;
        }

        // Parse jar file
        try {
            prop.load(is);
        } catch (IOException | NullPointerException ex) {
            LOGGER.debug("Unable to parse jar file: " + file.getAbsolutePath() + " [" + ex.getMessage() + "]");
            return false;
        }

        // Test if it is a connector
        if (null != prop.get("ConnectorBundle-Name")) {
            LOGGER.info("Discovered ICF bundle in JAR: " + prop.get("ConnectorBundle-Name") + " version: "
                    + prop.get("ConnectorBundle-Version"));
            return true;
        }

        LOGGER.debug("Provided file {} is not ConnId bundle jar", file.getAbsolutePath());
        return false;
    }

    /**
     * Gets ConnId {@link ConnectorInfo} for given midPoint {@link ConnectorType} object.
     *
     * Deals with both local and remove (connector host based) ConnId connectors.
     */
    private ConnectorInfo getConnectorInfo(ConnectorType connectorBean) throws ObjectNotFoundException {
        if (!SchemaConstants.ICF_FRAMEWORK_URI.equals(connectorBean.getFramework())) {
            throw new ObjectNotFoundException(
                    String.format("Requested connector for framework %s cannot be found in framework %s",
                            connectorBean.getFramework(), SchemaConstants.ICF_FRAMEWORK_URI),
                    ConnectorType.class, null);
        }
        ConnectorKey key = getConnectorKey(connectorBean);
        if (connectorBean.getConnectorHostRef() == null) {
            // Local connector
            return getLocalConnectorInfoManager().findConnectorInfo(key);
        }
        PrismObject<ConnectorHostType> host = connectorBean.getConnectorHostRef().asReferenceValue().getObject();
        if (host == null) {
            throw new ObjectNotFoundException(
                    "Attempt to use remote connector without ConnectorHostType resolved (there is only ConnectorHostRef)");
        }
        return getRemoteConnectorInfoManager(host.asObjectable()).findConnectorInfo(key);
    }

    static ConnectorKey getConnectorKey(ConnectorType connectorType) {
        return new ConnectorKey(
                connectorType.getConnectorBundle(),
                connectorType.getConnectorVersion(),
                connectorType.getConnectorType());
    }

    @Override
    public void selfTest(OperationResult parentTestResult) {
        selfTestGuardedString(parentTestResult);
    }

    @Override
    public boolean supportsFramework(String frameworkIdentifier) {
        return SchemaConstants.ICF_FRAMEWORK_URI.equals(frameworkIdentifier);
    }

    @Override
    public String getFrameworkVersion() {
        Version version = FrameworkUtil.getFrameworkVersion();
        if (version == null) {
            return null;
        }
        return version.getVersion();
    }

    private void selfTestGuardedString(OperationResult parentTestResult) {
        OperationResult result = parentTestResult.createSubresult(ConnectorFactoryConnIdImpl.class + ".selfTestGuardedString");

        OperationResult subresult = result.createSubresult(ConnectorFactoryConnIdImpl.class + ".selfTestGuardedString.encryptorReflection");
        EncryptorFactory encryptorFactory = EncryptorFactory.getInstance();
        subresult.addReturn("encryptorFactoryImpl", encryptorFactory.getClass());
        LOGGER.debug("Encryptor factory implementation class: {}", encryptorFactory.getClass());
        Encryptor encryptor = EncryptorFactory.getInstance().newRandomEncryptor();
        subresult.addReturn("encryptorImpl", encryptor.getClass());
        LOGGER.debug("Encryptor implementation class: {}", encryptor.getClass());
        if (encryptor.getClass().getName().equals("org.identityconnectors.common.security.impl.EncryptorImpl")) {
            // let's do some reflection magic to have a look inside
            try {
                LOGGER.trace("Encryptor fields: {}", Arrays.asList(encryptor.getClass().getDeclaredFields()));
                Field keyField = encryptor.getClass().getDeclaredField("key");
                keyField.setAccessible(true);
                Key key = (Key) keyField.get(encryptor);
                subresult.addReturn("keyAlgorithm", key.getAlgorithm());
                subresult.addReturn("keyLength", key.getEncoded().length * 8);
                subresult.addReturn("keyFormat", key.getFormat());
                subresult.recordSuccess();
            } catch (IllegalArgumentException | SecurityException | NoSuchFieldException | IllegalAccessException e) {
                subresult.recordPartialError("Reflection introspection failed", e);
            }
        }

        OperationResult encryptorSubresult = result.createSubresult(ConnectorFactoryConnIdImpl.class + ".selfTestGuardedString.encryptor");
        try {
            String plainString = "Scurvy seadog";
            byte[] encryptedBytes = encryptor.encrypt(plainString.getBytes());
            byte[] decryptedBytes = encryptor.decrypt(encryptedBytes);
            String decryptedString = new String(decryptedBytes);
            if (!plainString.equals(decryptedString)) {
                encryptorSubresult.recordFatalError("Encryptor roundtrip failed; encrypted=" + plainString + ", decrypted=" + decryptedString);
            } else {
                encryptorSubresult.recordSuccess();
            }
        } catch (Throwable e) {
            LOGGER.error("Encryptor operation error: {}", e.getMessage(), e);
            encryptorSubresult.recordFatalError("Encryptor operation error: " + e.getMessage(), e);
        }

        final OperationResult guardedStringSubresult = result.createSubresult(ConnectorFactoryConnIdImpl.class + ".selfTestGuardedString.guardedString");
        // try to encrypt and decrypt GuardedString
        try {
            final String origString = "Shiver me timbers";
            // This should encrypt it
            GuardedString guardedString = new GuardedString(origString.toCharArray());
            // and this should decrypt it
            guardedString.access(decryptedChars -> {
                if (!(new String(decryptedChars)).equals(origString)) {
                    guardedStringSubresult.recordFatalError("GuardedString roundtrip failed; encrypted=" + origString + ", decrypted=" + (new String(decryptedChars)));
                }
            });
            guardedStringSubresult.recordSuccessIfUnknown();
        } catch (Throwable e) {
            LOGGER.error("GuardedString operation error: {}", e.getMessage(), e);
            guardedStringSubresult.recordFatalError("GuardedString operation error: " + e.getMessage(), e);
        }

        result.computeStatus();
    }

    static Collection<Class<? extends APIOperation>> resolveApiOpClass(String opName) {
        return API_OP_MAP.get(opName);
    }

    static {
        API_OP_MAP.put("create", List.of(CreateApiOp.class));
        API_OP_MAP.put("get", List.of(GetApiOp.class));
        API_OP_MAP.put("update", List.of(UpdateApiOp.class, UpdateDeltaApiOp.class));
        API_OP_MAP.put("delete", List.of(DeleteApiOp.class));
        API_OP_MAP.put("test", List.of(TestApiOp.class));
        API_OP_MAP.put("scriptOnConnector", List.of(ScriptOnConnectorApiOp.class));
        API_OP_MAP.put("scriptOnResource", List.of(ScriptOnResourceApiOp.class));
        API_OP_MAP.put("authentication", List.of(AuthenticationApiOp.class));
        API_OP_MAP.put("search", List.of(SearchApiOp.class));
        API_OP_MAP.put("validate", List.of(ValidateApiOp.class));
        API_OP_MAP.put("sync", List.of(SyncApiOp.class));
        API_OP_MAP.put("schema", List.of(SchemaApiOp.class));
    }

    @Override
    public void shutdown() {
        LOGGER.info("Shutting down ConnId framework");
        ConnectorFacadeFactory.getInstance().dispose();
        if (localConnectorInfoManager != null) {
            localConnectorInfoManager.shutdown();
        }
    }

    @Override
    public void registerDiscoveryListener(ConnectorDiscoveryListener listener) {
        listeners.add(listener);
    }

    void notifyConnectorAdded() {
        for (ConnectorDiscoveryListener listener : listeners) {
            listener.newConnectorDiscovered(null);
        }
    }

    public List<ConnectorInfo> addLocalConnector(URI file) {
        var connectors = localConnectorInfoManager.registerConnector(file);
        if (!connectors.isEmpty()) {
            notifyConnectorAdded();
        };
        return connectors;
    }
}
