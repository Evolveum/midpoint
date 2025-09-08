package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.IOException;

public interface EditableConnector {

    void renameBundle(String groupId, String artifactId, String version);

    void saveFile(String filename, String content) throws IOException;

}
