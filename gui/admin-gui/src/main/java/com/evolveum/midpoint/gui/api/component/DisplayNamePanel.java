/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;

import com.evolveum.midpoint.gui.impl.util.RelationUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class DisplayNamePanel<C extends Containerable> extends BasePanel<C> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_RELATION = "relation";
    private static final String ID_KIND_INTENT = "kindIntent";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DESCRIPTION_LABELS = "descriptionLabels";
    private static final String ID_NAVIGATE_TO_OBJECT = "navigateToObject";
    private static final String ID_PANEL_STATUS = "panelStatus";

    public DisplayNamePanel(String id, IModel<C> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        String statusId = get(ID_PANEL_STATUS).getMarkupId();
        String displayName = get(ID_DISPLAY_NAME).getDefaultModelObjectAsString();
        String message = getString("DisplayNamePanel.opened", displayName);
        response.render(OnDomReadyHeaderItem.forScript(
                String.format("MidPointTheme.updateStatusMessage('%s', '%s', %d)", statusId, message, 250)));
    }

    private void initLayout() {
        Label panelStatus = new Label(ID_PANEL_STATUS, Model.of(""));
        panelStatus.setOutputMarkupId(true);
        add(panelStatus);

        WebMarkupContainer typeImage = createTypeImagePanel(ID_TYPE_IMAGE);
        add(typeImage);

        Label name = new Label(ID_DISPLAY_NAME, createHeaderModel());
        name.setOutputMarkupId(true);
        add(name);

        Label identifier = new Label(ID_IDENTIFIER, createIdentifierModel());
        identifier.setOutputMarkupId(true);
        identifier.add(new VisibleBehaviour(() -> isIdentifierVisible()));
        add(identifier);

        AjaxButton navigateToObject = new AjaxButton(ID_NAVIGATE_TO_OBJECT) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (DisplayNamePanel.this.getModelObject() instanceof ObjectType) {
                    ObjectType o = (ObjectType) DisplayNamePanel.this.getModelObject();
                    ObjectReferenceType ort = new ObjectReferenceType();
                    ort.setOid(o.getOid());
                    ort.setType(WebComponentUtil.classToQName(DisplayNamePanel.this.getPageBase().getPrismContext(), o.getClass()));
                    DetailsPageUtil.dispatchToObjectDetailsPage(ort, DisplayNamePanel.this, false);
                }
            }
        };
        navigateToObject.add(new VisibleBehaviour(() ->  hasDetailsPage()));
        navigateToObject.setOutputMarkupId(true);
        add(navigateToObject);

        Label relation = new Label(ID_RELATION, Model.of(getRelationLabel()));
        relation.setOutputMarkupId(true);
        relation.add(new VisibleBehaviour(() -> isRelationVisible()));
        add(relation);

        IModel<String> kindIntentLabelModel = getKindIntentLabelModel();
        Label kindIntent = new Label(ID_KIND_INTENT, kindIntentLabelModel);
        kindIntent.setOutputMarkupId(true);
        kindIntent.add(new VisibleBehaviour(() -> isKindIntentVisible(kindIntentLabelModel)));
        add(kindIntent);

        ListView<String> descriptionLabels = new ListView<String>(ID_DESCRIPTION_LABELS, getDescriptionLabelsModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_DESCRIPTION, item.getModel()));
            }
        };

        add(descriptionLabels);
    }

    private boolean hasDetailsPage() {
        C containerable = getModelObject();
        if (!(containerable instanceof ObjectType)) {
            return false;
        }
        if (StringUtils.isBlank(((ObjectType) containerable).getOid())) {
            return false;
        }
        return DetailsPageUtil.hasDetailsPage(containerable.getClass());
    }

    protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
        WebMarkupContainer typeImage = new WebMarkupContainer(idTypeImage);
        typeImage.setOutputMarkupId(true);
        typeImage.add(AttributeModifier.append("class", createImageModel()));
        return typeImage;
    }

    private boolean isObjectPolicyConfigurationType() {
        return QNameUtil.match(
                ObjectPolicyConfigurationType.COMPLEX_TYPE,
                getModelObject().asPrismContainerValue().getComplexTypeDefinition().getTypeName());
    }

    protected String createImageModel() {
        if (getModelObject() == null) {
            return "";
        }
        if (ConstructionType.class.isAssignableFrom(getModelObject().getClass())) {
            return IconAndStylesUtil.createDefaultColoredIcon(ResourceType.COMPLEX_TYPE);
        }

        return IconAndStylesUtil.createDefaultColoredIcon(getModelObject().asPrismContainerValue().getComplexTypeDefinition().getTypeName());

    }

    protected IModel<String> createHeaderModel() {
        // TODO: align with DisplayNameModel
        return new ReadOnlyModel<>(() -> {
            if (getModelObject() == null) {
                return "";
            }
            if (ObjectType.class.isAssignableFrom(getModelObject().getClass())) {
                return WebComponentUtil.getEffectiveName((ObjectType) getModelObject(), AbstractRoleType.F_DISPLAY_NAME);
            }
            if (isObjectPolicyConfigurationType()) {
                QName typeValue = WebComponentUtil.getValue(getModel().getObject().asPrismContainerValue(), ObjectPolicyConfigurationType.F_TYPE, QName.class);
                ObjectReferenceType objectTemplate = ((ObjectPolicyConfigurationType) getModel().getObject()).getObjectTemplateRef();
                if (objectTemplate == null || objectTemplate.getTargetName() == null) {
                    return "";
                }
                String objectTemplateNameValue = objectTemplate.getTargetName().toString();
                StringBuilder sb = new StringBuilder();
                if (typeValue != null) {
                    sb.append(typeValue.getLocalPart()).append(" - ");
                }
                sb.append(objectTemplateNameValue);
                return sb.toString();
            }
            PrismProperty<?> name = getModelObject().asPrismContainerValue().findProperty(ObjectType.F_NAME);
            if (name == null || name.isEmpty()) {
                return "";
            }

            if (QNameUtil.match(DOMUtil.XSD_STRING, name.getDefinition().getTypeName())) {
                return (String) name.getRealValue();
            } else if (QNameUtil.match(PolyStringType.COMPLEX_TYPE, name.getDefinition().getTypeName())) {
                return LocalizationUtil.translatePolyString((PolyString) name.getRealValue());
            }

            return name.getRealValue().toString();
        });

    }

    protected IModel<String> createIdentifierModel() {
        if (getModelObject() == null) {
            return Model.of("");
        }
        if (AbstractRoleType.class.isAssignableFrom(getModelObject().getClass())) {
            return Model.of(WebComponentUtil.getEffectiveName((ObjectType) getModelObject(), AbstractRoleType.F_IDENTIFIER));
        }
        return Model.of("");
    }

    private boolean isIdentifierVisible() {
        if (getModelObject() == null) {
            return false;
        }
        if (AbstractRoleType.class.isAssignableFrom(getModelObject().getClass())) {
            return getModelObject().asPrismContainerValue().findProperty(AbstractRoleType.F_IDENTIFIER) != null;
        }
        return false;
    }

    // TODO: maybe move relation methods to subclass if we want this panel to be really reusable

    private boolean isRelationVisible() {
        return !RelationUtil.isDefaultRelation(getRelation());
    }

    private boolean isKindIntentVisible(IModel<String> kindIntentLabelModel) {
        return kindIntentLabelModel != null && StringUtils.isNotEmpty(kindIntentLabelModel.getObject());
    }

    private String getRelationLabel() {
        QName relation = getRelation();
        if (relation == null) {
            return "";
        }
        // TODO: localization?
        return relation.getLocalPart();
    }

    protected IModel<String> getKindIntentLabelModel() {
        // To be overridden in subclasses
        return Model.of("");
    }

    protected IModel<String> getDescriptionLabelModel() {
        if (getModel().getObject() != null && getModel().getObject().asPrismContainerValue().contains(ObjectType.F_DESCRIPTION)) {
            return new PropertyModel<>(getModel(), ObjectType.F_DESCRIPTION.getLocalPart());
        }
        return null;
    }

    protected IModel<List<String>> getDescriptionLabelsModel() {
        List<String> descriptionLabels = new ArrayList<>();
        IModel<String> des = getDescriptionLabelModel();
        if (des != null) {
            descriptionLabels.add(des.getObject());
        }
        return (IModel<List<String>>) () -> descriptionLabels;
    }

    protected QName getRelation() {
        // To be overridden in subclasses
        return null;
    }
}
