package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstallationService;
import com.evolveum.midpoint.provisioning.ucf.api.DownloadedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.EditableConnector;
import com.evolveum.midpoint.schema.result.OperationResult;

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
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ConnectorInstallationServiceImpl implements ConnectorInstallationService {

    private static final String TMP_SUFFIX = ".tmp";

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
        if (!dirs.isEmpty()) {
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
    public EditableConnector editableConnectorFor(@NotNull ConnectorType objectable) {
        var connectorKey = ConnectorFactoryConnIdImpl.getConnectorKey(objectable);
        var uri =factoryImpl.getLocalConnectorInfoManager().findConnectorUri(connectorKey);
        if (uri != null && uri.getScheme().equals("file")) {
            return new DownloadedDirectoryConnector(new File(uri.getPath()));
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
                connectorFile.renameTo(targetFile);
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
            return ConnectorFactoryConnIdImpl.isThisJarFileBundle(connectorFile);
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
                manifest.write(new FileOutputStream(manifestFile));

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
        public String readFile(String filename) throws IOException {
            var file = newFile(connectorFile, filename);
            return Files.readString(file.toPath());
        }

        @Override
        public void updateProperty(String filename, String key, String value) throws IOException {
            var file = newFile(connectorFile, filename);
            var props = new Properties();
            props.load(new FileInputStream(file));
            props.setProperty(key, value);
            try (var stream = new FileOutputStream(file)) {
                props.store(stream, null);
            }
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
