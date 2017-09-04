package com.evolveum.midpoint.web.component.input;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

public class StringChoiceRenderer implements IChoiceRenderer<String> {

	private static final long serialVersionUID = 1L;

	private String keyPrefix;
	private String splitPattern;

	public StringChoiceRenderer(String keyPrefix) {
		this.keyPrefix = StringUtils.isNotBlank(keyPrefix) ? keyPrefix : "";
	}

	public StringChoiceRenderer(String keyPrefix, String splitPattern) {
		this.keyPrefix = StringUtils.isNotBlank(keyPrefix) ? keyPrefix : "";
		this.splitPattern = splitPattern;
	}

	@Override
	public String getObject(String id, IModel<? extends List<? extends String>> choices) {
		return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
	}

	@Override
	public String getDisplayValue(String object) {
		if (StringUtils.isNotBlank(splitPattern)){
			String[] fields = object.split(splitPattern);
			object = fields[1];
		}

		if (StringUtils.isNotBlank(keyPrefix)){
			return Application.get().getResourceSettings().getLocalizer().getString(keyPrefix + object, null);
		}

		return object;
	}

	@Override
	public String getIdValue(String object, int index) {
		return Integer.toString(index);
	}

}
