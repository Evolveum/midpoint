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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

/**
 * @author lazyman
 */
public class LoggingDto {

    private LoggingLevelType rootLevel;
    private String rootAppender;

    private LoggingLevelType midPointLevel;
    private String midPointAppender;

    public String getMidPointAppender() {
        return midPointAppender;
    }

    public void setMidPointAppender(String midPointAppender) {
        this.midPointAppender = midPointAppender;
    }

    public LoggingLevelType getMidPointLevel() {
        return midPointLevel;
    }

    public void setMidPointLevel(LoggingLevelType midPointLevel) {
        this.midPointLevel = midPointLevel;
    }

    public String getRootAppender() {
        return rootAppender;
    }

    public void setRootAppender(String rootAppender) {
        this.rootAppender = rootAppender;
    }

    public LoggingLevelType getRootLevel() {
        return rootLevel;
    }

    public void setRootLevel(LoggingLevelType rootLevel) {
        this.rootLevel = rootLevel;
    }
}
