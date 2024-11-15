/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
        navigationDetails.add(AttributeAppender.append(
                "aria-haspopup",
                () -> isSubMenuVisible(item.getModelObject()) ? true : null));
        navigationDetails.add(AttributeAppender.append(
                "aria-expanded",
                () -> {
                    if (isSubMenuVisible(item.getModelObject())) {
                        ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
                        ContainerPanelConfigurationType itemModelObject = item.getModelObject();
                        return isMenuActive(storageConfig, itemModelObject) | hasActiveSubmenu(storageConfig, itemModelObject);
                    }
                    return null;
                }));
        navigationDetails.add(AttributeAppender.append("class", createNavigationDetailsStyleModel(item)));
        return navigationDetails;
    }

    private IModel<String> createNavigationDetailsStyleModel(ListItem<ContainerPanelConfigurationType> item) {
        return () -> {
            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
            ContainerPanelConfigurationType itemModelObject = item.getModelObject();
            if (isMenuActive(storageConfig, itemModelObject)) {
                return "active open";
            }
            if (hasActiveSubmenu(storageConfig, itemModelObject)) {
                return "open";
            }
            return "";
        };
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

        link.add(AttributeModifier.append(
                "aria-current",
                () -> {
                    ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
                    ContainerPanelConfigurationType itemModelObject = item.getModelObject();
                    if (isMenuActive(storageConfig, itemModelObject)) {
                        return "page";
                    }
                    return null;
                }));

        addIcon(link, item);
        addSrCurrentMessage(link, item);
        addLabel(link, item);
        addCount(link, item);
        addSubmenuLink(link, item);

        return link;
    }

    private void addSrCurrentMessage(AjaxSubmitLink link, ListItem<ContainerPanelConfigurationType> item) {
        Label srCurrentMessage = new Label(ID_NAV_ITEM_SR_CURRENT_MESSAGE, () -> {
            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
            ContainerPanelConfigurationType itemModelObject = item.getModelObject();
            String key = "";
            if (isMenuActive(storageConfig, itemModelObject)) {
                key = "DetailsNavigationPanel.srCurrentMessage";
            }
            if (hasActiveSubmenu(storageConfig, itemModelObject)) {
                key = "DetailsNavigationPanel.srActiveSubItemMessage";
            }
            return getPageBase().createStringResource(key).getString();
        });
        srCurrentMessage.add(new VisibleBehaviour(() -> {
            ContainerPanelConfigurationType storageConfig = getConfigurationFromStorage();
            ContainerPanelConfigurationType itemModelObject = item.getModelObject();
            return isMenuActive(storageConfig, itemModelObject) || hasActiveSubmenu(storageConfig, itemModelObject);
        }));
        link.add(srCurrentMessage);
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
        submenuLink.add(new VisibleBehaviour(() -> isSubMenuVisible(item.getModelObject())));
        link.add(submenuLink);
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
        subPanel.add(new VisibleBehaviour(() -> isSubMenuVisible(item.getModelObject())));
        return subPanel;
    }

    private boolean isSubMenuVisible(ContainerPanelConfigurationType panelConfig) {
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
                    .anyMatch(subPanelConfig ->objectDetailsModel.containsModelForSubmenu(subPanelConfig.getIdentifier()));
        }
        return true;
    }

    private IModel<String> createCountModel(IModel<ContainerPanelConfigurationType> panelModel) {
        return () -> {
            SimpleCounter<ObjectDetailsModels<O>, O> counter = getCounterProvider(panelModel);
            if (counter == null) {
                return null;
            }
            int count = counter.count(objectDetailsModel, getPageBase());
            return String.valueOf(count);
        };
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

    private IModel<String> createButtonLabel(IModel<ContainerPanelConfigurationType> model) {

        return () -> {
            ContainerPanelConfigurationType config = model.getObject();

            if (config.getDisplay() == null) {
                return "N/A";
            }

            if (config.getDisplay().getLabel() == null) {
                return "N/A";
            }

            return LocalizationUtil.translatePolyString(config.getDisplay().getLabel());
        };
    }

    private IModel<String> getMenuItemIconClass(IModel<ContainerPanelConfigurationType> item) {
        return () -> {
            ContainerPanelConfigurationType config = item.getObject();
            if (config == null || config.getDisplay() == null) {
                return GuiStyleConstants.CLASS_CIRCLE_FULL;
            }
            String iconCss = GuiDisplayTypeUtil.getIconCssClass(config.getDisplay());
            return StringUtils.isNoneEmpty(iconCss) ? iconCss : GuiStyleConstants.CLASS_CIRCLE_FULL;
        };
    }
}
