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
 * 
 */
public class FormObject implements Serializable {

	private static final long serialVersionUID = -1567990045128005309L;
	private QName typeName;
	private String displayName;
	private boolean showingAllAttributes;
	private List<FormAttribute> attributes;

	@Deprecated
	public FormObject() {
		this(null, null);
	}

	public FormObject(QName typeName, String displayName) {
		// Validate.notEmpty(displayName,
		// "Display name must not be null or empty.");
		this.typeName = typeName;
		this.displayName = displayName;
	}

	public List<FormAttribute> getAttributes() {
		if (attributes == null) {
			attributes = new ArrayList<FormAttribute>();
		}
		return attributes;
	}

	public QName getTypeName() {
		return typeName;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Deprecated
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isShowingAllAttributes() {
		return showingAllAttributes;
	}

	public void setShowingAllAttributes(boolean showingAllAttributes) {
		this.showingAllAttributes = showingAllAttributes;
	}

	public void sort() {
		Collections.sort(getAttributes(), new FormAttributeComparator());
	}
}
