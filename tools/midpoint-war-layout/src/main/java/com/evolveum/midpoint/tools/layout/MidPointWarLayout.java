/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.layout;

import org.springframework.boot.loader.tools.CustomLoaderLayout;
import org.springframework.boot.loader.tools.JarWriter;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.LoaderClassesWriter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointWarLayout extends Layouts.War implements CustomLoaderLayout {

    private String name;

    public MidPointWarLayout(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void writeLoadedClasses(LoaderClassesWriter writer) throws IOException {
        JarWriter jarWriter = (JarWriter) writer;
        writer.writeLoaderClasses();

        JarFile self = createSelf();
        jarWriter.writeEntries(self);
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
