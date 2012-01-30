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

import com.evolveum.midpoint.web.jsf.form.converter.FormAnyURIConverter;
import com.evolveum.midpoint.web.jsf.form.converter.FormIntegerConverter;
import com.evolveum.midpoint.web.jsf.form.converter.FormShortConverter;
import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.icesoft.faces.component.ext.HtmlGraphicImage;
import com.icesoft.faces.component.ext.HtmlInputSecret;
import com.icesoft.faces.component.ext.HtmlInputText;
import com.icesoft.faces.component.ext.HtmlMessage;
import com.icesoft.faces.component.ext.HtmlOutputLabel;
import com.icesoft.faces.component.ext.HtmlOutputText;
import com.icesoft.faces.component.ext.HtmlPanelGrid;
import com.icesoft.faces.component.ext.HtmlSelectBooleanCheckbox;
import com.icesoft.faces.component.ext.HtmlSelectManyListbox;
import com.icesoft.faces.component.ext.HtmlSelectOneMenu;
import com.icesoft.faces.component.selectinputdate.SelectInputDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.context.FacesContext;
import javax.faces.convert.BooleanConverter;
import javax.faces.convert.Converter;
import javax.faces.convert.IntegerConverter;
import javax.faces.convert.LongConverter;

/**
 * 
 * @author Vilo Repan
 */
public final class AutoFormFactory {

	private AutoFormFactory() {
	}

	static Converter getInputConverter(Application application, FormAttributeDefinition definition) {
		Converter converter = null;

		AttributeType type = definition.getType();
		switch (type) {
			case INT:
			case INTEGER:
				converter = application.createConverter(IntegerConverter.CONVERTER_ID);
				break;
			case BOOLEAN:
				converter = application.createConverter(BooleanConverter.CONVERTER_ID);
				break;
			case LONG:
				converter = application.createConverter(LongConverter.CONVERTER_ID);
				break;
			case INTEGER_NEGATIVE:
				converter = application.createConverter(FormIntegerConverter.CONVERTER_ID);
				((FormIntegerConverter) converter).init(null, -1);
				break;
			case INTEGER_NON_NEGATIVE:
				converter = application.createConverter(FormIntegerConverter.CONVERTER_ID);
				((FormIntegerConverter) converter).init(0, null);
				break;
			case INTEGER_POSITIVE:
				converter = application.createConverter(FormIntegerConverter.CONVERTER_ID);
				((FormIntegerConverter) converter).init(1, null);
				break;
			case INTEGER_NON_POSITIVE:
				converter = application.createConverter(FormIntegerConverter.CONVERTER_ID);
				((FormIntegerConverter) converter).init(null, 0);
				break;
			case SHORT:
				converter = application.createConverter(FormShortConverter.CONVERTER_ID);
				break;
			case ANY_URI:
				converter = application.createConverter(FormAnyURIConverter.CONVERTER_ID);
				break;
			default:
				converter = null;
		}

		return converter;
	}

	static boolean isAttributeValueNull(Object value) {
		return (value == null);
	}

	static UIComponent createOutputText(Application application, int attributeIndex, int valueIndex,
			ValueExpression beanExpression) {
		HtmlOutputText text = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);

		ValueExpression valueExpression = createValueExpression(beanExpression, attributeIndex, valueIndex);
		text.setValueExpression("value", valueExpression);

		return text;
	}

	static UIComponent createBlankComponent(Application application) {
		return application.createComponent(HtmlOutputText.COMPONENT_TYPE);
	}

	static UIComponent createErrorMessage(Application application, UIComponent component) {
		HtmlMessage message = (HtmlMessage) application.createComponent(HtmlMessage.COMPONENT_TYPE);
		message.setStyle("color: red;");
		message.setFor(component.getId());
		message.setId("message_" + component.getId());

		return message;
	}
	
	static UICommand createShowContentImage(Application application) {
		return createButtonImage(application, "/resources/images/content_show.png", "ShowContent");
	}
	
	static UICommand createHideContentImage(Application application) {
		return createButtonImage(application, "/resources/images/content_hide.png", "HideContent");
	}

	static UICommand createAddImage(Application application) {
		return createButtonImage(application, "/resources/images/add.png", "Add");
	}

	static UICommand createDeleteImage(Application application) {
		return createButtonImage(application, "/resources/images/delete.png", "Delete");
	}

	static UICommand createHelpImage(Application application, String value) {
		return createButtonImage(application, "/resources/images/help.png", "Help");
	}

	static UIComponent createGrid(Application application, int columns) {
		HtmlPanelGrid grid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
		grid.setColumns(columns);

		return grid;
	}

	static UIComponent createBlankImage(Application application) {
		HtmlOutputText comp = (HtmlOutputText) application.createComponent(HtmlOutputText.COMPONENT_TYPE);
		comp.setStyle("width: 16px; height: 16px; display: block;");
		return comp;
	}

	private static UIComponent createImage(Application application, String image) {
		HtmlGraphicImage img = (HtmlGraphicImage) application
				.createComponent(HtmlGraphicImage.COMPONENT_TYPE);
		img.setMimeType("image/png");
		img.setStyle("border: 0px;");
		img.setValue(image);

		return img;
	}

	private static HtmlCommandLink createButtonImage(Application application, String image, String title) {
		HtmlCommandLink link = (HtmlCommandLink) application.createComponent(HtmlCommandLink.COMPONENT_TYPE);
		link.setImmediate(true);
		link.setTitle(title);
		link.getChildren().add(createImage(application, image));

		return link;
	}

	static UIComponent createLabel(Application application, String name) {
		HtmlOutputLabel label = (HtmlOutputLabel) application.createComponent(HtmlOutputLabel.COMPONENT_TYPE);
		label.setValue(name);

		return label;
	}

	static UIComponent createRequiredLabel(Application application, String name) {
		HtmlOutputLabel label = (HtmlOutputLabel) createLabel(application, name);
		label.setStyleClass("required-field");

		return label;
	}

	static UIComponent createFilledWithExpressionLabel(Application application, String name) {
		HtmlOutputLabel label = (HtmlOutputLabel) createLabel(application, name);
		/*label.setStyleClass("expression-field");*/

		return label;
	}

	static UIInput createComplexInput(Application application, final FormAttribute attribute, Object value) {
		HtmlSelectManyListbox listbox = (HtmlSelectManyListbox) application
				.createComponent(HtmlSelectManyListbox.COMPONENT_TYPE);
		listbox.setStyle("width: 98%;");
		listbox.setSize(3);
		listbox.setReadonly(isOnlyReadable(attribute.getDefinition()));
		// TODO: maybe use xml editor in here...
		return listbox;
	}

	static UIInput createSelectInputDate(Application application, final FormAttribute attribute,
			int attributeIndex, int valueIndex, ValueExpression beanExpression) {
		FormAttributeDefinition definition = attribute.getDefinition();

		SelectInputDate date = (SelectInputDate) application.createComponent(SelectInputDate.COMPONENT_TYPE);
		final String dateFormat = "dd. MMMM, yyyy";
		date.setPopupDateFormat(dateFormat);
		date.setRenderAsPopup(true);
		date.setPartialSubmit(true);
		date.setStyleClass("iceInpTxt");
		date.setReadonly(isOnlyReadable(definition));
		date.setImageDir("/resources/css/rime/css-images/");

		ValueExpression valueExpression = createValueExpression(beanExpression, attributeIndex, valueIndex);
		date.setValueExpression("value", valueExpression);

		return date;
	}

	static UIInput createSelectOneMenu(Application application, final FormAttribute attribute,
			int attributeIndex, int valueIndex, ValueExpression beanExpression) {
		FormAttributeDefinition definition = attribute.getDefinition();

		HtmlSelectOneMenu combo = (HtmlSelectOneMenu) application
				.createComponent(HtmlSelectOneMenu.COMPONENT_TYPE);
		combo.setReadonly(isOnlyReadable(definition));
		combo.setPartialSubmit(true);
		UISelectItem selectItem = null;

		List<UISelectItem> items = new ArrayList<UISelectItem>();
		for (Object item : definition.getAvailableValues()) {
			selectItem = (UISelectItem) application.createComponent(UISelectItem.COMPONENT_TYPE);
			selectItem.setItemLabel(item.toString());
			selectItem.setItemValue(item);

			items.add(selectItem);
		}

		selectItem = (UISelectItem) application.createComponent(UISelectItem.COMPONENT_TYPE);
		selectItem.setItemLabel("");
		selectItem.setItemValue(null);
		combo.getChildren().add(selectItem);

		Collections.sort(items, new Comparator<UISelectItem>() {

			@Override
			public int compare(UISelectItem item1, UISelectItem item2) {
				return String.CASE_INSENSITIVE_ORDER.compare(item1.getItemLabel(), item2.getItemLabel());
			}
		});
		combo.getChildren().addAll(items);

		ValueExpression valueExpression = createValueExpression(beanExpression, attributeIndex, valueIndex);
		combo.setValueExpression("value", valueExpression);

		return combo;
	}

	static UIInput createInputText(Application application, final FormAttribute attribute,
			int attributeIndex, int valueIndex, ValueExpression beanExpression, boolean password) {

		FormAttributeDefinition definition = attribute.getDefinition();

		HtmlInputText text = null;
		if (!password) {
			text = (HtmlInputText) application.createComponent(HtmlInputText.COMPONENT_TYPE);
		} else {
			text = (HtmlInputSecret) application.createComponent(HtmlInputSecret.COMPONENT_TYPE);
		}
		//////////////////// Execute submit after lose focus ///////////////
		//text.setPartialSubmit(true);
		////////////////////////////////////////////////////////////////////
		
		text.setStyle("width: 95%;");
		text.setAutocomplete("off");
		text.setReadonly(isOnlyReadable(definition));
		ValueExpression valueExpression = createValueExpression(beanExpression, attributeIndex, valueIndex);
		text.setValueExpression("value", valueExpression);

		return text;
	}

	private static ValueExpression createValueExpression(ValueExpression beanExpression, int attributeIndex,
			int valueIndex) {
		String expr = beanExpression.getExpressionString();
		StringBuilder builder = new StringBuilder();
		builder.append("#{");
		builder.append(expr.substring(2, expr.length() - 1));
		builder.append(".attributes[");
		builder.append(attributeIndex);
		builder.append("].values[");
		builder.append(valueIndex);
		builder.append("]}");

		FacesContext context = FacesContext.getCurrentInstance();
		ExpressionFactory factory = context.getApplication().getExpressionFactory();
		return factory.createValueExpression(context.getELContext(), builder.toString(), Object.class);
	}

	static UIInput createBooleanCheckbox(Application application, final FormAttribute attribute,
			int attributeIndex, int valueIndex, ValueExpression beanExpression) {
		FormAttributeDefinition definition = attribute.getDefinition();

		HtmlSelectBooleanCheckbox check = (HtmlSelectBooleanCheckbox) application
				.createComponent(HtmlSelectBooleanCheckbox.COMPONENT_TYPE);
		check.setReadonly(isOnlyReadable(definition));
		check.setPartialSubmit(true);

		ValueExpression valueExpression = createValueExpression(beanExpression, attributeIndex, valueIndex);
		check.setValueExpression("value", valueExpression);

		return check;
	}

	private static boolean isOnlyReadable(FormAttributeDefinition definition) {
		return definition.canRead() && !definition.canUpdate() && !definition.canCreate();
	}
}
