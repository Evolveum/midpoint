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
package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;

/**
 * 
 * @author lazyman
 * 
 */
public class AssignmentBean extends SelectableBean implements Serializable {

	private static final long serialVersionUID = 1018852396117899734L;
	private int id;
	private AssignmentBeanType type;
	private boolean editing;

	private boolean enabled = true;
	private boolean showActivationDate = false;
	private AssignmentType assignment;

	public int getId() {
		return id;
	}

	public AssignmentBean(int id, AssignmentType assignment) {
		Validate.notNull(assignment, "Assignment must not be null.");
		this.id = id;
		this.assignment = assignment;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	public String getTypeString() {
		if (type == null) {
			return null;
		}
		return FacesUtils.translateKey(type.getLocalizationKey());
	}

	public boolean isShowActivationDate() {
		return showActivationDate;
	}

	public void setShowActivationDate(boolean showActivationDate) {
		this.showActivationDate = showActivationDate;
	}

	public Date getFromActivation() {
		if (assignment.getActivation() == null) {
			return new Date();
		}

		ActivationType activation = assignment.getActivation();
		XMLGregorianCalendar calendar = activation.getValidFrom();
		return calendar.toGregorianCalendar().getTime();
	}

	public Date getToActivation() {
		if (assignment.getActivation() == null) {
			return new Date();
		}

		ActivationType activation = assignment.getActivation();
		XMLGregorianCalendar calendar = activation.getValidTo();
		return calendar.toGregorianCalendar().getTime();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
