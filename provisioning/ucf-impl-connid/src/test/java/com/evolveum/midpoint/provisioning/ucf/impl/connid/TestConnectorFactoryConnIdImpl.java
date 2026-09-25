package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestConnectorFactoryConnIdImpl  {



    private File tempRootDir;
    private File rootJarFile;
    private File subDirJarFile;



    @BeforeClass
    public void setUp() throws IOException {
        tempRootDir = File.createTempFile("midpoint-connectors-test-", "");
        assertTrue(tempRootDir.delete());
        assertTrue(tempRootDir.mkdir());


        rootJarFile = new File(tempRootDir, "connector-root.jar");
        copyTestConnectorResource(rootJarFile);


        File subDir = new File(tempRootDir, "scripted");
        assertTrue(subDir.mkdir());
        subDirJarFile = new File(subDir, "connector-subdir.jar");
        copyTestConnectorResource(subDirJarFile);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (tempRootDir != null && tempRootDir.exists()) {
            deleteRecursively(tempRootDir);
        }
    }

    private void copyTestConnectorResource(File targetFile) throws IOException {
        String resourceName = "dummy-connector-fake-4.2.jar";

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new FileNotFoundException("Test connector " + resourceName + " was not found on the classpath.");
            }
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    public void testScanDirectoryNowRecursive() {
        ConnectorFactoryConnIdImpl factory = new ConnectorFactoryConnIdImpl();
        Set<URI> bundleUris = new HashSet<>();


        factory.scanDirectoryNow(bundleUris, tempRootDir);


        assertEquals(bundleUris.size(), 2, "Expected exactly 2 JAR connectors.");
        assertTrue(bundleUris.contains(rootJarFile.toURI()), "JAR in root was not found");
        assertTrue(bundleUris.contains(subDirJarFile.toURI()), "JAR in subdirectory was not  found during recursive scan");
        System.out.println("Nájdené URI:");
        bundleUris.forEach(System.out::println);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

}
