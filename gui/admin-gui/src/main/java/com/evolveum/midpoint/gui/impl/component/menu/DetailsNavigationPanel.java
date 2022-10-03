/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import java.util.List;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.ObjectDetailsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

public class DetailsNavigationPanel<O extends ObjectType> extends BasePanel<List<ContainerPanelConfigurationType>> {

    private static final String ID_NAV = "menu";
    private static final String ID_NAV_ITEM = "navItem";
    private static final String ID_NAV_ITEM_LINK = "navItemLink";
    private static final String ID_NAV_ITEM_ICON = "navItemIcon";
    private static final String ID_SUB_NAVIGATION = "subNavigation";
    private static final String ID_COUNT = "count";
    private static final String ID_NAVIGATION_DETAILS = "navLinkStyle";
    private static final String ID_SUBMENU_LINK = "submenuLink";

    private final ObjectDetailsModels<O> objectDetailsModel;

    public DetailsNavigationPanel(String id, ObjectDetailsModels<O> objectDetailsModel, IModel<List<ContainerPanelConfigurationType>> model) {
        super(id, model);
        this.objectDetailsModel = objectDetailsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        ListView<ContainerPanelConfigurationType> listView = new ListView<>(ID_NAV, getModel()) {

            @Override
            protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                populateNavigationMenuItem(item);
            }
        };
        listView.setOutputMarkupId(true);
        add(listView);
    }

    private void populateNavigationMenuItem(ListItem<ContainerPanelConfigurationType> item) {
        WebMarkupContainer navigationDetails = createNavigationItemParentPanel(item);
        item.add(navigationDetails);

        AjaxSubmitLink link = createNavigationLink(item);
        navigationDetails.add(link);

        DetailsNavigationPanel<O> subPanel = createDetailsSubNavigationPanel(item);
        navigationDetails.add(subPanel);

        item.add(new VisibleBehaviour(() -> isMenuItemVisible(item.getModelObject())));

    }

    private WebMarkupContainer createNavigationItemParentPanel(ListItem<ContainerPanelConfigurationType> item) {
        WebMarkupContainer navigationDetails = new WebMarkupContainer(ID_NAVIGATION_DETAILS);
        navigationDetails.add(AttributeAppender.append("class", createNavigationDetailsStyleModel(item)));
        return navigationDetails;
    }

    private IModel<String> createNavigationDetailsStyleModel(ListItem<ContainerPanelConfigurationType> item) {
        return new ReadOnlyModel<>(() -> {
            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
            ContainerPanelConfigurationType itemModelObject = item.getModelObject();
            if (isMenuActive(storageConfig, itemModelObject)) {
                return "active open";
            }
            if (hasActiveSubmenu(storageConfig, itemModelObject)) {
                return "open";
            }
            return "";
        });
    }

    private ContainerPanelConfigurationType getConfigurationFromStorage() {
        ObjectDetailsStorage storage = getPageBase().getSessionStorage().getObjectDetailsStorage("details" + objectDetailsModel.getObjectWrapperModel().getObject().getCompileTimeClass().getSimpleName());
        return storage.getDefaultConfiguration();
    }

    private boolean isMenuActive(ContainerPanelConfigurationType storageConfig, ContainerPanelConfigurationType itemModelObject) {
        return storageConfig.getIdentifier() != null && storageConfig.getIdentifier().equals(itemModelObject.getIdentifier());
    }

    private boolean hasActiveSubmenu(ContainerPanelConfigurationType storageConfig, ContainerPanelConfigurationType itemModelObject) {
        for (ContainerPanelConfigurationType subPanel : itemModelObject.getPanel()) {
            if (subPanel.getIdentifier().equals(storageConfig.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    private AjaxSubmitLink createNavigationLink(ListItem<ContainerPanelConfigurationType> item) {
        AjaxSubmitLink link = new AjaxSubmitLink(ID_NAV_ITEM_LINK) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);

                target.add(DetailsNavigationPanel.this);
                onClickPerformed(item.getModelObject(), target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);

                target.add(getPageBase().getFeedbackPanel());
            }
        };

        addIcon(link, item);
        addLabel(link, item);
        addCount(link, item);
        addSubmenuLink(link, item);

        return link;
    }

    private void addIcon(AjaxSubmitLink link, ListItem<ContainerPanelConfigurationType> item) {
        WebMarkupContainer icon = new WebMarkupContainer(ID_NAV_ITEM_ICON);
        icon.setOutputMarkupId(true);
        icon.add(AttributeAppender.append("class", getMenuItemIconClass(item.getModel())));
        link.add(icon);
    }

    private void addLabel(org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink link, ListItem<ContainerPanelConfigurationType> item) {
        Label buttonLabel = new Label(ID_NAV_ITEM, createButtonLabel(item.getModel()));
        link.add(buttonLabel);
    }

    private void addCount(org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink link, ListItem<ContainerPanelConfigurationType> item) {
        Label label = new Label(ID_COUNT, createCountModel(item.getModel()));
        label.add(new VisibleBehaviour(() -> getCounterProvider(item.getModel()) != null));
        link.add(label);
    }

    private void addSubmenuLink(org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink link, ListItem<ContainerPanelConfigurationType> item) {
        WebMarkupContainer submenuLink = new WebMarkupContainer(ID_SUBMENU_LINK);
        submenuLink.add(new VisibleBehaviour(() -> hasSubmenu(item.getModelObject())));
        link.add(submenuLink);
    }

    private boolean hasSubmenu(ContainerPanelConfigurationType config) {
        if (config == null) {
            return false;
        }

        return !config.getPanel().isEmpty();
    }
    private DetailsNavigationPanel<O> createDetailsSubNavigationPanel(ListItem<ContainerPanelConfigurationType> item) {
        DetailsNavigationPanel<O> subPanel = new DetailsNavigationPanel<>(ID_SUB_NAVIGATION, objectDetailsModel, new PropertyModel<>(item.getModel(), ContainerPanelConfigurationType.F_PANEL.getLocalPart())) {

            @Override
            protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                if (config.getPath() == null) {
                    config.setPath(item.getModelObject().getPath());
                }
                target.add(DetailsNavigationPanel.this);
                DetailsNavigationPanel.this.onClickPerformed(config, target);
            }
        };
        subPanel.add(new VisibleBehaviour(() -> !item.getModelObject().getPanel().isEmpty()));
        return subPanel;
    }

    private IModel<String> createCountModel(IModel<ContainerPanelConfigurationType> panelModel) {
        return new ReadOnlyModel<>( () -> {
            SimpleCounter<ObjectDetailsModels<O>, O> counter = getCounterProvider(panelModel);
            if (counter == null) {
                return null;
            }
            int count = counter.count(objectDetailsModel, getPageBase());
            return String.valueOf(count);
        });
    }

    private SimpleCounter<ObjectDetailsModels<O>, O> getCounterProvider(IModel<ContainerPanelConfigurationType> panelModel) {
        ContainerPanelConfigurationType config = panelModel.getObject();
        String panelInstanceIdentifier = config.getIdentifier();

        SimpleCounter<ObjectDetailsModels<O>, O> counter = getPageBase().getCounterProvider(panelInstanceIdentifier);
        if (counter == null || counter.getClass().equals(SimpleCounter.class)) {
            return null;
        }
        return counter;
    }

    private boolean isMenuItemVisible(ContainerPanelConfigurationType config) {
        if (config == null) {
            return true;
        }

        return WebComponentUtil.getElementVisibility(config.getVisibility()) && isVisibleForAddApply(config);
    }

    private boolean isVisibleForAddApply(ContainerPanelConfigurationType config) {
        ItemStatus status = objectDetailsModel.getObjectStatus();
        if (status == ItemStatus.NOT_CHANGED) {
            return isAllowedForStatus(config, OperationTypeType.MODIFY);
        }
        return ItemStatus.ADDED == status && isAllowedForStatus(config, OperationTypeType.ADD);
    }

    private boolean isAllowedForStatus(ContainerPanelConfigurationType config, OperationTypeType status) {
        OperationTypeType applicableForOperation = config.getApplicableForOperation();
        return applicableForOperation == null || status == applicableForOperation;
    }

    protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {

    }

    private IModel<String> createButtonLabel(IModel<ContainerPanelConfigurationType> model) {

        return new ReadOnlyModel<>(() -> {
            ContainerPanelConfigurationType config = model.getObject();

            if (config.getDisplay() == null) {
                return "N/A";
            }

            if (config.getDisplay().getLabel() == null) {
                return "N/A";
            }

            return WebComponentUtil.getTranslatedPolyString(config.getDisplay().getLabel());
        });
    }

    private IModel<String> getMenuItemIconClass(IModel<ContainerPanelConfigurationType> item) {
        return new ReadOnlyModel<>(() -> {
            ContainerPanelConfigurationType config = item.getObject();
            if (config == null || config.getDisplay() == null) {
                return GuiStyleConstants.CLASS_CIRCLE_FULL;
            }
            String iconCss = GuiDisplayTypeUtil.getIconCssClass(config.getDisplay());
            return StringUtils.isNoneEmpty(iconCss) ? iconCss : GuiStyleConstants.CLASS_CIRCLE_FULL;
        });

    }

}
