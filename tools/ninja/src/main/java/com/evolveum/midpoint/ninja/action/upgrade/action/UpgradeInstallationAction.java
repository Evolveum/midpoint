package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class UpgradeInstallationAction extends Action<UpgradeInstallationOptions, Void> {

    private static final String VAR_DIRECTORY = "var";

    @Override
    public String getOperationName() {
        return "upgrade installation";
    }

    @Override
    public Void execute() throws Exception {
        final File distributionDirectory = options.getDistributionDirectory();

        final boolean backupFiles = options.isBackup();

        File midpointInstallation = NinjaUtils.computeInstallationDirectory(options.getInstallationDirectory(), context);

        File backupDirectory = null;
        if (backupFiles) {
            backupDirectory = new File(midpointInstallation, ".backup-" + System.currentTimeMillis());
            backupDirectory.mkdir();

            log.info("Backing up installation directory to: {}", backupDirectory.getAbsolutePath());
        }

        for (File file : emptyIfNull(distributionDirectory.listFiles())) {
            String fileName = file.getName();

            if (backupFiles) {
                File newFile = new File(midpointInstallation, fileName);

                if (!VAR_DIRECTORY.equals(fileName)) {
                    if (newFile.exists()) {
                        FileUtils.moveToDirectory(newFile, backupDirectory, false);
                    }
                } else {
                    // don't back up var directory, upgrade shouldn't touch it, back up only content if needed
                    FileUtils.forceMkdir(new File(backupDirectory, fileName));
                }
            }

            if (VAR_DIRECTORY.equals(fileName)) {
                copyFiles(file, new File(midpointInstallation, fileName), new File(backupDirectory, fileName), backupFiles);
            } else {
                File targetFile = new File(midpointInstallation, fileName);
                deleteExisting(targetFile);

                FileUtils.moveToDirectory(file, midpointInstallation, false);
            }
        }

        log.info("");

        return null;
    }

    private File[] emptyIfNull(File[] files) {
        if (files == null) {
            return new File[0];
        }

        return files;
    }

    private void deleteExisting(File targetFile) throws IOException {
        if (!targetFile.exists()) {
            return;
        }

        if (targetFile.isDirectory()) {
            FileUtils.deleteDirectory(targetFile);
        } else {
            targetFile.delete();
        }
    }

    private void copyFiles(File srcDir, File dstDir, File backupDir, boolean backup) throws IOException {
        File[] files = srcDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();

            if (backup) {
                File newFile = new File(dstDir, fileName);
                if (newFile.exists()) {
                    FileUtils.moveToDirectory(newFile, backupDir, false);
                }
            }

            deleteExisting(new File(dstDir, fileName));
            FileUtils.moveToDirectory(file, dstDir, false);
        }
    }
}
