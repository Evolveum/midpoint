/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
