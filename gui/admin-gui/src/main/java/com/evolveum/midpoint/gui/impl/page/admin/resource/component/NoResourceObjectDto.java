/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * DTO representing a "no resource objects" state, including toolbar buttons
 * and title/subtitle text for empty object type panels.
 */
public class NoResourceObjectDto implements Serializable {

    private final ItemPath targetPath;
    private final List<ToolbarButtonDto> toolbarButtons;
    IModel<ResourceDetailsModel> resourceDetailsModel;

    /**
     * Constructs a NoResourceObjectDto for a specific container path.
     *
     * @param targetPath           The item path for the resource type.
     * @param resourceDetailsModel Model containing resource details.
     * @param toolbarButtons       List of toolbar buttons to show in the panel.
     */
    public NoResourceObjectDto(
            ItemPath targetPath,
            IModel<ResourceDetailsModel> resourceDetailsModel,
            List<ToolbarButtonDto> toolbarButtons) {
        this.targetPath = targetPath;
        this.resourceDetailsModel = resourceDetailsModel;
        this.toolbarButtons = toolbarButtons;
    }


    /**
     * Returns the localized title message for the  title panel.
     */
    protected StringResourceModel getTitle() {
        return createStringResource("NoResourceObjectDto.noObjectToShow.title", getTypeTitle(false));
    }

    /**
     * Returns the localized subtitle message for the subtitle panel.
     */
    protected StringResourceModel getSubtitle() {
        return createStringResource("NoResourceObjectDto.noObjectToShow.subtitle", getTypeTitle(true));
    }

    /**
     * Builds a type-specific title key based on the path, optionally pluralized.
     */
    protected StringResourceModel getTypeTitle(boolean plural) {
        String suffix = plural ? "s" : "";
        return createStringResource("SchemaHandlingObjectsPanel."
                + targetPath.toString().replaceAll("/", ".") + suffix);
    }

    /**
     * Represents a toolbar buttons with icon, label, style, and action handler.
     */
    public abstract static class ToolbarButtonDto implements Serializable {
        private final IModel<String> label;
        private final IModel<String> iconCss;
        private final String cssClass;
        private final boolean isVisible;

        public ToolbarButtonDto(IModel<String> label, IModel<String> iconCss, String cssClass, boolean isVisible) {
            this.label = label;
            this.iconCss = iconCss;
            this.cssClass = cssClass;
            this.isVisible = isVisible;
        }

        public IModel<String> getLabel() {
            return label;
        }

        public IModel<String> getIconCss() {
            return iconCss;
        }

        public String getCssClass() {
            return cssClass;
        }

        public boolean isVisible() {
            return isVisible;
        }

        /**
         * Called when the button is clicked.
         */
        public abstract void action(AjaxRequestTarget ajaxRequestTarget);

    }

    public ItemPath getTargetPath() {
        return targetPath;
    }

    public IModel<ResourceDetailsModel> getResourceDetailsModel() {
        return resourceDetailsModel;
    }

    public List<ToolbarButtonDto> getToolbarButtons() {
        return toolbarButtons;
    }

    protected static @NotNull String getIconForCreateObjectButton() {
        return GuiStyleConstants.CLASS_PLUS_CIRCLE;
    }

    protected static @NotNull String getIconForSuggestObjectButton() {
        return GuiStyleConstants.CLASS_ICON_WIZARD;
    }

    @Contract(pure = true)
    protected static @NotNull String getKeyOfTitleForCreateObjectButton() {
        return "NoResourceObjectsTypePanel.createNew";
    }

    @Contract(pure = true)
    protected static @NotNull String getKeyOfTitleForSuggestObjectButton() {
        return "NoResourceObjectsTypePanel.suggestNew";
    }

}
