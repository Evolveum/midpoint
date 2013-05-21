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

package com.evolveum.midpoint.web.component.option;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class OptionItem extends Border {
	
	public OptionItem(String id, IModel<String> title){
		this(id, title, false);
	}

    public OptionItem(String id, IModel<String> title, boolean isHeaderSeparator) {
        super(id);        

        WebMarkupContainer parent = new WebMarkupContainer("parent");
        parent.setOutputMarkupId(true);
        addToBorder(parent);
        
        WebMarkupContainer titleContainer = new WebMarkupContainer("titleContainer");
        titleContainer.setOutputMarkupId(true);
        parent.add(titleContainer);
        
        if(isHeaderSeparator) {
        	titleContainer.add(new AttributeAppender("class", "headerSeparator"));
        }
        
        titleContainer.add(new Label("title", title));
    }
}
