package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;

import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;

public class UpgradeCommonOptions {

    public static final String P_TEMP_DIR_LONG = "--temp-dir";

    @Parameter(names = { P_TEMP_DIR_LONG }, descriptionKey = "upgrade.tempDir") // todo fix key
    private File tempDirectory;

    public File getTempDirectory() {
        if (this.tempDirectory == null) {
            this.tempDirectory = new File(FileUtils.getTempDirectory(), UpgradeConstants.UPGRADE_TEMP_DIRECTORY);
        }

        return tempDirectory;
    }

    public void setTempDirectory(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }
}
