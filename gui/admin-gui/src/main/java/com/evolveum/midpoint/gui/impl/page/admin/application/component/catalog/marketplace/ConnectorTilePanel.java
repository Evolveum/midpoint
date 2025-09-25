/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import org.jetbrains.annotations.*;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.visibleWhenModelIsNotBlank;

/**
 * @param <O> The type of the payload object the tile works with.
 * @param <T> The type of the tile model object, bound to the payload type O.
 */
public abstract class ConnectorTilePanel<O extends Serializable, T extends TemplateTile<O>> extends BasePanel<T> {
    private static final String ID_ACTION = "action";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ICON = "icon";
    private static final String ID_ICON_FRAGMENT = "iconFragment";
    private static final String ID_IMAGE_CONTAINER = "imageContainer";
    private static final String ID_LINK = "link";
    private static final String ID_TAGS = "tags";
    private static final String ID_TITLE = "title";
    private static final Trace LOGGER = TraceManager.getTrace(ConnectorTilePanel.class);

    public ConnectorTilePanel(String id, IModel<T> model) {
        super(id, model);
    }

    /**
     * Hook method for the "More Details" action.
     * @implSpec Subclasses should override to provide a specific action, e.g., navigation.
     * The default implementation only logs the event.
     */
    public void onLinkClick(AjaxRequestTarget target) {
        LOGGER.trace("More Details button clicked: " + getModelObject().getTitle());
    }

    /**
     * Hook method for the "Add Application" action.
     * @implSpec Subclasses should override to initiate the application creation process.
     * The default implementation only logs the event.
     */
    public void onActionClick(AjaxRequestTarget target) {
        LOGGER.trace("Add Application button clicked: " + getModelObject().getTitle());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        IModel<List<Badge>> tagsModel = LambdaModel.of(this::getTags);
        IModel<String> iconModel = LambdaModel.of(this::getIcon);
        IModel<String> titleModel = LambdaModel.of(this::getTitle);
        IModel<String> descriptionModel = LambdaModel.of(this::getDescription);

        onConfigurePanel();

        add(new BadgeListPanel(ID_TAGS, tagsModel).add(visibleWhenModelIsNotBlank()));
        add(createIconFragment(iconModel).add(visibleWhenModelIsNotBlank()));
        add(new Label(ID_TITLE, titleModel).add(visibleWhenModelIsNotBlank()));
        add(new Label(ID_DESCRIPTION, descriptionModel).add(visibleWhenModelIsNotBlank()));
        add(new AjaxLink<Void>(ID_LINK) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ConnectorTilePanel.this.onLinkClick(target);
            }
        });
        add(new AjaxButton(ID_ACTION) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ConnectorTilePanel.this.onActionClick(target);
            }
        });
    }

    /**
     * @implSpec The default implementation does nothing and returns empty {@code List}.
     * Subclasses should override this method to implement a logic for mapping model to badges, that will be rendered at the
     * top of the component
     */
    @Nullable
    protected String getIcon() {
        return Optional.ofNullable(getModelObject()).map(Tile::getIcon).map(icon -> "text-lightblue " + icon).orElse(null);
    }

    /**
     * Provides the description text for the tile.
     * @implSpec The text is displayed below the title. Returning `null` or a blank
     * string will hide the component.
     */
    @Nullable
    protected String getDescription() {
        return Optional.ofNullable(getModelObject()).map(Tile::getDescription).orElse(null);
    }

    /**
     * Provides the main title for the tile.
     * @implSpec Returning `null` or a blank string will hide the component.
     */
    @Nullable
    protected String getTitle() {
        return Optional.ofNullable(getModelObject()).map(Tile::getTitle).orElse(null);
    }

    /**
     * Provides a list of `Badge` objects to be displayed as tags.
     * @implSpec The default implementation maps `DisplayType` objects to `Badge`s.
     * This method must return a non-null `List`.
     */
    @NotNull
    protected List<Badge> getTags() {
        List<DisplayType> tags = Optional.ofNullable(getModelObject())
                .map(TemplateTile::getTags)
                .orElse(Collections.emptyList());

        return tags.stream()
                .map(this::mapTagToBadge)
                .toList();
    }

    /**
     * Configuration hook for the main panel component.
     * <p>
     * This method allows subclasses to add any kind of {@link Behavior}, such as
     * attribute modifiers for 'style' or 'data-*' attributes, without overriding
     * the entire {@code onInitialize} method.
     *
     * @implSpec
     * The default implementation applies the standard set of CSS classes for the tile.
     * Subclasses can override this method, and optionally call {@code super.onConfigurePanel()}
     * to inherit the default classes before adding their own modifications.
     */
    protected void onConfigurePanel() {}

    private Fragment createIconFragment(IModel<String> iconCssClassModel) {
        Fragment iconFragment = new Fragment(ID_IMAGE_CONTAINER, ID_ICON_FRAGMENT, this);
        iconFragment.setDefaultModel(iconCssClassModel);
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(new AttributeAppender("class", iconCssClassModel));
        iconFragment.add(icon);
        return iconFragment;
    }

    private Badge mapTagToBadge(DisplayType tag) {
        return new Badge(
                tag.getCssClass(),
                tag.getLabel().getOrig(),
                tag.getColor()
        );
    }
}
