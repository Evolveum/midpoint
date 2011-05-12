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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.integration.identityconnector;

import com.evolveum.midpoint.provisioning.integration.identityconnector.converter.ICFConverterFactory;
import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.converter.ConverterFactory;
import com.evolveum.midpoint.util.ClasspathUrlFinder;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.ConnectorRef;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.OperationTimeouts;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.OperationType;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.PoolConfigOption;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.xml.namespace.QName;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
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
import org.identityconnectors.common.security.GuardedString;
import org.w3c.dom.Element;

/**
 * Sample Class Doc
 * 
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ConnectorUtil {

	private static ConverterFactory converters = ICFConverterFactory.getInstance();

	public static final String code_id = "$Id$";

	public static final String BUNDLES_REL_PATH = "";

	private static final Trace TRACE = TraceManager.getTrace(ConnectorUtil.class);

	private static ConnectorInfoManager localConnectorInfoManager = null;

	private static Map<OperationType, Class> opMap = new HashMap();

	static {
		opMap.put(OperationType.CREATE, CreateApiOp.class);
		opMap.put(OperationType.UPDATE, UpdateApiOp.class);
		opMap.put(OperationType.DELETE, DeleteApiOp.class);
		opMap.put(OperationType.TEST, TestApiOp.class);
		opMap.put(OperationType.SCRIPT_ON_CONNECTOR, ScriptOnConnectorApiOp.class);
		opMap.put(OperationType.SCRIPT_ON_RESOURCE, ScriptOnResourceApiOp.class);
		opMap.put(OperationType.GET, GetApiOp.class);
		opMap.put(OperationType.AUTHENTICATE, AuthenticationApiOp.class);
		opMap.put(OperationType.SEARCH, SearchApiOp.class);
		opMap.put(OperationType.VALIDATE, ValidateApiOp.class);
		opMap.put(OperationType.SYNC, SyncApiOp.class);
		opMap.put(OperationType.SCHEMA, SchemaApiOp.class);
	}

	/**
	 * Returns URLs of ICF bundles
	 * 
	 * Temporary hack. The bundles are files org.identityconnectors.*.jar
	 * located at the classpath. It should be modified later to some more
	 * intelligent method.
	 * 
	 * @throws IOException
	 */
	public static URL[] getBundleURLs() {
		Set<URL> bundleURLs = new HashSet<URL>();
		File icfFolder = null;
		try {
			icfFolder = new File(new File(ConnectorUtil.class.getClassLoader().getResource("com").toURI()),
					"../../lib/icf-connectors");
		} catch (URISyntaxException ex) {
			TRACE.debug("Couldn't find icf-connectors folder, reason: " + ex.getMessage());
		}

		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				if (!file.exists() || file.isDirectory()) {
					return false;
				}
				String fileName = file.getName();
				if (fileName.startsWith("org.identityconnectors") && fileName.endsWith(".jar")) {
					return true;
				}
				return false;
			}
		};

		if (icfFolder == null || !icfFolder.exists() || !icfFolder.isDirectory()) {
			// old load (for test purposes)
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
			// looking in WEB-INF/lib/icf-connectors
			File[] connectors = icfFolder.listFiles(fileFilter);
			for (File file : connectors) {
				try {
					bundleURLs.add(file.toURI().toURL());
				} catch (MalformedURLException ex) {
					TRACE.debug("Couldn't transform file path " + file.getAbsolutePath()
							+ " to URL, reason: " + ex.getMessage());
				}
			}
		}

		if (TRACE.isDebugEnabled()) {
			for (URL u : bundleURLs) {
				TRACE.debug("Bundle URL: {}", u);
			}
		}

		return bundleURLs.toArray(new URL[0]);
	}

	/**
	 * <p>
	 * Retrieve a list of filepaths from a given directory within a jar file. If
	 * filtered results are needed, you can supply a |filter| regular expression
	 * which will match each entry.
	 * 
	 * @param filter
	 *            to filter the results within a regular expression.
	 * @return a list of files within the jar |file|
	 */
	public static Vector<URL> getJarFileListing(URL jarLocation, String filter) {
		Vector<URL> files = new Vector<URL>();
		if (jarLocation == null) {
			return files; // Empty.
		}

		// strip out the file: and the !META-INF/bundles so only the JAR file
		// left
		String jarPath = jarLocation.getPath().substring(5, jarLocation.getPath().indexOf("!"));

		try {
			// Lets stream the jar file
			JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath));
			JarEntry jarEntry;

			// Iterate the jar entries within that jar. Then make sure it
			// follows the
			// filter given from the user.
			do {
				jarEntry = jarInputStream.getNextJarEntry();
				if (jarEntry != null) {
					String fileName = jarEntry.getName();

					// The filter could be null or has a matching regular
					// expression.
					if (filter == null || fileName.matches(filter)) {
						files.add(new URL(jarLocation, fileName.replace(BUNDLES_REL_PATH, "")));
					}
				}
			} while (jarEntry != null);
			jarInputStream.close();
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to get Jar input stream from '" + jarLocation + "'", ioe);
		}
		return files;
	}

	public static void clearManagerCaches() {
		ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
		factory.clearRemoteCache();
	}

	public static ConnectorFacade createConnectorFacade(IdentityConnector resource) throws MidPointException {
		return createConnectorFacade(resource, null, null);
	}

	public static ConnectorFacade createConnectorFacade(IdentityConnector resource, CreateOptions options)
			throws MidPointException {
		return createConnectorFacade(resource, null, options);
	}

	/**
	 * Initialize an ICF facade based on attributes parsed from the type.
	 * 
	 * @param resourceConfig
	 * @param locale
	 * @param options
	 * @return
	 * @throws MidPointException
	 */
	public static ConnectorFacade createConnectorFacade(IdentityConnector resourceConfig, Locale locale,
			CreateOptions options) {
		ConnectorFacade connectorFacade = null;

		if (options == null) {
			options = new CreateOptions();
		}

		if (locale != null) {
			CurrentLocale.set(locale);
		}
		ConnectorRef connRef = resourceConfig.getConfiguration().getConnectorRef();

		// find the right bundle
		ConnectorInfo connectorInfo = getConnectorInfo(connRef);

		if (connectorInfo == null) {
			Object[] args = { connRef.getBundleName(), connRef.getBundleVersion(), connRef.getConnectorName() };
			TRACE.error("Identity connector bundle is not found {} / {} / {} ", args);
			throw new MidPointException("Identity connector bundle is not found " + connRef.getBundleName()
					+ "/" + connRef.getBundleVersion() + "/" + connRef.getConnectorName());
		}

		TRACE.debug(
				"Looking for connector {} / {} / {}, got {}",
				new Object[] { connRef.getBundleName(), connRef.getBundleVersion(),
						connRef.getConnectorName(), connectorInfo.getConnectorDisplayName() });

		APIConfiguration apiConfig = connectorInfo.createDefaultAPIConfiguration();

		// global configurations
		OperationTimeouts opTimeouts = resourceConfig.getConfiguration().getOperationTimeouts();
		if (opTimeouts != null) {
			setTimeouts(opTimeouts, apiConfig);
		}

		PoolConfigOption poolConfigOptions = resourceConfig.getConfiguration().getPoolConfigOption();
		if (poolConfigOptions != null) {
			setPoolConfiguration(poolConfigOptions, apiConfig);
		}

		ConfigurationProperties configProps = apiConfig.getConfigurationProperties();

		// per bundle configurations
		loadConfigurationProperties(resourceConfig, configProps);

		if (options.getDisableSearchBuffer()) {
			apiConfig.setProducerBufferSize(0);
			apiConfig.setTimeout(SearchApiOp.class, -1);
		}

		if (TRACE.isDebugEnabled()) {
			StringBuilder msg = new StringBuilder();
			for (String name : configProps.getPropertyNames()) {
				ConfigurationProperty property = configProps.getProperty(name);
				msg.append("  ").append(property.getName()).append(": ").append(property.getValue())
						.append("\n");
			}

			TRACE.debug("Connector {} configuration:\n{}", connectorInfo.getConnectorDisplayName(),
					msg.toString());
		}

		ConnectorFacadeFactory facadeFactory = ConnectorFacadeFactory.getInstance();
		connectorFacade = facadeFactory.newInstance(apiConfig);

		return connectorFacade;
	}

	/**
	 * COnfigure a bundle b
	 * 
	 * @param resource
	 *            connector configuration part from ResourceType
	 * @param configProperties
	 *            config properties descriptor
	 * @param forTest
	 * @todo use the parameters from resource
	 */
	public static void loadConfigurationProperties(IdentityConnector resource,
			ConfigurationProperties configProperties) {

		List<String> configPropNames = configProperties.getPropertyNames();
		Map<QName, List<Element>> attrValues = new HashMap<QName, List<Element>>(configPropNames.size());

		for (Element el : resource.getConfiguration().getBundleProperties().getAny()) {
			String propertyName = el.getLocalName();
			if (!(configPropNames.contains(propertyName))) {
				TRACE.warn("Ignoring unknown configuration property {} in resource {}", propertyName,
						resource.getOid());
				continue;
			}
			QName attrName = new QName(el.getNamespaceURI(), el.getLocalName());
			List<Element> value = attrValues.get(attrName);
			if (null == value) {
				value = new ArrayList<Element>(1);
				attrValues.put(attrName, value);
			}
			value.add(el);
		}

		for (Entry<QName, List<Element>> e : attrValues.entrySet()) {
			ConfigurationProperty configProp = configProperties.getProperty(e.getKey().getLocalPart());
			Class propClass = configProp.getType();
			List<Element> attrValue = e.getValue();
			Object propertyValue = null;
			if (propClass.isArray()) {
				Class propBaseClass = propClass.getComponentType();
				Object propertyValues = Array.newInstance(propBaseClass, attrValue.size());
				for (int i = 0; i < attrValue.size(); ++i) {
					Element prop = attrValue.get(i);
					Object obj = convertToConnectorPropertyObject(prop, propBaseClass);
					Array.set(propertyValues, i, obj);
				}
				propertyValue = propertyValues;
			} else if (Collection.class.isAssignableFrom(propClass)) {
				Class propBaseClass = propClass.getComponentType();
				// Set, Map, Collection can not initialise
				// Collection propertyValues = (Collection)
				// propClass.getConstructor().newInstance();

			} else {
				propertyValue = convertToConnectorPropertyObject(attrValue.get(0), propClass);
			}
			configProperties.setPropertyValue(e.getKey().getLocalPart(), propertyValue);
		}
	}

	private static Object convertToConnectorPropertyObject(Element obj, Class configPropertyClass)
			throws MidPointException {
		if (!obj.hasChildNodes()) {
			return null;
		}
		Object result = null;
		try {
			String nodeValue = obj.getFirstChild().getNodeValue();
			result = converters.getConverter(configPropertyClass, nodeValue).convert(nodeValue);
		} catch (NumberFormatException e) {
			String msg = "Invalid format for attribute " + obj.getLocalName() + ".  Cannot parse as type "
					+ configPropertyClass.getCanonicalName();
			throw new MidPointException(msg);
		} catch (UnsupportedOperationException e) {
			String msg = "Conversion failed for  " + obj.getLocalName()
					+ ".  No known coercion from type String to type "
					+ configPropertyClass.getCanonicalName();
			throw new MidPointException(msg);
		}
		return result;
	}

	public static ConnectorInfo getConnectorInfo(ConnectorRef connectorRef) throws MidPointException {
		ConnectorInfo connectorInfo = null;
		if (connectorRef != null) {
			// TODO: GET the configuration from RAC
			String connectorHostId = connectorRef.getConnectorHostRef();

			String bundleName = connectorRef.getBundleName();
			String bundleVersion = connectorRef.getBundleVersion();
			String connectorName = connectorRef.getConnectorName();
			connectorInfo = getConnectorInfo(connectorRef, null, bundleName, bundleVersion, connectorName);
		} else {
			throw new MidPointException("Empty connector reference");
		}

		return connectorInfo;
	}

	public static ConnectorInfo getConnectorInfo(ConnectorRef connectorRef,
			ObjectReferenceType connectorHostId, String bundleName, String bundleVersion, String connectorName)
			throws MidPointException {
		ConnectorInfo connectorInfo = null;
		try {
			ConnectorInfoManager mgr = null;
			if (connectorRef.getConnectorHost() != null) {
				RemoteFrameworkConnectionInfo connection = new RemoteFrameworkConnectionInfo(
						connectorRef.getConnectorHost(), connectorRef.getConnectorPort(), new GuardedString(
								connectorRef.getConnectorSecret().toCharArray()));
				mgr = getRemoteConnectorInfoManager(connection);
			} else {
				mgr = getLocalConnectorInfoManager();
			}
			ConnectorKey connKey = new ConnectorKey(bundleName, bundleVersion, connectorName);
			connectorInfo = mgr.findConnectorInfo(connKey);
		} catch (MidPointException e) {
			throw e;
		} catch (Exception e) {
			throw new MidPointException(e);
		}

		return connectorInfo;
	}

	public static ConnectorInfoManager getLocalConnectorInfoManager() throws MidPointException {
		if (null == localConnectorInfoManager) {
			URL[] bundleUrls = getBundleURLs();
			ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
			localConnectorInfoManager = factory.getLocalManager(bundleUrls);
		}
		return localConnectorInfoManager;
	}

	public static ConnectorInfoManager getRemoteConnectorInfoManager(String test) throws MidPointException {
		RemoteFrameworkConnectionInfo remBean = getRFCI(test);
		return getRemoteConnectorInfoManager(remBean);
	}

	public static ConnectorInfoManager getRemoteConnectorInfoManager(RemoteFrameworkConnectionInfo remBean)
			throws MidPointException {
		ConnectorInfoManager mgr = null;
		ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
		try {
			mgr = factory.getRemoteManager(remBean);
		} catch (Exception e) {
			throw new MidPointException(e);
		}
		return mgr;
	}

	private static RemoteFrameworkConnectionInfo getRFCI(String connectorHostType) {
		RemoteFrameworkConnectionInfo remBean = null;
		if (connectorHostType != null) {
			String host = "120.0.0.1";
			int port = 8759;
			int timeout = 0;
			boolean useSsl = false;
			String key = "xOS4IeeE6eb/AhMbhxZEC37PgtE\\=";
			GuardedString pwd = new GuardedString(key.toCharArray());
			remBean = new RemoteFrameworkConnectionInfo(host, port, pwd, useSsl, null, timeout);
		}

		return remBean;
	}

	private static void setTimeouts(OperationTimeouts opTimeouts, APIConfiguration apiConfig)
			throws MidPointException {
		if (opTimeouts != null) {
			for (OperationTimeouts.OperationTimeout opTimeout : opTimeouts.getOperationTimeout()) {
				if (opTimeout != null) {
					int timeout = opTimeout.getTimeout();
					Class api = opMap.get(opTimeout.getName());
					if (api != null) {
						apiConfig.setTimeout(api, timeout);
					} else {
						throw new MidPointException("Unknown operation timeout label '" + opTimeout.getName()
								+ "'");
					}
				}
			}
		}
	}

	private static void setPoolConfiguration(PoolConfigOption poolConfigOps, APIConfiguration apiConfig)
			throws MidPointException {
		ObjectPoolConfiguration connectorPoolConfig = apiConfig.getConnectorPoolConfiguration();
		if (poolConfigOps != null) {
			if (null != poolConfigOps.getMaxIdle()) {
				connectorPoolConfig.setMaxIdle(poolConfigOps.getMaxIdle());
			}
			if (null != poolConfigOps.getMinIdle()) {
				connectorPoolConfig.setMinIdle(poolConfigOps.getMinIdle());
			}
			if (null != poolConfigOps.getMaxObjects()) {
				connectorPoolConfig.setMaxObjects(poolConfigOps.getMaxObjects());
			}
			if (null != poolConfigOps.getMaxWait()) {
				connectorPoolConfig.setMaxWait(poolConfigOps.getMaxWait());
			}
			if (null != poolConfigOps.getMinEvictTimeMillis()) {
				connectorPoolConfig.setMinEvictableIdleTimeMillis(poolConfigOps.getMinEvictTimeMillis());
			}
		}
	}

	private static OperationTimeouts getTimeouts(APIConfiguration apiConfig) throws MidPointException {
		OperationTimeouts opTimeouts = new OperationTimeouts();
		if (apiConfig != null) {
			for (Map.Entry<OperationType, Class> e : opMap.entrySet()) {
				int timeout = apiConfig.getTimeout(e.getValue());
				OperationTimeouts.OperationTimeout opTimeout = new OperationTimeouts.OperationTimeout();
				opTimeout.setName(e.getKey());
				opTimeout.setTimeout(timeout);
				opTimeouts.getOperationTimeout().add(opTimeout);
			}
		}
		return opTimeouts;
	}

	public static PoolConfigOption getPoolConfigOptions(APIConfiguration apiConfig) throws MidPointException {
		PoolConfigOption poolConfigOptions = new PoolConfigOption();
		if ((apiConfig != null) && (apiConfig.isConnectorPoolingSupported())) {
			ObjectPoolConfiguration poolConfig = apiConfig.getConnectorPoolConfiguration();

			int maxIdle = poolConfig.getMaxIdle();
			int minIdle = poolConfig.getMinIdle();
			int maxObjects = poolConfig.getMaxObjects();
			long evictTime = poolConfig.getMinEvictableIdleTimeMillis();
			long maxWait = poolConfig.getMaxWait();

			poolConfigOptions.setMaxIdle(maxIdle);
			poolConfigOptions.setMinIdle(minIdle);
			poolConfigOptions.setMaxObjects(maxObjects);
			poolConfigOptions.setMinEvictTimeMillis(evictTime);
			poolConfigOptions.setMaxWait(maxWait);
		}

		return poolConfigOptions;
	}

	public static String normalizeConnectorName(String connectorName) {
		String name = null;
		if (connectorName != null) {
			int lastDot = connectorName.lastIndexOf(46);
			if (lastDot != -1) {
				name = connectorName.substring(lastDot + 1);
			}
		}

		return name;
	}

	private static boolean supportsActiveSync(APIConfiguration apiConfig) {
		boolean result = false;
		if (apiConfig != null) {
			Set ops = apiConfig.getSupportedOperations();
			if (ops.contains(SyncApiOp.class)) {
				result = true;
			}
		}

		return result;
	}

	public static String getDefaultExecMode(ConnectorFacade facade) {
		boolean resource = facade.getOperation(ScriptOnResourceApiOp.class) != null;
		boolean connector = facade.getOperation(ScriptOnConnectorApiOp.class) != null;
		if ((resource) && (connector)) {
			return null;
		}
		if (resource) {
			return "resource";
		}
		if (connector) {
			return "connector";
		}

		return null;
	}

	public static class CreateOptions {

		private boolean _disableSearchBuffer;

		private boolean _forTest;

		public boolean getDisableSearchBuffer() {
			return this._disableSearchBuffer;
		}

		public void setDisableSearchBuffer(boolean disableSearchBuffer) {
			this._disableSearchBuffer = disableSearchBuffer;
		}
	}
}
