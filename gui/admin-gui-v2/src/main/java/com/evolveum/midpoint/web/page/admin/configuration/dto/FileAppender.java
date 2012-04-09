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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_1.FileAppenderConfigurationType;

/**
 * @author lazyman
 */
public class FileAppender extends AppenderConfiguration<FileAppenderConfigurationType> {

    public FileAppender(FileAppenderConfigurationType config) {
        super(config);
    }

    @Override
    public boolean appending() {
        return getConfig().isAppend();
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
