/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.factory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractGuiComponentFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private transient GuiComponentRegistry registry;

	public GuiComponentRegistry getRegistry() {
		return registry;
	}

	@Override
	public Panel createPanel(PrismPropertyPanelContext<T> panelCtx) {
		Panel panel = getPanel(panelCtx);
		
		if (panel instanceof InputPanel) {
			InputPanel inputPanel = (InputPanel) panel;
		
			panelCtx.getFeedbackPanel()
					.setFilter(new ComponentFeedbackMessageFilter(((InputPanel) panel).getBaseFormComponent()));

			//LambdaModel.of(panelCtx.unwrapWrapperModel()::getFormComponentValidator)
//			ExpressionValidator<T> expressionValidator = new ExpressionValidator<T>(
//					null, panelCtx.getPageBase(),
//					panelCtx.getItemWrapperModel());
//			inputPanel.getBaseFormComponent().add(expressionValidator);
//			panelCtx.getForm().add(expressionValidator);
//
//			final List<FormComponent> formComponents = inputPanel.getFormComponents();
//			for (FormComponent<T> formComponent : formComponents) {
//				PropertyModel<String> label = new PropertyModel<>(panelCtx.getItemWrapperModel(), "displayName");
////				IModel<String> label = LambdaModel.of(panelCtx.unwrapWrapperModel()::getDisplayName);
//				formComponent.setLabel(label);
//				formComponent.setRequired(panelCtx.unwrapWrapperModel().isMandatory());
//
//				if (formComponent instanceof TextField) {
//					formComponent.add(new AttributeModifier("size", "42"));
//				}
//				formComponent.add(new AjaxFormComponentUpdatingBehavior("blur") {
//					
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					protected void onUpdate(AjaxRequestTarget target) {
//						target.add(panelCtx.getPageBase().getFeedbackPanel());
//						target.add(panelCtx.getFeedbackPanel());
//					}
//
//					@Override
//					protected void onError(AjaxRequestTarget target, RuntimeException e) {
//						target.add(panelCtx.getPageBase().getFeedbackPanel());
//						target.add(panelCtx.getFeedbackPanel());
//					}
//
//				});
//			}
		}
		return panel;
	}
	
	@Override
	public Integer getOrder() {
		// TODO Auto-generated method stub
		return Integer.MAX_VALUE;
	}
	
	protected abstract Panel getPanel(PrismPropertyPanelContext<T> panelCtx);
	
	protected List<String> prepareAutoCompleteList(String input, LookupTableType lookupTable) {
		List<String> values = new ArrayList<>();

		if (lookupTable == null) {
			return values;
		}

		List<LookupTableRowType> rows = lookupTable.getRow();

		if (input == null || input.isEmpty()) {
			for (LookupTableRowType row : rows) {
				values.add(WebComponentUtil.getLocalizedOrOriginPolyStringValue(row.getLabel() != null ? row.getLabel().toPolyString() : null));
			}
		} else {
			for (LookupTableRowType row : rows) {
				if (row.getLabel() == null) {
					continue;
				}
				String rowLabel = WebComponentUtil.getLocalizedOrOriginPolyStringValue(row.getLabel().toPolyString());
				if (rowLabel != null && rowLabel.toLowerCase().contains(input.toLowerCase())) {
					values.add(rowLabel);
				}
			}
		}
		return values;
	}
			
}
