package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;

public class UpgradeInstallationAction extends Action<UpgradeInstallationOptions> {

    private static final String VAR_DIRECTORY = "var";

    @Override
    public void execute() throws Exception {
        final File distributionDirectory = options.getDistributionDirectory();

        final boolean backupFiles = options.isBackup();

        File midpointInstallation = options.getInstallationDirectory();
        if (midpointInstallation == null) {
            final ConnectionOptions connectionOptions = context.getOptions(ConnectionOptions.class);
            midpointInstallation = new File(connectionOptions.getMidpointHome()).getParentFile();
        }

        File backupDirectory = null;
        if (backupFiles) {
            backupDirectory = new File(midpointInstallation, ".backup-" + System.currentTimeMillis());
            backupDirectory.mkdir();
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
                FileUtils.moveToDirectory(file, midpointInstallation, false);
            }
        }
    }

    private File[] emptyIfNull(File[] files) {
        if (files == null) {
            return new File[0];
        }

        return files;
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

            FileUtils.moveToDirectory(file, dstDir, false);
        }
    }
}
