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

import javax.faces.FacesException;
import javax.faces.component.ContextCallback;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.icesoft.faces.component.ext.HtmlPanelGrid;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import static com.evolveum.midpoint.web.jsf.form.AutoFormFactory.*;

/**
 * 
 * @author lazyman
 */
@FacesComponent(value = "AutoForm")
public class AutoForm extends UIComponentBase implements NamingContainer, Serializable {

	private static final long serialVersionUID = 296759306259926240L;
	private static final Trace TRACE = TraceManager.getTrace(AutoForm.class);
	public static final String COMPONENT_TYPE = AutoForm.class.getName();
	static final String ATTR_BEAN = "bean";
	static final String ATTR_EDITABLE = "editable";
	static final String ATTR_INDEX = "index";
	private static final int GRID_COLUMNS_COUNT = 5;
	// children
	private transient HtmlPanelGrid grid;

	public AutoForm() {
		printMessage("constructor");
	}

	@Override
	public String getFamily() {
		return "AutoFormFamily";
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public List<UIComponent> getChildren() {
		printMessage("getChildren " + getChildCount() + " " + grid.getClientId());
		if (super.getChildCount() > 1) {
			recursivePrint(this, 0, 2);
			printMessage("---------------------------------------------------------------------");
		}

		if (super.getChildCount() != 1) {
			List<UIComponent> children = super.getChildren();
			children.clear();
			children.add(grid);
		}

		return super.getChildren();
	}

	@Override
	public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
			throws FacesException {
		printMessage("invokeOnComponent " + clientId + ", " + callback + " " + getChildCount());
		if (getClientId().equals(clientId)) {
			callback.invokeContextCallback(context, this);
			return true;
		}

		return false;
		// if (grid != null && grid.getClientId().equals(clientId)) {
		// callback.invokeContextCallback(context, grid);
		// }
		//
		// return grid.invokeOnComponent(context, clientId, callback);
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		if (!isRendered()) {
			printMessage("encodeChildren: skipping rendering");
			return;
		}
		printMessage("encodeChildren " + getChildCount());
		createChildren();
		grid.encodeAll(context);
	}

	@Override
	public void decode(FacesContext context) {
		printMessage("decode " + getChildCount());
		// grid.decode(context);
		super.decode(context);
	}

	@Override
	public void processRestoreState(FacesContext context, Object state) {
		printMessage("processRestoreState " + getChildCount());
		Object[] object = (Object[]) state;
		// grid.processRestoreState(context, object[0]);
		// popup.processRestoreState(context, object[1]);
		super.processRestoreState(context, object[2]);
	}

	@Override
	public void processDecodes(FacesContext context) {
		printMessage("processDecodes " + getChildCount());
		createChildren();
		// grid.processDecodes(context);
		// popup.processDecodes(context);
		super.processDecodes(context);
	}

	@Override
	public Object processSaveState(FacesContext context) {
		printMessage("processSaveState " + getChildCount());
		Object[] state = new Object[3];
		// state[0] = grid.processSaveState(context);
		// state[1] = popup.processSaveState(context);
		state[2] = super.processSaveState(context);

		return state;
	}

	@Override
	public Object saveState(FacesContext context) {
		printMessage("saveState " + getChildCount());
		Object values[] = new Object[3];
		values[0] = super.saveState(context);
		// values[1] = grid.saveState(context);
		// // values[2] = popup.saveState(context);
		return ((Object) (values));
	}

	@Override
	public void restoreState(FacesContext context, Object state) {
		printMessage("restoreState " + getChildCount());
		createChildren();
		Object values[] = (Object[]) state;
		super.restoreState(context, values[0]);
		// grid.restoreState(context, values[1]);
		// popup.restoreState(context, values[2]);
		//
		// isRestoringState = true;
	}

	@Override
	public void processUpdates(FacesContext context) {
		printMessage("processUpdates " + getChildCount());
		// grid.processUpdates(context);
		// popup.processUpdates(context);
		super.processUpdates(context);
	}

	@Override
	public void processValidators(FacesContext context) {
		printMessage("processUpdates " + getChildCount());
		// grid.processValidators(context);
		// popup.processValidators(context);
		super.processValidators(context);
	}

	private FormObject getBean() {
		if (getValueExpression(ATTR_BEAN) == null) {
			printMessage("getBean: null");
			return null;
		}

		FormObject object = (FormObject) getBeanExpression().getValue(getFacesContext().getELContext());
		printMessage("getBean: " + object);
		return object;
	}

	private ValueExpression getBeanExpression() {
		ValueExpression indexExpr = getValueExpression(ATTR_INDEX);
		if (indexExpr == null) {
			printMessage("getBeanExpression");
			return getValueExpression(ATTR_BEAN);
		}

		FacesContext context = getFacesContext();
		ExpressionFactory factory = context.getApplication().getExpressionFactory();
		StringBuilder builder = new StringBuilder();
		String bean = getValueExpression(ATTR_BEAN).getExpressionString();
		builder.append("#{");
		builder.append(bean.substring(2, bean.length() - 1));
		builder.append("[");
		builder.append(indexExpr.getValue(context.getELContext()));
		builder.append("]}");
		printMessage("getBeanExpression " + builder.toString());
		return factory.createValueExpression(context.getELContext(), builder.toString(), FormObject.class);
	}

	private boolean isEditable() {
		if (getValueExpression(ATTR_EDITABLE) == null) {
			return true;
		}
		return (Boolean) getValueExpression(ATTR_EDITABLE).getValue(getFacesContext().getELContext());
	}

	private void createChildren() {
		List<FormAttribute> attributes = getBean().getAttributes();
		if (grid == null) {
			printMessage("creating grid");
			Application application = getFacesContext().getApplication();
			grid = (HtmlPanelGrid) application.createComponent(HtmlPanelGrid.COMPONENT_TYPE);
			grid.setColumns(GRID_COLUMNS_COUNT);
			grid.getChildren().addAll(createAttributeForm(attributes));
		} else {
			printMessage("not creating grid, only updating");
			for (FormAttribute attribute : attributes) {
				if (!attribute.isChanged()) {
					continue;
				}

				updateAttributeRows(attribute.getChangeType(), attributes.indexOf(attribute));
				attribute.clearChanges();
			}
		}

		List<UIComponent> children = getChildren();
		children.clear();
		children.add(grid);
	}

	private List<UIComponent> createAttributeForm(List<FormAttribute> list) {
		List<UIComponent> formList = new ArrayList<UIComponent>();
		for (FormAttribute attribute : list) {
			FormAttributeDefinition definition = attribute.getDefinition();
			List<Object> values = attribute.getValues();
			if (values.isEmpty()) {
				values.add(null);
			}

			while (values.size() < definition.getMinOccurs()) {
				values.add(null);
			}

			List<UIComponent> attributeRows = createAttributeRows(attribute, list.indexOf(attribute));
			formList.addAll(attributeRows);
		}

		return formList;
	}

	private List<UIComponent> createAttributeRows(FormAttribute attribute, int attributeIndex) {
		Application application = getFacesContext().getApplication();
		List<UIComponent> attributeRows = new ArrayList<UIComponent>();
		FormAttributeDefinition definition = attribute.getDefinition();

		for (int valueIndex = 0; valueIndex < attribute.getValuesSize(); valueIndex++) {
			UIComponent help;
			UIComponent label;
			if (valueIndex == 0) {
				if (definition.getDescription() != null && !definition.getDescription().isEmpty()) {
					UICommand helpButton = createHelpImage(application, definition.getDescription());
					helpButton.setId("help_" + attributeIndex + "_" + valueIndex);
					helpButton.addActionListener(new FormButtonListener(getBeanExpression(),
							FormButtonType.HELP, attributeIndex, valueIndex));
					help = helpButton;
				} else {
					help = createBlankImage(application);
				}

				label = createLabel(application, definition.getDisplayName());
				if (definition.getMinOccurs() > 0) {
					UIComponent required = createRequiredLabel(application, "*");
					label.getChildren().add(required);
				}
				if (definition.isFilledWithExpression()) {
					UIComponent filled = createFilledWithExpressionLabel(application, "*");
					label.getChildren().add(filled);
				}
			} else {
				help = createBlankComponent(application);
				label = createBlankComponent(application);
			}
			attributeRows.add(help);
			attributeRows.add(label);

			if (isEditable()) {
				UIInput input = createInput(attribute, attributeIndex, valueIndex);
				attributeRows.add(input);

				UIComponent column = createButtonPanel(attribute, attributeIndex, valueIndex);
				attributeRows.add(column);

				UIComponent errorLabel = createErrorMessage(application, input);
				attributeRows.add(errorLabel);
			} else {
				UIComponent output = createOutputText(application, attributeIndex, valueIndex,
						getBeanExpression());
				attributeRows.add(output);
				attributeRows.add(createBlankImage(application));
				attributeRows.add(createBlankImage(application));
			}
		}

		return attributeRows;
	}

	private UIComponent createButtonPanel(FormAttribute attribute, int attributeIndex, int valueIndex) {
		Application application = getFacesContext().getApplication();
		UIComponent panel = createGrid(application, 2);

		if (attribute.canRemoveValue() && attribute.getValuesSize() > 1) {
			UICommand button = createDeleteImage(application);
			button.setId("delete_" + attributeIndex + "_" + valueIndex);
			button.addActionListener(new FormButtonListener(getBeanExpression(), FormButtonType.DELETE,
					attributeIndex, valueIndex));
			panel.getChildren().add(button);
		} else {
			panel.getChildren().add(createBlankImage(application));
		}

		if (attribute.canAddValue() && (valueIndex + 1 == attribute.getValuesSize())) {
			UICommand button = createAddImage(application);
			button.setId("add_" + attributeIndex + "_" + valueIndex);
			button.addActionListener(new FormButtonListener(getBeanExpression(), FormButtonType.ADD,
					attributeIndex, valueIndex));
			panel.getChildren().add(button);
		} else {
			panel.getChildren().add(createBlankImage(application));
		}

		return panel;
	}

	private UIInput createInput(FormAttribute attribute, int attributeIndex, int valueIndex) {
		Application application = FacesContext.getCurrentInstance().getApplication();
		UIInput component = null;
		FormAttributeDefinition definition = attribute.getDefinition();
		if (AttributeType.DATE.equals(definition.getType())) {
			component = createSelectInputDate(application, attribute, attributeIndex, valueIndex,
					getBeanExpression());
		} else if (AttributeType.BOOLEAN.equals(definition.getType())) {
			component = createBooleanCheckbox(application, attribute, attributeIndex, valueIndex,
					getBeanExpression());
		} else if (AttributeType.PASSWORD.equals(definition.getType())) {
			component = createInputText(application, attribute, attributeIndex, valueIndex,
					getBeanExpression(), true);
		} else {
			if (definition.getAvailableValues() != null) {
				component = createSelectOneMenu(application, attribute, attributeIndex, valueIndex,
						getBeanExpression());
			}
		}

		if (component == null) {
			component = createInputText(application, attribute, attributeIndex, valueIndex,
					getBeanExpression(), false);
		}

		component.setId(definition.getElementName().getLocalPart() + "_" + Integer.toString(attributeIndex)
				+ "_" + Integer.toString(valueIndex));
		component.setConverter(getInputConverter(application, definition));

		return component;
	}

	private void updateAttributeRows(FormAttribute.Change changeType, int attributeIndex) {
		FormObject form = getBean();
		FormAttribute attribute = form.getAttributes().get(attributeIndex);

		List<UIComponent> row = createAttributeRows(attribute, attributeIndex);

		int absoluteIndex = getAbsoluteIndex(form, attributeIndex, 0);
		int start = absoluteIndex * GRID_COLUMNS_COUNT;
		int end = (absoluteIndex + attribute.getValuesSize() - 1) * GRID_COLUMNS_COUNT;
		if (changeType == FormAttribute.Change.DELETE) {
			end += 2 * GRID_COLUMNS_COUNT;
		}

		List<UIComponent> gridChildren = grid.getChildren();
		for (int index = start; index < end; index++) {
			gridChildren.remove(start);
		}

		for (int i = 0; i < row.size(); i++) {
			gridChildren.add(start + i, row.get(i));
		}
	}

	private int getAbsoluteIndex(FormObject object, int attrIndex, int valueIndex) {
		int index = valueIndex;
		for (int i = 0; i < attrIndex; i++) {
			FormAttribute attribute = object.getAttributes().get(i);
			index += attribute.getValuesSize();
		}

		return index;
	}

	@Deprecated
	private void printMessage(String message) {
		if (TRACE.isTraceEnabled()) {
			TRACE.trace(this.toString() + ":\t" + message);
		}
	}

	@Deprecated
	private void recursivePrint(UIComponent c, int depth, int maxDepth) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			builder.append("  ");
		}
		builder.append(c.getClass().getSimpleName());
		builder.append(", ");
		builder.append(c.getClientId());
		printMessage(builder.toString());
		if (depth >= maxDepth) {
			return;
		}
		List<UIComponent> children = null;
		if (c == this) {
			children = super.getChildren();
		} else {
			children = c.getChildren();
		}
		if (children.isEmpty()) {
			return;
		}
		for (UIComponent a : children) {
			recursivePrint(a, depth + 1, maxDepth);
		}
	}
}
