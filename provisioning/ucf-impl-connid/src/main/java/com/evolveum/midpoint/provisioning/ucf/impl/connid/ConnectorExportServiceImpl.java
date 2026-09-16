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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    public File packAsJar(String directory, File targetFile, Map<String, Map<String, String>> propertyOverrides)
            throws IOException {
        var bundleDir = validateBundleDir(directory);
        var manifestFile = new File(bundleDir, MANIFEST_PATH);
        return writeAtomically(targetFile, tmpFile -> {
            Manifest manifest;
            try (var manifestStream = new FileInputStream(manifestFile)) {
                manifest = new Manifest(manifestStream);
            }
            // The bundle's own manifest becomes the jar's official one, so it must not also be
            // written again as a regular entry by writeFileEntries() below.
            try (var jar = new JarOutputStream(new FileOutputStream(tmpFile), manifest)) {
                writeEntries(jar, bundleDir, propertyOverrides, entryName -> !MANIFEST_PATH.equals(entryName));
            }
        });
    }

    @Override
    public File packAsZip(String directory, File targetFile, Map<String, Map<String, String>> propertyOverrides, Collection<String> excludedPathPrefixes)
            throws IOException {
        var bundleDir = validateBundleDir(directory);
        return writeAtomically(targetFile, tmpFile -> {
            try (var zip = new ZipOutputStream(new FileOutputStream(tmpFile))) {
                writeEntries(zip, bundleDir, propertyOverrides,
                        entryName -> excludedPathPrefixes.stream().noneMatch(entryName::startsWith));
            }
        });
    }

    /** Writes to a {@code .tmp} sibling of {@code targetFile} first, then atomically renames it into place. */
    private File writeAtomically(File targetFile, TmpFileWriter writer) throws IOException {
        var tmpFile = new File(targetFile.getPath() + TMP_SUFFIX);
        try {
            writer.writeTo(tmpFile);
            Files.move(tmpFile.toPath(), targetFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            tmpFile.delete();
            throw e;
        }
        return targetFile;
    }

    @FunctionalInterface
    private interface TmpFileWriter {
        void writeTo(File tmpFile) throws IOException;
    }

    private File validateBundleDir(String directory) {
        if (directory.contains("/") || directory.contains("\\")) {
            throw new IllegalArgumentException("Invalid directory name: " + directory);
        }
        var bundleDir = new File(bundleDirectory, directory);
        if (!bundleDir.isDirectory()) {
            throw new IllegalStateException("Directory " + directory + " does not exist");
        }
        if (!new File(bundleDir, MANIFEST_PATH).isFile()) {
            throw new IllegalStateException(
                    "Directory " + directory + " is not a connector bundle, " + MANIFEST_PATH + " is missing");
        }
        return bundleDir;
    }

    private void writeEntries(
            ZipOutputStream archive, File bundleDir, Map<String, Map<String, String>> propertyOverrides, Predicate<String> entryFilter)
            throws IOException {
        var remainingOverrides = new HashMap<>(propertyOverrides);
        writeFileEntries(archive, bundleDir, remainingOverrides, entryFilter);
        writeMissingOverriddenEntries(archive, remainingOverrides);
    }

    private void writeFileEntries(
            ZipOutputStream archive, File bundleDir, Map<String, Map<String, String>> remainingOverrides, Predicate<String> entryFilter)
            throws IOException {
        Path root = bundleDir.toPath();
        try (Stream<Path> files = Files.walk(root)) {
            var regularFiles = files.filter(Files::isRegularFile).sorted().toList();
            for (Path file : regularFiles) {
                String entryName = root.relativize(file).toString().replace(File.separatorChar, '/');
                if (file.getFileName().toString().endsWith(TMP_SUFFIX) || !entryFilter.test(entryName)) {
                    continue;
                }
                archive.putNextEntry(new ZipEntry(entryName));
                var overrides = remainingOverrides.remove(entryName);
                if (overrides != null) {
                    archive.write(overriddenProperties(file.toFile(), overrides));
                } else {
                    Files.copy(file, archive);
                }
                archive.closeEntry();
            }
        }
    }

    private void writeMissingOverriddenEntries(ZipOutputStream archive, Map<String, Map<String, String>> remainingOverrides)
            throws IOException {
        for (var override : remainingOverrides.entrySet()) {
            archive.putNextEntry(new ZipEntry(override.getKey()));
            archive.write(overriddenProperties(null, override.getValue()));
            archive.closeEntry();
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
