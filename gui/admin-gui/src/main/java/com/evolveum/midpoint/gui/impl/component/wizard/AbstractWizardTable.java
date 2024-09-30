/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class AbstractWizardTable<C extends Containerable, CV extends Containerable> extends MultivalueContainerListPanelWithDetailsPanel<C> {

    private final IModel<PrismContainerValueWrapper<CV>> valueModel;

    public AbstractWizardTable(
            String id,
            IModel<PrismContainerValueWrapper<CV>> valueModel,
            ContainerPanelConfigurationType config,
            Class<C> type) {
        super(id, type, config);
        this.valueModel = valueModel;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getTable().setShowAsCard(false);
    }

    public IModel<PrismContainerValueWrapper<CV>> getValueModel() {
        return valueModel;
    }

    @Override
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected void newItemPerformed(PrismContainerValue<C> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {
        createNewValue(value, target);
        refreshTable(target);
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = new ArrayList<>();

        AjaxIconButton newObjectButton = new AjaxIconButton(
                idButton,
                new Model<>("fa fa-circle-plus"),
                createStringResource(getKeyOfTitleForNewObjectButton())) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                newItemPerformed(target, null);
            }
        };
        newObjectButton.add(AttributeAppender.append("class", getNewButtonCssClass()));
        newObjectButton.showTitleAsLabel(true);
        newObjectButton.add(new VisibleBehaviour(() -> isCreateNewObjectVisible()));
        buttons.add(newObjectButton);
        return buttons;
    }

    protected String getNewButtonCssClass() {
        return "btn btn-primary btn-sm ml-3";
    }

    @Override
    public void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<C>> rowModel,
            List<PrismContainerValueWrapper<C>> listItems) {
    }

    protected PrismContainerValueWrapper createNewValue(PrismContainerValue<C> value, AjaxRequestTarget target) {
        PrismContainerWrapper<C> container = getContainerModel().getObject();
        PrismContainerValue<C> newValue = value;
        if (newValue == null) {
            newValue = container.getItem().createNewValue();
        }
        return createNewItemContainerValueWrapper(newValue, container, target);
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createEditItemMenu());
        items.add(createDeleteItemMenu());
        return items;
    }

    private InlineMenuItem createEditItemMenu() {
        return new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction();
            }
        };
    }

    protected InlineMenuItem createDeleteItemMenu() {
        return new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
    }

    @Override
    protected abstract IModel<PrismContainerWrapper<C>> getContainerModel();

    @Override
    public void refreshTable(AjaxRequestTarget target) {
        getContainerModel().detach();
        clearCache();
        super.refreshTable(target);
    }

    @Override
    protected WebMarkupContainer getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<C>> item) {
        return new WebMarkupContainer(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS);
    }
}
