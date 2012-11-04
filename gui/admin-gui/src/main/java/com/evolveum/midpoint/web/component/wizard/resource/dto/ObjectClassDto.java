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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ObjectClassDto implements Serializable {

    public static final String F_ATTRIBUTES_VISIBLE = "attributesVisible";

    private boolean attributesVisible;
    private List<QName> attributes;

    public List<QName> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<QName>();
        }
        return attributes;
    }

    public void setAttributes(List<QName> attributes) {
        this.attributes = attributes;
    }

    public boolean isAttributesVisible() {
        return attributesVisible;
    }

    public void setAttributesVisible(boolean attributesVisible) {
        this.attributesVisible = attributesVisible;
    }
}
