package com.evolveum.midpoint.web.component.input;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectTypeChoiceRenderer<T extends ObjectType> implements IChoiceRenderer<T>  {

	@Override
	public Object getDisplayValue(T object) {
		return WebComponentUtil.getName(object);
	}

	@Override
	public String getIdValue(T object, int index) {
		return Integer.toString(index);
	}

	@Override
	public T getObject(String id, IModel<? extends List<? extends T>> choices) {
		if (StringUtils.isBlank(id)){
			return null;
		}
		return choices.getObject().get(Integer.parseInt(id));
	}

}
