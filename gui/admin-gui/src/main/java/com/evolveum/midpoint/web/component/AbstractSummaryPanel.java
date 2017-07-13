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
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyRealValueFromContainerableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;

import javax.xml.namespace.QName;

/**
 * @author semancik
 * @author mederly
 */
public abstract class AbstractSummaryPanel<C extends Containerable> extends BasePanel<C> {
	private static final long serialVersionUID = 1L;

	protected static final String ID_BOX = "summaryBox";
    protected static final String ID_ICON_BOX = "summaryIconBox";
	protected static final String ID_TAG_BOX = "summaryTagBox";
    protected static final String ID_ICON = "summaryIcon";
    protected static final String ID_DISPLAY_NAME = "summaryDisplayName";
    protected static final String ID_IDENTIFIER = "summaryIdentifier";
    protected static final String ID_IDENTIFIER_PANEL = "summaryIdentifierPanel";
    protected static final String ID_TITLE = "summaryTitle";
    protected static final String ID_TITLE2 = "summaryTitle2";
    protected static final String ID_TITLE3 = "summaryTitle3";

    protected static final String ID_PHOTO = "summaryPhoto";                  // perhaps useful only for focal objects but it was simpler to include it here
    protected static final String ID_ORGANIZATION = "summaryOrganization";    // similar (requires ObjectWrapper to get parent organizations so hard to use in ObjectSummaryPanel)

    protected static final String BOX_CSS_CLASS = "info-box";
    protected static final String ICON_BOX_CSS_CLASS = "info-box-icon";

    protected WebMarkupContainer box;
    protected WebMarkupContainer tagBox;
    protected WebMarkupContainer iconBox;

    public AbstractSummaryPanel(String id, IModel<C> model) {
        super(id, model);
        setOutputMarkupId(true);
    }

    protected void initLayoutCommon() {

        box = new WebMarkupContainer(ID_BOX);
        add(box);

        box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));

        box.add(new Label(ID_DISPLAY_NAME, new PrismPropertyRealValueFromContainerableModel<>(getModel(), getDisplayNamePropertyName())));

        WebMarkupContainer identifierPanel = new WebMarkupContainer(ID_IDENTIFIER_PANEL);
        identifierPanel.add(new Label(ID_IDENTIFIER, new PrismPropertyRealValueFromContainerableModel<>(getModel(), getIdentifierPropertyName())));
        identifierPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return isIdentifierVisible();
            }
        });
        box.add(identifierPanel);

        if (getTitlePropertyName() != null) {
            box.add(new Label(ID_TITLE, new PrismPropertyRealValueFromContainerableModel<>(getModel(), getTitlePropertyName())));
        } else if (getTitleModel() != null) {
            box.add(new Label(ID_TITLE, getTitleModel()));
        } else {
            box.add(new Label(ID_TITLE, " "));
        }

        if (getTitle2PropertyName() != null) {
            box.add(new Label(ID_TITLE2, new PrismPropertyRealValueFromContainerableModel<>(getModel(), getTitle2PropertyName())));
        } else if (getTitle2Model() != null) {
            box.add(new Label(ID_TITLE2, getTitle2Model()));
        } else {
            Label label = new Label(ID_TITLE2, " ");
            label.setVisible(false);
            box.add(label);
        }

		if (getTitle3PropertyName() != null) {
			box.add(new Label(ID_TITLE3, new PrismPropertyRealValueFromContainerableModel<>(getModel(), getTitle3PropertyName())));
		} else if (getTitle3Model() != null) {
			box.add(new Label(ID_TITLE3, getTitle3Model()));
		} else {
			Label label = new Label(ID_TITLE3, " ");
			label.setVisible(false);
			box.add(label);
		}

        Label parentOrgLabel = new Label(ID_ORGANIZATION, getParentOrgModel());
        parentOrgLabel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getParentOrgModel().getObject() != null;
            }
        });
        box.add(parentOrgLabel);

        iconBox = new WebMarkupContainer(ID_ICON_BOX);
        box.add(iconBox);

        if (getIconBoxAdditionalCssClass() != null) {
            iconBox.add(new AttributeModifier("class", ICON_BOX_CSS_CLASS + " " + getIconBoxAdditionalCssClass()));
        }

        Label icon = new Label(ID_ICON, "");
        icon.add(new AttributeModifier("class", getIconCssClass()));
        icon.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible(){
                return getPhotoModel().getObject() == null;
            }
        });
        iconBox.add(icon);
        NonCachingImage img = new NonCachingImage(ID_PHOTO, getPhotoModel());
        img.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return getPhotoModel().getObject() != null;
            }
        });
        iconBox.add(img);

		tagBox = new WebMarkupContainer(ID_TAG_BOX);
		if (getTagBoxCssClass() != null) {
			tagBox.add(new AttributeModifier("class", getTagBoxCssClass()));
		}
		box.add(tagBox);
    }

	protected String getTagBoxCssClass() {
		return null;
	}

	public void addTag(Component tag) {
        tagBox.add(tag);
    }

	public Component getTag(String id) {
		return tagBox.get(id);
	}

	protected abstract String getIconCssClass();

    protected abstract String getIconBoxAdditionalCssClass();

    protected abstract String getBoxAdditionalCssClass();

    protected QName getIdentifierPropertyName() {
        return FocusType.F_NAME;
    }

    protected QName getDisplayNamePropertyName() {
		return ObjectType.F_NAME;
	}

    protected QName getTitlePropertyName() {
        return null;
    }

    protected IModel<String> getTitleModel() {
        return null;
    }

    protected QName getTitle2PropertyName() {
        return null;
    }

	protected IModel<String> getTitle2Model() {
		return null;
	}

	protected QName getTitle3PropertyName() {
		return null;
	}

	protected IModel<String> getTitle3Model() {
		return null;
	}

	protected boolean isIdentifierVisible() {
        return true;
    }

    protected IModel<String> getParentOrgModel() {
        return new Model<>(null);
    }

    protected IModel<AbstractResource> getPhotoModel() {
        return new Model<>(null);
    }

}
