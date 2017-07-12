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
package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.prism.Containerable;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * The same as SummaryTag, but based on Containerable model, not ObjectWrapper one.
 * TODO fix somehow
 *
 * @author semancik
 * @author mederly
 */
public abstract class SummaryTagSimple<C extends Containerable> extends Panel {

	private static final String ID_TAG_ICON = "summaryTagIcon";
	private static final String ID_TAG_LABEL = "summaryTagLabel";

	private boolean initialized = false;
	private String iconCssClass;
	private String label;
	private String color = null;
	private boolean hideTag = false;

	public SummaryTagSimple(String id, final IModel<C> model) {
		super(id, model);
		
		Label tagIcon = new Label(ID_TAG_ICON, "");
		tagIcon.add(new AttributeModifier("class", new SummaryTagModel<String>(model) {
			@Override
			protected String getValue() {
				return getIconCssClass();
			}
		}));
		add(tagIcon);
		
		add(new Label(ID_TAG_LABEL, new SummaryTagModel<String>(model) {
			@Override
			protected String getValue() {
				return getLabel();
			}
		}));
		
		add(new AttributeModifier("style", new SummaryTagModel<String>(model) {
			@Override
			protected String getValue() {
				if (getColor() == null) {
					return null;
				}
				return "color: " + getColor();
			}
		}));
		
		add(new VisibleEnableBehaviour(){    		
            @Override
            public boolean isVisible(){
            	if (!initialized) {
    				initialize(model.getObject());
    			}
            	return !isHideTag();
            }
        });
	}

	public String getIconCssClass() {
		return iconCssClass;
	}

	public void setIconCssClass(String iconCssClass) {
		this.iconCssClass = iconCssClass;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
	public boolean isHideTag() {
		return hideTag;
	}

	public void setHideTag(boolean hideTag) {
		this.hideTag = hideTag;
	}

	protected abstract void initialize(C object);

	abstract class SummaryTagModel<T> extends AbstractReadOnlyModel<T> {

		IModel<C> objectModel;

		public SummaryTagModel(IModel<C> objectModel) {
			this.objectModel = objectModel;
		}

		@Override
		public T getObject() {
			if (!initialized) {
				initialize(objectModel.getObject());
			}
			return getValue();
		}

		protected abstract T getValue();

	}
}
