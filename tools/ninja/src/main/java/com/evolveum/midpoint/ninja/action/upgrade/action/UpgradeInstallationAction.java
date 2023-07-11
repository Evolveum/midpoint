package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class UpgradeInstallationAction extends Action<UpgradeInstallationOptions, Void> {

    private static final boolean IS_POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    @Override
    public String getOperationName() {
        return "upgrade installation";
    }

    @Override
    public Void execute() throws Exception {
        final File distributionDirectory = options.getDistributionDirectory();

        final boolean backupFiles = options.isBackup();

        File midpointInstallation = NinjaUtils.computeInstallationDirectory(options.getInstallationDirectory(), context);
        if (midpointInstallation == null) {
            throw new NinjaException("Undefined midpoint installation directory");
        }

        File backupDirectory = null;
        if (backupFiles) {
            backupDirectory = new File(midpointInstallation, ".backup-" + System.currentTimeMillis());
            backupDirectory.mkdir();

            log.info("Backing up installation directory to: {}", backupDirectory.getAbsolutePath());
        }

        for (File file : emptyIfNull(distributionDirectory.listFiles())) {
            backupAndCopyFiles(file, new File(midpointInstallation, file.getName()), backupFiles, backupDirectory);
        }

        return null;
    }

    private void backupAndCopyFiles(File from, File to, boolean doBackup, File backupDirectory) throws IOException {
        if (from.isFile()) {
            Set<PosixFilePermission> permissions = null;
            if (to.exists()) {
                permissions = IS_POSIX ? Files.getPosixFilePermissions(to.toPath()) : null;

                if (doBackup) {
                    FileUtils.copyFile(to, new File(backupDirectory, to.getName()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            FileUtils.copyFile(from, to, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            if (permissions != null) {
                Files.setPosixFilePermissions(to.toPath(), permissions);
            }

            return;
        }

        // handling directory
        Set<PosixFilePermission> permissions = null;
        if (to.exists()) {
            permissions = IS_POSIX ? Files.getPosixFilePermissions(to.toPath()) : null;

            if (doBackup) {
                File backedUp = new File(backupDirectory, to.getName());
                backedUp.mkdir();

                if (permissions != null) {
                    Files.setPosixFilePermissions(backedUp.toPath(), permissions);
                }
            }
        }

        to.mkdir();
        if (permissions != null) {
            Files.setPosixFilePermissions(to.toPath(), permissions);
        }

        for (File file : emptyIfNull(from.listFiles())) {
            backupAndCopyFiles(file, new File(to, file.getName()), doBackup, new File(backupDirectory, from.getName()));
        }
    }

    private File[] emptyIfNull(File[] files) {
        if (files == null) {
            return new File[0];
        }

        return files;
    }
}
