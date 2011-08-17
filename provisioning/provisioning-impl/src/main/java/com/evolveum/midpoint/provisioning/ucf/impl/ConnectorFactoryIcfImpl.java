/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.identityconnectors.common.security.GuardedString;

import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.ClasspathUrlFinder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;


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
	public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_ELEMENT = new QName(NS_ICF_CONFIGURATION,"connectorPoolConfiguration");
	public static final QName CONNECTOR_SCHEMA_CONNECTOR_POOL_CONFIGURATION_TYPE = new QName(NS_ICF_CONFIGURATION,"ConnectorPoolConfigurationType");
	public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_ELEMENT = new QName(NS_ICF_CONFIGURATION,"producerBufferSize");
	public static final QName CONNECTOR_SCHEMA_PRODUCER_BUFFER_SIZE_TYPE = DOMUtil.XSD_INTEGER;
	public static final QName CONNECTOR_SCHEMA_TIMEOUTS_ELEMENT = new QName(NS_ICF_CONFIGURATION,"timeouts");
	public static final QName CONNECTOR_SCHEMA_TIMEOUTS_TYPE = new QName(NS_ICF_CONFIGURATION,"TimeoutsType");
	
	// This usually refers to WEB-INF/lib/icf-connectors
	private static final String BUNDLE_PATH = "../../lib/icf-connectors";
	private static final String BUNDLE_PREFIX = "org.identityconnectors";
	private static final String BUNDLE_SUFFIX = ".jar";
	private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = ICF_FRAMEWORK_URI + "/bundle/";
	private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryIcfImpl.class);
	private static final String CONNECTOR_IDENTIFIER_SEPARATOR = "/";
	
	private ConnectorInfoManagerFactory connectorInfoManagerFactory;
	private ConnectorInfoManager localConnectorInfoManager;
	private Set<URL> bundleURLs;
	private Map<String, ConnectorInfo> connectors;

	public ConnectorFactoryIcfImpl() {
	}

	/**
	 * Initialize the ICF implementation. Look for all connector bundles,
	 * get basic information about them and keep that in memory.
	 */
	@PostConstruct
	public void initialize() {
		bundleURLs = listBundleJars();
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
	 * transforming it to the ICF configuration and applying that to the 
	 * new connector instance.
	 * @throws ObjectNotFoundException 
	 * 
	 */
	@Override
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace) throws ObjectNotFoundException {
		
		ConnectorInfo cinfo = getConnectorInfo(connectorType);
		
		if (cinfo==null) {
			LOGGER.error("Failed to instantiate {}",ObjectTypeUtil.toShortString(connectorType));
			LOGGER.debug("Connector key: {}, host: {}",getConnectorKey(connectorType),ObjectTypeUtil.toShortString(connectorType));
			LOGGER.trace("Connector object: {}",ObjectTypeUtil.dump(connectorType));
			LOGGER.trace("Connector host object: {}",ObjectTypeUtil.dump(connectorType.getConnectorHost()));
			throw new ObjectNotFoundException("The connector "+ObjectTypeUtil.toShortString(connectorType)+" was not found");
		}
		
		// Create new midPoint ConnectorInstance and pass it the ICF connector facade
		ConnectorInstanceIcfImpl connectorImpl = new ConnectorInstanceIcfImpl(cinfo, connectorType, namespace);

		return connectorImpl;
	}

	/**
	 * Returns a list XML representation of the ICF connectors.
	 */
	@Override
	public Set<ConnectorType> listConnectors(ConnectorHostType host) {
		if (host == null) {
			return listLocalConnectors();
		} else {
			return listRemoteConnectors(host);
		}
	}
		
	private Set<ConnectorType> listLocalConnectors() {
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		for (Map.Entry<String, ConnectorInfo> e : connectors.entrySet()) {
			ConnectorInfo cinfo = e.getValue();
			ConnectorType connectorType = convertToConnectorType(cinfo,null);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}
	
	private Set<ConnectorType> listRemoteConnectors(ConnectorHostType host) {
		ConnectorInfoManager remoteConnectorInfoManager = getRemoteConnectorInfoManager(host);
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		List<ConnectorInfo> connectorInfos = remoteConnectorInfoManager.getConnectorInfos();
		for (ConnectorInfo connectorInfo : connectorInfos) {
			ConnectorType connectorType = convertToConnectorType(connectorInfo,host);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}


	/**
	 * Converts ICF ConnectorInfo into a midPoint XML connector representation.
	 * 
	 * TODO: schema transformation
	 * @param hostType host that this connector runs on or null for local connectors
	 */
	private ConnectorType convertToConnectorType(ConnectorInfo cinfo, ConnectorHostType hostType) {
		ConnectorType connectorType = new ConnectorType();
		ConnectorKey key = cinfo.getConnectorKey();
		String stringID = keyToNamespaceSuffix(key);
		StringBuilder connectorName = new StringBuilder("ICF ");
		connectorName.append(key.getConnectorName());
		if (hostType!=null) {
			connectorName.append(" @");
			connectorName.append(hostType.getName());
		}
		connectorType.setName(connectorName.toString());
		connectorType.setFramework(ICF_FRAMEWORK_URI);
		connectorType.setConnectorType(key.getConnectorName());
		connectorType.setNamespace(ICF_CONFIGURATION_NAMESPACE_PREFIX + stringID);
		connectorType.setConnectorVersion(key.getBundleVersion());
		connectorType.setConnectorBundle(key.getBundleName());
		if (hostType!=null) {
			if (hostType.getOid()!=null) {
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
//		sb.append(key.getBundleVersion());
//		sb.append(CONNECTOR_IDENTIFIER_SEPARATOR);
		sb.append(key.getConnectorName());
		return sb.toString();
	}


	/**
	 * Returns ICF connector info manager that manages local connectors.
	 * The manager will be created if it does not exist yet.
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
	 * Returns ICF connector info manager that manages local connectors.
	 * The manager will be created if it does not exist yet.
	 * 
	 * @return ICF connector info manager that manages local connectors
	 */
	private ConnectorInfoManager getRemoteConnectorInfoManager(ConnectorHostType hostType) {
		String hostname = hostType.getHostname();
		int port = Integer.parseInt(hostType.getPort());
		GuardedString key = new GuardedString(hostType.getSharedSecret().toCharArray());
		// TODO: SSL
		RemoteFrameworkConnectionInfo remoteFramewrorkInfo = new RemoteFrameworkConnectionInfo(hostname, port, key);
		return connectorInfoManagerFactory.getRemoteManager(remoteFramewrorkInfo);
	}
	
	/**
	 * Lists all ICF connector bundles, either in BUNDLE_PATH or in current classpath.
	 * 
	 * @return set of connector bundle URLs.
	 */
	private Set<URL> listBundleJars() {
		Set<URL> bundleURLs = new HashSet<URL>();

		// Look for connectors in the BUNDLE_PATH folder

		File icfFolder = null;
		try {
			icfFolder = new File(new File(this.getClass().getClassLoader().getResource("com").toURI()),
					BUNDLE_PATH);
		} catch (URISyntaxException ex) {
			LOGGER.debug("Couldn't find icf-connectors folder, reason: " + ex.getMessage());
		}

		// Take only those that start with BUNDLE_PREFIX and end with BUNDLE_SUFFIX

		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				if (!file.exists() || file.isDirectory()) {
					return false;
				}
				String fileName = file.getName();
				if (fileName.startsWith(BUNDLE_PREFIX) && fileName.endsWith(BUNDLE_SUFFIX)) {
					return true;
				}
				return false;
			}
		};

		if (icfFolder == null || !icfFolder.exists() || !icfFolder.isDirectory()) {
			// cannot find icfFolder, therefore we are going to seach classpath
			// this is useful in tests
			URL[] resourceURLs = ClasspathUrlFinder.findClassPaths();
			for (int j = 0; j < resourceURLs.length; j++) {
				URL bundleUrl = resourceURLs[j];
				if ("file".equals(bundleUrl.getProtocol())) {
					File file = new File(bundleUrl.getFile());
					if (fileFilter.accept(file)) {
						bundleURLs.add(bundleUrl);
					}
				}
			}
		} else {
			// Looking in icfFolder
			File[] connectors = icfFolder.listFiles(fileFilter);
			for (File file : connectors) {
				try {
					bundleURLs.add(file.toURI().toURL());
				} catch (MalformedURLException ex) {
					LOGGER.debug("Couldn't transform file path " + file.getAbsolutePath()
							+ " to URL, reason: " + ex.getMessage());
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			for (URL u : bundleURLs) {
				LOGGER.debug("Bundle URL: {}", u);
			}
		}

		return bundleURLs;
	}

	private ConnectorInfo getConnectorInfo(ConnectorType connectorType) throws ObjectNotFoundException {
		if (!ICF_FRAMEWORK_URI.equals(connectorType.getFramework())) {
			throw new ObjectNotFoundException("Requested connector for framework "+connectorType.getFramework()+" cannot be found in framework "+ICF_FRAMEWORK_URI);
		}
		ConnectorKey key = getConnectorKey(connectorType);
		if (connectorType.getConnectorHost()==null && connectorType.getConnectorHostRef()==null) {
			// Local connector
			return getLocalConnectorInfoManager().findConnectorInfo(key);
		}
		ConnectorHostType host = connectorType.getConnectorHost();
		if (host==null) {
			throw new ObjectNotFoundException("Attempt to use remote connector without ConnectorHostType resolved (there is only ConnectorHostRef");
		}
		return getRemoteConnectorInfoManager(host).findConnectorInfo(key);
	}
	
	private ConnectorKey getConnectorKey(ConnectorType connectorType) {
		return new ConnectorKey(connectorType.getConnectorBundle(),
				connectorType.getConnectorVersion(),connectorType.getConnectorType());
	}
	
	
	
}
