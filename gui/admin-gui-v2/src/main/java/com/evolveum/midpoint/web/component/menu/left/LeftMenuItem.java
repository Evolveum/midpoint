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

package com.evolveum.midpoint.web.component.menu.left;

import org.apache.wicket.Page;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class LeftMenuItem implements Serializable {

    private String label;
    private String icon;
    private Class<? extends Page> page;

    public LeftMenuItem(String label, String icon, Class<? extends Page> page) {
        this.label = label;
        this.icon = icon;
        this.page = page;
    }

    public String getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    public Class<? extends Page> getPage() {
        return page;
    }
}
