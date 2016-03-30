package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyRealValueFromPrismObjectModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.AbstractResource;

import javax.xml.namespace.QName;

/**
 * @author semancik
 * @author mederly
 */
public abstract class AbstractSummaryPanel<O extends ObjectType> extends BasePanel<PrismObject<O>> {

    protected static final String ID_BOX = "summaryBox";
    protected static final String ID_ICON_BOX = "summaryIconBox";
    protected static final String ID_ICON = "summaryIcon";
    protected static final String ID_DISPLAY_NAME = "summaryDisplayName";
    protected static final String ID_IDENTIFIER = "summaryIdentifier";
    protected static final String ID_IDENTIFIER_PANEL = "summaryIdentifierPanel";
    protected static final String ID_TITLE = "summaryTitle";
    protected static final String ID_TITLE2 = "summaryTitle2";

    protected static final String ID_PHOTO = "summaryPhoto";                  // perhaps useful only for focal objects but it was simpler to include it here
    protected static final String ID_ORGANIZATION = "summaryOrganization";    // similar (requires ObjectWrapper to get parent organizations so hard to use in ObjectSummaryPanel)
    protected static final String ID_FIRST_SUMMARY_TAG = "firstSummaryTag";

    protected static final String BOX_CSS_CLASS = "info-box";
    protected static final String ICON_BOX_CSS_CLASS = "info-box-icon";

    protected WebMarkupContainer box;
    protected WebMarkupContainer iconBox;

    public AbstractSummaryPanel(String id, IModel<PrismObject<O>> model) {
        super(id, model);
    }

    protected void initLayoutCommon() {

        box = new WebMarkupContainer(ID_BOX);
        add(box);

        box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));

        box.add(new Label(ID_DISPLAY_NAME, new PrismPropertyRealValueFromPrismObjectModel<>(getModel(), getDisplayNamePropertyName())));

        WebMarkupContainer identifierPanel = new WebMarkupContainer(ID_IDENTIFIER_PANEL);
        identifierPanel.add(new Label(ID_IDENTIFIER, new PrismPropertyRealValueFromPrismObjectModel<>(getModel(), getIdentifierPropertyName())));
        identifierPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return isIdentifierVisible();
            }
        });
        box.add(identifierPanel);

        if (getTitlePropertyName() != null) {
            box.add(new Label(ID_TITLE, new PrismPropertyRealValueFromPrismObjectModel<>(getModel(), getTitlePropertyName())));
        } else if (getTitleModel() != null) {
            box.add(new Label(ID_TITLE, getTitleModel()));
        } else {
            box.add(new Label(ID_TITLE, " "));
        }

        if (getTitle2PropertyName() != null) {
            box.add(new Label(ID_TITLE2, new PrismPropertyRealValueFromPrismObjectModel<>(getModel(), getTitle2PropertyName())));
        } else if (getTitle2Model() != null) {
            box.add(new Label(ID_TITLE2, getTitle2Model()));
        } else {
            Label label = new Label(ID_TITLE2, " ");
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
        Image img = new Image(ID_PHOTO, getPhotoModel());
        img.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return getPhotoModel().getObject() != null;
            }
        });
        iconBox.add(img);
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

    protected IModel<String> getTitleModel() {
        return null;
    }

    protected QName getTitle2PropertyName() {
        return null;
    }

    protected IModel<String> getTitle2Model() {
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
