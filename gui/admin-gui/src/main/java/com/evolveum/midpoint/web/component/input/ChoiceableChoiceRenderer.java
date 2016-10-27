package com.evolveum.midpoint.web.component.input;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.Choiceable;

public class ChoiceableChoiceRenderer<T extends Choiceable> implements IChoiceRenderer<T> {

	private static final long serialVersionUID = 1L;

	@Override
	public Object getDisplayValue(T object) {
		if (object == null) {
			return "";
		}

		return object.getName();
	}

	@Override
	public String getIdValue(T object, int index) {
		return Integer.toString(index);
	}

	@Override
	public T getObject(String id, IModel<? extends List<? extends T>> choices) {
		return StringUtils.isNotEmpty(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
	}
}
