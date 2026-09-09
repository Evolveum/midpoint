package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface ConnectorExportService {

    /**
     * Packs an exploded connector bundle directory into a full, deployable connector jar (uses the
     * bundle's own manifest as the jar's official one).
     *
     * The directory is identified by its name inside the connector scan directory, the same way
     * as in {@link ConnectorInstallationService#editableConnectorFor(String)}. The jar is written
     * to a temporary file first and then atomically renamed to {@code targetFile}.
     *
     * The {@code propertyOverrides} map keys are properties file names relative to the bundle root,
     * values are properties to be set in that file. Overrides are applied inside the created jar
     * only; the source directory is left untouched.
     */
    File packAsJar(String directory, File targetFile, Map<String, Map<String, String>> propertyOverrides)
            throws IOException;

    /**
     * Packs an exploded connector bundle directory into a plain zip - not meant to be a deployable
     * connector, unlike {@link #packAsJar}. Same directory resolution, temp-file-then-atomic-rename,
     * and property override semantics as {@link #packAsJar}.
     *
     * {@code excludedPathPrefixes} skips any bundle-relative entry ({@code '/'}-separated) that
     * starts with one of these prefixes (e.g. {@code "lib/"}) - this service has no opinion on what
     * a connector bundle's directories mean, so callers decide what to leave out.
     */
    File packAsZip(String directory, File targetFile, Map<String, Map<String, String>> propertyOverrides, Collection<String> excludedPathPrefixes)
            throws IOException;
}
