/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * Simple builder for {@link ButtonInlineMenuItem} and {@link InlineMenuItem}.
 * Mirrors all fields and methods from InlineMenuItem and ButtonInlineMenuItem.
 */
public class InlineMenuItemBuilder implements Serializable {

    // Fields from InlineMenuItem
    private IModel<String> label = Model.of("");
    private IModel<Boolean> enabled = Model.of(true);
    private IModel<Boolean> visible = Model.of(true);
    private boolean submit = false;
    private InlineMenuItemAction action;
    private int id = -1;
    private InlineMenuItem.VisibilityChecker visibilityChecker;
    private IModel<String> additionalCssClass = Model.of("");
    private IModel<String> confirmationMessageModel;
    private boolean showConfirmationDialog = true;
    private boolean divider = false;
    private boolean menuHeader = false;
    private boolean headerMenuItem = true;
    private boolean menuLinkVisible = true;

    // Fields from ButtonInlineMenuItem
    private boolean badgeVisible = false;
    private boolean labelVisible = false;
    private transient CompositedIconBuilder iconBuilder;
    private IModel<String> buttonLabelModel;

    private InlineMenuItemBuilder() {
    }

    public static @NotNull InlineMenuItemBuilder create() {
        return new InlineMenuItemBuilder();
    }

    public InlineMenuItemBuilder label(IModel<String> label) {
        this.label = label;
        return this;
    }

    public InlineMenuItemBuilder enabled(IModel<Boolean> enabled) {
        this.enabled = enabled;
        return this;
    }

    public InlineMenuItemBuilder visible(IModel<Boolean> visible) {
        this.visible = visible;
        return this;
    }

    public InlineMenuItemBuilder submit(boolean submit) {
        this.submit = submit;
        return this;
    }

    public InlineMenuItemBuilder action(InlineMenuItemAction action) {
        this.action = action;
        return this;
    }

    public InlineMenuItemBuilder id(int id) {
        this.id = id;
        return this;
    }

    public InlineMenuItemBuilder visibilityChecker(InlineMenuItem.VisibilityChecker checker) {
        this.visibilityChecker = checker;
        return this;
    }

    public InlineMenuItemBuilder additionalCssClass(String cssClass) {
        this.additionalCssClass = Model.of(cssClass);
        return this;
    }

    public InlineMenuItemBuilder additionalCssClass(IModel<String> cssClassModel) {
        this.additionalCssClass = cssClassModel;
        return this;
    }

    public InlineMenuItemBuilder confirmationMessage(IModel<String> confirmationMessageModel) {
        this.confirmationMessageModel = confirmationMessageModel;
        return this;
    }

    public InlineMenuItemBuilder showConfirmationDialog(boolean show) {
        this.showConfirmationDialog = show;
        return this;
    }

    public InlineMenuItemBuilder divider(boolean divider) {
        this.divider = divider;
        return this;
    }

    public InlineMenuItemBuilder menuHeader(boolean menuHeader) {
        this.menuHeader = menuHeader;
        return this;
    }

    public InlineMenuItemBuilder headerMenuItem(boolean headerMenuItem) {
        this.headerMenuItem = headerMenuItem;
        return this;
    }

    public InlineMenuItemBuilder menuLinkVisible(boolean visible) {
        this.menuLinkVisible = visible;
        return this;
    }

    public InlineMenuItemBuilder labelVisible(boolean visible) {
        this.labelVisible = visible;
        return this;
    }

    public InlineMenuItemBuilder badgeVisible(boolean visible) {
        this.badgeVisible = visible;
        return this;
    }

    public InlineMenuItemBuilder icon(String basicIconCss) {
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(basicIconCss, IconCssStyle.IN_ROW_STYLE);
        this.iconBuilder = builder;
        return this;
    }

    public InlineMenuItemBuilder iconBuilder(CompositedIconBuilder builder) {
        this.iconBuilder = builder;
        return this;
    }

    public InlineMenuItemBuilder buttonLabelModel(IModel<String> buttonLabelModel) {
        this.buttonLabelModel = buttonLabelModel;
        return this;
    }

    public ButtonInlineMenuItem buildButtonMenu() {
        ButtonInlineMenuItem item = new ButtonInlineMenuItem(label, submit) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return action;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return iconBuilder;
            }

            @Override
            public boolean isLabelVisible() {
                return labelVisible;
            }

            @Override
            protected boolean isBadgeVisible() {
                return badgeVisible;
            }

            @Override
            protected boolean isMenuLinkVisible() {
                return menuLinkVisible;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return headerMenuItem;
            }

            @Override
            public boolean isMenuHeader() {
                return menuHeader;
            }

            @Override
            public boolean isDivider() {
                return divider;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return confirmationMessageModel;
            }

            @Override
            public boolean showConfirmationDialog() {
                return showConfirmationDialog;
            }

            @Override
            public IModel<String> getButtonLabelModel() {
                return buttonLabelModel != null ? buttonLabelModel : super.getButtonLabelModel();
            }

            @Override
            public IModel<String> getAdditionalCssClass() {
                return additionalCssClass != null ? additionalCssClass : super.getAdditionalCssClass();
            }
        };

        item.setEnabled(enabled);
        item.setVisible(visible);
        item.setId(id);
        item.setVisibilityChecker(visibilityChecker);

        return item;
    }

    public InlineMenuItem buildInlineMenu() {
        InlineMenuItem item = new InlineMenuItem(label, submit) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return action;
            }

            @Override
            protected boolean isMenuLinkVisible() {
                return menuLinkVisible;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return headerMenuItem;
            }

            @Override
            public boolean isMenuHeader() {
                return menuHeader;
            }

            @Override
            public boolean isDivider() {
                return divider;
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return confirmationMessageModel;
            }

            @Override
            public boolean showConfirmationDialog() {
                return showConfirmationDialog;
            }

            @Override
            public IModel<String> getAdditionalCssClass() {
                return additionalCssClass != null ? additionalCssClass : super.getAdditionalCssClass();
            }
        };

        item.setEnabled(enabled);
        item.setVisible(visible);
        item.setId(id);
        item.setVisibilityChecker(visibilityChecker);

        return item;
    }

}
