/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.web.component;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.ContextRelativeResource;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * @author semancik
 *
 */
public abstract class FocusSummaryPanel<F extends FocusType> extends Panel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3755521482914447912L;
	private static final String ID_PHOTO = "summaryPhoto";
	private static final String ID_DISPLAY_NAME = "summaryDisplayName";
	private static final String ID_IDENTIFIER = "summaryIdentifier";
	private static final String ID_TITLE = "summaryTitle";
	

	public FocusSummaryPanel(String id, final IModel<PrismObject<F>> model) {
		super(id, model);
		
		add(new Label(ID_DISPLAY_NAME, new PrismPropertyModel<>(model, getDisplayNamePropertyName())));
		add(new Label(ID_IDENTIFIER, new PrismPropertyModel<>(model, getIdentifierPropertyName())));
		if (getTitlePropertyName() == null) {
			add(new Label(ID_TITLE, ""));
		} else {
			add(new Label(ID_TITLE, new PrismPropertyModel<>(model, getTitlePropertyName())));
		}
		
		Image img = new Image(ID_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
                if(model.getObject().asObjectable().getJpegPhoto() != null){
                    return new ByteArrayResource("image/jpeg", model.getObject().asObjectable().getJpegPhoto());
                } else {
                    return new ContextRelativeResource("img/placeholder.png");
                }

            }
        });
        add(img);
	}
	
	protected QName getIdentifierPropertyName() {
		return FocusType.F_NAME;
	}

	protected abstract QName getDisplayNamePropertyName();
	
	protected QName getTitlePropertyName() {
		return null;
	}
	
}
