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

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;

import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class AppenderConfiguration<T extends AppenderConfigurationType, O extends AppenderConfiguration>
        extends Selectable implements Editable, Comparable<O> {

    private boolean editing;
    private T config;

    public AppenderConfiguration(T config) {
        Validate.notNull(config, "Appender configuration type must not be null.");
        this.config = config;
    }

    public T getConfig() {
        return config;
    }

    public String getPattern() {
        return config.getPattern();
    }

    public void setPattern(String pattern) {
        config.setPattern(pattern);
    }

    public void setName(String name) {
        config.setName(name);
    }

    public String getName() {
        return config.getName();
    }

    public String getFilePath() {
        return null;
    }

    public String getFilePattern() {
        return null;
    }

    public Integer getMaxHistory() {
        return null;
    }

    public String getMaxFileSize() {
        return null;
    }

    public boolean isAppending() {
        return false;
    }
	
	public boolean isPrudent() {
        return false;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    @Override
    public int compareTo(O o) {
        if (o == null) {
            return 0;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(getName(), o.getName());
    }
}
