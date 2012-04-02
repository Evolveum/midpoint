/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.menu.top;

import org.apache.wicket.Page;

import java.io.Serializable;

public class TopMenuItem implements Serializable {

    private String label;
    private String description;
    private Class<? extends Page> page;

    public TopMenuItem(String label, String description, Class<? extends Page> page) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("Label must not be null or empty.");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException(("Description must not be null or empty."));
        }
        if (page == null) {
            throw new IllegalArgumentException("Page must not be null or empty.");
        }
        this.label = label;
        this.description = description;
        this.page = page;
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
}
