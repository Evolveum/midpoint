package com.evolveum.midpoint.gui.api.component.autocomplete;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.prism.ItemDefinition;

public class AutoCompleteItemDefinitionRenderer extends AbstractAutoCompleteRenderer<ItemDefinition<?>>{

	private static final long serialVersionUID = 1L;

	@Override
	protected String getTextValue(ItemDefinition<?> object) {
		return object.getName().getLocalPart();
	}

	@Override
	protected void renderChoice(ItemDefinition<?> object, Response response, String criteria) {
		String textValue = getTextValue(object);
		textValue = Strings.escapeMarkup(textValue).toString();
		response.write(textValue);

	}


}
