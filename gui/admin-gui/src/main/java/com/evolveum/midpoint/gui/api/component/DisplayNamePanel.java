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

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;

import com.evolveum.midpoint.util.DOMUtil;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class DisplayNamePanel<C extends Containerable> extends BasePanel<C> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DisplayNamePanel.class);

    private final static String ID_TYPE_IMAGE = "typeImage";
    private final static String ID_DISPLAY_NAME = "displayName";
    private final static String ID_IDENTIFIER = "identifier";
    private final static String ID_RELATION = "relation";
    private final static String ID_KIND_INTENT = "kindIntent";
    private final static String ID_DESCRIPTION = "description";
    private final static String ID_DESCRIPTION_LABELS = "descriptionLabels";
    private final static String ID_NAVIGATE_TO_OBJECT = "navigateToObject";

    public DisplayNamePanel(String id, IModel<C> model) {
        super(id, model);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.setOutputMarkupId(true);
        typeImage.add(AttributeModifier.append("class", createImageModel()));
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
                    WebComponentUtil.dispatchToObjectDetailsPage(ort, DisplayNamePanel.this, false);
                }
            }
        };
        navigateToObject.add(new VisibleBehaviour(() -> DisplayNamePanel.this.getModelObject() instanceof ObjectType &&
                WebComponentUtil.getObjectDetailsPage(((ObjectType) DisplayNamePanel.this.getModelObject()).getClass()) != null));
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

//        add(new Label(ID_DESCRIPTION, getDescriptionLabelsModel()));
//        add(new Label(ID_PENDING_OPERATION, getPendingOperationLabelModel()));
    }

    private boolean isObjectPolicyConfigurationType() {
        if (QNameUtil.match(ObjectPolicyConfigurationType.COMPLEX_TYPE, getModelObject().asPrismContainerValue().getComplexTypeDefinition().getTypeName())) {
            return true;
        }
        return false;
    }

    protected String createImageModel() {
        if (getModelObject() == null) {
            return "";
        }
        if (ConstructionType.class.isAssignableFrom(getModelObject().getClass())) {
            return WebComponentUtil.createDefaultColoredIcon(ResourceType.COMPLEX_TYPE);
        }

        return WebComponentUtil.createDefaultColoredIcon(getModelObject().asPrismContainerValue().getComplexTypeDefinition().getTypeName());

    }

    private IModel<String> createHeaderModel() {
        // TODO: align with DisplayNameModel
        return new ReadOnlyModel<String>(() -> {
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

            if (QNameUtil.match(DOMUtil.XSD_STRING,name.getDefinition().getTypeName())) {
                return (String) name.getRealValue();
            } else if (QNameUtil.match(PolyStringType.COMPLEX_TYPE, name.getDefinition().getTypeName())) {
                return WebComponentUtil.getTranslatedPolyString((PolyString) name.getRealValue());
            }

            return name.getRealValue().toString();
        });

    }

    private IModel<String> createIdentifierModel() {
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
        return !WebComponentUtil.isDefaultRelation(getRelation());
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
            return new PropertyModel<String>(getModel(), ObjectType.F_DESCRIPTION.getLocalPart());
        }
        return null;
    }

    protected IModel<List<String>> getDescriptionLabelsModel() {
        List<String> descriptionLabels = new ArrayList<String>();
        IModel<String> des = getDescriptionLabelModel();
        if (des != null) {
            descriptionLabels.add(des.getObject());
        }
        return new IModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return descriptionLabels;
            }
        };
    }

    protected QName getRelation() {
        // To be overridden in subclasses
        return null;
    }
}
