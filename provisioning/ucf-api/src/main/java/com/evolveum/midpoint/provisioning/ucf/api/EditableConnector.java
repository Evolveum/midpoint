package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.IOException;

public interface EditableConnector extends DownloadedConnector {

    void renameBundle(String groupId, String artifactId, String version);

    void saveFile(String filename, String content) throws IOException;

    String readFile(String filename) throws IOException;

    void updateProperty(String filename, String key, String value) throws IOException;
}
