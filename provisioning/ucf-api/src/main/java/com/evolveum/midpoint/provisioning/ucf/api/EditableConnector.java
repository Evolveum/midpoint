package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.IOException;

/**
 * A fully editable view of a local connector bundle directory. Writing is only possible for
 * directory bundles - a JAR bundle can only be viewed through its {@link ReadableConnector}
 * counterpart (use {@link ConnectorInstallationService#copyBundle} to obtain an editable
 * directory copy of it).
 */
public interface EditableConnector extends DownloadedConnector, ReadableConnector {

    void renameBundle(String groupId, String artifactId, String version);

    void saveFile(String filename, String content) throws IOException;

    void deleteFileIfExists(String filename) throws IOException;

    void updateProperty(String filename, String key, String value) throws IOException;
}
