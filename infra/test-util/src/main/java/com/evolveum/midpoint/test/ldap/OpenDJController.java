/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.ldap;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import org.opends.server.config.ConfigException;
import org.opends.server.core.*;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.*;
import org.opends.server.util.*;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

/**
 * This class controls embedded OpenDJ instance.
 * <p>
 * It is used in Unit tests. It configures and starts and stops the instance. It
 * can even manage a "template" configuration of OpenDJ and copy it to working
 * instance configuration.
 *
 * @author Radovan Semancik
 */
public class OpenDJController extends AbstractResourceController {

    private final String dataTemplateDir = "test-data";
    private final String ldapSuffix = "dc=example,dc=com";
    private String serverRootDirectoryName = "target/test-data/opendj";

    public static final String DEFAULT_TEMPLATE_NAME = "opendj.template";
    public static final String RI_TEMPLATE_NAME = "opendj.template.ri";

    public static final String OBJECT_CLASS_INETORGPERSON_NAME = "inetOrgPerson";
    public static final QName OBJECT_CLASS_INETORGPERSON_QNAME =
            new QName(MidPointConstants.NS_RI, OBJECT_CLASS_INETORGPERSON_NAME);
    public static final String RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME = "entryUUID";
    public static final String RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME = "dn";
    public static final ItemName RESOURCE_OPENDJ_SECONDARY_IDENTIFIER =
            new ItemName(MidPointConstants.NS_RI, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME);
    public static final String RESOURCE_OPENDJ_DN_LOCAL_NAME = "dn";
    public static final ItemName RESOURCE_OPENDJ_DN = new ItemName(MidPointConstants.NS_RI, RESOURCE_OPENDJ_DN_LOCAL_NAME);

    protected File serverRoot = new File(serverRootDirectoryName);
    protected File configFile = null;
    protected File templateRoot;
    protected File logDirectory = new File(serverRoot, "logs");
    protected File accessLogFile = new File(logDirectory, "access");

    private static final Trace LOGGER = TraceManager.getTrace(OpenDJController.class);

    protected InternalClientConnection internalConnection;

    public OpenDJController() {
        init();
    }

    public OpenDJController(String serverRoot) {
        serverRootDirectoryName = serverRoot;
        this.serverRoot = new File(serverRoot);
        init();
    }

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
     * <p>
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
     * <p>
     * The top directory of working OpenDS installation. The OpenDS placed in
     * this directory will be used during the tests.
     *
     * @param serverRoot new value of serverRoot
     */
    public void setServerRoot(File serverRoot) {
        this.serverRoot = serverRoot;
    }

    /**
     * Get the value of configFile
     * <p>
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
     * <p>
     * File name of primary OpenDS configuration file. Normally
     * <serverRoot>/config/config.ldif
     *
     * @param configFile new value of configFile
     */
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Get the value of templateServerRoot
     * <p>
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
        return ldapSuffix;
    }

    public String getSuffixPeople() {
        return "ou=People," + ldapSuffix;
    }

    public String getAccountDn(String username) {
        return "uid=" + username + "," + getSuffixPeople();
    }

    /**
     * Get the value of internalConnection
     * <p>
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
     * <p>
     * The existing working OpenDS installation (in serverRoot) will be
     * discarded and replaced by a fresh known-state setup (from
     * templateServerRoot).
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

        templateRoot = new File(dataTemplateDir, templateName);
        String templateRootPath = dataTemplateDir + "/" + templateName;        // templateRoot.getPath does not work on Windows, as it puts "\" into the path name (leading to problems with getSystemResource)

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
            URI srcFileUri = new URI(srcUrl.getPath().split("!/")[0]);        // e.g. file:/C:/Documents%20and%20Settings/user/.m2/repository/com/evolveum/midpoint/infra/test-util/2.1-SNAPSHOT/test-util-2.1-SNAPSHOT.jar
            File srcFile = new File(srcFileUri);
            JarFile jar = new JarFile(srcFile);
            LOGGER.debug("Extracting OpenDJ from JAR file {} to {}", srcFile.getPath(), dst.getPath());

            Enumeration<JarEntry> entries = jar.entries();

            JarEntry e;
            byte[] buf = new byte[655360];
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
        if (isRunning()) {
            LOGGER.warn("OpenDJ is still running - stopping now."
                    + "Did previous test failed to call OpenDJController.stop()?");
            stop();
        }

        refreshFromTemplate(templateName);
        return start();
    }

    /**
     * Start the embedded OpenDJ directory server.
     * <p>
     * Configuration and databases from serverRoot location will be used.
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
     * @param dir The name of the directory to delete.
     */
    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            // Recursively delete sub-directories and files.
            for (String child : dir.list()) {
                deleteDirectory(new File(dir, child));
            }
        }

        dir.delete();
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
            AssertJUnit.fail("Multiple matches for Entry UUID " + entryUuid + ": " + searchEntries);
        }
        return searchEntries.get(0);
    }

    public Entry searchAndAssertByEntryUuid(String entryUuid) throws DirectoryException {
        Entry entry = searchByEntryUuid(entryUuid);
        if (entry == null) {
            AssertJUnit.fail("Entry UUID " + entryUuid + " not found");
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
            AssertJUnit.fail("Found too many entries (" + op.getEntriesSent() + ") for filter " + filter);
        }
        return op.getSearchEntries().get(0);
    }

    public List<? extends Entry> search(String filter) throws DirectoryException {
        return search(getSuffix(), SearchScope.WHOLE_SUBTREE, filter);
    }

    public List<? extends Entry> search(String baseDn, String filter) throws DirectoryException {
        return search(baseDn, SearchScope.WHOLE_SUBTREE, filter);
    }

    public List<? extends Entry> search(String baseDn, SearchScope scope, String filter) throws DirectoryException {
        InternalSearchOperation op = getInternalConnection().processSearch(
                baseDn, scope, DereferencePolicy.NEVER_DEREF_ALIASES, 0,
                0, false, filter, getSearchAttributes());

        return op.getSearchEntries();
    }

    public Entry searchByUid(String string) throws DirectoryException {
        return searchSingle("(uid=" + string + ")");
    }

    public @NotNull Entry fetchEntryRequired(String dn) throws DirectoryException {
        Entry entry = fetchEntry(dn);
        if (entry == null) {
            throw new AssertionError("No entry for DN " + dn);
        }
        return entry;
    }

    public Entry fetchEntry(String dn) throws DirectoryException {
        Validate.notNull(dn);
        InternalSearchOperation op = getInternalConnection().processSearch(
                dn, SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
                100, false, "(objectclass=*)", getSearchAttributes());

        if (op.getEntriesSent() == 0) {
            return null;
        } else if (op.getEntriesSent() > 1) {
            AssertJUnit.fail("Found too many entries (" + op.getEntriesSent() + ") for dn " + dn);
        }
        return op.getSearchEntries().get(0);
    }

    public Entry fetchAndAssertEntry(String dn, String objectClass) throws DirectoryException {
        Entry entry = fetchEntryRequired(dn);
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
        return getAttributeValue(response, name, null);
    }

    public static String getAttributeValue(Entry response, String name, String option) {
        List<Attribute> attrs = response.getAttribute(name.toLowerCase());
        Attribute attribute = findAttribute(option, attrs);
        if (attribute == null) {
            return null;
        }
        return getAttributeValue(attribute);
    }

    private static Attribute findAttribute(String option, List<Attribute> attributes) {
        if (attributes == null || attributes.size() == 0) {
            return null;
        }
        for (Attribute attr : attributes) {
            if (option == null) {
                if (attr.getOptions() == null || attr.getOptions().isEmpty()) {
                    return attr;
                }
            } else {
                if (attr.getOptions() != null && attr.getOptions().contains(option)) {
                    return attr;
                }
            }
        }
        return null;
    }

    public static String getAttributeValue(Attribute attribute) {
        return attribute.iterator().next().getValue().toString();
    }

    public static byte[] getAttributeValueBinary(Entry response, String name) {
        List<Attribute> attrs = response.getAttribute(name.toLowerCase());
        if (attrs == null || attrs.size() == 0) {
            return null;
        }
        assertEquals("Too many attributes for name " + name + ": ",
                1, attrs.size());
        Attribute attribute = attrs.get(0);
        ByteString value = attribute.iterator().next().getValue();
        return value.toByteArray();
    }

    public static Collection<String> getAttributeValues(Entry response, String name) {
        List<Attribute> attrs = response.getAttribute(name.toLowerCase());
        if (attrs == null || attrs.isEmpty()) {
            return null;
        }
        assertEquals("Too many attributes for name " + name + ": ",
                1, attrs.size());
        Attribute attribute = attrs.get(0);
        Collection<String> values = new ArrayList<>(attribute.size());
        for (AttributeValue attributeValue : attribute) {
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
            AssertJUnit.fail("Wrong DN, expected " + expected + " but was " + actualDn.toString());
        }
    }

    public void assertNoEntry(String dn) throws DirectoryException {
        Entry entry = fetchEntry(dn);
        if (entry != null) {
            AssertJUnit.fail("Found entry for dn " + dn + " while not expecting it: " + entry);
        }
    }

    public static void assertObjectClass(Entry response, String expected) {
        Collection<String> objectClassValues = getAttributeValues(response, "objectClass");
        AssertJUnit.assertTrue("Wrong objectclass for entry " + getDn(response) + ", expected " + expected + " but got " + objectClassValues,
                objectClassValues.contains(expected));
    }

    public static void assertNoObjectClass(Entry response, String unexpected) {
        Collection<String> objectClassValues = getAttributeValues(response, "objectClass");
        AssertJUnit.assertFalse("Unexpected objectclass for entry " + getDn(response) + ": " + unexpected + ", got " + objectClassValues,
                objectClassValues.contains(unexpected));
    }

    public static void assertContainsDn(String message, Collection<String> actualValues, String expectedValue) throws DirectoryException {
        AssertJUnit.assertNotNull(message + ", expected " + expectedValue + ", got null", actualValues);
        DN expectedDn = DN.decode(expectedValue);
        for (String actualValue : actualValues) {
            DN actualDn = DN.decode(actualValue);
            if (actualDn.compareTo(expectedDn) == 0) {
                return;
            }
        }
        AssertJUnit.fail(message + ", expected " + expectedValue + ", got " + actualValues);
    }

    public static void assertDns(String message, Collection<String> actualValues, String... expectedValues) {
        if (actualValues == null && expectedValues.length == 0) {
            return;
        }
        if (!MiscUtil.unorderedCollectionEquals(actualValues, Arrays.asList(expectedValues),
                (actualValue, expectedValue) -> {
                    try {
                        DN actualDn = DN.decode(actualValue);
                        DN expectedDn = DN.decode(expectedValue);
                        return actualDn.compareTo(expectedDn) == 0;
                    } catch (DirectoryException e) {
                        throw new SystemException("DN decode problem: " + e.getMessage(), e);
                    }
                })) {
            AssertJUnit.fail(message + ", expected " + ArrayUtils.toString(expectedValues) + ", got " + actualValues);
        }
    }

    public void assertUniqueMember(Entry groupEntry, String accountDn) throws DirectoryException {
        Collection<String> members = getAttributeValues(groupEntry, "uniqueMember");
        assertContainsDn("No member " + accountDn + " in group " + getDn(groupEntry),
                members, accountDn);
    }

    public void assertUniqueMembers(Entry groupEntry, String... accountDns) {
        Collection<String> members = getAttributeValues(groupEntry, "uniqueMember");
        assertDns("Wrong members in group " + getDn(groupEntry),
                members, accountDns);
    }

    public void assertMemberUids(Entry groupEntry, String... uids) {
        Collection<String> members = getAttributeValues(groupEntry, "memberUid");
        if (uids.length == 0 && members == null) {
            return;
        }
        assertThat(members)
                .as("Members in group " + getDn(groupEntry))
                .containsExactlyInAnyOrder(uids);
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
        MidPointAsserts.assertNotContainsCaseIgnore(
                "Member " + accountDn + " in group " + getDn(groupEntry), members, accountDn);
    }

    public void assertUniqueMembers(String groupDn, String... accountDns) throws DirectoryException {
        Entry groupEntry = fetchEntry(groupDn);
        assertUniqueMembers(groupEntry, accountDns);
    }

    public void assertMemberUids(String groupDn, String... uids) throws DirectoryException {
        Entry groupEntry = fetchEntry(groupDn);
        assertMemberUids(groupEntry, uids);
    }

    public static void assertAttribute(Entry response, String name, String... values) {
        List<Attribute> attrs = response.getAttribute(name.toLowerCase());
        if (attrs == null || attrs.size() == 0) {
            if (values.length == 0) {
                return;
            } else {
                AssertJUnit.fail("Attribute " + name + " does not have any value");
            }
        }
        assertEquals("Too many \"attributes\" for " + name + ": ",
                1, attrs.size());
        Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
        if (values.length != attribute.size()) {
            AssertJUnit.fail("Wrong number of values for attribute " + name + ", expected " + values.length + " values but got " + attribute.size() + " values: " + attribute);
        }
        for (String value : values) {
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
                AssertJUnit.fail("Attribute " + name + " does not contain value " + value + ", it has values: " + attrVals);
            }
        }
    }

    public static void assertNoAttribute(Entry response, String name) {
        List<Attribute> attrs = response.getAttribute(name.toLowerCase());
        if (attrs == null || attrs.size() == 0) {
            return;
        }
        assertEquals("Too many \"attributes\" for " + name + ": ",
                1, attrs.size());
        Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
        if (attribute.size() == 0) {
            return;
        }
        if (attribute.isEmpty()) {
            return;
        }
        AssertJUnit.fail("Attribute " + name + " exists while not expecting it: " + attribute);
    }

    public static void assertAttributeLang(Entry entry, String attributeName, String expectedOrigValue, String... params) {
        List<Attribute> attrs = entry.getAttribute(attributeName.toLowerCase());
        if (attrs == null || attrs.size() == 0) {
            if (expectedOrigValue == null && params.length == 0) {
                // everything is as it should be
                return;
            }
            AssertJUnit.fail("Attribute " + attributeName + " does not have any value");
        }
        Map<String, String> expectedLangs = MiscUtil.paramsToMap(params);
        List<String> langsSeen = new ArrayList<>();
        for (Attribute attr : attrs) {
            if (attr.size() == 0) {
                throw new IllegalArgumentException("No values in attribute " + attributeName + ": " + attr);
            }
            if (attr.size() > 1) {
                throw new IllegalArgumentException("Too many values in attribute " + attributeName + ": " + attr);
            }
            String attrValue = attr.iterator().next().toString();
            if (attr.getOptions() == null || attr.getOptions().isEmpty()) {
                assertEquals("Wrong orig value in attribute '" + attributeName + " in entry " + entry.getDN(), expectedOrigValue, attrValue);
            } else if (attr.getOptions().size() == 1) {
                String option = attr.getOptions().iterator().next();
                if (!option.startsWith("lang-")) {
                    throw new IllegalArgumentException("Non-lang option " + option + " in attribute " + attributeName + ": " + attr);
                }
                String lang = option.substring("lang-".length());
                String expectedValue = expectedLangs.get(lang);
                assertEquals("Wrong " + option + " value in attribute '" + attributeName + " in entry " + entry.getDN(), expectedValue, attrValue);
                langsSeen.add(lang);
            } else {
                throw new IllegalArgumentException("More than one option in attribute " + attributeName + ": " + attr);
            }
        }
        for (Map.Entry<String, String> expectedLangEntry : expectedLangs.entrySet()) {
            if (!langsSeen.contains(expectedLangEntry.getKey())) {
                AssertJUnit.fail("No lang " + expectedLangEntry.getKey() + " in attribute " + attributeName + " in entry " + entry.getDN() + "; expected " + expectedLangEntry.getValue());
            }
        }
    }

    public void assertActive(Entry response, boolean active) {
        assertEquals("Unexpected activation of entry " + response, active, isAccountEnabled(response));
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

    public List<Entry> addEntriesFromLdifFile(File file) throws IOException, LDIFException {
        return addEntriesFromLdifFile(file.getPath());
    }

    public List<Entry> addEntriesFromLdifFile(String filename) throws IOException, LDIFException {
        List<Entry> retval = new ArrayList<>();
        LDIFImportConfig importConfig = new LDIFImportConfig(filename);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        for (; ; ) {
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
            throw new RuntimeException("LDAP operation error: " + addOperation.getResultCode() + ": " + addOperation.getErrorMessage());
        }
    }

    public Entry addEntry(String ldif) throws IOException, LDIFException {
        LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(ldif, StandardCharsets.UTF_8));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry ldifEntry = ldifReader.readEntry();
        addEntry(ldifEntry);
        return ldifEntry;
    }

    public ChangeRecordEntry executeRenameChange(File file) throws LDIFException, IOException {
        return executeRenameChange(file.getPath());
    }

    public ChangeRecordEntry executeRenameChange(String filename) throws LDIFException, IOException {
        LDIFImportConfig importConfig = new LDIFImportConfig(filename);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);

        if (!(entry instanceof ModifyDNChangeRecordEntry)) {
            throw new LDIFException(new MessageBuilder("Could not execute rename..Bad change").toMessage());
        }

        ModifyDNOperation modifyOperation = getInternalConnection().processModifyDN((ModifyDNChangeRecordEntry) entry);

        if (ResultCode.SUCCESS != modifyOperation.getResultCode()) {
            throw new RuntimeException("LDAP operation error: " + modifyOperation.getResultCode() + ": " + modifyOperation.getErrorMessage());
        }
        return entry;

    }

    public ChangeRecordEntry executeLdifChange(File file) throws IOException, LDIFException {
        LDIFImportConfig importConfig = new LDIFImportConfig(file.getPath());
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);

        return executeChangeInternal(entry);
    }

    @NotNull
    private ChangeRecordEntry executeChangeInternal(ChangeRecordEntry entry) {
        ModifyOperation modifyOperation = getInternalConnection().processModify((ModifyChangeRecordEntry) entry);

        if (ResultCode.SUCCESS != modifyOperation.getResultCode()) {
            throw new RuntimeException("LDAP operation error: " + modifyOperation.getResultCode() + ": " + modifyOperation.getErrorMessage());
        }
        return entry;
    }

    public void executeLdifChanges(File file) throws IOException, LDIFException {
        LDIFImportConfig importConfig = new LDIFImportConfig(file.getPath());
        LDIFReader ldifReader = new LDIFReader(importConfig);
        for (; ; ) {
            ChangeRecordEntry entry = ldifReader.readChangeRecord(false);
            if (entry == null) {
                break;
            }
            executeChangeInternal(entry);
        }
    }

    public ChangeRecordEntry executeLdifChange(String ldif) throws IOException, LDIFException {
        InputStream ldifInputStream = IOUtils.toInputStream(ldif, StandardCharsets.UTF_8);
        LDIFImportConfig importConfig = new LDIFImportConfig(ldifInputStream);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);
        return executeChangeInternal(entry);
    }

    public ChangeRecordEntry modifyReplace(String entryDn, String attributeName, String value) throws IOException, LDIFException {
        String ldif = "dn: " + entryDn + "\nchangetype: modify\nreplace: " + attributeName + "\n" + attributeName + ": " + value;
        return executeLdifChange(ldif);
    }

    public ChangeRecordEntry modifyAdd(String entryDn, String attributeName, String value) throws IOException, LDIFException {
        String ldif = "dn: " + entryDn + "\nchangetype: modify\nadd: " + attributeName + "\n" + attributeName + ": " + value;
        return executeLdifChange(ldif);
    }

    public ChangeRecordEntry modifyDelete(String entryDn, String attributeName, String value) throws IOException, LDIFException {
        String ldif = "dn: " + entryDn + "\nchangetype: modify\ndelete: " + attributeName + "\n" + attributeName + ": " + value;
        return executeLdifChange(ldif);
    }

    public void delete(String entryDn) {
        DeleteOperation deleteOperation = getInternalConnection().processDelete(entryDn);
        if (ResultCode.SUCCESS != deleteOperation.getResultCode()) {
            throw new RuntimeException("LDAP operation error: " + deleteOperation.getResultCode() + ": " + deleteOperation.getErrorMessage());
        }
    }

    public String dumpEntries() throws DirectoryException {
        InternalSearchOperation op = getInternalConnection().processSearch(ldapSuffix, SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
                100, false, "(objectclass=*)", getSearchAttributes());

        StringBuilder sb = new StringBuilder();
        for (SearchResultEntry searchEntry : op.getSearchEntries()) {
            sb.append(toHumanReadableLdifoid(searchEntry));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String dumpTree() throws DirectoryException {
        StringBuilder sb = new StringBuilder();
        sb.append(ldapSuffix).append("\n");
        dumpTreeLevel(sb, ldapSuffix, 1);
        return sb.toString();
    }

    private void dumpTreeLevel(StringBuilder sb, String dn, int indent) throws DirectoryException {
        InternalSearchOperation op = getInternalConnection().processSearch(
                dn, SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
                100, false, "(objectclass=*)", getSearchAttributes());

        for (SearchResultEntry searchEntry : op.getSearchEntries()) {
            ident(sb, indent);
            sb.append(searchEntry.getDN().getRDN());
            sb.append("\n");
            dumpTreeLevel(sb, searchEntry.getDN().toString(), indent + 1);
        }
    }

    private void ident(StringBuilder sb, int indent) {
        sb.append("  ".repeat(indent));
    }

    public String toHumanReadableLdifoid(Entry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("dn: ").append(entry.getDN()).append("\n");
        for (Attribute attribute : entry.getAttributes()) {
            for (AttributeValue val : attribute) {
                sb.append(attribute.getName());
                sb.append(": ");
                sb.append(val);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public Collection<String> getGroupUniqueMembers(String groupDn) throws DirectoryException {
        Entry groupEntry = fetchEntryRequired(groupDn);
        return getAttributeValues(groupEntry, "uniqueMember");
    }

    public @NotNull Collection<String> getGroupMembers(String groupDn) throws DirectoryException {
        Entry groupEntry = fetchEntryRequired(groupDn);
        return emptyIfNull(getAttributeValues(groupEntry, "member"));
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
            AssertJUnit.fail("Expected that entry " + entryDn + " will have password '" + password + "'. But the check failed.");
        }
    }

    public void assertHasObjectClass(Entry entry, String expectedObjectclass) {
        Collection<String> objectclasses = getAttributeValues(entry, "objectClass");
        if (!objectclasses.contains(expectedObjectclass)) {
            AssertJUnit.fail("Expected that entry " + entry.getDN() + " will have object class '" + expectedObjectclass
                    + "'. But it has object classes: " + objectclasses);
        }
    }

    public void assertHasNoObjectClass(Entry entry, String expectedObjectclass) {
        Collection<String> objectclasses = getAttributeValues(entry, "objectClass");
        if (objectclasses.contains(expectedObjectclass)) {
            AssertJUnit.fail("Expected that entry " + entry.getDN() + " will NOT have object class '" + expectedObjectclass
                    + "'. But it was there: " + objectclasses);
        }
    }

    public void scanAccessLog(Consumer<String> lineConsumer) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(accessLogFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineConsumer.accept(line);
            }
        }
    }
}
