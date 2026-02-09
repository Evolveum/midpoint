/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.menu;

import java.io.Serial;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.ObjectDetailsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

public class DetailsNavigationPanel<O extends ObjectType> extends BasePanel<List<ContainerPanelConfigurationType>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_NAV = "menu";
    private static final String ID_NAV_ITEM = "navItem";
    private static final String ID_NAV_ITEM_SR_CURRENT_MESSAGE = "navItemSrCurrentMessage";
    private static final String ID_NAV_ITEM_LINK = "navItemLink";
    private static final String ID_NAV_ITEM_ICON = "navItemIcon";
    private static final String ID_SUB_NAVIGATION = "subNavigation";
    private static final String ID_COUNT = "count";
    private static final String ID_NAVIGATION_DETAILS = "navLinkStyle";
    private static final String ID_SUBMENU_LINK = "submenuLink";

    private final ObjectDetailsModels<O> objectDetailsModel;
    private String clickedMenuItemName;

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

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        if (StringUtils.isNotEmpty(clickedMenuItemName)) {
            response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.focusSelectedNavigationLink('" +
                    clickedMenuItemName + "');"));
            clickedMenuItemName = null;
        }
    }

    private void initLayout() {
        ListView<ContainerPanelDto> listView = new ListView<>(ID_NAV, getContainerPanelListModel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ContainerPanelDto> item) {
                populateNavigationMenuItem(item);
            }
        };
        listView.setOutputMarkupId(true);
        add(listView);
    }

    private IModel<List<ContainerPanelDto>> getContainerPanelListModel() {
        return Model.ofList(getModelObject()
                        .stream()
                        .map(ContainerPanelDto::new)
                        .toList());
    }

    private void populateNavigationMenuItem(ListItem<ContainerPanelDto> item) {
        ContainerPanelDto containerPanelDto = item.getModelObject();
        ContainerPanelConfigurationType panelConfig = containerPanelDto.getContainerPanelConfig();
        WebMarkupContainer navigationDetails = createNavigationItemParentPanel(containerPanelDto);
        item.add(navigationDetails);

        AjaxSubmitLink link = createNavigationLink(containerPanelDto);
        navigationDetails.add(link);

        addParentMenuItemDescription(link, panelConfig);

        DetailsNavigationPanel<O> subPanel = createDetailsSubNavigationPanel(containerPanelDto);
        navigationDetails.add(subPanel);

        item.add(new VisibleBehaviour(() -> isMenuItemVisible(panelConfig)));

    }

    private WebMarkupContainer createNavigationItemParentPanel(ContainerPanelDto panelConfigDto) {
        ContainerPanelConfigurationType panelConfig = panelConfigDto.getContainerPanelConfig();
        WebMarkupContainer navigationDetails = new WebMarkupContainer(ID_NAVIGATION_DETAILS);
        navigationDetails.add(AttributeAppender.append(
                "aria-expanded",
                () -> {
                    if (isSubMenuVisible(panelConfigDto)) {
                        ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
                        return (isMenuActive(storageConfig, panelConfig) | hasActiveSubmenu(storageConfig, panelConfig))
                                && panelConfigDto.isExpanded();
                    }
                    return null;
                }));
        navigationDetails.add(AttributeAppender.append("class", createNavigationDetailsStyleModel(panelConfigDto)));
        return navigationDetails;
    }

    private boolean isSubMenuVisible(ContainerPanelDto containerPanelDto) {
        ContainerPanelConfigurationType panelConfig = containerPanelDto.getContainerPanelConfig();
        return doesSubMenuExist(panelConfig) && containerPanelDto.isExpanded();
    }

    private IModel<String> createNavigationDetailsStyleModel(ContainerPanelDto panelConfigDto) {
        return () -> {
            ContainerPanelConfigurationType panelConfig = panelConfigDto.getContainerPanelConfig();
            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
            if (isMenuActive(storageConfig, panelConfig)) {
                return panelConfigDto.isExpanded() ? "active open" : "active";
            }
            if (hasActiveSubmenu(storageConfig, panelConfig)) {
                return "open";
            }
            return "";
        };
    }

    private ContainerPanelConfigurationType getConfigurationFromStorage() {
        ObjectDetailsStorage storage = getPageBase().getBrowserTabSessionStorage().getObjectDetailsStorage(
                "details" + objectDetailsModel.getObjectWrapperModel().getObject().getCompileTimeClass().getSimpleName());
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

    private AjaxSubmitLink createNavigationLink(ContainerPanelDto panelConfigDto) {
        ContainerPanelConfigurationType panelConfig = panelConfigDto.getContainerPanelConfig();
        AjaxSubmitLink link = new AjaxSubmitLink(ID_NAV_ITEM_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);

                target.add(DetailsNavigationPanel.this);
                onClickPerformed(panelConfig, target);
                clickedMenuItemName = createButtonLabel(panelConfig).getObject();
                panelConfigDto.setExpanded(!panelConfigDto.isExpanded());
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);

                target.add(getPageBase().getFeedbackPanel());
            }
        };

        link.add(AttributeModifier.append(
                "aria-current",
                () -> {
                    ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
                    if (isMenuActive(storageConfig, panelConfig)) {
                        return "page";
                    }
                    return null;
                }));

        addIcon(link, panelConfig);
        addSrCurrentMessage(link, panelConfigDto);
        addLabel(link, panelConfig);
        addCount(link, panelConfig);
        addSubmenuLink(link, panelConfigDto);

        return link;
    }

    private void addSrCurrentMessage(AjaxSubmitLink link, ContainerPanelDto panelConfigDto) {
        ContainerPanelConfigurationType panelConfig = panelConfigDto.getContainerPanelConfig();
        Label srCurrentMessage = new Label(ID_NAV_ITEM_SR_CURRENT_MESSAGE, () -> {
            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
            String key = "";
            if (isMenuActive(storageConfig, panelConfig)) {
                key = "DetailsNavigationPanel.srCurrentMessage";
            }
            if (hasActiveSubmenu(storageConfig, panelConfig)) {
                key = "DetailsNavigationPanel.srActiveSubItemMessage";
            }
            if (key.isEmpty()) {
                key = "DetailsNavigationPanel.srMenuItemMessage";
            }
            StringBuilder sb = new StringBuilder(getString(key));
            if (doesSubMenuExist(panelConfig)) {
                sb.append(getString(panelConfigDto.isExpanded() ?
                        "DetailsNavigationPanel.srSubItemsExpanded" : "DetailsNavigationPanel.srSubItemsCollapsed"));
            }
//            if (ID_SUB_NAVIGATION.equals(this.getId())) {
//                key = "DetailsNavigationPanel.srSubItemMessage";
//                return getPageBase().createStringResource(key).getString();
//            }
            return sb.toString();
        });
//        srCurrentMessage.add(new VisibleBehaviour(() -> {
//            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
//            return isMenuActive(storageConfig, panelConfig) || hasActiveSubmenu(storageConfig, panelConfig);
//        }));
        link.add(srCurrentMessage);
    }

    private void addIcon(AjaxSubmitLink link, ContainerPanelConfigurationType panelConfig) {
        WebMarkupContainer icon = new WebMarkupContainer(ID_NAV_ITEM_ICON);
        icon.setOutputMarkupId(true);
        icon.add(AttributeAppender.append("class", getMenuItemIconClass(panelConfig)));
        link.add(icon);
    }

    private void addLabel(AjaxSubmitLink link, ContainerPanelConfigurationType panelConfig) {
        Label buttonLabel = new Label(ID_NAV_ITEM, createButtonLabel(panelConfig));
        link.add(buttonLabel);
    }

    private void addCount(AjaxSubmitLink link, ContainerPanelConfigurationType panelConfig) {
        Label label = new Label(ID_COUNT, createCountModel(panelConfig));
        label.add(new VisibleBehaviour(() -> getCounterProvider(panelConfig) != null));
        link.add(label);
    }

    private void addSubmenuLink(AjaxSubmitLink link, ContainerPanelDto containerPanelDto) {
        WebMarkupContainer submenuLink = new WebMarkupContainer(ID_SUBMENU_LINK);
        submenuLink.add(new VisibleBehaviour(() -> doesSubMenuExist(containerPanelDto.getContainerPanelConfig())));
        link.add(submenuLink);
    }

    private void addParentMenuItemDescription(AjaxSubmitLink link, ContainerPanelConfigurationType panelConfig) {
        // in case there is parent menu item (e.g. Assignments is parent for Role menu item)
        // we want to add a description for a screen reader to describe this parent item
        DetailsNavigationPanel<?> parentPanel = link.findParent(DetailsNavigationPanel.class);
        WebMarkupContainer parentMenuItemPanel = parentPanel != null ? (WebMarkupContainer) parentPanel.getParent() : null;
        if (parentMenuItemPanel != null && ID_NAVIGATION_DETAILS.equals(parentMenuItemPanel.getId())) {
            Component labelComponent = parentMenuItemPanel.get(ID_NAV_ITEM_LINK).get(ID_NAV_ITEM);
            Object parentLinkLabel = labelComponent.getDefaultModelObject();
            if (parentLinkLabel instanceof String parentLabel && StringUtils.isNotEmpty(parentLabel)) {
                String currentItemLabel = createButtonLabel(panelConfig).getObject();
                link.add(AttributeAppender.append("aria-label",
                        createStringResource("DetailsNavigationPanel.parentMenuItemDescription", currentItemLabel, parentLabel)));
            }
        }
    }

    private DetailsNavigationPanel<O> createDetailsSubNavigationPanel(ContainerPanelDto containerPanelDto) {
        ContainerPanelConfigurationType panelConfig = containerPanelDto.getContainerPanelConfig();
        DetailsNavigationPanel<O> subPanel = new DetailsNavigationPanel<>(ID_SUB_NAVIGATION, objectDetailsModel,
                Model.ofList(panelConfig.getPanel())) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                if (config.getPath() == null) {
                    config.setPath(panelConfig.getPath());
                }
                target.add(DetailsNavigationPanel.this);
                DetailsNavigationPanel.this.onClickPerformed(config, target);
                clickedMenuItemName = createButtonLabel(config).getObject();
            }
        };
        subPanel.add(new VisibleBehaviour(() -> isSubMenuVisible(containerPanelDto)));
        return subPanel;
    }

    private boolean doesSubMenuExist(ContainerPanelConfigurationType panelConfig) {
        if (panelConfig == null) {
            return false;
        }

        if (panelConfig.getPanel().isEmpty()) {
            return false;
        }

        Class<? extends Panel > panelClass = getPageBase().findObjectPanel(panelConfig.getIdentifier());
        if (panelClass == null) {
            return true;
        }

        if (MultivalueDetailsPanel.class.isAssignableFrom(panelClass)) {
            return panelConfig.getPanel().stream()
                    .anyMatch(subPanelConfig -> objectDetailsModel.containsModelForSubmenu(subPanelConfig.getIdentifier()));
        }
        return true;
    }

    private IModel<String> createCountModel(ContainerPanelConfigurationType panelConfig) {
        return () -> {
            SimpleCounter<ObjectDetailsModels<O>, O> counter = getCounterProvider(panelConfig);
            if (counter == null) {
                return null;
            }
            int count = counter.count(objectDetailsModel, getPageBase());
            return String.valueOf(count);
        };
    }

    private SimpleCounter<ObjectDetailsModels<O>, O> getCounterProvider(ContainerPanelConfigurationType panelConfig) {
        String panelInstanceIdentifier = panelConfig.getIdentifier();

        SimpleCounter<ObjectDetailsModels<O>, O> counter = getPageBase().getCounterProvider(panelInstanceIdentifier);
        if (counter == null || counter.getClass().equals(SimpleCounter.class)) {
            return null;
        }
        return counter;
    }

    protected boolean isMenuItemVisible(ContainerPanelConfigurationType config) {
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

    private IModel<String> createButtonLabel(ContainerPanelConfigurationType config) {
        return () -> {
            if (config.getDisplay() == null) {
                return "N/A";
            }

            if (config.getDisplay().getLabel() == null) {
                return "N/A";
            }

            return LocalizationUtil.translatePolyString(config.getDisplay().getLabel());
        };
    }

    private IModel<String> getMenuItemIconClass(ContainerPanelConfigurationType panelConfig) {
        return () -> {
            if (panelConfig == null || panelConfig.getDisplay() == null) {
                return GuiStyleConstants.CLASS_CIRCLE_FULL;
            }
            String iconCss = GuiDisplayTypeUtil.getIconCssClass(panelConfig.getDisplay());
            return StringUtils.isNoneEmpty(iconCss) ? iconCss : GuiStyleConstants.CLASS_CIRCLE_FULL;
        };
    }
}
