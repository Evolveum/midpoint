/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.AbstractResource;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.model.FlexibleLabelModel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public abstract class AbstractSummaryPanel<C extends Containerable> extends BasePanel<C> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSummaryPanel.class);

    protected static final String ID_BOX = "summaryBox";
    protected static final String ID_ICON_BOX = "summaryIconBox";
    protected static final String ID_TAG_BOX = "summaryTagBox";
    protected static final String ID_SUMMARY_TAG = "summaryTag";
    protected static final String ID_ICON = "summaryIcon";
    protected static final String ID_SR_MESSAGE_FOR_SUMMARY_PANEL = "srMessageForSummaryPanel";
    protected static final String ID_DISPLAY_NAME = "summaryDisplayName";
    protected static final String ID_IDENTIFIER = "summaryIdentifier";
    protected static final String ID_IDENTIFIER_PANEL = "summaryIdentifierPanel";
    protected static final String ID_NAVIGATE_TO_OBJECT_BUTTON = "navigateToObject";
    protected static final String ID_TITLE = "summaryTitle";
    protected static final String ID_TITLE2 = "summaryTitle2";
    protected static final String ID_TITLE3 = "summaryTitle3";
    protected static final String ID_BADGES = "badges";
    protected static final String ID_MARKS = "marks";

    protected static final String ID_PHOTO = "summaryPhoto";                  // perhaps useful only for focal objects but it was simpler to include it here
    protected static final String ID_ORGANIZATION = "summaryOrganization";    // similar (requires ObjectWrapper to get parent organizations so hard to use in ObjectSummaryPanel)

    protected static final String BOX_CSS_CLASS = "col-xs-12 info-box";
    protected static final String ICON_BOX_CSS_CLASS = "info-box-icon";

    protected static final String ID_SR_MESSAGE_FOR_DISPLAY_NAME = "srMessageForSummaryDisplayName";
    protected static final String ID_SR_MESSAGE_FOR_IDENTIFIER = "srMessageForSummaryIdentifier";
    protected static final String ID_SR_MESSAGE_FOR_BADGES = "srMessageForBadges";
    protected static final String ID_SR_MESSAGE_FOR_TITLE = "srMessageForSummaryTitle";
    protected static final String ID_SR_MESSAGE_FOR_ORGANIZATION = "srMessageForSummaryOrganization";
    protected static final String ID_SR_MESSAGE_FOR_TAG_BOX = "srMessageForSummaryTagBox";

    protected SummaryPanelSpecificationType configuration;

    protected WebMarkupContainer box;
    protected RepeatingView tagBox;
    protected WebMarkupContainer iconBox;

    public AbstractSummaryPanel(String id, IModel<C> model, SummaryPanelSpecificationType configuration) {
        super(id, model);
        this.configuration = configuration;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        box = new WebMarkupContainer(ID_BOX);
        add(box);

        IModel<String> archetypePolicyAdditionalCssClassModel = () -> {
            String archetypePolicyAdditionalCssClass = getArchetypePolicyAdditionalCssClass();
            if (archetypePolicyAdditionalCssClass == null) {
                return "";
            }
            return "border-color: " + archetypePolicyAdditionalCssClass + ";";
        };

        box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));
        box.add(AttributeModifier.append("style", archetypePolicyAdditionalCssClassModel));

        if (getDisplayNameModel() != null) {
            box.add(new Label(ID_DISPLAY_NAME, getDisplayNameModel()));
        } else if (getDisplayNamePropertyName() != null) {
            box.add(new Label(ID_DISPLAY_NAME, createLabelModel(getDisplayNamePropertyName(), SummaryPanelSpecificationType.F_DISPLAY_NAME)));
        } else {
            Label displayName = new Label(ID_DISPLAY_NAME, " ");
            displayName.add(VisibleBehaviour.ALWAYS_INVISIBLE);
            box.add(displayName);
        }


        ObjectTypes type = ObjectTypes.getObjectTypeIfKnown(getModelObject().getClass());
        IModel<String> messageModel;
        if (type != null) {
            String typeMessage = getPageBase().createStringResource(type).getString();
            messageModel = getPageBase().createStringResource("AbstractSummaryPanel.srMessageWithType", typeMessage);
        } else {
            messageModel = getPageBase().createStringResource("AbstractSummaryPanel.srMessage");
        }
        box.add(new Label(ID_SR_MESSAGE_FOR_SUMMARY_PANEL, messageModel));

        box.add(new Label(ID_SR_MESSAGE_FOR_DISPLAY_NAME, new ResourceModel("AbstractSummaryPanel.srMessageDisplayName"))
                .setMarkupId(ID_SR_MESSAGE_FOR_DISPLAY_NAME)
                .setOutputMarkupId(true));

        WebMarkupContainer identifierPanel = new WebMarkupContainer(ID_IDENTIFIER_PANEL);
        Label identifier = new Label(ID_IDENTIFIER, createLabelModel(getIdentifierPropertyName(), SummaryPanelSpecificationType.F_IDENTIFIER));
        identifier.setRenderBodyOnly(true);
        identifierPanel.add(identifier);
        identifierPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isIdentifierVisible();
            }
        });
        box.add(identifierPanel);

        box.add(new Label(ID_SR_MESSAGE_FOR_IDENTIFIER, new ResourceModel("AbstractSummaryPanel.srMessageIdentifier"))
                .setMarkupId(ID_SR_MESSAGE_FOR_IDENTIFIER)
                .setOutputMarkupId(true));


        IModel<List<Badge>> badgesModel = createBadgesModel();
        BadgeListPanel badges = new BadgeListPanel(ID_BADGES, badgesModel);
        badges.add(new VisibleBehaviour(() -> !badgesModel.getObject().isEmpty()));
        box.add(badges);
        box.add(new Label(ID_SR_MESSAGE_FOR_BADGES, new ResourceModel("AbstractSummaryPanel.srMessageBadges"))
                .setMarkupId(ID_SR_MESSAGE_FOR_BADGES)
                .setOutputMarkupId(true));

        AjaxButton navigateToObject = new AjaxButton(ID_NAVIGATE_TO_OBJECT_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                ObjectReferenceType ort = getReferencedObjectToNavigate();
                DetailsPageUtil.dispatchToObjectDetailsPage(ort, AbstractSummaryPanel.this, false);
            }
        };
        navigateToObject.add(AttributeAppender.append("title", getReferenceObjectTitleModel()));
        navigateToObject.add(new VisibleBehaviour(() -> {
            ObjectReferenceType ort = getReferencedObjectToNavigate();
            Class refType = !isReferencedObjectNull()
                    ? WebComponentUtil.qnameToClass(ort.getType())
                    : null;
            return ort != null && refType != null
                    && DetailsPageUtil.getObjectDetailsPage(refType) != null;
        }));
        navigateToObject.setOutputMarkupId(true);
        box.add(navigateToObject);

        addTitle(box, getTitleModel(), getTitlePropertyName(), SummaryPanelSpecificationType.F_TITLE1, ID_TITLE);
        addTitle(box, getTitle2Model(), getTitle2PropertyName(), SummaryPanelSpecificationType.F_TITLE2, ID_TITLE2);
        addTitle(box, getTitle3Model(), getTitle3PropertyName(), SummaryPanelSpecificationType.F_TITLE3, ID_TITLE3);

        box.add(new Label(ID_SR_MESSAGE_FOR_TITLE, new ResourceModel("AbstractSummaryPanel.srMessageTitles"))
                .setMarkupId(ID_SR_MESSAGE_FOR_TITLE)
                .setOutputMarkupId(true));

        final IModel<String> parentOrgModel = getParentOrgModel();
        Label parentOrgLabel = new Label(ID_ORGANIZATION, parentOrgModel);
        parentOrgLabel.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(parentOrgModel.getObject())));
        box.add(parentOrgLabel);

        box.add(new Label(ID_SR_MESSAGE_FOR_ORGANIZATION, new ResourceModel("AbstractSummaryPanel.srMessageParentOrganization"))
                .setMarkupId(ID_SR_MESSAGE_FOR_ORGANIZATION)
                .setOutputMarkupId(true));

        IModel<String> marksModel = createMarksModel();
        Label marks = new Label(ID_MARKS, marksModel);
        marks.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(marksModel.getObject())));
        box.add(marks);

        iconBox = new WebMarkupContainer(ID_ICON_BOX);
        box.add(iconBox);

        String iconAdditionalCssClass = getIconBoxAdditionalCssClass();
        if (StringUtils.isNotEmpty(iconAdditionalCssClass)) {
            iconBox.add(new AttributeModifier("class", ICON_BOX_CSS_CLASS + " " + iconAdditionalCssClass));
        }

        iconBox.add(AttributeModifier.append("style", createArchetypeBackgroundModel()));

        Label icon = new Label(ID_ICON, "");

        icon.add(AttributeModifier.append("class", getIconCssClass()));
        icon.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getPhotoModel().getObject() == null;
            }
        });
        iconBox.add(icon);
        NonCachingImage img = new NonCachingImage(ID_PHOTO, getPhotoModel());
        img.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getPhotoModel().getObject() != null;
            }
        });
        iconBox.add(img);

        tagBox = new RepeatingView(ID_TAG_BOX);
        List<SummaryTag<C>> summaryTags = getSummaryTagComponentList();

        summaryTags.add(getArchetypeSummaryTag());
        summaryTags.forEach(summaryTag -> {
            WebMarkupContainer summaryTagPanel = new WebMarkupContainer(tagBox.newChildId());
            summaryTagPanel.setOutputMarkupId(true);

            summaryTagPanel.add(summaryTag);
            tagBox.add(summaryTagPanel);
        });
        if (getTagBoxCssClass() != null) {
            tagBox.add(new AttributeModifier("class", getTagBoxCssClass()));
        }
        tagBox.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(summaryTags)));
        box.add(tagBox);

        box.add(new Label(ID_SR_MESSAGE_FOR_TAG_BOX, new ResourceModel("AbstractSummaryPanel.srMessageTags"))
                .setMarkupId(ID_SR_MESSAGE_FOR_TAG_BOX)
                .setOutputMarkupId(true));
    }

    private void addTitle(
            WebMarkupContainer box,
            IModel<String> titleModel,
            QName titlePropertyName,
            ItemName configurationPropertyName,
            String id) {
        IModel<String> labelModel;
        if (titleModel != null) {
            labelModel = titleModel;
        } else if (getTitlePropertyName() != null) {
            labelModel = createLabelModel(titlePropertyName, configurationPropertyName);
        } else {
            labelModel = Model.of(" ");
        }
        Label title = new Label(id, labelModel);
        title.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(labelModel.getObject())));
        box.add(title);
    }

    protected IModel<String> createMarksModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                C c = getModelObject();
                if (!(c instanceof ObjectType object)) {
                    return null;
                }

                List<ObjectReferenceType> refs = object.getEffectiveMarkRef();

                return WebComponentUtil.createMarkList(refs, getPageBase());
            }
        };
    }

    protected IModel<List<Badge>> createBadgesModel() {
        return Model.ofList(new ArrayList<>());
    }

    private IModel<String> getIconCssClass() {
        return () -> {
            String archetypeIcon = getArchetypeIconCssClass();
            if (StringUtils.isNotBlank(archetypeIcon)) {
                return archetypeIcon;
            }

            return getDefaultIconCssClass();
        };
    }

    private IModel<String> createArchetypeBackgroundModel() {
        return () -> {

            String archetypePolicyAdditionalCssClass = getArchetypePolicyAdditionalCssClass();
            if (archetypePolicyAdditionalCssClass == null) {
                return "";
            }
            return "background-color: " + archetypePolicyAdditionalCssClass + ";";
        };
    }

    private FlexibleLabelModel<C> createLabelModel(QName modelPropertyName, QName configurationPropertyName) {
        return createFlexibleLabelModel(modelPropertyName, getLabelConfiguration(configurationPropertyName));
    }

    private FlexibleLabelModel<C> createFlexibleLabelModel(QName modelPropertyName, GuiFlexibleLabelType configuration) {
        return new FlexibleLabelModel<>(getModel(), ItemName.fromQName(modelPropertyName), getPageBase(), configuration) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void addAdditionalVariablesMap(VariablesMap variables) {
                super.addAdditionalVariablesMap(variables);
                AbstractSummaryPanel.this.addAdditionalVariablesMap(variables);
            }
        };
    }

    protected List<SummaryTag<C>> getSummaryTagComponentList() {
        return new ArrayList<>();
    }

    protected ObjectReferenceType getReferencedObjectToNavigate() {
        return null;
    }

    private boolean isReferencedObjectNull() {
        return getReferencedObjectToNavigate() == null || StringUtils.isEmpty(getReferencedObjectToNavigate().getOid()) ||
                getReferencedObjectToNavigate().getType() == null;
    }

    protected IModel<String> getReferenceObjectTitleModel() {
        return null;
    }

    private SummaryTag<C> getArchetypeSummaryTag() {
        IModel<String> archetypeLabelModel = new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return getArchetypeLabel();
            }
        };

        SummaryTag<C> archetypeSummaryTag = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(C object) {
                setIconCssClass(getArchetypeIconCssClass());
                setLabel(archetypeLabelModel.getObject());
                setColor(getArchetypePolicyAdditionalCssClass());
            }

        };
        archetypeSummaryTag.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(archetypeLabelModel.getObject())));
        return archetypeSummaryTag;
    }

    protected void addAdditionalVariablesMap(VariablesMap variables) {

    }

    private GuiFlexibleLabelType getLabelConfiguration(QName configurationPropertyName) {
        if (configuration == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        PrismContainer<GuiFlexibleLabelType> subContainer = configuration.asPrismContainerValue().findContainer(configurationPropertyName);
        if (subContainer == null) {
            return null;
        }
        return subContainer.getRealValue();
    }

    protected String getTagBoxCssClass() {
        return null;
    }

    public Component getTag(String id) {
        return tagBox.get(id);
    }

    private String getArchetypePolicyAdditionalCssClass() {
        if (getModelObject() instanceof AssignmentHolderType) {
            DisplayType displayType = getArchetypePolicyDisplayType();
            return GuiDisplayTypeUtil.getIconColor(displayType);
        }
        return "";
    }

    private String getArchetypeLabel() {
        if (!(getModelObject() instanceof AssignmentHolderType holder)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        DisplayType displayType = getArchetypePolicyDisplayType();
        sb.append(translateDisplayLabelOrDefault(displayType, ""));

        try {
            OperationResult result = new OperationResult("Determine archetypes");
            List<ArchetypeType> archetypes = getPageBase().getModelInteractionService().determineArchetypes(holder, result);
            String auxiliary = archetypes.stream()
                    .filter(a -> Objects.equals(ArchetypeTypeType.AUXILIARY, a.getArchetypeType()))
                    .map(a -> {
                        DisplayType display = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(a, getPageBase());
                        return translateDisplayLabelOrDefault(display, WebComponentUtil.getName(a));
                    })
                    .filter(l -> StringUtils.isNotEmpty(l))
                    .collect(Collectors.joining(", "));

            if (StringUtils.isNotEmpty(auxiliary)) {
                sb.append(" (");
                sb.append(auxiliary);
                sb.append(")");
            }
        } catch (SchemaException ex) {
            LOGGER.debug("Cannot determine archetypes for {}", holder);
            LOGGER.trace("Cannot determine archetypes", ex);
        }

        return sb.toString();
    }

    private String translateDisplayLabelOrDefault(DisplayType display, String defValue) {
        if (display == null || display.getLabel() == null) {
            return defValue;
        }
        String label = display.getLabel().getOrig();

        return getString(label, Model.of(), label);
    }

    private String getArchetypeIconCssClass() {
        if (getModelObject() instanceof AssignmentHolderType) {
            DisplayType displayType = getArchetypePolicyDisplayType();
            return GuiDisplayTypeUtil.getIconCssClass(displayType);
        }
        return "";
    }

    private DisplayType getArchetypePolicyDisplayType() {
        return GuiDisplayTypeUtil.getArchetypePolicyDisplayType(
                getAssignmentHolderTypeObjectForArchetypeDisplayType(),
                getPageBase());
    }

    protected AssignmentHolderType getAssignmentHolderTypeObjectForArchetypeDisplayType() {
        return (AssignmentHolderType) getModelObject();
    }

    protected abstract String getDefaultIconCssClass();

    protected abstract String getIconBoxAdditionalCssClass();

    protected abstract String getBoxAdditionalCssClass();

    protected QName getIdentifierPropertyName() {
        return FocusType.F_NAME;
    }

    protected QName getDisplayNamePropertyName() {
        return ObjectType.F_NAME;
    }

    protected IModel<String> getDisplayNameModel() {
        return null;
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
        GuiFlexibleLabelType config = getLabelConfiguration(SummaryPanelSpecificationType.F_ORGANIZATION);
        if (config != null) {
            return createFlexibleLabelModel(ObjectType.F_PARENT_ORG_REF, config);
        } else {
            return getDefaultParentOrgModel();
        }
    }

    protected IModel<String> getDefaultParentOrgModel() {
        return new Model<>(null);
    }

    protected IModel<AbstractResource> getPhotoModel() {
        return new Model<>(null);
    }

    protected WebMarkupContainer getSummaryBoxPanel() {
        return (WebMarkupContainer) get(ID_BOX);
    }
}
