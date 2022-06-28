/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.tools.CustomLoaderLayout;
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LoaderClassesWriter;

/**
 * Common custom midPoint JAR layout functionality.
 */
public interface MidPointLayoutCommon extends CustomLoaderLayout, Layout {

    @Override
    default void writeLoadedClasses(LoaderClassesWriter writer) throws IOException {
        String zipEntryName = getLauncherClassName().replace('.', '/') + ".class";
        writer.writeLoaderClasses(); // this writes the default classes
        try (JarFile self = customLayoutJar();
                InputStream classInputStream = self.getInputStream(new ZipEntry(zipEntryName))) {
            writer.writeEntry(zipEntryName, classInputStream);
        }
    }

    /**
     * Creates JarFile instance for midpoint-boot-layout JAR for launcher classes extraction.
     */
    private JarFile customLayoutJar() throws IOException {
        try {
            ProtectionDomain protectionDomain = getClass().getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
            String path = (location == null ? null : location.getSchemeSpecificPart());

            if (path == null) {
                throw new IllegalStateException("Unable to determine code source archive");
            }

            File root = new File(path);
            if (!root.exists()) {
                throw new IllegalStateException("Unable to determine code source archive from " + root);
            }

            return new JarFile(root);
        } catch (Exception e) {
            throw new IOException("Could not find self JAR", e);
        }
    }
}
