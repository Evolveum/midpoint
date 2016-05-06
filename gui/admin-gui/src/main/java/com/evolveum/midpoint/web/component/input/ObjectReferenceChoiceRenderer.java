package com.evolveum.midpoint.web.component.input;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ObjectReferenceChoiceRenderer implements IChoiceRenderer<ObjectReferenceType> {

	private static final long serialVersionUID = 1L;
	private Map<String, String> referenceMap;

	public ObjectReferenceChoiceRenderer(Map<String, String> referenceMap) {
		super();
		this.referenceMap = referenceMap;
	}

	@Override
	public ObjectReferenceType getObject(String id, IModel<? extends List<? extends ObjectReferenceType>> choices) {
		return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
	}

	@Override
	public Object getDisplayValue(ObjectReferenceType object) {
		return referenceMap.get(object.getOid());
	}

	@Override
	public String getIdValue(ObjectReferenceType object, int index) {
		return Integer.toString(index);
	}
}
