/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.evolveum.midpoint.ninja.util.Log;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.action.upgrade.*;

import org.fusesource.jansi.Ansi;

public class DownloadDistributionStep implements UpgradeStep<DownloadDistributionResult> {

    private UpgradeStepsContext context;

    public DownloadDistributionStep(UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public String getIdentifier() {
        return "downloadDistribution";
    }

    @Override
    public String getPresentableName() {
        return "download distribution";
    }

    @Override
    public DownloadDistributionResult execute() throws IOException {
        final Log log = context.getContext().getLog();

        final File tempDirectory = context.getTempDirectory();
        final UpgradeOptions options = context.getOptions();

        File distributionZipFile = options.getDistributionArchive();
        if (distributionZipFile == null || !distributionZipFile.exists()) {
            DistributionManager manager = new DistributionManager(tempDirectory);
            ProgressListener listener = new ConsoleProgressListener();

            distributionZipFile = manager.downloadDistribution(UpgradeConstants.SUPPORTED_VERSION_TARGET, listener);
        } else {
            log.info(Ansi.ansi().fgGreen().a("Distribution zip already downloaded here: " + distributionZipFile.getAbsolutePath()).reset().toString());
        }

        File distributionDirectory = unzipDistribution(distributionZipFile);

        return new DownloadDistributionResult(distributionZipFile, distributionDirectory);
    }

    private File unzipDistribution(File distributionZip) throws IOException {
        final File tempDirectory = context.getOptions().getTempDirectory();

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
