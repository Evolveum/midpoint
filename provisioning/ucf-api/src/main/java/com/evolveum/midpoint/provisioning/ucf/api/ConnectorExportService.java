package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface ConnectorExportService {

    /**
     * Packs an exploded connector bundle directory into a connector bundle jar.
     *
     * The directory is identified by its name inside the connector scan directory, the same way
     * as in {@link ConnectorInstallationService#editableConnectorFor(String)}. The jar is written
     * to a temporary file first and then atomically renamed to {@code targetFile}.
     *
     * The {@code propertyOverrides} map keys are properties file names relative to the bundle root,
     * values are properties to be set in that file. Overrides are applied inside the created jar
     * only; the source directory is left untouched.
     */
    File packBundle(String directory, File targetFile, Map<String, Map<String, String>> propertyOverrides)
            throws IOException;
}
