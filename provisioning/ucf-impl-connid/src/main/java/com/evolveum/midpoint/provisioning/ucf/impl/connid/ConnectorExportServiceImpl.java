package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorExportService;

import jakarta.annotation.PostConstruct;
import org.apache.commons.configuration2.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class ConnectorExportServiceImpl implements ConnectorExportService {

    private static final String TMP_SUFFIX = ".tmp";
    private static final String PREFERRED_DIRECTORY = "connid-connectors";
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    @Autowired private MidpointConfiguration configuration;

    private File bundleDirectory;

    @PostConstruct
    public void init() {
        Configuration config = configuration.getConfiguration(MidpointConfiguration.ICF_CONFIGURATION);
        List<Object> dirs = config.getList("scanDirectory");

        bundleDirectory = dirs.stream().filter(d -> d.toString().contains(PREFERRED_DIRECTORY))
                .findFirst()
                .map(Object::toString)
                .map(File::new)
                .orElse(null);

        if (bundleDirectory == null && !dirs.isEmpty()) {
            bundleDirectory = new File(dirs.iterator().next().toString());
        }
    }

    @Override
    public File packBundle(String directory, File targetFile, Map<String, Map<String, String>> propertyOverrides)
            throws IOException {
        if (directory.contains("/") || directory.contains("\\")) {
            throw new IllegalArgumentException("Invalid directory name: " + directory);
        }
        var bundleDir = new File(bundleDirectory, directory);
        if (!bundleDir.isDirectory()) {
            throw new IllegalStateException("Directory " + directory + " does not exist");
        }
        var manifestFile = new File(bundleDir, MANIFEST_PATH);
        if (!manifestFile.isFile()) {
            throw new IllegalStateException(
                    "Directory " + directory + " is not a connector bundle, " + MANIFEST_PATH + " is missing");
        }

        var tmpFile = new File(targetFile.getPath() + TMP_SUFFIX);
        try {
            Manifest manifest;
            try (var manifestStream = new FileInputStream(manifestFile)) {
                manifest = new Manifest(manifestStream);
            }
            try (var jar = new JarOutputStream(new FileOutputStream(tmpFile), manifest)) {
                var remainingOverrides = new HashMap<>(propertyOverrides);
                writeFileEntries(jar, bundleDir, remainingOverrides);
                writeMissingOverriddenEntries(jar, remainingOverrides);
            }
            Files.move(tmpFile.toPath(), targetFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            tmpFile.delete();
            throw e;
        }
        return targetFile;
    }

    private void writeFileEntries(JarOutputStream jar, File bundleDir, Map<String, Map<String, String>> remainingOverrides)
            throws IOException {
        Path root = bundleDir.toPath();
        try (Stream<Path> files = Files.walk(root)) {
            var regularFiles = files.filter(Files::isRegularFile).sorted().toList();
            for (Path file : regularFiles) {
                String entryName = root.relativize(file).toString().replace(File.separatorChar, '/');
                if (MANIFEST_PATH.equals(entryName) || file.getFileName().toString().endsWith(TMP_SUFFIX)) {
                    continue;
                }
                jar.putNextEntry(new JarEntry(entryName));
                var overrides = remainingOverrides.remove(entryName);
                if (overrides != null) {
                    jar.write(overriddenProperties(file.toFile(), overrides));
                } else {
                    Files.copy(file, jar);
                }
                jar.closeEntry();
            }
        }
    }

    private void writeMissingOverriddenEntries(JarOutputStream jar, Map<String, Map<String, String>> remainingOverrides)
            throws IOException {
        for (var override : remainingOverrides.entrySet()) {
            jar.putNextEntry(new JarEntry(override.getKey()));
            jar.write(overriddenProperties(null, override.getValue()));
            jar.closeEntry();
        }
    }

    private byte[] overriddenProperties(File file, Map<String, String> overrides) throws IOException {
        var props = new Properties();
        if (file != null) {
            try (var stream = new FileInputStream(file)) {
                props.load(stream);
            }
        }
        overrides.forEach(props::setProperty);
        var out = new ByteArrayOutputStream();
        props.store(out, null);
        return out.toByteArray();
    }
}
