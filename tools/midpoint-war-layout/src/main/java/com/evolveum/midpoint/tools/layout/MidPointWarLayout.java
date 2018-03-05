/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
