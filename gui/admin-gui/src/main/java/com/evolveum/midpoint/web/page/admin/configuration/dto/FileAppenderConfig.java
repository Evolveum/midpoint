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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileAppenderConfigurationType;

/**
 * @author lazyman
 */
public class FileAppenderConfig extends AppenderConfiguration<FileAppenderConfigurationType, FileAppenderConfig> {

    public FileAppenderConfig(FileAppenderConfigurationType config) {
        super(config);
    }

    @Override
    public boolean isAppending() {
        return Boolean.TRUE.equals(getConfig().isAppend());
    }

    @Override
    public boolean isPrudent() {
        return Boolean.TRUE.equals(getConfig().isPrudent());
    }

    @Override
    public String getFilePath() {
        return getConfig().getFileName();
    }

    @Override
    public String getFilePattern() {
        return getConfig().getFilePattern();
    }

    @Override
    public Integer getMaxHistory() {
        return getConfig().getMaxHistory();
    }

    @Override
    public String getMaxFileSize() {
        return getConfig().getMaxFileSize();
    }

    public void setAppending(boolean appending) {
        getConfig().setAppend(appending);
    }

    public void setPrudent(boolean prudent) {
        getConfig().setPrudent(prudent);
    }

    public void setFilePath(String filePath) {
        getConfig().setFileName(filePath);
    }

    public void setFilePattern(String filePattern) {
        getConfig().setFilePattern(filePattern);
    }

    public void setMaxFileSize(String maxFileSize) {
        getConfig().setMaxFileSize(maxFileSize);
    }

    public void setMaxHistory(Integer maxHistory) {
        getConfig().setMaxHistory(maxHistory);
    }
}
