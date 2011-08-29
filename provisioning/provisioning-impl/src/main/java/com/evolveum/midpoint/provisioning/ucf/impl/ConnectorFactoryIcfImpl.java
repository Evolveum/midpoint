/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import static com.evolveum.midpoint.provisioning.ucf.impl.IcfUtil.processIcfException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import javax.xml.namespace.QName;

import org.apache.commons.configuration.Configuration;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;

/**
 * Currently the only implementation of the UCF Connector Manager API interface.
 * 
 * It is hardcoded to ICF now.
 * 
 * This class holds a list of all known ICF connectors in the system.
 * 
 * @author Radovan Semancik
 */

@Component
public class ConnectorFactoryIcfImpl implements ConnectorFactory {

	public static final String ICF_FRAMEWORK_URI = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1";
	public static final String NS_ICF_CONFIGURATION = ICF_FRAMEWORK_URI + "/connector-schema-1.xsd";
	// Note! This is also specified in SchemaConstants (MID-356)
	public static final String NS_ICF_SCHEMA = ICF_FRAMEWORK_URI + "/resource-schema-1.xsd";
	public static final String NS_ICF_SCHEMA_PREFIX = "icfs";
	public static final String NS_ICF_RESOURCE_INSTANCE_PREFIX = "ri";
	public static final QName ICFS_NAME = new QName(NS_ICF_SCHEMA, "name");
	public static final QName ICFS_UID = new QName(NS_ICF_SCHEMA, "uid");
	public static final QName ICFS_PASSWORD = new QName(NS_ICF_SCHEMA, "password");
	public static final QName ICFS_ACCOUNT = new QName(NS_ICF_SCHEMA, "account");
	public static final String CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME = "configurationProperties";
	public static final String CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_TYPE_LOCAL_NAME = "ConfigurationPropertiesType";
	public static final String CONNECTOR_SCHEMA_CONFIGURATION_ELEMENT_LOCAL_NAME = "configuration";
	public static final String CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME = "ConfigurationType";
	public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT = new QName(
			NS_ICF_CONFIGURATION, "connectorPoolConfiguration");
	public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE = new QName(
			NS_ICF_CONFIGURATION, "ConnectorPoolConfigurationType");
	public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT = new QName(NS_ICF_CONFIGURATION,
			"producerBufferSize");
	public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE = DOMUtil.XSD_INTEGER;
	public static final QName CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT = new QName(NS_ICF_CONFIGURATION, "timeouts");
	public static final QName CONNECTOR_SCHEMA_TIMEOUTS_TYPE = new QName(NS_ICF_CONFIGURATION, "TimeoutsType");

	private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = ICF_FRAMEWORK_URI + "/bundle/";
	private static final String CONNECTOR_IDENTIFIER_SEPARATOR = "/";

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryIcfImpl.class);

	private ConnectorInfoManagerFactory connectorInfoManagerFactory;
	private ConnectorInfoManager localConnectorInfoManager;
	private Set<URL> bundleURLs;
	private Map<String, ConnectorInfo> connectors;

	@Autowired(required = true)
	MidpointConfiguration midpointConfiguration;

	public ConnectorFactoryIcfImpl() {
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

		connectors = new HashMap<String, ConnectorInfo>();
		List<ConnectorInfo> connectorInfos = getLocalConnectorInfoManager().getConnectorInfos();
		for (ConnectorInfo connectorInfo : connectorInfos) {
			ConnectorKey key = connectorInfo.getConnectorKey();
			String mapKey = keyToNamespaceSuffix(key);
			connectors.put(mapKey, connectorInfo);
		}
	}

	/**
	 * Creates new connector instance.
	 * 
	 * It will initialize the connector by taking the XML Resource definition,
	 * transforming it to the ICF configuration and applying that to the new
	 * connector instance.
	 * 
	 * @throws ObjectNotFoundException
	 * 
	 */
	@Override
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace)
			throws ObjectNotFoundException {

		ConnectorInfo cinfo = getConnectorInfo(connectorType);

		if (cinfo == null) {
			LOGGER.error("Failed to instantiate {}", ObjectTypeUtil.toShortString(connectorType));
			LOGGER.debug("Connector key: {}, host: {}", getConnectorKey(connectorType),
					ObjectTypeUtil.toShortString(connectorType));
			LOGGER.trace("Connector object: {}", ObjectTypeUtil.dump(connectorType));
			LOGGER.trace("Connector host object: {}", ObjectTypeUtil.dump(connectorType.getConnectorHost()));
			throw new ObjectNotFoundException("The connector " + ObjectTypeUtil.toShortString(connectorType)
					+ " was not found");
		}

		// Create new midPoint ConnectorInstance and pass it the ICF connector
		// facade
		ConnectorInstanceIcfImpl connectorImpl = new ConnectorInstanceIcfImpl(cinfo, connectorType, namespace);

		return connectorImpl;
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
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryIcfImpl.class);
		result.addParam("host", host);

		try {
			if (host == null) {
				Set<ConnectorType> connectors = listLocalConnectors();
				result.recordSuccess();
				return connectors;
			} else {
				Set<ConnectorType> connectors = listRemoteConnectors(host);
				result.recordSuccess();
				return connectors;
			}
		} catch (Exception icfException) {
			Exception ex = processIcfException(icfException, result);
			result.recordFatalError(ex.getMessage(), ex);
			if (ex instanceof CommunicationException) {
				throw (CommunicationException) ex;
			} else if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			} else {
				throw new SystemException("Unexpected ICF exception: " + ex.getMessage(), ex);
			}
		}
	}

	private Set<ConnectorType> listLocalConnectors() {
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		for (Map.Entry<String, ConnectorInfo> e : connectors.entrySet()) {
			ConnectorInfo cinfo = e.getValue();
			ConnectorType connectorType = convertToConnectorType(cinfo, null);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
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
		StringBuilder connectorName = new StringBuilder("ICF ");
		connectorName.append(key.getConnectorName());
		if (hostType != null) {
			connectorName.append(" @");
			connectorName.append(hostType.getName());
		}
		connectorType.setName(connectorName.toString());
		connectorType.setFramework(ICF_FRAMEWORK_URI);
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
			localConnectorInfoManager = connectorInfoManagerFactory.getLocalManager(bundleURLs
					.toArray(new URL[0]));
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
		GuardedString key = new GuardedString(hostType.getSharedSecret().toCharArray());
		// TODO: SSL
		RemoteFrameworkConnectionInfo remoteFramewrorkInfo = new RemoteFrameworkConnectionInfo(hostname,
				port, key);
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
			en = ConnectorFactoryIcfImpl.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
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
				LOGGER.info("Discovered icf bundle on CLASSPATH: " + prop.get("ConnectorBundle-Name")
						+ " version: " + prop.get("ConnectorBundle-Version"));

				// hack to split MANIFEST from name
				try {
					URL tmp = new URL(u.getPath().split("!")[0]);
					bundle.add(tmp);
				} catch (MalformedURLException e) {
					LOGGER.error("This never happend we hope. URL:" + u.getPath(), e);
					throw new SystemException(e);
				}
			}
		}
		return bundle;
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
				bundle.add(dir.toURI().toURL());
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
					bundle.add(dirEntries[i].toURI().toURL());
				} catch (MalformedURLException e) {
					LOGGER.error("This never happend we hope.", e);
					throw new SystemException(e);
				}
			}
		}
		return bundle;
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
			LOGGER.debug("Unable to fine MANIFEST.MF in jar file: " + file.getAbsolutePath() + " ["
					+ ex.getMessage() + "]");
			return false;
		}

		// Parse jar file
		try {
			prop.load(is);
		} catch (IOException ex) {
			LOGGER.debug("Unable to parse jar file: " + file.getAbsolutePath() + " [" + ex.getMessage() + "]");
			return false;
		}

		// Test if it is a connector
		if (null != prop.get("ConnectorBundle-Name")) {
			LOGGER.info("Discovered icf bundle in JAR: " + prop.get("ConnectorBundle-Name") + " version: "
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
		if (!ICF_FRAMEWORK_URI.equals(connectorType.getFramework())) {
			throw new ObjectNotFoundException("Requested connector for framework "
					+ connectorType.getFramework() + " cannot be found in framework " + ICF_FRAMEWORK_URI);
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

}
