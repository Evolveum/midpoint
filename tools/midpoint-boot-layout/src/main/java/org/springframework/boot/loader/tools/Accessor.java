
package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.util.jar.JarFile;

/**
 * Nasty hack to access package visible methods, which were public in Spring Boot 2.5
 * but package protected in Spring Boot 2.7
 */
public class Accessor {

    public static void writeEntries(LoaderClassesWriter writer, JarFile self) throws IOException {
        ((JarWriter) writer).writeEntries(self, AbstractJarWriter.EntryTransformer.NONE, AbstractJarWriter.UnpackHandler.NEVER, (entry) -> null);
    }
}
