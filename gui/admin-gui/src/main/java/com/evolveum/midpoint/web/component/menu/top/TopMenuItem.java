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

package com.evolveum.midpoint.web.component.menu.top;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Page;

import java.io.Serializable;

public class TopMenuItem implements Serializable {

    private String label;
    private String description;
    private Class<? extends Page> page;
    private Class<? extends Page> marker;

    public TopMenuItem(String label, String description, Class<? extends Page> page) {
        this(label, description, page, null);
    }

    public TopMenuItem(String label, String description, Class<? extends Page> page,
            Class<? extends Page> marker) {
        Validate.notEmpty(label, "Label must not be null or empty.");
        Validate.notEmpty(description, "Description must not be null or empty.");
        Validate.notNull(page, "Page must not be null or empty.");

        this.label = label;
        this.description = description;
        this.page = page;
        this.marker = marker;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Class<? extends Page> getPage() {
        return page;
    }

    public Class<?> getMarker() {
        if (marker == null) {
            return page;
        }
        return marker;
    }
}
