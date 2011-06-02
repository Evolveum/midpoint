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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.jsf.form;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author lazyman
 */
public class FormAttributeDefinition implements Serializable {

	private static final long serialVersionUID = -8758500863474942393L;
	public static enum Flag {

        READ, CREATE, UPDATE;
    }
    private String displayName;
    private String description;
    private QName elementName;
    private List<Object> availableValues;
    private boolean filledWithExpression = false;
    private int minOccurs = 1;
    private int maxOccurs = 1;
    private List<Flag> flags;
    private AttributeType type;

    public List<Flag> getFlags() {
        if (flags == null) {
            flags = new ArrayList<Flag>();
        }
        return flags;
    }
    
    public boolean isFilledWithExpression() {
		return filledWithExpression;
	}

    public int getMinOccurs() {
        return minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public List<Object> getAvailableValues() {
        if (availableValues == null) {
            return null;
        }
        return Collections.unmodifiableList(availableValues);
    }

    public QName getElementName() {
        return elementName;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AttributeType getType() {
        return type;
    }

    public boolean canRead() {
        return canDoByFlag(Flag.READ);
    }

    public boolean canCreate() {
        return canDoByFlag(Flag.CREATE);
    }

    public boolean canUpdate() {
        return canDoByFlag(Flag.UPDATE);
    }

    private boolean canDoByFlag(Flag flag) {
        if (getFlags().isEmpty()) {
            return true;
        }
        return getFlags().contains(flag);
    }

    void setType(AttributeType type) {
        this.type = type;
    }

    void setAvailableValues(List<Object> availableValues) {
        this.availableValues = availableValues;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    void setElementName(QName elementName) {
        this.elementName = elementName;
    }

    void setFlags(List<Flag> flags) {
        this.flags = flags;
    }

    void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }
    
    void setFilledWithExpression(boolean filledWithExpression) {
		this.filledWithExpression = filledWithExpression;
	}
}
