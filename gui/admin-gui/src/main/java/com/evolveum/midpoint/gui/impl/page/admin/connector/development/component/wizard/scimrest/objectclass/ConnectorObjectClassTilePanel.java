/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTile;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public abstract class ConnectorObjectClassTilePanel extends BasePanel<TemplateTile<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>>{

    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ACTIONS = "actions";
    private static final String ID_DELETE_BUTTON = "deleteButton";

    private static final String ID_CAPABILITIES_COUNT = "capabilitiesCount";
    private static final String ID_CAPABILITIES = "capabilities";
    private static final String ID_CAPABILITY = "capability";
    private static final String ID_CAPABILITY_LABEL = "capabilityLabel";

    public ConnectorObjectClassTilePanel(String id, IModel<TemplateTile<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append(
                "class",
                "card col-12 tile d-flex flex-column  p-3 mb-0"));
        setOutputMarkupId(true);

        IModel<String> titleModel = () -> {
            String titleText = getModelObject().getTitle();
            return titleText != null ? getString(titleText, null, titleText) : null;
        };

        Label title = new Label(ID_TITLE, titleModel);
        title.add(AttributeAppender.append("title", titleModel));
        title.add(new TooltipBehavior());
        add(title);

        IModel<String> descriptionModel = () -> {
            String description = getModelObject().getDescription();
            return description != null ? getString(description, null, description) : null;
        };

        Label descriptionPanel = new Label(ID_DESCRIPTION, descriptionModel);
        descriptionPanel.add(AttributeAppender.append("title", descriptionModel));
        descriptionPanel.add(new TooltipBehavior());
        descriptionPanel.add(new VisibleBehaviour(() -> getModelObject().getDescription() != null));
        add(descriptionPanel);

        DropdownButtonPanel actions = new DropdownButtonPanel(ID_ACTIONS, new DropdownButtonDto(
                null, null, getString("ConnectorObjectClassTilePanel.configure"), getActionsItems(getModelObject().getValue()))){
            @Override
            protected String getSpecialButtonClass() {
                return "btn-link py-0";
            }
        };
        actions.setOutputMarkupId(true);
        add(actions);

        AjaxIconButton deleteButton = new AjaxIconButton(
                ID_DELETE_BUTTON,
                Model.of("fa fa-trash"),
                createStringResource("ConnectorObjectClassTilePanel.delete")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteObjectClassPerformed(target);
            }
        };
        deleteButton.setOutputMarkupId(true);
        add(deleteButton);

        Label capabilityCount = new Label(ID_CAPABILITIES_COUNT, () -> "(" + getModelObject().getTags().size() + ")");
        capabilityCount.setOutputMarkupId(true);
        add(capabilityCount);

        ListView<DisplayType> tagPanel = new ListView<>(ID_CAPABILITIES, () -> getModelObject().getTags()) {

            @Override
            protected void populateItem(ListItem<DisplayType> item) {
                DisplayType tag = item.getModelObject();

                WebMarkupContainer tagContainer = new WebMarkupContainer(ID_CAPABILITY);
                item.add(tagContainer);

                Label tagLabel = new Label(ID_CAPABILITY_LABEL, () -> WebComponentUtil.getTranslatedPolyString(tag.getLabel()));
                tagContainer.add(tagLabel);
            }
        };
        tagPanel.add(new VisibleBehaviour(() -> getModelObject().getTags() != null));
        add(tagPanel);
    }

    protected abstract void deleteObjectClassPerformed(AjaxRequestTarget target);

    private List<InlineMenuItem> getActionsItems(PrismContainerValueWrapper<ConnDevObjectClassInfoType> value) {
        List<InlineMenuItem> actions = new ArrayList<>();
        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.schema")) {
            @Serial private static final long serialVersionUID = 1L;
            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editSchemaPerformed(getObjectClassName(), target);
                    }
                };
            }
        });
        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.searchAll")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editSearchAllPerformed(getObjectClassName(), target);
                    }
                };
            }
        });
//
//        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.get")) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            public InlineMenuItemAction initAction() {
//                return new ColumnMenuAction<>() {
//                    @Serial private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        editGetPerformed(target);
//                    }
//                };
//            }
//        });
//
//        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.searchFilter")) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            public InlineMenuItemAction initAction() {
//                return new ColumnMenuAction<>() {
//                    @Serial private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        editSearchFilterPerformed(target);
//                    }
//                };
//            }
//        });
//
        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.create")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editCreatePerformed(getObjectClassName(), target);
                    }
                };
            }
        });

        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.update")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editUpdatePerformed(getObjectClassName(), target);
                    }
                };
            }
        });

        actions.add(new InlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editDeletePerformed(getObjectClassName(), target);
                    }
                };
            }
        });

        actions.add(new ButtonInlineMenuItem(createStringResource("ConnectorObjectClassTilePanel.actions.deleteCapabilities")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-times-circle");
            }

            @Override
            public IModel<String> getAdditionalCssClass() {
                return Model.of("text-danger border-top");
            }

            @Serial private static final long serialVersionUID = 1L;
            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteCapabilitiesPerformed(target);
                    }
                };
            }
        });

        return actions;
    }

    private String getObjectClassName() {
        return getModelObject().getValue().getRealValue().getName();
    }

    private void deleteCapabilitiesPerformed(AjaxRequestTarget target) {

    }

//    protected abstract void editSearchFilterPerformed(AjaxRequestTarget target);
//
//    protected abstract void editGetPerformed(AjaxRequestTarget target);

    protected abstract void editSearchAllPerformed(String objectClassName, AjaxRequestTarget target);

    protected abstract void editSchemaPerformed(String objectClassName, AjaxRequestTarget target);

    protected abstract void editCreatePerformed(String objectClassName, AjaxRequestTarget target);

    protected abstract void editUpdatePerformed(String objectClassName, AjaxRequestTarget target);

    protected abstract void editDeletePerformed(String objectClassName, AjaxRequestTarget target);
}
