/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processIcfException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.PostConstruct;
import javax.net.ssl.TrustManager;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.Version;
import org.identityconnectors.common.security.Encryptor;
import org.identityconnectors.common.security.EncryptorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.AuthenticationApiOp;
import org.identityconnectors.framework.api.operations.CreateApiOp;
import org.identityconnectors.framework.api.operations.DeleteApiOp;
import org.identityconnectors.framework.api.operations.GetApiOp;
import org.identityconnectors.framework.api.operations.SchemaApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.api.operations.SearchApiOp;
import org.identityconnectors.framework.api.operations.SyncApiOp;
import org.identityconnectors.framework.api.operations.TestApiOp;
import org.identityconnectors.framework.api.operations.UpdateApiOp;
import org.identityconnectors.framework.api.operations.ValidateApiOp;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
	public static final QName ICFS_ACCOUNT = new QName(SchemaConstants.NS_ICF_SCHEMA, "account");
	

	public static final String CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME = "ConfigurationPropertiesType";
	public static final QName CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_QNAME = new QName(SchemaConstants.NS_ICF_CONFIGURATION, 
			CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME);
	public static final String CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME = "ConfigurationType";

	public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_XML_ELEMENT_NAME = "connectorPoolConfiguration";
	public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
			"connectorPoolConfiguration");
	public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
			"ConnectorPoolConfigurationType");
	protected static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_EVICTABLE_IDLE_TIME_MILLIS = "minEvictableIdleTimeMillis";
	public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MIN_IDLE = "minIdle";
	public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_IDLE = "maxIdle";
	public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_OBJECTS = "maxObjects";
	public static final String CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_MAX_WAIT = "maxWait";

	public static final String CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_XML_ELEMENT_NAME = "producerBufferSize";
	public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
			CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_XML_ELEMENT_NAME);
	public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE = DOMUtil.XSD_INT;
	
	public static final String CONNECTOR_SCHEMA_LEGACY_SCHEMA_XML_ELEMENT_NAME = "legacySchema";
	public static final QName CONNECTOR_SCHEMA_LEGACY_SCHEMA_ELEMENT = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
			CONNECTOR_SCHEMA_LEGACY_SCHEMA_XML_ELEMENT_NAME);
	public static final QName CONNECTOR_SCHEMA_LEGACY_SCHEMA_TYPE = DOMUtil.XSD_BOOLEAN;

	public static final String CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME = "timeouts";
	public static final QName CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
			CONNECTOR_SCHEMA_TIMEOUTS_XML_ELEMENT_NAME);
	public static final QName CONNECTOR_SCHEMA_TIMEOUTS_TYPE = new QName(SchemaConstants.NS_ICF_CONFIGURATION, "TimeoutsType");

    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME = "resultsHandlerConfiguration";
    public static final QName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
            CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ELEMENT_LOCAL_NAME);
    public static final QName CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_TYPE = new QName(SchemaConstants.NS_ICF_CONFIGURATION,
            "ResultsHandlerConfigurationType");
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_NORMALIZING_RESULTS_HANDLER = "enableNormalizingResultsHandler";
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_FILTERED_RESULTS_HANDLER = "enableFilteredResultsHandler";
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE = "filteredResultsHandlerInValidationMode";
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_CASE_INSENSITIVE_HANDLER = "enableCaseInsensitiveFilter";
    public static final String CONNECTOR_SCHEMA_RESULTS_HANDLER_CONFIGURATION_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER = "enableAttributesToGetSearchResultsHandler";

    static final Map<String, Class<? extends APIOperation>> apiOpMap = new HashMap<String, Class<? extends APIOperation>>();

	private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = SchemaConstants.ICF_FRAMEWORK_URI + "/bundle/";
	private static final String CONNECTOR_IDENTIFIER_SEPARATOR = "/";
	
	public static final int ATTR_DISPLAY_ORDER_START = 120;
	public static final int ATTR_DISPLAY_ORDER_INCREMENT = 10;

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryConnIdImpl.class);
	
	// This is not really used in the code. It is here just to make sure that the JUL logger is loaded
	// by the parent classloader so we can correctly adjust the log levels from the main code
	static final java.util.logging.Logger JUL_LOGGER = java.util.logging.Logger.getLogger(ConnectorFactoryConnIdImpl.class.getName());
	

	private ConnectorInfoManagerFactory connectorInfoManagerFactory;
	private ConnectorInfoManager localConnectorInfoManager;
	private Set<URL> bundleURLs;
	private Set<ConnectorType> localConnectorTypes = null;
	
	@Autowired(required = true)
	private MidpointConfiguration midpointConfiguration;

	@Autowired(required = true)
	private Protector protector;
	
	@Autowired(required = true)
	private PrismContext prismContext;

	public ConnectorFactoryConnIdImpl() {
	}

	/**
	 * Initialize the ICF implementation. Look for all connector bundles, get
	 * basic information about them and keep that in memory.
	 */
	@PostConstruct
	public void initialize() {

		// OLD
		// bundleURLs = listBundleJars();
		bundleURLs = new HashSet<URL>();

		Configuration config = midpointConfiguration.getConfiguration("midpoint.icf");

		// Is classpath scan enabled
		if (config.getBoolean("scanClasspath")) {
			// Scan class path
			bundleURLs.addAll(scanClassPathForBundles());
		}

		// Scan all provided directories
		@SuppressWarnings("unchecked")
		List<String> dirs = config.getList("scanDirectory");
		for (String dir : dirs) {
			bundleURLs.addAll(scanDirectory(dir));
		}

		for (URL u : bundleURLs) {
			LOGGER.debug("ICF bundle URL : {}", u);
		}

		connectorInfoManagerFactory = ConnectorInfoManagerFactory.getInstance();

	}

	/**
	 * Creates new connector instance.
	 * 
	 * It will initialize the connector by taking the XML Resource definition,
	 * transforming it to the ICF configuration and applying that to the new
	 * connector instance.
	 * 
	 * @throws ObjectNotFoundException
	 * @throws SchemaException 
	 * 
	 */
	@Override
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace, String desc)
			throws ObjectNotFoundException, SchemaException {

		ConnectorInfo cinfo = getConnectorInfo(connectorType);

		if (cinfo == null) {
			LOGGER.error("Failed to instantiate {}", ObjectTypeUtil.toShortString(connectorType));
			LOGGER.debug("Connector key: {}, host: {}", getConnectorKey(connectorType),
					ObjectTypeUtil.toShortString(connectorType));
			LOGGER.trace("Connector object: {}", ObjectTypeUtil.dump(connectorType));
			LOGGER.trace("Connector host object: {}", ObjectTypeUtil.dump(connectorType.getConnectorHost()));
			throw new ObjectNotFoundException("The classes (JAR) of " + ObjectTypeUtil.toShortString(connectorType)
					+ " were not found by the ICF framework; bundle="+connectorType.getConnectorBundle()+" connector type=" + connectorType.getConnectorType() + ", version="+connectorType.getConnectorVersion());
		}

		PrismSchema connectorSchema = getConnectorSchema(connectorType, namespace);
		
		// Create new midPoint ConnectorInstance and pass it the ICF connector
		// facade
		ConnectorInstanceConnIdImpl connectorImpl = new ConnectorInstanceConnIdImpl(cinfo, connectorType, namespace,
				connectorSchema, protector, prismContext);
		connectorImpl.setDescription(desc);
		
		return connectorImpl;
	}

	private PrismSchema getConnectorSchema(ConnectorType connectorType, String namespace) throws SchemaException {
		XmlSchemaType xmlSchema = connectorType.getSchema();
		if (xmlSchema == null) {
			return null;
		}
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchema);
		if (xsdElement == null) {
			return null;
		}
		PrismSchema connectorSchema = PrismSchemaImpl.parse(xsdElement, true, connectorType.toString(), prismContext);
		return connectorSchema;
	}

	/**
	 * Returns a list XML representation of the ICF connectors.
	 * 
	 * @throws CommunicationException
	 */
	@Override
	public Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentRestul)
			throws CommunicationException {
		OperationResult result = parentRestul.createSubresult(ConnectorFactory.OPERATION_LIST_CONNECTOR);
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
			Throwable ex = processIcfException(icfException, "list connectors", result);
			result.recordFatalError(ex.getMessage(), ex);
			if (ex instanceof CommunicationException) {
				throw (CommunicationException) ex;
			} else if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			} else if (ex instanceof Error) {
				throw (Error) ex;
			} else {
				throw new SystemException("Unexpected ICF exception: " + ex.getMessage(), ex);
			}
		}
	}

	private Set<ConnectorType> listLocalConnectors() {
		if (localConnectorTypes == null) {
			// Lazy initialize connector list
			localConnectorTypes = new HashSet<ConnectorType>();

			
			// Fetch list of local connectors from ICF
			List<ConnectorInfo> connectorInfos = getLocalConnectorInfoManager().getConnectorInfos();

			for (ConnectorInfo connectorInfo : connectorInfos) {
				ConnectorType connectorType = convertToConnectorType(connectorInfo, null);
				localConnectorTypes.add(connectorType);
			}
		}

		return localConnectorTypes;
	}

	private Set<ConnectorType> listRemoteConnectors(ConnectorHostType host) {
		ConnectorInfoManager remoteConnectorInfoManager = getRemoteConnectorInfoManager(host);
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		List<ConnectorInfo> connectorInfos = remoteConnectorInfoManager.getConnectorInfos();
		for (ConnectorInfo connectorInfo : connectorInfos) {
			ConnectorType connectorType = convertToConnectorType(connectorInfo, host);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}

	/**
	 * Converts ICF ConnectorInfo into a midPoint XML connector representation.
	 * 
	 * TODO: schema transformation
	 * 
	 * @param hostType
	 *            host that this connector runs on or null for local connectors
	 */
	private ConnectorType convertToConnectorType(ConnectorInfo cinfo, ConnectorHostType hostType) {
		ConnectorType connectorType = new ConnectorType();
		ConnectorKey key = cinfo.getConnectorKey();
		String stringID = keyToNamespaceSuffix(key);
		StringBuilder displayName = new StringBuilder(StringUtils.substringAfterLast(key.getConnectorName(), "."));
		StringBuilder connectorName = new StringBuilder("ICF ");
		connectorName.append(key.getConnectorName());
		connectorName.append(" v");
		connectorName.append(key.getBundleVersion());
		if (hostType != null) {
			connectorName.append(" @");
			connectorName.append(hostType.getName());
			displayName.append(" @");
			displayName.append(hostType.getName());
			
		}
		connectorType.setName(new PolyStringType(connectorName.toString()));
		connectorType.setFramework(SchemaConstants.ICF_FRAMEWORK_URI);
		connectorType.setConnectorType(key.getConnectorName());
		connectorType.setNamespace(ICF_CONFIGURATION_NAMESPACE_PREFIX + stringID);
		connectorType.setConnectorVersion(key.getBundleVersion());
		connectorType.setConnectorBundle(key.getBundleName());
		if (hostType != null) {
			if (hostType.getOid() != null) {
				// bind using connectorHostRef and OID
				ObjectReferenceType ref = new ObjectReferenceType();
				ref.setOid(hostType.getOid());
				ref.setType(ObjectTypes.CONNECTOR_HOST.getTypeQName());
				connectorType.setConnectorHostRef(ref);
			} else {
				// Embed the object
				connectorType.setConnectorHost(hostType);
			}
		}
		return connectorType;
	}

	/**
	 * Converts ICF connector key to a simple string.
	 * 
	 * The string may be used as an OID.
	 */
	private String keyToNamespaceSuffix(ConnectorKey key) {
		StringBuilder sb = new StringBuilder();
		sb.append(key.getBundleName());
		sb.append(CONNECTOR_IDENTIFIER_SEPARATOR);
		// Don't include version. It is lesser evil.
		// sb.append(key.getBundleVersion());
		// sb.append(CONNECTOR_IDENTIFIER_SEPARATOR);
		sb.append(key.getConnectorName());
		return sb.toString();
	}

	/**
	 * Returns ICF connector info manager that manages local connectors. The
	 * manager will be created if it does not exist yet.
	 * 
	 * @return ICF connector info manager that manages local connectors
	 */
	private ConnectorInfoManager getLocalConnectorInfoManager() {
		if (null == localConnectorInfoManager) {
			localConnectorInfoManager = connectorInfoManagerFactory.getLocalManager(bundleURLs.toArray(new URL[0]));
		}
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
			throw new SystemException("Shared secret decryption error: "+e.getMessage(),e);
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
				new Object[] {hostname, port, key, useSSL, trustManagers, timeout});
		RemoteFrameworkConnectionInfo remoteFramewrorkInfo = new RemoteFrameworkConnectionInfo(hostname, port, key, useSSL, trustManagers, timeout);
		return connectorInfoManagerFactory.getRemoteManager(remoteFramewrorkInfo);
	}

	/**
	 * Scan class path for connector bundles
	 * 
	 * @return Set of all bundle URL
	 */
	private Set<URL> scanClassPathForBundles() {
		Set<URL> bundle = new HashSet<URL>();

		// scan class path for bundles
		Enumeration<URL> en = null;
		try {
			// Search all jars in classpath
			en = ConnectorFactoryConnIdImpl.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
		} catch (IOException ex) {
			LOGGER.debug("Error during reding content from class path");
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

			if (null != prop.get("ConnectorBundle-Name")) {
				LOGGER.info("Discovered ICF bundle on CLASSPATH: " + prop.get("ConnectorBundle-Name") + " version: "
						+ prop.get("ConnectorBundle-Version"));

				// hack to split MANIFEST from name
				try {
                                        String upath = u.getPath();
					URL tmp = new URL(toUrl(upath.substring(0, upath.lastIndexOf("!"))));
					if (isThisBundleCompatible(tmp)) {
						bundle.add(tmp);
					} else {
						LOGGER.warn("Skip loading ICF bundle {} due error occured", tmp);
					}
				} catch (MalformedURLException e) {
					LOGGER.error("This never happend we hope. URL:" + u.getPath(), e);
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
	 * Scan directory for bundles only on first lval we do the scan
	 * 
	 * @param path
	 * @return
	 */
	private Set<URL> scanDirectory(String path) {

		// Prepare return object
		Set<URL> bundle = new HashSet<URL>();
		// COnvert path to object File
		File dir = new File(path);

		// Test if this path is single jar or need to do deep examination
		if (isThisJarFileBundle(dir)) {
			try {
				if (isThisBundleCompatible(dir.toURI().toURL())) {
					bundle.add(dir.toURI().toURL());
				} else {
					LOGGER.warn("Skip loading budle {} due error occured", dir.toURI().toURL());
				}
			} catch (MalformedURLException e) {
				LOGGER.error("This never happend we hope.", e);
				throw new SystemException(e);
			}
			return bundle;
		}

		// Test if it is a directory
		if (!dir.isDirectory()) {
			LOGGER.error("Provided Icf connector path {} is not a directory.", dir.getAbsolutePath());
		}

		// List directory items
		File[] dirEntries = dir.listFiles();
		if (null == dirEntries) {
			LOGGER.warn("No bundles found in directory {}", dir.getAbsolutePath());
			return bundle;
		}

		// test all entires for bundle
		for (int i = 0; i < dirEntries.length; i++) {
			if (isThisJarFileBundle(dirEntries[i])) {
				try {
					if (isThisBundleCompatible(dirEntries[i].toURI().toURL())) {
						bundle.add(dirEntries[i].toURI().toURL());
					} else {
						LOGGER.warn("Skip loading budle {} due error occured", dirEntries[i].toURI().toURL());
					}
				} catch (MalformedURLException e) {
					LOGGER.error("This never happend we hope.", e);
					throw new SystemException(e);
				}
			}
		}
		return bundle;
	}

	/**
	 * Test if bundle internal configuration and dependencies are OK
	 * @param bundleUrl
	 * 			tested bundle URL
	 * @return true if OK
	 */
	private Boolean isThisBundleCompatible(URL bundleUrl) {
		if (null == bundleUrl)
			return false;
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
				LOGGER.error("Error instantiating ICF bundle using URL '{}': {}", new Object[] { bundleUrl, ex.getMessage(), ex});
			} else {
				LOGGER.error("Error instantiating ICF bundle using URL '{}': {}", new Object[] { bundleUrl, ex.getMessage()});
			}
			return false;
		}
	}

	/**
	 * Test if provided file is connector bundle
	 * 
	 * @param file
	 *            tested file
	 * @return boolean
	 */
	private Boolean isThisJarFileBundle(File file) {
		// Startup tests
		if (null == file) {
			throw new IllegalArgumentException("No file is providied for bundle test.");
		}

		// Skip all processing if it is not a file
		if (!file.isFile()) {
			LOGGER.debug("This {} is not a file", file.getAbsolutePath());
			return false;
		}

		Properties prop = new Properties();
		JarFile jar = null;
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
		} catch (IOException ex) {
			LOGGER.debug("Unable to parse jar file: " + file.getAbsolutePath() + " [" + ex.getMessage() + "]");
			return false;
		} catch (NullPointerException ex) {
			LOGGER.debug("Unable to parse jar file: " + file.getAbsolutePath() + " [" + ex.getMessage() + "]");
			return false;
		}

		// Test if it is a connector
		if (null != prop.get("ConnectorBundle-Name")) {
			LOGGER.info("Discovered ICF bundle in JAR: " + prop.get("ConnectorBundle-Name") + " version: "
					+ prop.get("ConnectorBundle-Version"));
			return true;
		}

		LOGGER.debug("Provided file {} is not iCF bundle jar", file.getAbsolutePath());
		return false;
	}

	/**
	 * Get contect informations
	 * 
	 * @param connectorType
	 * @return
	 * @throws ObjectNotFoundException
	 */
	private ConnectorInfo getConnectorInfo(ConnectorType connectorType) throws ObjectNotFoundException {
		if (!SchemaConstants.ICF_FRAMEWORK_URI.equals(connectorType.getFramework())) {
			throw new ObjectNotFoundException("Requested connector for framework " + connectorType.getFramework()
					+ " cannot be found in framework " + SchemaConstants.ICF_FRAMEWORK_URI);
		}
		ConnectorKey key = getConnectorKey(connectorType);
		if (connectorType.getConnectorHost() == null && connectorType.getConnectorHostRef() == null) {
			// Local connector
			return getLocalConnectorInfoManager().findConnectorInfo(key);
		}
		ConnectorHostType host = connectorType.getConnectorHost();
		if (host == null) {
			throw new ObjectNotFoundException(
					"Attempt to use remote connector without ConnectorHostType resolved (there is only ConnectorHostRef");
		}
		return getRemoteConnectorInfoManager(host).findConnectorInfo(key);
	}

	private ConnectorKey getConnectorKey(ConnectorType connectorType) {
		return new ConnectorKey(connectorType.getConnectorBundle(), connectorType.getConnectorVersion(),
				connectorType.getConnectorType());
	}
	
	@Override
	public void selfTest(OperationResult parentTestResult) {
		selfTestGuardedString(parentTestResult);
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
				subresult.addReturn("keyLength", key.getEncoded().length*8);
				subresult.addReturn("keyFormat", key.getFormat());
				subresult.recordSuccess();
			} catch (IllegalArgumentException e) {
				subresult.recordPartialError("Reflection introspection failed", e);
			} catch (IllegalAccessException e) {
				subresult.recordPartialError("Reflection introspection failed", e);
			} catch (NoSuchFieldException e) {
				subresult.recordPartialError("Reflection introspection failed", e);
			} catch (SecurityException e) {
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
				encryptorSubresult.recordFatalError("Encryptor roundtrip failed; encrypted="+plainString+", decrypted="+decryptedString);
			} else {
				encryptorSubresult.recordSuccess();
			}
		} catch (Throwable e) {
			LOGGER.error("Encryptor operation error: {}", e.getMessage(), e);
			encryptorSubresult.recordFatalError("Encryptor opeation error: " + e.getMessage(), e);
		}
		
		
		final OperationResult guardedStringSubresult = result.createSubresult(ConnectorFactoryConnIdImpl.class + ".selfTestGuardedString.guardedString");
		// try to encrypt and decrypt GuardedString
		try {
			final String origString = "Shiver me timbers";
			// This should encrypt it
			GuardedString guardedString = new GuardedString(origString.toCharArray());
			// and this should decrypt it
			guardedString.access(new GuardedString.Accessor() {
				@Override
				public void access(char[] decryptedChars) {
					if (!(new String(decryptedChars)).equals(origString)) {
						guardedStringSubresult.recordFatalError("GuardeString roundtrip failed; encrypted="+origString+", decrypted="+(new String(decryptedChars))); 
					}
				}
			});
			guardedStringSubresult.recordSuccessIfUnknown();
		} catch (Throwable e) {
			LOGGER.error("GuardedString operation error: {}", e.getMessage(), e);
			guardedStringSubresult.recordFatalError("GuardedString opeation error: " + e.getMessage(), e);
		}
		
		result.computeStatus();
	}

	static Class<? extends APIOperation> resolveApiOpClass(String opName) {
		return apiOpMap.get(opName);
	}

	static {
		apiOpMap.put("create", CreateApiOp.class);
		apiOpMap.put("get", GetApiOp.class);
		apiOpMap.put("update", UpdateApiOp.class);
		apiOpMap.put("delete", DeleteApiOp.class);
		apiOpMap.put("test", TestApiOp.class);
		apiOpMap.put("scriptOnConnector", ScriptOnConnectorApiOp.class);
		apiOpMap.put("scriptOnResource", ScriptOnResourceApiOp.class);
		apiOpMap.put("authentication", AuthenticationApiOp.class);
		apiOpMap.put("search", SearchApiOp.class);
		apiOpMap.put("validate", ValidateApiOp.class);
		apiOpMap.put("sync", SyncApiOp.class);
		apiOpMap.put("schema", SchemaApiOp.class);
	}

	@Override
	public void shutdown() {
		LOGGER.info("Shutting down ConnId framework");
		ConnectorFacadeFactory.getInstance().dispose();
	}

}
