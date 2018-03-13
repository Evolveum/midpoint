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

package com.evolveum.midpoint.test.ldap;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import org.opends.server.config.ConfigException;
import org.opends.server.core.AddOperation;
import org.opends.server.core.BindOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.ModifyDNOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.ByteString;
import org.opends.server.types.DN;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryEnvironmentConfig;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.InitializationException;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;
import org.opends.server.util.ChangeRecordEntry;
import org.opends.server.util.EmbeddedUtils;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.opends.server.util.ModifyChangeRecordEntry;
import org.opends.server.util.ModifyDNChangeRecordEntry;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This class controls embedded OpenDJ instance.
 *
 * It is used in Unit tests. It configures and starts and stops the instance. It
 * can even manage a "template" configuration of OpenDJ and copy it to working
 * instance configuration.
 *
 * @author Radovan Semancik
 */
public class OpenDJController extends AbstractResourceController {

	private String DATA_TEMPLATE_DIR = "test-data";
	private String SERVER_ROOT = "target/test-data/opendj";
	private String LDAP_SUFFIX = "dc=example,dc=com";

	public static final String DEFAULT_TEMPLATE_NAME = "opendj.template";
	public static final String RI_TEMPLATE_NAME = "opendj.template.ri";

	public static final String OBJECT_CLASS_INETORGPERSON_NAME = "inetOrgPerson";
	public static final String RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME = "entryUUID";
	public static final String RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME = "dn";

	protected File serverRoot = new File(SERVER_ROOT);
	protected File configFile = null;
	protected File templateRoot;

	private static final Trace LOGGER = TraceManager.getTrace(OpenDJController.class);

	protected InternalClientConnection internalConnection;

	public OpenDJController() {
		init();
	}

	public OpenDJController(String serverRoot) {
		SERVER_ROOT = serverRoot;
		this.serverRoot = new File(serverRoot);
		init();
	}

	/**
	 * Initialize
	 *
	 */

	private void init() {
		if (!serverRoot.exists()) {
			serverRoot.mkdirs();
		}

		if (configFile == null) {
			configFile = new File(serverRoot, "config/config.ldif");
		}
	}

	/**
	 * Get the value of serverRoot.
	 *
	 * The top directory of working OpenDS installation. The OpenDS placed in
	 * this directory will be used during the tests.
	 *
	 * @return the value of serverRoot
	 */
	public File getServerRoot() {
		return this.serverRoot;
	}

	/**
	 * Set the value of serverRoot
	 *
	 * The top directory of working OpenDS installation. The OpenDS placed in
	 * this directory will be used during the tests.
	 *
	 * @param serverRoot
	 *            new value of serverRoot
	 */
	public void setServerRoot(File serverRoot) {
		this.serverRoot = serverRoot;
	}

	/**
	 * Get the value of configFile
	 *
	 * File name of primary OpenDS configuration file. Normally
	 * <serverRoot>/config/config.ldif
	 *
	 * @return the value of configFile
	 */
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * Set the value of configFile
	 *
	 * File name of primary OpenDS configuration file. Normally
	 * <serverRoot>/config/config.ldif
	 *
	 * @param configFile
	 *            new value of configFile
	 */
	public void setConfigFile(File configFile) {
		this.configFile = configFile;
	}

	/**
	 * Get the value of templateServerRoot
	 *
	 * The top directory of template OpenDS installation. All the files from
	 * this directory will be copied to the working OpenDS directory
	 * (serverRoot). This usually happens before the tests.
	 *
	 * @return the value of templateServerRoot
	 */
	public File getTemplateServerRoot() {
		return templateRoot;
	}

	public String getSuffix() {
		return LDAP_SUFFIX;
	}


	public String getSuffixPeople() {
		return "ou=People,"+LDAP_SUFFIX;
	}

	public String getAccountDn(String username) {
		return "uid="+username+","+getSuffixPeople();
	}

	/**
	 * Get the value of internalConnection
	 *
	 * The connection to the OpenDS instance. It can be used to fetch and
	 * manipulate the data.
	 *
	 * @return the value of internelConnection
	 */
	public InternalClientConnection getInternalConnection() {
		Validate.notNull(internalConnection, "Not connected");
		return internalConnection;
	}

	/**
	 * Refresh working OpenDS installation from the template.
	 *
	 * The existing working OpenDS installation (in serverRoot) will be
	 * discarded and replaced by a fresh known-state setup (from
	 * templateServerRoot).
	 *
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public void refreshFromTemplate(String templateName) throws IOException, URISyntaxException {
		deleteDirectory(serverRoot);
		extractTemplate(serverRoot, templateName);
	}

	/**
	 * Extract template from class
	 */
	private void extractTemplate(File dst, String templateName) throws IOException, URISyntaxException {

		LOGGER.info("Extracting OpenDJ template....");
		if (!dst.exists()) {
			LOGGER.debug("Creating target dir {}", dst.getPath());
			dst.mkdirs();
		}

		templateRoot = new File(DATA_TEMPLATE_DIR, templateName);
		String templateRootPath = DATA_TEMPLATE_DIR + "/" + templateName;		// templateRoot.getPath does not work on Windows, as it puts "\" into the path name (leading to problems with getSystemResource)

		// Determing if we need to extract from JAR or directory
		if (templateRoot.isDirectory()) {
			LOGGER.trace("Need to do directory copy.");
            MiscUtil.copyDirectory(templateRoot, dst);
			return;
		}

		LOGGER.debug("Try to localize OpenDJ Template in JARs as " + templateRootPath);

		URL srcUrl = ClassLoader.getSystemResource(templateRootPath);
		LOGGER.debug("srcUrl " + srcUrl);
		// sample:
		// file:/C:/.m2/repository/test-util/1.9-SNAPSHOT/test-util-1.9-SNAPSHOT.jar!/test-data/opendj.template
		// output:
		// /C:/.m2/repository/test-util/1.9-SNAPSHOT/test-util-1.9-SNAPSHOT.jar
		//
		// beware that in the URL there can be spaces encoded as %20, e.g.
		// file:/C:/Documents%20and%20Settings/user/.m2/repository/com/evolveum/midpoint/infra/test-util/2.1-SNAPSHOT/test-util-2.1-SNAPSHOT.jar!/test-data/opendj.template
		//
		if (srcUrl.getPath().contains("!/")) {
			URI srcFileUri = new URI(srcUrl.getPath().split("!/")[0]);		// e.g. file:/C:/Documents%20and%20Settings/user/.m2/repository/com/evolveum/midpoint/infra/test-util/2.1-SNAPSHOT/test-util-2.1-SNAPSHOT.jar
			File srcFile = new File(srcFileUri);
			JarFile jar = new JarFile(srcFile);
			LOGGER.debug("Extracting OpenDJ from JAR file {} to {}", srcFile.getPath(), dst.getPath());

			Enumeration<JarEntry> entries = jar.entries();

			JarEntry e;
			byte buf[] = new byte[655360];
			while (entries.hasMoreElements()) {
				e = entries.nextElement();

				// skip other files
				if (!e.getName().contains(templateRootPath)) {
					continue;
				}

				// prepare destination file
				String filepath = e.getName().substring(templateRootPath.length());
				File dstFile = new File(dst, filepath);

				// test if directory
				if (e.isDirectory()) {
					LOGGER.debug("Create directory: {}", dstFile.getAbsolutePath());
					dstFile.mkdirs();
					continue;
				}

				LOGGER.debug("Extract {} to {}", filepath, dstFile.getAbsolutePath());
				// Find file on classpath
				InputStream is = ClassLoader.getSystemResourceAsStream(e.getName());
				// InputStream is = jar.getInputStream(e); //old way

				// Copy content
				OutputStream out = new FileOutputStream(dstFile);
				int len;
				while ((len = is.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				is.close();
			}
			jar.close();
		} else {
			try {
				File file = new File(srcUrl.toURI());
				File[] files = file.listFiles();
				for (File subFile : files) {
					if (subFile.isDirectory()) {
						MiscUtil.copyDirectory(subFile, new File(dst, subFile.getName()));
					} else {
						MiscUtil.copyFile(subFile, new File(dst, subFile.getName()));
					}
				}
			} catch (Exception ex) {
				throw new IOException(ex);
			}
		}
		LOGGER.debug("OpenDJ Extracted");
	}

	/**
	 * Start the embedded OpenDJ directory server using files copied from the default
	 * template.
	 */
	public InternalClientConnection startCleanServer() throws IOException, URISyntaxException {
		return startCleanServer(DEFAULT_TEMPLATE_NAME);
	}

	/**
	 * Start the embedded OpenDJ directory server using files copied from the
	 * template with referential integrity plugin turned on.
	 */
	public InternalClientConnection startCleanServerRI() throws IOException, URISyntaxException {
		return startCleanServer(RI_TEMPLATE_NAME);
	}


	/**
	 * Start the embedded OpenDJ directory server using files copied from the specified
	 * template.
	 */
	public InternalClientConnection startCleanServer(String templateName) throws IOException, URISyntaxException {
		refreshFromTemplate(templateName);
		return start();
	}

	/**
	 * Start the embedded OpenDJ directory server.
	 *
	 * Configuration and databases from serverRoot location will be used.
	 *
	 * @return
	 */
	public InternalClientConnection start() {

		LOGGER.info("Starting OpenDJ server");

		DirectoryEnvironmentConfig envConfig = new DirectoryEnvironmentConfig();
		try {
			envConfig.setServerRoot(serverRoot);
			envConfig.setConfigFile(configFile);
			// envConfig.setDisableConnectionHandlers(true);
		} catch (InitializationException ex) {
			ex.printStackTrace();
			throw new RuntimeException("OpenDJ initialization failed", ex);
		}

		// Check if the server is already running
		if (EmbeddedUtils.isRunning()) {
			throw new RuntimeException("Server already running");
		} else {
			try {

				EmbeddedUtils.startServer(envConfig);

			} catch (ConfigException ex) {
				LOGGER.error("Possible OpenDJ misconfiguration: " + ex.getMessage(), ex);
				throw new RuntimeException("OpenDJ startup failed", ex);
			} catch (InitializationException ex) {
				LOGGER.error("OpenDJ startup failed", ex);
				throw new RuntimeException("OpenDJ startup failed", ex);
			}
		}

		internalConnection = InternalClientConnection.getRootConnection();
		if (internalConnection == null) {
			LOGGER.error("OpenDJ cannot get internal connection (null)");
			throw new RuntimeException("OpenDS cannot get internal connection (null)");
		}

		LOGGER.info("OpenDJ server started");

		return internalConnection;
	}

	/**
	 * Stop the embedded OpenDS server.
	 *
	 */
	public void stop() {
		if (EmbeddedUtils.isRunning()) {
			LOGGER.debug("Stopping OpenDJ server");
			EmbeddedUtils.stopServer(this.getClass().getName(), Message.EMPTY);
			LOGGER.info("OpenDJ server is stopped");
		} else {
			LOGGER.warn("Attempt to stop OpenDJ server that is already stopped.");
		}
	}

	public boolean isRunning() {
		return EmbeddedUtils.isRunning();
	}

	public void assumeRunning() {
		if (!isRunning()) {
			start();
		}
	}

	public void assumeStopped() {
		if (isRunning()) {
			stop();
		}
	}

	/**
	 * Delete a directory and its contents.
	 *
	 * @param dir
	 *            The name of the directory to delete.
	 * @throws IOException
	 *             If the directory could not be deleted.
	 */
	public static void deleteDirectory(File dir) throws IOException {
		if (dir.isDirectory()) {
			// Recursively delete sub-directories and files.
			for (String child : dir.list()) {
				deleteDirectory(new File(dir, child));
			}
		}

		dir.delete();
	}

	public Set<String> asSet(List<Attribute> attributes) {
		// Just blindly get the fist one now.
		// There is most likely just one anyway.
		// TODO: improve that later

		// Attribute attr = attributes.get(0);
		Set<String> result = new HashSet<>();

		// TODO find newer OpenDS jar
		// Iterator<AttributeValue> iterator = attr.iterator();
		// while (iterator.hasNext()) {
		// result.add(iterator.next().toString());
		// }

		return result;
	}

	// Generic utility methods

	public Entry searchByEntryUuid(String entryUuid) throws DirectoryException {
		InternalSearchOperation op = getInternalConnection().processSearch(
				"dc=example,dc=com", SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
				100, false, "(entryUUID=" + entryUuid + ")", getSearchAttributes());

		LinkedList<SearchResultEntry> searchEntries = op.getSearchEntries();
		if (searchEntries == null || searchEntries.isEmpty()) {
			return null;
		}
		if (searchEntries.size() > 1) {
			AssertJUnit.fail("Multiple matches for Entry UUID "+entryUuid+": "+searchEntries);
		}
		return searchEntries.get(0);
	}

	public Entry searchAndAssertByEntryUuid(String entryUuid) throws DirectoryException {
		Entry entry = searchByEntryUuid(entryUuid);
		if (entry == null) {
			AssertJUnit.fail("Entry UUID "+entryUuid+" not found");
		}
		return entry;
	}

	public Entry searchSingle(String filter) throws DirectoryException {
		InternalSearchOperation op = getInternalConnection().processSearch(
				getSuffix(), SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
				100, false, filter, getSearchAttributes());

		if (op.getEntriesSent() == 0) {
			return null;
		} else if (op.getEntriesSent() > 1) {
			AssertJUnit.fail("Found too many entries ("+op.getEntriesSent()+") for filter "+filter);
		}
		return op.getSearchEntries().get(0);
	}

	public Entry searchByUid(String string) throws DirectoryException {
		return searchSingle("(uid=" + string + ")");
	}

	public Entry fetchEntry(String dn) throws DirectoryException {
		Validate.notNull(dn);
		InternalSearchOperation op = getInternalConnection().processSearch(
				dn, SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
				100, false, "(objectclass=*)", getSearchAttributes());

		if (op.getEntriesSent() == 0) {
			return null;
		} else if (op.getEntriesSent() > 1) {
			AssertJUnit.fail("Found too many entries ("+op.getEntriesSent()+") for dn "+dn);
		}
		return op.getSearchEntries().get(0);
	}

	public Entry fetchAndAssertEntry(String dn, String objectClass) throws DirectoryException {
		Entry entry = fetchEntry(dn);
		AssertJUnit.assertNotNull("No entry for DN "+dn, entry);
		assertDn(entry, dn);
		assertObjectClass(entry, objectClass);
		return entry;
	}

	private LinkedHashSet<String> getSearchAttributes() {
		LinkedHashSet<String> attrs = new LinkedHashSet<>();
		attrs.add("*");
		attrs.add("ds-pwp-account-disabled");
		attrs.add("createTimestamp");
		attrs.add("modifyTimestamp");
		return attrs;
	}

	public boolean isAccountEnabled(Entry ldapEntry) {
		String pwpAccountDisabled = getAttributeValue(ldapEntry, "ds-pwp-account-disabled");
		if (pwpAccountDisabled != null && pwpAccountDisabled.equalsIgnoreCase("true")) {
			return false;
		}
		return true;
	}

	public static String getAttributeValue(Entry response, String name) {
		List<Attribute> attrs = response.getAttribute(name.toLowerCase());
		if (attrs == null || attrs.size() == 0) {
			return null;
		}
		assertEquals("Too many attributes for name "+name+": ",
				1, attrs.size());
		Attribute attribute = attrs.get(0);
		return getAttributeValue(attribute);
	}

	public static String getAttributeValue(Attribute attribute) {
		return attribute.iterator().next().getValue().toString();
	}

	public static byte[] getAttributeValueBinary(Entry response, String name) {
		List<Attribute> attrs = response.getAttribute(name.toLowerCase());
		if (attrs == null || attrs.size() == 0) {
			return null;
		}
		assertEquals("Too many attributes for name "+name+": ",
				1, attrs.size());
		Attribute attribute = attrs.get(0);
		ByteString value = attribute.iterator().next().getValue();
		return value.toByteArray();
	}

	public static Collection<String> getAttributeValues(Entry response, String name) {
		List<Attribute> attrs = response.getAttribute(name.toLowerCase());
		if (attrs == null || attrs.size() == 0) {
			return null;
		}
		assertEquals("Too many attributes for name "+name+": ",
				1, attrs.size());
		Attribute attribute = attrs.get(0);
		Collection<String> values = new ArrayList<>(attribute.size());
		Iterator<AttributeValue> iterator = attribute.iterator();
		while (iterator.hasNext()) {
			AttributeValue attributeValue = iterator.next();
			values.add(attributeValue.getValue().toString());
		}
		return values;
	}

	public static String getDn(Entry response) {
		DN dn = response.getDN();
		return dn.toString();
	}

	public static void assertDn(Entry response, String expected) throws DirectoryException {
		DN actualDn = response.getDN();
		if (actualDn.compareTo(DN.decode(expected)) != 0) {
			AssertJUnit.fail("Wrong DN, expected "+expected+" but was "+actualDn.toString());
		}
	}

	public void assertNoEntry(String dn) throws DirectoryException {
		Entry entry = fetchEntry(dn);
		if (entry != null) {
			AssertJUnit.fail("Found entry for dn "+dn+" while not expecting it: "+entry);
		}
	}

	public static void assertObjectClass(Entry response, String expected) throws DirectoryException {
		Collection<String> objectClassValues = getAttributeValues(response, "objectClass");
		AssertJUnit.assertTrue("Wrong objectclass for entry "+getDn(response)+", expected "+expected+" but got "+objectClassValues,
				objectClassValues.contains(expected));
	}

	public static void assertNoObjectClass(Entry response, String unexpected) throws DirectoryException {
		Collection<String> objectClassValues = getAttributeValues(response, "objectClass");
		AssertJUnit.assertFalse("Unexpected objectclass for entry "+getDn(response)+": "+unexpected+", got "+objectClassValues,
				objectClassValues.contains(unexpected));
	}

	public void assertUniqueMember(Entry groupEntry, String accountDn) throws DirectoryException {
		Collection<String> members = getAttributeValues(groupEntry, "uniqueMember");
		assertContainsDn("No member "+accountDn+" in group "+getDn(groupEntry),
				members, accountDn);
	}

	public static void assertContainsDn(String message, Collection<String> actualValues, String expectedValue) throws DirectoryException {
		AssertJUnit.assertNotNull(message+", expected "+expectedValue+", got null", actualValues);
		DN expectedDn = DN.decode(expectedValue);
		for (String actualValue: actualValues) {
			DN actualDn = DN.decode(actualValue);
			if (actualDn.compareTo(expectedDn)==0) {
				return;
			}
		}
		AssertJUnit.fail(message+", expected "+expectedValue+", got "+actualValues);
	}

	public void assertUniqueMember(String groupDn, String accountDn) throws DirectoryException {
		Entry groupEntry = fetchEntry(groupDn);
		assertUniqueMember(groupEntry, accountDn);
	}

	public void assertNoUniqueMember(String groupDn, String accountDn) throws DirectoryException {
		Entry groupEntry = fetchEntry(groupDn);
		assertNoUniqueMember(groupEntry, accountDn);
	}

	public void assertNoUniqueMember(Entry groupEntry, String accountDn) {
		Collection<String> members = getAttributeValues(groupEntry, "uniqueMember");
		MidPointAsserts.assertNotContainsCaseIgnore("Member "+accountDn+" in group "+getDn(groupEntry),
				members, accountDn);
	}

	public static void assertAttribute(Entry response, String name, String... values) {
		List<Attribute> attrs = response.getAttribute(name.toLowerCase());
		if (attrs == null || attrs.size() == 0) {
			if (values.length == 0) {
				return;
			} else {
				AssertJUnit.fail("Attribute "+name+" does not have any value");
			}
		}
		assertEquals("Too many \"attributes\" for "+name+": ",
				1, attrs.size());
		Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
		if (values.length != attribute.size()) {
			AssertJUnit.fail("Wrong number of values for attribute "+name+", expected "+values.length+" values but got "+attribute.size()+" values: "+attribute);
		}
		for (String value: values) {
			boolean found = false;
			Iterator<AttributeValue> iterator = attribute.iterator();
			List<String> attrVals = new ArrayList<>();
			while (iterator.hasNext()) {
				AttributeValue attributeValue = iterator.next();
				String attrVal = attributeValue.toString();
				attrVals.add(attrVal);
				if (attrVal.equals(value)) {
					found = true;
				}
			}
			if (!found) {
				AssertJUnit.fail("Attribute "+name+" does not contain value "+value+", it has values: "+attrVals);
			}
		}
	}

	public static void assertNoAttribute(Entry response, String name) {
		List<Attribute> attrs = response.getAttribute(name.toLowerCase());
		if (attrs == null || attrs.size() == 0) {
			return;
		}
		assertEquals("Too many \"attributes\" for "+name+": ",
				1, attrs.size());
		Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
		if (attribute.size() == 0) {
			return;
		}
		if (attribute.isEmpty()) {
			return;
		}
		AssertJUnit.fail("Attribute "+name+" exists while not expecting it: "+attribute);
	}

	public void assertActive(Entry response, boolean active) {
		assertEquals("Unexpected activation of entry "+response, active, isAccountEnabled(response));
	}

	public Entry addEntryFromLdifFile(File file) throws IOException, LDIFException {
		return addEntryFromLdifFile(file.getPath());
	}

	public Entry addEntryFromLdifFile(String filename) throws IOException, LDIFException {
		LDIFImportConfig importConfig = new LDIFImportConfig(filename);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry ldifEntry = ldifReader.readEntry();
        addEntry(ldifEntry);
        return ldifEntry;
	}

	public List<Entry> addEntriesFromLdifFile(String filename) throws IOException, LDIFException {
		List<Entry> retval = new ArrayList<>();
		LDIFImportConfig importConfig = new LDIFImportConfig(filename);
		LDIFReader ldifReader = new LDIFReader(importConfig);
		for (;;) {
			Entry ldifEntry = ldifReader.readEntry();
			if (ldifEntry == null) {
				break;
			}
			addEntry(ldifEntry);
			retval.add(ldifEntry);
		}
		return retval;
	}

	public void addEntry(Entry ldapEntry) {
		 AddOperation addOperation = getInternalConnection().processAdd(ldapEntry);

        if (ResultCode.SUCCESS != addOperation.getResultCode()) {
        	throw new RuntimeException("LDAP operation error: "+addOperation.getResultCode()+": "+addOperation.getErrorMessage());
        }
	}

	public void addEntry(String ldif) throws IOException, LDIFException {
		LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(ldif, "utf-8"));
	    LDIFReader ldifReader = new LDIFReader(importConfig);
	    Entry ldifEntry = ldifReader.readEntry();
	    addEntry(ldifEntry);
	}

	public ChangeRecordEntry executeRenameChange(String filename) throws LDIFException, IOException{
		LDIFImportConfig importConfig = new LDIFImportConfig(filename);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);

        if (!(entry instanceof ModifyDNChangeRecordEntry)){
        	throw new LDIFException(new MessageBuilder("Could not execute rename..Bad change").toMessage());
        }

        ModifyDNOperation modifyOperation = getInternalConnection().processModifyDN((ModifyDNChangeRecordEntry)entry);

        if (ResultCode.SUCCESS != modifyOperation.getResultCode()) {
        	throw new RuntimeException("LDAP operation error: "+modifyOperation.getResultCode()+": "+modifyOperation.getErrorMessage());
        }
        return entry;

	}

	public ChangeRecordEntry executeLdifChange(File file) throws IOException, LDIFException {
		LDIFImportConfig importConfig = new LDIFImportConfig(file.getPath());
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);

        ModifyOperation modifyOperation = getInternalConnection()
        		.processModify((ModifyChangeRecordEntry) entry);

        if (ResultCode.SUCCESS != modifyOperation.getResultCode()) {
        	throw new RuntimeException("LDAP operation error: "+modifyOperation.getResultCode()+": "+modifyOperation.getErrorMessage());
        }
        return entry;
	}

	public ChangeRecordEntry executeLdifChange(String ldif) throws IOException, LDIFException {
		InputStream ldifInputStream = IOUtils.toInputStream(ldif, "UTF-8");
		LDIFImportConfig importConfig = new LDIFImportConfig(ldifInputStream);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);

        ModifyOperation modifyOperation = getInternalConnection()
        		.processModify((ModifyChangeRecordEntry) entry);

        if (ResultCode.SUCCESS != modifyOperation.getResultCode()) {
        	throw new RuntimeException("LDAP operation error: "+modifyOperation.getResultCode()+": "+modifyOperation.getErrorMessage());
        }
        return entry;
	}

	public ChangeRecordEntry modifyReplace(String entryDn, String attributeName, String value) throws IOException, LDIFException {
        String ldif = "dn: " + entryDn + "\nchangetype: modify\nreplace: "+attributeName+"\n"+attributeName+": " + value;
        return executeLdifChange(ldif);
    }

	public ChangeRecordEntry modifyAdd(String entryDn, String attributeName, String value) throws IOException, LDIFException {
        String ldif = "dn: " + entryDn + "\nchangetype: modify\nadd: "+attributeName+"\n"+attributeName+": " + value;
        return executeLdifChange(ldif);
    }

	public ChangeRecordEntry modifyDelete(String entryDn, String attributeName, String value) throws IOException, LDIFException {
        String ldif = "dn: " + entryDn + "\nchangetype: modify\ndelete: "+attributeName+"\n"+attributeName+": " + value;
        return executeLdifChange(ldif);
    }

	public void delete(String entryDn) {
		DeleteOperation deleteOperation = getInternalConnection().processDelete(entryDn);
		if (ResultCode.SUCCESS != deleteOperation.getResultCode()) {
        	throw new RuntimeException("LDAP operation error: "+deleteOperation.getResultCode()+": "+deleteOperation.getErrorMessage());
        }
	}

	public String dumpEntries() throws DirectoryException {
		InternalSearchOperation op = getInternalConnection().processSearch(
				LDAP_SUFFIX, SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
				100, false, "(objectclass=*)", getSearchAttributes());

		StringBuilder sb = new StringBuilder();
		for (SearchResultEntry searchEntry: op.getSearchEntries()) {
			sb.append(toHumanReadableLdifoid(searchEntry));
			sb.append("\n");
		}

		return sb.toString();
	}

	public String dumpTree() throws DirectoryException {
		StringBuilder sb = new StringBuilder();
		sb.append(LDAP_SUFFIX).append("\n");
		dumpTreeLevel(sb, LDAP_SUFFIX, 1);
		return sb.toString();
	}

	private void dumpTreeLevel(StringBuilder sb, String dn, int indent) throws DirectoryException {
		InternalSearchOperation op = getInternalConnection().processSearch(
				dn, SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
				100, false, "(objectclass=*)", getSearchAttributes());

		for (SearchResultEntry searchEntry: op.getSearchEntries()) {
			ident(sb, indent);
			sb.append(searchEntry.getDN().getRDN());
			sb.append("\n");
			dumpTreeLevel(sb, searchEntry.getDN().toString(), indent + 1);
		}
	}

	private void ident(StringBuilder sb, int indent) {
		for(int i=0; i < indent; i++) {
			sb.append("  ");
		}
	}

	public String toHumanReadableLdifoid(Entry entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("dn: ").append(entry.getDN()).append("\n");
		for (Attribute attribute: entry.getAttributes()) {
			for (AttributeValue val: attribute) {
				sb.append(attribute.getName());
				sb.append(": ");
				sb.append(val);
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public Collection<String> getGroupUniqueMembers(String groupDn) throws DirectoryException {
    	Entry groupEntry = fetchEntry(groupDn);
        if (groupEntry == null) {
            throw new IllegalArgumentException(groupDn + " was not found");
        }
        return getAttributeValues(groupEntry, "uniqueMember");
    }

    /*
        dn: <group>
        changetype: modify
        delete: uniqueMember
        uniqueMember: <member>
     */
    public ChangeRecordEntry removeGroupUniqueMember(String groupDn, String memberDn) throws IOException, LDIFException {
        String ldif = "dn: " + groupDn + "\nchangetype: modify\ndelete: uniqueMember\nuniqueMember: " + memberDn;
        return executeLdifChange(ldif);
    }

	public ChangeRecordEntry addGroupUniqueMember(String groupDn, String memberDn) throws IOException, LDIFException {
		String ldif = "dn: " + groupDn + "\nchangetype: modify\nadd: uniqueMember\nuniqueMember: " + memberDn;
		return executeLdifChange(ldif);
	}

	public ChangeRecordEntry addGroupUniqueMembers(String groupDn, List<String> memberDns) throws IOException, LDIFException {
		if (memberDns.isEmpty()) {
			return null; // garbage in garbage out, sorry
		}

		StringBuilder sb = new StringBuilder();
		sb.append("dn: ").append(groupDn).append("\nchangetype: modify\nadd: uniqueMember");
		for (String memberDn : memberDns) {
			sb.append("\nuniqueMember: ").append(memberDn);
		}
		return executeLdifChange(sb.toString());
	}

	public boolean checkPassword(String entryDn, String password) throws DirectoryException {
		InternalClientConnection conn = new InternalClientConnection(DN.decode(entryDn));
		BindOperation op = conn.processSimpleBind(entryDn, password);
		if (op.getResultCode() == ResultCode.SUCCESS) {
			return true;
		} else {
			LOGGER.error("Bind error: {} ({})", op.getAuthFailureReason(), op.getResultCode());
			return false;
		}
	}

	public void assertPassword(String entryDn, String password) throws DirectoryException {
		if (!checkPassword(entryDn, password)) {
			AssertJUnit.fail("Expected that entry "+entryDn+" will have password '"+password+"'. But the check failed.");
		}
	}

	public void assertHasObjectClass(Entry entry, String expectedObjectclass) {
		Collection<String> objectclasses = getAttributeValues(entry, "objectClass");
		if (!objectclasses.contains(expectedObjectclass)) {
			AssertJUnit.fail("Expected that entry "+entry.getDN()+" will have object class '"+expectedObjectclass
					+"'. But it has object classes: "+objectclasses);
		}
	}
	
	public void assertHasNoObjectClass(Entry entry, String expectedObjectclass) {
		Collection<String> objectclasses = getAttributeValues(entry, "objectClass");
		if (objectclasses.contains(expectedObjectclass)) {
			AssertJUnit.fail("Expected that entry "+entry.getDN()+" will NOT have object class '"+expectedObjectclass
					+"'. But it was there: "+objectclasses);
		}
	}

}
