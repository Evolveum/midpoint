/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
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
