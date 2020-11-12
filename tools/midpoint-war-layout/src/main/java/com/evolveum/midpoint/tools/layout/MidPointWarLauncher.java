/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.layout;

import java.io.File;
import java.util.List;

import org.springframework.boot.loader.WarLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

/**
 * Created by Viliam Repan (lazyman).
 * <p>
 * Supports JAR loading out of executable JAR, this is supported in newer Spring Boot via
 * loader.path (or Loader-Path in Manifest or environment variable):
 * https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-executable-jar-format.html#executable-jar-property-launcher-features
 * <p>
 * It's unsure whether we can tie the path to dynamic midpoint.home,
 * but perhaps midpoint.sh can take care of that.
 *
 * TODO: This is to be deprecated, but is accidentally still used in service.bat (Windows Service support).
 *  MP service should be launched via PropertyLauncher (see midpoint.sh or BAT).
 */
@Deprecated
public class MidPointWarLauncher extends WarLauncher {

    private static volatile MidPointWarLauncher warLauncher = null;
    private static volatile ClassLoader classLoader = null;

    public MidPointWarLauncher() {
    }

    public MidPointWarLauncher(Archive archive) {
        super(archive);
    }

    public static void main(String[] args) throws Exception {
        String mode = args != null && args.length > 0 ? args[0] : null;

        if ("start".equals(mode)) {
            MidPointWarLauncher.start(args);
        } else if ("stop".equals(mode)) {
            MidPointWarLauncher.stop(args);
        } else {
            new MidPointWarLauncher().launch(args);
        }
    }

    // start/stop was added to support Windows Service using https://commons.apache.org/proper/commons-daemon/procrun.html
    public static synchronized void start(String[] args) throws Exception {
        warLauncher = new MidPointWarLauncher();

        try {
            JarFile.registerUrlProtocolHandler();
            classLoader = warLauncher.createClassLoader(warLauncher.getClassPathArchives());
            warLauncher.launch(args, warLauncher.getMainClass(), classLoader, true);
        } catch (Exception ex) {
            throw new Exception(
                    "Could not start MidPoint application" + ";" + ex.getLocalizedMessage(), ex);
        }
    }

    // why stop calls warLauncher.launch is a mystery, Spring Boot should just as well handle simple System.exit()
    public static synchronized void stop(String[] args) throws Exception {
        try {
            if (warLauncher != null) {
                warLauncher.launch(args, warLauncher.getMainClass(), classLoader, true);
                warLauncher = null;
                classLoader = null;
            }
        } catch (Exception ex) {
            throw new Exception(
                    "Could not stop MidPoint application" + ";" + ex.getLocalizedMessage(), ex);
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void launch(String[] args, String mainClass, ClassLoader classLoader, boolean wait) throws Exception {
        Thread.currentThread().setContextClassLoader(classLoader);

        Thread runnerThread = new Thread(() -> {
            try {
                createMainMethodRunner(mainClass, args, classLoader).run();
            } catch (Exception ex) {
                // ignored
            }
        });
        runnerThread.setContextClassLoader(classLoader);
        runnerThread.setName(Thread.currentThread().getName());
        runnerThread.start();
        if (wait) {
            runnerThread.join();
        }
    }

    @Override
    protected List<Archive> getClassPathArchives() throws Exception {
        List<Archive> archives = super.getClassPathArchives();

        File midPointHomeLib = getMidPointHomeLib();
        if (!midPointHomeLib.exists() || !midPointHomeLib.isDirectory()) {
            return archives;
        }

        File[] files = midPointHomeLib.listFiles(file -> file.getName().toLowerCase().endsWith(".jar"));
        if (files == null) {
            return archives;
        }

        for (File file : files) {
            archives.add(new JarFileArchive(file));
        }

        return archives;
    }

    private File getMidPointHomeLib() {
        String midPointHome = System.getProperty("midpoint.home");
        return new File(midPointHome, "lib");
    }
}
