/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.tools.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author lazyman
 */
public class GeneratorConfiguration {

    private String propertiesLocaleDelimiter = "_";
    private File targetFolder;

    private File baseFolder;

    private List<Locale> localesToCheck;
    private List<String> recursiveFolderToCheck;
    private List<String> nonRecursiveFolderToCheck;

    private boolean disableBackup;

    public File getBaseFolder() {
        return baseFolder;
    }

    public void setBaseFolder(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    public List<Locale> getLocalesToCheck() {
        if (localesToCheck == null) {
            localesToCheck = new ArrayList<Locale>();
        }
        return localesToCheck;
    }

    public void setLocalesToCheck(List<Locale> localesToCheck) {
        this.localesToCheck = localesToCheck;
    }

    public List<String> getNonRecursiveFolderToCheck() {
        if (nonRecursiveFolderToCheck == null) {
            nonRecursiveFolderToCheck = new ArrayList<String>();
        }
        return nonRecursiveFolderToCheck;
    }

    public void setNonRecursiveFolderToCheck(List<String> nonRecursiveFolderToCheck) {
        this.nonRecursiveFolderToCheck = nonRecursiveFolderToCheck;
    }

    public List<String> getRecursiveFolderToCheck() {
        if (recursiveFolderToCheck == null) {
            recursiveFolderToCheck = new ArrayList<String>();
        }
        return recursiveFolderToCheck;
    }

    public void setRecursiveFolderToCheck(List<String> recursiveFolderToCheck) {
        this.recursiveFolderToCheck = recursiveFolderToCheck;
    }

    public File getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(File targetFolder) {
        this.targetFolder = targetFolder;
    }

    public String getPropertiesLocaleDelimiter() {
        return propertiesLocaleDelimiter;
    }

    public void setPropertiesLocaleDelimiter(String propertiesLocaleDelimiter) {
        this.propertiesLocaleDelimiter = propertiesLocaleDelimiter;
    }

    public boolean isDisableBackup() {
        return disableBackup;
    }

    public void setDisableBackup(boolean disableBackup) {
        this.disableBackup = disableBackup;
    }
}
