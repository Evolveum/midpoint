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

package com.evolveum.midpoint.web.jsf.form.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.faces.convert.IntegerConverter;

/**
 * 
 * @author Vilo Repan
 */
@FacesConverter(value = "formIntegerConverter")
public class FormIntegerConverter extends IntegerConverter implements StateHolder {

	public static final String CONVERTER_ID = "formIntegerConverter";
	private boolean isTransient;
	private Integer minValue;
	private Integer maxValue;

	public void init(Integer minValue, Integer maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public void setMaxValue(Integer maxValue) {
		this.maxValue = maxValue;
	}

	public void setMinValue(Integer minValue) {
		this.minValue = minValue;
	}

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		Integer number = (Integer) super.getAsObject(context, component, value);
		if (number == null) {
			return null;
		}

		if (minValue != null) {
			if (minValue > number) {
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "To low value.",
						"Value must be more than or equal '" + minValue + "'."));
			}
		}

		if (maxValue != null) {
			if (maxValue < number) {
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "To high value.",
						"Value must be less than or equal '" + maxValue + "'."));
			}
		}

		return number;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		return super.getAsString(context, component, value);
	}

	@Override
	public boolean isTransient() {
		return isTransient;
	}

	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	public Object saveState(FacesContext context) {
		Object[] values = new Object[5];
		values[0] = isTransient;
		values[1] = minValue;
		values[2] = maxValue;

		return values;
	}

	public void restoreState(FacesContext context, Object object) {
		Object[] state = (Object[]) object;

		isTransient = (Boolean) state[0];
		minValue = (Integer) state[1];
		maxValue = (Integer) state[2];
	}
}
