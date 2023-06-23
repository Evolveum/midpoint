package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class TestUtils {

    public static void zipFile(File input, File output) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
            zipFile(input, input.toPath(), zos);
        }
    }

    private static void zipFile(File input, Path rootPath, ZipOutputStream zos) throws IOException {
        if (input.isFile()) {
            Path filePath = input.toPath();
            String relativePath = rootPath.relativize(filePath).toString();
            if (StringUtils.isEmpty(relativePath)) {
                relativePath = input.getName();
            }

            ZipEntry ze = new ZipEntry(relativePath);
            zos.putNextEntry(ze);

            try (FileInputStream fis = new FileInputStream(input)) {
                IOUtils.copy(fis, zos);
            }
            zos.closeEntry();
        } else {
            File[] fileList = input.listFiles();
            if (fileList == null) {
                return;
            }

            for (File file : fileList) {
                zipFile(file, rootPath, zos);
            }
        }
    }
}
