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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyRealValueFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ReadOnlyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author semancik
 *
 */
public abstract class FocusSummaryPanel<O extends ObjectType> extends Panel {	
	private static final long serialVersionUID = -3755521482914447912L;
	
	private static final String ID_BOX = "summaryBox";
	private static final String ID_ICON_BOX = "summaryIconBox";
	private static final String ID_PHOTO = "summaryPhoto";
	private static final String ID_ICON = "summaryIcon";
	private static final String ID_DISPLAY_NAME = "summaryDisplayName";
	private static final String ID_IDENTIFIER = "summaryIdentifier";
	private static final String ID_TITLE = "summaryTitle";
	private static final String ID_ORGANIZATION = "summaryOrganization";
	private static final String ID_TAG_ACTIVATION = "summaryTagActivation";
	
	private static final String BOX_CSS_CLASS = "info-box";
	private static final String ICON_BOX_CSS_CLASS = "info-box-icon";

	protected static final String ICON_CLASS_ACTIVATION_ACTIVE = "fa fa-check";
	protected static final String ICON_CLASS_ACTIVATION_INACTIVE = "fa fa-times";
	
	private WebMarkupContainer box;

	public FocusSummaryPanel(String id, final IModel<ObjectWrapper<O>> model) {
		super(id, model);
		
		box = new WebMarkupContainer(ID_BOX);
		add(box);
		
		box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));
		
		box.add(new Label(ID_DISPLAY_NAME, new PrismPropertyRealValueFromObjectWrapperModel<>(model, getDisplayNamePropertyName())));
		box.add(new Label(ID_IDENTIFIER, new PrismPropertyRealValueFromObjectWrapperModel<>(model, getIdentifierPropertyName())));
		if (getTitlePropertyName() == null) {
			box.add(new Label(ID_TITLE, " "));
		} else {
			box.add(new Label(ID_TITLE, new PrismPropertyRealValueFromObjectWrapperModel<>(model, getTitlePropertyName(), " ")));
		}
		
		box.add(new Label(ID_ORGANIZATION, new ReadOnlyWrapperModel<String,O>(model) {
			@Override
			public String getObject() {
				Collection<PrismObject<OrgType>> parentOrgs = getWrapper().getParentOrgs();
				if (parentOrgs.isEmpty()) {
					return "";
				}
				// Kinda hack now .. "functional" orgType always has preference
				// this whole thing should be driven by an expression later on
				for (PrismObject<OrgType> org: parentOrgs) {
					OrgType orgType = org.asObjectable();
					if (orgType.getOrgType().contains("functional")) {
						return PolyString.getOrig(orgType.getDisplayName());
					}
				}
				// Just use the first one as a fallback
				return PolyString.getOrig(parentOrgs.iterator().next().asObjectable().getDisplayName());
			}
		}));
		
		SummaryTag<O> tagActivation = new SummaryTag<O>(ID_TAG_ACTIVATION, model) {
			@Override
			protected void initialize(ObjectWrapper<O> wrapper) {
				ActivationType activation = null;
				O object = wrapper.getObject().asObjectable();
				if (object instanceof FocusType) {
					activation = ((FocusType)object).getActivation();
				}
				if (activation == null) {
					setIconCssClass(ICON_CLASS_ACTIVATION_ACTIVE);
					setLabel("Active");
					setColor("green");
				} else if (activation.getEffectiveStatus() == ActivationStatusType.ENABLED) {
					setIconCssClass(ICON_CLASS_ACTIVATION_ACTIVE);
					setLabel("Active");
					setColor("green");
				} else {
					setIconCssClass(ICON_CLASS_ACTIVATION_INACTIVE);
					setLabel("Inactive");
					setColor("red");
				}
			}
		};
		addTag(tagActivation);
		
		WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
		box.add(iconBox);
		
		if (getIconBoxAdditionalCssClass() != null) {
			iconBox.add(new AttributeModifier("class", ICON_BOX_CSS_CLASS + " " + getIconBoxAdditionalCssClass()));
		}
		
		Image img = new Image(ID_PHOTO, new AbstractReadOnlyModel<AbstractResource>() {

            @Override
            public AbstractResource getObject() {
            	byte[] jpegPhoto = null;
            	O object = model.getObject().getObject().asObjectable();
				if (object instanceof FocusType) {
					jpegPhoto = ((FocusType)object).getJpegPhoto();
				}
                if(jpegPhoto == null) {
                	return null;
                } else {
                    return new ByteArrayResource("image/jpeg",jpegPhoto);
                }
            }
        });
		img.add(new VisibleEnableBehaviour(){    		
            @Override
            public boolean isVisible(){
            	O object = model.getObject().getObject().asObjectable();
				if (object instanceof FocusType) {
					if (((FocusType)object).getJpegPhoto() != null) {
						return true;
					}
				}
            	return false;
            }
        });
		iconBox.add(img);
        
        Label icon = new Label(ID_ICON,"");
        icon.add(new AttributeModifier("class", getIconCssClass()));
        icon.add(new VisibleEnableBehaviour(){    		
            @Override
            public boolean isVisible(){
            	O object = model.getObject().getObject().asObjectable();
				if (object instanceof FocusType) {
					if (((FocusType)object).getJpegPhoto() != null) {
						return false;
					}
				}
            	return true;
            }
        });
        iconBox.add(icon);
	}
	
	public void addTag(SummaryTag<O> tag) {
		box.add(tag);
	}
	
	protected abstract String getIconCssClass();
	
	protected abstract String getIconBoxAdditionalCssClass();
	
	protected abstract String getBoxAdditionalCssClass();

	protected QName getIdentifierPropertyName() {
		return FocusType.F_NAME;
	}

	protected abstract QName getDisplayNamePropertyName();
	
	protected QName getTitlePropertyName() {
		return null;
	}
	
}
