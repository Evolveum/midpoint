/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts.KnownArtifactType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

/**
 * Popup for the "Clear capability" object class action. Lets the user pick which of the
 * currently configured capabilities (schema, search variants, create, update, delete) should
 * be removed from the object class, then confirms the removal via {@link #confirmPerformed}.
 */
public abstract class ClearCapabilityPopup extends BasePanel implements Popupable {

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_CHECKBOX = "checkbox";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CANCEL = "cancel";
    private static final String ID_CONFIRM = "confirm";

    private final List<CapabilityItem> items;

    private Fragment footer;
    private AjaxButton confirmButton;

    public ClearCapabilityPopup(String id, List<KnownArtifactType> capabilities) {
        super(id);
        items = capabilities.stream().map(CapabilityItem::new).collect(Collectors.toList());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        initFooter();
    }

    private void initLayout() {
        add(new Label(ID_DESCRIPTION, createStringResource("ClearCapabilityPopup.description")));

        ListView<CapabilityItem> itemsView = new ListView<>(ID_ITEMS, items) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<CapabilityItem> listItem) {
                CapabilityItem item = listItem.getModelObject();

                WebMarkupContainer itemContainer = new WebMarkupContainer(ID_ITEM);
                itemContainer.setOutputMarkupId(true);
                listItem.add(itemContainer);

                CheckBox checkbox = new CheckBox(ID_CHECKBOX, new PropertyModel<>(item, "selected"));
                checkbox.add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(confirmButton);
                    }
                });
                itemContainer.add(checkbox);

                itemContainer.add(new Label(ID_LABEL, createStringResource(labelKey(item.getType()))));
            }
        };
        itemsView.setOutputMarkupId(true);
        add(itemsView);
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        footer.add(new AjaxButton(ID_CANCEL, createStringResource("Button.cancel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        });

        confirmButton = new AjaxButton(
                ID_CONFIRM,
                () -> getString("ClearCapabilityPopup.confirm") + " (" + selectedCount() + ")") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (selectedCount() == 0) {
                    return;
                }
                List<KnownArtifactType> selected = items.stream()
                        .filter(CapabilityItem::isSelected)
                        .map(CapabilityItem::getType)
                        .collect(Collectors.toList());
                confirmPerformed(selected, target);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setEnabled(selectedCount() > 0);
            }
        };
        confirmButton.setOutputMarkupId(true);
        footer.add(confirmButton);
    }

    private long selectedCount() {
        return items.stream().filter(CapabilityItem::isSelected).count();
    }

    private static String labelKey(KnownArtifactType type) {
        switch (type) {
            case NATIVE_SCHEMA_DEFINITION:
                return "ConnectorObjectClassTilePanel.actions.schema";
            case SEARCH_ALL_DEFINITION:
                return "ConnectorObjectClassTilePanel.actions.searchAll";
            case SEARCH_BY_ID_DEFINITION:
                return "ConnectorObjectClassTilePanel.actions.get";
            case SEARCH_FILTER_DEFINITION:
                return "ConnectorObjectClassTilePanel.actions.searchFilter";
            case CREATE:
                return "ConnectorObjectClassTilePanel.actions.create";
            case UPDATE:
                return "ConnectorObjectClassTilePanel.actions.update";
            case DELETE:
                return "ConnectorObjectClassTilePanel.actions.delete";
            default:
                return type.name();
        }
    }

    protected abstract void confirmPerformed(List<KnownArtifactType> selected, AjaxRequestTarget target);

    @Override
    public int getWidth() {
        return 500;
    }

    @Override
    public int getHeight() {
        return 450;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("ConnectorObjectClassTilePanel.actions.deleteCapabilities");
    }

    @Override
    public Component getContent() {
        return ClearCapabilityPopup.this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private static class CapabilityItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private final KnownArtifactType type;
        private boolean selected;

        private CapabilityItem(KnownArtifactType type) {
            this.type = type;
        }

        public KnownArtifactType getType() {
            return type;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
