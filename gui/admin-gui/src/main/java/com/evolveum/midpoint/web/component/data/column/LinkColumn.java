/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author lazyman
 */
public class LinkColumn<T> extends AbstractColumn<T, String> {
	private static final long serialVersionUID = 1L;

	private String propertyExpression;
    
    private String identifier;
    
    private boolean rememberId = false;

    public LinkColumn(IModel<String> displayModel, boolean rememberId) {
        super(displayModel);
        this.rememberId = rememberId;
    }

    
    public LinkColumn(IModel<String> displayModel) {
        super(displayModel);
//        this.rememberId = rememberId;
    }

    public LinkColumn(IModel<String> displayModel, String propertyExpression) {
        this(displayModel, null, propertyExpression);
    }

    public LinkColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty);
        this.propertyExpression = propertyExpression;
    }

    protected String getPropertyExpression() {
        return propertyExpression;
    }

    protected IModel createLinkModel(IModel<T> rowModel) {
        return new PropertyModel<String>(rowModel, propertyExpression);
    }
    
//    protected IModel cre

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
    	
    	IModel model = createLinkModel(rowModel);
    	if (rememberId){
    		FocusType focus = ((FocusType)model.getObject());
    		if (focus != null){
    			identifier = focus.getOid();
    		}
    		model = new PropertyModel<String>(model, FocusType.F_NAME.getLocalPart() + ".orig");
    	}
        cellItem.add(new LinkPanel(componentId, model) {
        	private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                LinkColumn.this.onClick(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return LinkColumn.this.isEnabled(rowModel);
            }
        });
    }

    public boolean isEnabled(IModel<T> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<T> rowModel) {
    }
    
    public void setModelObjectIdentifier(String identifier){
    	this.identifier = identifier;
    }
    
    public String getModelObjectIdentifier(){
    	return identifier;
    }
}
