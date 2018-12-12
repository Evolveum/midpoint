/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.component.prism;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.PrismValuePanel2;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ExpressionWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.PrismHeaderPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.List;

/**
 * @author lazyman
 */
public class PrismPropertyPanel<IW extends ItemWrapper> extends AbstractPrismPropertyPanel<IW> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);

    private boolean labelContainerVisible = true;
    
    public PrismPropertyPanel(String id, IModel<IW> model, Form form, ItemVisibilityHandler visibilityHandler,
			PageBase pageBase) {
		super(id, model, form, visibilityHandler, pageBase);
	}
    
    protected WebMarkupContainer getHeader(String idComponent) {
    	PrismPropertyHeaderPanel<IW> headerLabel = new PrismPropertyHeaderPanel<IW>(idComponent, getModel(), getPageBase()) {
			private static final long serialVersionUID = 1L;

			@Override
    		public String getContainerLabelCssClass() {
				return " col-md-2 col-xs-12 prism-property-label ";
    		}
    	};
    	headerLabel.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override public boolean isVisible() {
                return labelContainerVisible;
            }
        });
    	
    	return headerLabel;
    }
    
    protected WebMarkupContainer getValues(String idComponent, final IModel<IW> model, final Form form) {
    	IModel<String> label = WebComponentUtil.getDisplayName((IModel<ItemWrapper>)model, PrismPropertyPanel.this);
        ListView<ValueWrapper> values = new ListView<ValueWrapper>(idComponent,
            new PropertyModel<>(model, "values")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<ValueWrapper> item) {
                BasePanel panel;
                ItemWrapper itemWrapper = item.getModelObject().getItem();
                if ((itemWrapper.getPath().containsNameExactly(ConstructionType.F_ASSOCIATION) ||
                                itemWrapper.getPath().containsNameExactly(ConstructionType.F_ATTRIBUTE))&&
                        itemWrapper.getPath().containsNameExactly(ResourceObjectAssociationType.F_OUTBOUND) &&
                        itemWrapper.getPath().containsNameExactly(MappingType.F_EXPRESSION)){
                    ExpressionWrapper expressionWrapper = (ExpressionWrapper)item.getModelObject().getItem();
                    panel = new ExpressionValuePanel("value", new PropertyModel(item.getModel(), "value.value"),
                            expressionWrapper.getConstruction(), getPageBase()){
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected boolean isAssociationExpression(){
                            return itemWrapper.getPath().containsNameExactly(ConstructionType.F_ASSOCIATION);
                        }
                    };
                } else {
                    panel = new PrismValuePanel2("value", item.getModel(), label, form, getValueCssClass(), getInputCssClass(), getButtonsCssClass());
                }
                item.add(panel);
                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));

                item.add(new VisibleEnableBehaviour() {
                	private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return isVisibleValue(item.getModel());
                    }
                });
            }
        };
        values.add(new AttributeModifier("class", getValuesClass()));
        values.setReuseItems(true);
        return values;
    }

    protected String getInputCssClass() {
        return"col-xs-10";
    }
    
    protected String getButtonsCssClass() {
        return"col-xs-2";
    }

    protected String getValuesClass() {
        return "col-md-6";
    }

    protected String getValueCssClass() {
        return "row";
    }

    protected IModel<String> createStyleClassModel(final IModel<ValueWrapper> value) {
        return new IModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (getIndexOfValue(value.getObject()) > 0) {
                    return getItemCssClass();
                }

                return null;
            }
        };
    }
    
    protected String getItemCssClass() {
    	return " col-md-offset-2 prism-value ";
    }

    private int getIndexOfValue(ValueWrapper value) {
        ItemWrapper property = value.getItem();
        List<ValueWrapper> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isVisibleValue(IModel<ValueWrapper> model) {
        ValueWrapper value = model.getObject();
        return !ValueStatus.DELETED.equals(value.getStatus());
    }

    public boolean isLabelContainerVisible() {
        return labelContainerVisible;
    }

    public void setLabelContainerVisible(boolean labelContainerVisible) {
        this.labelContainerVisible = labelContainerVisible;
    }
}
