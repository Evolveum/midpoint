package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;
import com.evolveum.midpoint.provisioning.ucf.api.DownloadedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.EditableConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ReadableConnector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.util.CheckedConsumer;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import jakarta.annotation.PostConstruct;
import org.apache.commons.configuration2.Configuration;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class ConnectorInstallationServiceImpl implements ConnectorInstallationService {

    private static final String TMP_SUFFIX = ".tmp";
    private static final String PREFERRED_DIRECTORY = "connid-connectors";
    //private final WebClient webClient;

    private static final Attributes.Name MANIFEST_CONNECTOR_CLASS = new Attributes.Name("ConnectorBundle-ConnectorClass");
    private static final Attributes.Name MANIFEST_CONNECTOR_BUNDLE = new Attributes.Name("ConnectorBundle-Name");
    private static final Attributes.Name MANIFEST_CONNECTOR_VERSION = new Attributes.Name("ConnectorBundle-Version");

    @Autowired private MidpointConfiguration configuration;
    @Autowired private ConnectorFactoryConnIdImpl factoryImpl;

    private File downloadDirectory;

    public ConnectorInstallationServiceImpl() {
        //this.webClient = WebClient.create();

    }

    @PostConstruct
    public void init() {
        Configuration config = configuration.getConfiguration(MidpointConfiguration.ICF_CONFIGURATION);
        List<Object> dirs = config.getList("scanDirectory");

        downloadDirectory = dirs.stream().filter( d -> d.toString().contains(PREFERRED_DIRECTORY))
                .findFirst()
                .map(Object::toString)
                .map(File::new)
                .orElse(null);

        if (downloadDirectory == null && !dirs.isEmpty()) {
            downloadDirectory = new File(dirs.iterator().next().toString());
        }
    }

    @Override
    public DownloadedConnector downloadConnector(String uri, String targetName, OperationResult result) {

        try {
            URL url = new URL(uri);
            File target = temporaryTargetFile(targetName);
            var readableByteChannel = Channels.newChannel(url.openStream());
            var fileOutputStream = new FileOutputStream(target);
            var fileChannel = fileOutputStream.getChannel();
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return new DownloadedJarConnector(target);
        } catch (MalformedURLException e) {
            throw new SystemException(e);
        } catch (FileNotFoundException e) {
            throw new SystemException(e);
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    @Override
    public DownloadedConnector writeConnector(String targetName, CheckedConsumer<FileOutputStream> writter) throws CommonException {
        File target = temporaryTargetFile(targetName);
        try (var stream = new FileOutputStream(target)) {
            writter.accept(stream);
        } catch (IOException e) {
            throw new SystemException("Can not download connector", e);
        }
        return new DownloadedJarConnector(target);
    }

    @Override
    public ReadableConnector readableConnectorFor(@NotNull ConnectorType objectable) {
        var connectorKey = ConnectorFactoryConnIdImpl.getConnectorKey(objectable);
        var uri = factoryImpl.getLocalConnectorInfoManager().findConnectorUri(connectorKey);
        if (uri != null && uri.getScheme().equals("file")) {
            var bundleFile = new File(uri.getPath());
            if (bundleFile.isDirectory()) {
                return new DownloadedDirectoryConnector(bundleFile);
            }
            if (bundleFile.isFile()) {
                return new JarBundleConnector(bundleFile);
            }
        }
        return null;
    }

    @Override
    public EditableConnector editableConnectorFor(String directory) {
        var connDir = new File(downloadDirectory, directory);
        if (!connDir.exists()) {
            throw new IllegalStateException("Directory " + directory + " does not exist");
        }
        return new DownloadedDirectoryConnector(connDir);
    }

    @Override
    public boolean isConnectorDirectoryPresent(String directory) {
        return new File(downloadDirectory, directory).isDirectory();
    }

    @Override
    public EditableConnector copyBundle(@NotNull ConnectorType sourceConnector, @NotNull String targetDirectoryName,
            OperationResult result) {
        if (targetDirectoryName.contains("/") || targetDirectoryName.contains("\\")) {
            throw new IllegalArgumentException("Invalid target name: " + targetDirectoryName);
        }
        var sourceBundle = bundleFile(sourceConnector);
        var targetDir = new File(downloadDirectory, targetDirectoryName);
        var stagingDir = new File(downloadDirectory, targetDirectoryName + TMP_SUFFIX);

        deleteRecursively(targetDir);
        deleteRecursively(stagingDir);

        try {
            if (sourceBundle.isDirectory()) {
                copyDirectory(sourceBundle, stagingDir);
            } else {
                unpackJar(sourceBundle, stagingDir);
            }
            Files.move(stagingDir.toPath(), targetDir.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new SystemException("Couldn't copy connector bundle from " + sourceBundle + " to " + targetDir, e);
        }
        return new DownloadedDirectoryConnector(targetDir);
    }

    @Override
    public String getConnectorClass(@NotNull ConnectorType connector) {
        var bundleFile = bundleFile(connector);
        Manifest manifest;
        try {
            if (bundleFile.isDirectory()) {
                var manifestFile = new File(bundleFile, "META-INF/MANIFEST.MF");
                if (!manifestFile.exists()) {
                    return null;
                }
                try (var fs = new FileInputStream(manifestFile)) {
                    manifest = new Manifest(fs);
                }
            } else {
                try (var jar = new JarFile(bundleFile)) {
                    var entry = jar.getEntry("META-INF/MANIFEST.MF");
                    if (entry == null) {
                        return null;
                    }
                    try (var is = jar.getInputStream(entry)) {
                        manifest = new Manifest(is);
                    }
                }
            }
        } catch (IOException e) {
            throw new SystemException("Couldn't read bundle manifest of " + ObjectTypeUtil.toShortString(connector), e);
        }
        return manifest.getMainAttributes().getValue(MANIFEST_CONNECTOR_CLASS);
    }

    /**
     * The local bundle of the given connector - its bundle directory, or its bundle JAR file.
     */
    private File bundleFile(@NotNull ConnectorType connector) {
        var connectorKey = ConnectorFactoryConnIdImpl.getConnectorKey(connector);
        var uri = factoryImpl.getLocalConnectorInfoManager().findConnectorUri(connectorKey);
        if (uri == null || !uri.getScheme().equals("file")) {
            throw new SystemException("No local bundle found for " + ObjectTypeUtil.toShortString(connector));
        }
        var bundle = new File(uri.getPath());
        if (!bundle.exists() || (!bundle.isDirectory() && !bundle.isFile())) {
            throw new SystemException("Local connector bundle of " + ObjectTypeUtil.toShortString(connector)
                    + " does not exist: " + bundle);
        }
        return bundle;
    }

    private static void copyDirectory(File sourceDir, File targetDir) throws IOException {
        Files.walk(sourceDir.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    var relative = sourceDir.toPath().relativize(path);
                    var target = targetDir.toPath().resolve(relative);
                    if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(TMP_SUFFIX)) {
                        return; // staging leftovers are not bundle content
                    }
                    try {
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            Files.copy(path, target, REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new SystemException("Couldn't copy " + path + " to " + target, e);
                    }
                });
    }

    private static void unpackJar(File jarFile, File targetDir) throws IOException {
        var targetRoot = targetDir.getCanonicalFile().toPath();
        try (var zip = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                var target = new File(targetDir, entry.getName()).getCanonicalFile();
                // guard against zip-slip
                if (!target.toPath().startsWith(targetRoot)) {
                    throw new IOException("Invalid zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) {
                        throw new IOException("Failed to create directory " + target);
                    }
                } else {
                    var parent = target.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (var out = Files.newOutputStream(target.toPath())) {
                        zip.transferTo(out);
                    }
                }
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        try (var paths = Files.walk(file.toPath())) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new SystemException("Couldn't delete " + path, e);
                }
            });
        } catch (IOException e) {
            throw new SystemException("Couldn't delete " + file, e);
        }
    }

    @Override
    public void reloadLocalConnectorBundle(@NotNull ConnectorType connector) throws ObjectNotFoundException {
        var connectorKey = ConnectorFactoryConnIdImpl.getConnectorKey(connector);
        var uri = factoryImpl.getLocalConnectorInfoManager().findConnectorUri(connectorKey);
        if (uri == null) {
            throw new ObjectNotFoundException(
                    "Local connector bundle not found for " + ObjectTypeUtil.toShortString(connector),
                    ConnectorType.class, connector.getOid());
        }
        factoryImpl.reloadLocalConnector(uri);
    }

    private File temporaryTargetFile(String targetName) {
        // FIXME: sanitize to not alloiw out-of-context write / download
        if (targetName.contains("/") || targetName.contains("\\")) {
            throw new IllegalArgumentException("Invalid target name: " + targetName);
        }
        return new File(downloadDirectory, targetName + TMP_SUFFIX);
    }

    private abstract class AbstractDownloadedConnector implements DownloadedConnector {

        protected final File connectorFile;

        public AbstractDownloadedConnector(File target) {
            this.connectorFile = target;
        }

        @Override
        public List<ConnectorType> install(OperationResult result) {
            if (!connectorFile.exists()) {
                throw new SystemException("File does not exist: " + connectorFile);
            }
            if (!isInstallable()) {
                throw new SystemException("Cannot install connector: " + connectorFile);
            }
            var targetFile = connectorFile;
            if (connectorFile.getName().endsWith(TMP_SUFFIX)) {
                var name = connectorFile.getName().substring(0, connectorFile.getName().length() - TMP_SUFFIX.length());
                targetFile = new File(downloadDirectory, name);
                try {
                    Files.move(
                            connectorFile.toPath(),
                            targetFile.toPath(),
                            ATOMIC_MOVE,
                            REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new SystemException(
                            "Cannot move connector file to final location. From: " + connectorFile + " To: " + targetFile, e);
                }
            }
            var connectors = factoryImpl.addLocalConnector(targetFile.toURI());
            return connectors.stream().map(ConnectorInstallationServiceImpl::toConnectorType).toList();
        }

        abstract boolean isInstallable();

        @Override
        public void remove() {
            connectorFile.delete();
        }
    }

    private static ConnectorType toConnectorType(ConnectorInfo connectorInfo) {
        return new ConnectorType()
                .connectorBundle(connectorInfo.getConnectorKey().getBundleName())
                .connectorType(connectorInfo.getConnectorKey().getConnectorName())
                .connectorVersion(connectorInfo.getConnectorKey().getBundleVersion());
    }

    private class DownloadedJarConnector extends AbstractDownloadedConnector {

        public DownloadedJarConnector(File target) {
            super(target);
        }

        @Override
        boolean isInstallable() {
            return ConnectorFactoryConnIdImpl.isThisJarFileBundle(connectorFile, false);
        }

        public DownloadedConnector unpack(String directory, OperationResult result) {
            var destDir = temporaryTargetFile(directory);
            try {
                destDir.mkdir();
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(new FileInputStream(connectorFile));
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = newFile(destDir, zipEntry.getName());
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        // fix for Windows-created archives
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        // write file content
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.close();
                return new DownloadedDirectoryConnector(destDir);
            } catch (IOException e) {
                throw new SystemException(e);
            }
        }

        @Override
        public EditableConnector asEditable() {
            throw new UnsupportedOperationException("Jar connector is not editable");
        }
    }

    private class DownloadedDirectoryConnector extends AbstractDownloadedConnector implements EditableConnector {

        public DownloadedDirectoryConnector(File target) {
            super(target);
        }

        @Override
        boolean isInstallable() {
            return connectorFile.isDirectory() && new File(connectorFile, "META-INF/MANIFEST.MF").exists();
        }

        @Override
        public DownloadedConnector unpack(String directory, OperationResult result) {
            return null;
        }

        @Override
        public EditableConnector asEditable() {
            return this;
        }

        @Override
        public void renameBundle(String groupId, String artifactId, String version) {
            var manifestFile = new File(connectorFile, "META-INF/MANIFEST.MF");
            var bundleName = groupId + "." + artifactId;
            try (var fs = new FileInputStream(manifestFile)) {
                var manifest = new Manifest(fs);
                var mainAttributes = manifest.getMainAttributes();
                mainAttributes.put(MANIFEST_CONNECTOR_BUNDLE, bundleName);
                mainAttributes.put(MANIFEST_CONNECTOR_VERSION, version);
                try (var out = new FileOutputStream(manifestFile)) {
                    manifest.write(out);
                }
            } catch (IOException e) {
                throw new SystemException(e);
            }
        }

        @Override
        public void saveFile(String filename, String content) throws IOException {
            var file = newFile(connectorFile, filename);
            //if (!file.exists()) {
            //    file.createNewFile();
            //}
            try (var writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }
        }

        @Override
        public void deleteFileIfExists(String filename) throws IOException {
            var file = newFile(connectorFile, filename);
            Files.deleteIfExists(file.toPath());
        }

        @Override
        public String readFile(String filename) throws IOException {
            var file = newFile(connectorFile, filename);
            return Files.readString(file.toPath());
        }

        @Override
        public void updateProperty(String filename, String key, String value) throws IOException {
            var file = newFile(connectorFile, filename);
            var props = new Properties();
            try (var inputStream = new FileInputStream(file)) {
                props.load(inputStream);
            }
            props.setProperty(key, value);
            try (var outputStream = new FileOutputStream(file)) {
                props.store(outputStream, null);
            }
        }

        @Override
        public boolean fileExists(String filename) {
            try {
                return newFile(connectorFile, filename).exists();
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * A read-only view of a local connector bundle installed as a JAR file: the bundle content
     * is read straight from the JAR. The bundle is installed and must not be modified - to work
     * with an editable copy, use {@link #copyBundle(ConnectorType, String, OperationResult)}.
     */
    private static class JarBundleConnector implements ReadableConnector {

        private final File jarFile;

        private JarBundleConnector(File jarFile) {
            this.jarFile = jarFile;
        }

        @Override
        public boolean fileExists(String filename) {
            var entryName = normalizeEntryName(filename);
            if (entryName == null) {
                return false;
            }
            try (var jar = new JarFile(jarFile)) {
                return jar.getEntry(entryName) != null;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public String readFile(String filename) throws IOException {
            var entryName = normalizeEntryName(filename);
            if (entryName == null) {
                throw new NoSuchFileException(filename);
            }
            try (var jar = new JarFile(jarFile)) {
                var entry = jar.getEntry(entryName);
                if (entry == null) {
                    throw new NoSuchFileException(filename);
                }
                try (var is = jar.getInputStream(entry)) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        /**
         * The JAR entry name for the given (relative) file name, or {@code null} for invalid
         * names (blank, absolute or escaping the bundle root).
         */
        private String normalizeEntryName(String filename) {
            if (filename == null || filename.isBlank()) {
                return null;
            }
            var name = filename.startsWith("/") ? filename.substring(1) : filename;
            if (name.isEmpty() || name.contains("..") || name.contains("\\")) {
                return null;
            }
            return name;
        }
    }

    private static File newFile(File destinationDir, String filePath) throws IOException {
        File destFile = new File(destinationDir, filePath);

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("New file path is outside of the target dir: " + filePath);
        }

        return destFile;
    }

}
