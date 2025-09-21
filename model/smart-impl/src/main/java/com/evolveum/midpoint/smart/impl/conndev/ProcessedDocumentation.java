/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedDocumentationType;

import java.io.*;

public class ProcessedDocumentation {

    private static final File DIRECTORY = new File("docs-tmp");

    private final String uri;
    private final String uuid;
    private final File storage;
    private String mimeType;

    ProcessedDocumentation(ProcessedDocumentationType base) {
        this(base.getUuid(), base.getUri());
        mimeType = base.getContentType();
    }

    ProcessedDocumentation(String uuid, String uri) {
        this.uuid = uuid;
        this.uri = uri;
        DIRECTORY.mkdirs();
        storage = new File(DIRECTORY, uuid);
    }

    public InputStream asInputStream() throws FileNotFoundException {
        return new FileInputStream(storage);
    }

    public FileOutputStream asOutputStream() throws FileNotFoundException {
        return new FileOutputStream(storage);
    }

    public ProcessedDocumentationType toBean() {
        return new ProcessedDocumentationType()
                .uri(uri)
                .uuid(uuid)
                .contentType(contentType());
    }

    public void write(String value) throws IOException {
        try (DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(asOutputStream()))) {
            outStream.writeUTF(value);
        }
    }

    String contentType() {
        if (mimeType == null) {
            if (uri.endsWith(".json")) {
                mimeType = "application/json";
            } else if (uri.endsWith(".yml") || uri.endsWith(".yaml")) {
                mimeType = "application/yaml";
            } else {
                mimeType = "text/html";
            }
        }
        return mimeType;
    }
}
