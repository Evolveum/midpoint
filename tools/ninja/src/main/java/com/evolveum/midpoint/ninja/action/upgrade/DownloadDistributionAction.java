package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.Log;

public class DownloadDistributionAction extends Action<DownloadDistributionOptions, DownloadDistributionResult> {

    @Override
    public DownloadDistributionResult execute() throws Exception {
        final Log log = context.getLog();

        final File tempDirectory = options.getTempDirectory();

        File distributionZipFile = options.getDistributionArchive();
        if (distributionZipFile == null || !distributionZipFile.exists()) {
            DistributionManager manager = new DistributionManager(tempDirectory);
            ProgressListener listener = new ConsoleProgressListener(context.out);

            distributionZipFile = manager.downloadDistribution(UpgradeConstants.SUPPORTED_VERSION_TARGET, listener);
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

        String name = distributionZip.getName();
        File distribution = new File(tempDirectory, StringUtils.left(name, name.length() - 4));

        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(distributionZip))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(distribution, zipEntry);
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

        File[] files = distribution.listFiles();
        if (files != null && files.length == 1) {
            File zipRootDirectory = files[0];
            for (File file : zipRootDirectory.listFiles()) {
                FileUtils.moveToDirectory(file, distribution, false);
            }
            zipRootDirectory.delete();
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
