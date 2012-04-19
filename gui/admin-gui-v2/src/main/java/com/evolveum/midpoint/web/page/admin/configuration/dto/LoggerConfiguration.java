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

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class LoggerConfiguration extends Selectable {

    private LoggingLevelType level;
    private List<String> appenders = new ArrayList<String>();

    public abstract String getName();
    public abstract void setName(String name);

    public List<String> getAppenders() {
        return appenders;
    }

    public void setAppenders(List<String> appenders) {
        this.appenders = appenders;
    }

    public LoggingLevelType getLevel() {
        return level;
    }

    public void setLevel(LoggingLevelType level) {
        this.level = level;
    }
    
    
}
