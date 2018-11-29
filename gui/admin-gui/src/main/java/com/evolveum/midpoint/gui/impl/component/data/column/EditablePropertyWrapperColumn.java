/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.gui.impl.component.data.column;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerValueWrapperModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPropertyColumn;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class EditablePropertyWrapperColumn<C extends Containerable, S> extends AbstractColumn<ContainerValueWrapper<C>, S> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(EditablePropertyWrapperColumn.class);
	
	private QName qNameOfProperty;
	private PageBase page;

	public EditablePropertyWrapperColumn(IModel<String> displayModel, QName item, PageBase page) {
        super(displayModel);
        this.qNameOfProperty = item;
        this.page = page;
    }

    public EditablePropertyWrapperColumn(IModel<String> displayModel, S sortProperty, QName item, PageBase page) {
        super(displayModel, sortProperty);
        this.qNameOfProperty = item;
        this.page = page;
    }

    @Override
    public void populateItem(Item<ICellPopulator<ContainerValueWrapper<C>>> cellItem, String componentId,
            final IModel<ContainerValueWrapper<C>> rowModel) {
        if (!rowModel.getObject().isSelected()) {
        	cellItem.add(createStaticPanel(componentId, rowModel));
        } else {
            cellItem.add(createInputPanel(componentId, rowModel));
        }
    }
    
    protected Component createInputPanel(String componentId, IModel<ContainerValueWrapper<C>> rowModel) {
    	Form form= new Form("form");
    	PropertyWrapperFromContainerValueWrapperModel model = new PropertyWrapperFromContainerValueWrapperModel<>(rowModel, qNameOfProperty);
    	PrismPropertyColumn panel = new PrismPropertyColumn(componentId, model, form, getPageBase());
    	return panel;
    }

    protected Component createStaticPanel(String componentId, IModel<ContainerValueWrapper<C>> rowModel) {
    	Form form= new Form("form");
    	PropertyWrapperFromContainerValueWrapperModel model = new PropertyWrapperFromContainerValueWrapperModel<>(rowModel, qNameOfProperty);
    	
    	if(model.getObject().getPath().namedSegmentsOnly().equivalent(
			    ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER, ClassLoggerConfigurationType.F_APPENDER))){
			if(((PropertyWrapper<AppenderConfigurationType>)model.getObject()).isEmpty()) {
				IModel<String> inheritModel = page.createStringResource("LoggingConfigPanel.appenders.Inherit");
				return new Label(componentId, inheritModel);
			}
    	}
    	
    	PrismPropertyColumn panel = new PrismPropertyColumn<>(componentId, model, form, getPageBase());
    	panel.setEnabled(false);
    	return panel;
    }
    
    private PageBase getPageBase() {
		return page;
	}
}
