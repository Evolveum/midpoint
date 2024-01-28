package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;

import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.upgrade.ConsoleProgressListener;
import com.evolveum.midpoint.ninja.action.upgrade.DistributionManager;
import com.evolveum.midpoint.ninja.action.upgrade.ProgressListener;
import com.evolveum.midpoint.ninja.impl.Log;

import org.jetbrains.annotations.NotNull;

public class DownloadDistributionAction extends Action<DownloadDistributionOptions, DownloadDistributionResult> {

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        return NinjaApplicationContextLevel.NONE;
    }

    @Override
    public String getOperationName() {
        return "download distribution";
    }

    @Override
    public DownloadDistributionResult execute() throws Exception {
        final Log log = context.getLog();

        final File tempDirectory = options.getTempDirectory();

        File distributionZipFile = options.getDistributionArchive();
        if (distributionZipFile == null || !distributionZipFile.exists()) {
            String version = options.getDistributionVersion();
            if (version == null) {
                throw new IllegalStateException("No version to upgrade to.");
            }
            log.info("Downloading version: {}", version);

            DistributionManager manager = new DistributionManager(tempDirectory);
            ProgressListener listener = new ConsoleProgressListener(log);
            distributionZipFile = manager.downloadDistribution(version, listener);
        } else {
            log.info("Distribution zip already downloaded.");
        }

        File distributionDirectory = unzipDistribution(distributionZipFile);

        log.info(Ansi.ansi().a("Distribution zip: ").a(distributionZipFile.getAbsolutePath()).reset().toString());
        log.info(Ansi.ansi().a("Distribution directory: ").a(distributionDirectory.getAbsolutePath()).reset().toString());

        return new DownloadDistributionResult(distributionZipFile, distributionDirectory);
    }

    private File unzipDistribution(File distributionZip) throws IOException {
        final File tempDirectory = options.getTempDirectory();

        File distribution;

        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(distributionZip))) {
            ZipEntry zipEntry = zis.getNextEntry();

            distribution = newFile(tempDirectory, zipEntry);
            if (distribution.exists() && distribution.isDirectory()) {
                FileUtils.deleteDirectory(distribution);
            }

            while (zipEntry != null) {
                File newFile = newFile(tempDirectory, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
        }

        if (options.getDistributionDirectory() != null) {
            File distributionDirectory = options.getDistributionDirectory();

            return distribution.renameTo(distributionDirectory) ? distributionDirectory : distribution;
        }

        return distribution;
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
