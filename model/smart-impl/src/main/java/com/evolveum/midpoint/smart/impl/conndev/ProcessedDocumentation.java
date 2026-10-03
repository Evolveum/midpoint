/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.smart.api.conndev.DocumentationContentTypes;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProcessedDocumentationType;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ProcessedDocumentation {

    public static final String STORAGE_DIR_NAME = "tmp-docs";

    private final File directory;
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
        directory = new File(ConnDevBeans.get().getMidpointHome(), STORAGE_DIR_NAME);
        directory.mkdirs();
        storage = new File(directory, uuid);
    }

    ProcessedDocumentation contentType(String contentType) {
        this.mimeType = contentType;
        return this;
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
            outStream.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Deletes the stored file. Returns false when there is no file to delete.
     */
    public boolean delete() {
        return storage.exists() && storage.delete();
    }

    public String uri() {
        return uri;
    }

    public String uuid() {
        return uuid;
    }

    /**
     * The stored content type, detected from the file name when none was stored. May be {@code null}
     * for a file whose suffix carries no format information; the generation-service upload then
     * lets the service infer the type from the file name itself.
     */
    String contentType() {
        if (mimeType == null) {
            mimeType = DocumentationContentTypes.detect(uri);
        }
        return mimeType;
    }
}
