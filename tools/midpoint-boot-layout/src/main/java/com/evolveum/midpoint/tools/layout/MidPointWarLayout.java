/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.layout;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

import org.springframework.boot.loader.tools.*;

public class MidPointWarLayout extends Layouts.War implements CustomLoaderLayout {

    @Override
    public void writeLoadedClasses(LoaderClassesWriter writer) throws IOException {
        writer.writeLoaderClasses(); // this writes

        // This writes this JAR (layout) to the root of the Spring Boot archive.
        // TODO: Is it possible to write *Launcher classes without layout and factory?
        JarFile self = createSelf();

        Accessor.writeEntries(writer, self);
    }

    @Override
    public String getLauncherClassName() {
        return MidPointWarLauncher.class.getName();
    }

    private JarFile createSelf() throws IOException {
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
            throw new IOException("Could not find self jar", e);
        }
    }
}
