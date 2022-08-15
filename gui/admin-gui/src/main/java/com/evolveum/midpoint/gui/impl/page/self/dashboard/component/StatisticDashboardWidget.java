/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.web.session.ObjectDetailsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.flow.RedirectToUrlException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PanelType(name = "statisticWidget")
public class StatisticDashboardWidget extends BasePanel<ContainerPanelConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticDashboardWidget.class);
    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
    private static final String ID_STATISTIC_DATA = "statisticData";
    private static final String ICON_DEFAULT_CSS_CLASS = "fa fa-angle-double-right";

    public StatisticDashboardWidget(String id, IModel<ContainerPanelConfigurationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Link<Void> linkItem = new Link<>(ID_LINK) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                redirectActionPerformed();
            }

        };
        add(linkItem);

        Label icon = new Label(ID_IMAGE);
        icon.add(AttributeAppender.append("class", getIconClassModel()));
        linkItem.add(icon);

        linkItem.add(new Label(ID_LABEL, () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            return WebComponentUtil.getCollectionLabel(panel.getDisplay());
        }));

        Label description = new Label(ID_DESCRIPTION, () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            return WebComponentUtil.getHelp(panel.getDisplay());
        });
        description.setEnabled(false);
        linkItem.add(description);

        Label statisticData = new Label(ID_STATISTIC_DATA, getCollectionViewCountLabelModel());
        linkItem.add(statisticData);
    }

    private void redirectActionPerformed() {
        List<GuiActionType> actionList = getModelObject().getAction();
        if (CollectionUtils.isEmpty(actionList)) {
            return;
        }
        Optional<GuiActionType> actionWithRedirection = actionList.stream().filter(this::isRedirectionTargetNotEmpty).findFirst();
        if (actionWithRedirection.isEmpty()) {
            return;
        }
        RedirectionTargetType redirectionTarget = actionWithRedirection.get().getTarget();
        String url = redirectionTarget.getTargetUrl();
        String pageClass = redirectionTarget.getPageClass();
        String panelType = redirectionTarget.getPanelType();
        if (StringUtils.isNotEmpty(url) && new UrlValidator().isValid(url)) {
            throw new RedirectToUrlException(url);
        } else if (!StringUtils.isAllEmpty(pageClass, panelType)) {
            redirectToPanel(pageClass, panelType);
        }
    }

    private boolean isRedirectionTargetNotEmpty(GuiActionType action) {
        if (action == null || action.getTarget() == null) {
            return false;
        }
        return !StringUtils.isAllEmpty(action.getTarget().getTargetUrl(), action.getTarget().getPageClass(), action.getTarget().getPanelType());
    }

    private void redirectToPanel(String pageClass, String panelType) {
        if (StringUtils.isNotEmpty(pageClass)) {
            try {
                Class<?> clazz = Class.forName(pageClass);
                ContainerPanelConfigurationType config = null;
                if (hasContainerPanelConfigurationField(clazz)) {
                    Class<? extends Panel> panel = getPageBase().findObjectPanel(panelType);
                    //todo get subPanels from ContainerPanelConfigurationType? or get details page panels? and redirect exactly on panelType
                }
                if (config == null) {
                    config = new ContainerPanelConfigurationType();
                    config.setPanelType(panelType);
                }
                Constructor<?> constructor = clazz.getConstructor();
                Object pageInstance = constructor.newInstance();
                if (pageInstance instanceof AbstractPageObjectDetails && StringUtils.isNotEmpty(panelType)) {
                    String storageKey = "details" + ((AbstractPageObjectDetails<?, ?>) pageInstance).getType().getSimpleName();
                    ObjectDetailsStorage pageStorage = getPageBase().getSessionStorage().getObjectDetailsStorage(storageKey);
                    if (pageStorage == null) {
                        getPageBase().getSessionStorage().setObjectDetailsStorage(storageKey, config);
                    } else {
                        pageStorage.setDefaultConfiguration(config);
                    }
                } else if (pageInstance instanceof WebPage) {
                    getPageBase().navigateToNext((WebPage) pageInstance);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                LOGGER.trace("No constructor found for (String, LoadableModel, ContainerPanelConfigurationType). Continue with lookup.", e);
            }
        }
    }

    private boolean hasContainerPanelConfigurationField(Class<?> clazz) {
        return Arrays.stream(clazz.getFields()).anyMatch(this::isContainerPanelConfigurationField);
    }

    private boolean isContainerPanelConfigurationField(Field f) {
        return f.getType().equals(ContainerPanelConfigurationType.class);
    }

    private IModel<String> getIconClassModel() {
        return () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            String cssClass = WebComponentUtil.getIconCssClass(panel.getDisplay());
            if (StringUtils.isEmpty(cssClass)) {
                cssClass = ICON_DEFAULT_CSS_CLASS;
            }
            return "info-box-icon " + getIconColor() + cssClass;
        };
    }

    private IModel<String> getCollectionViewCountLabelModel() {
        return () -> {
            CompiledObjectCollectionView view = getObjectCollectionView();
            if (view == null) {
                return "";
            }
            ObjectFilter filter = view.getFilter();
            Class<? extends Containerable> type = (Class<? extends Containerable>) WebComponentUtil.qnameToClass(getPrismContext(), view.getContainerType());
            ObjectQuery query = getPrismContext().queryFor(type)
                    .build();
            if (filter != null) {
                query.addFilter(filter);
            }
            return "" + WebModelServiceUtils.countContainers(type, query, null, getPageBase());
        };
    }

    private CompiledObjectCollectionView getObjectCollectionView() {
        ContainerPanelConfigurationType config = getModelObject();
        GuiObjectListViewType view = config.getListView();
        if (view == null) {
            return null;
        }
        String viewIdentifier = view.getIdentifier();
        if (StringUtils.isEmpty(viewIdentifier)) {
            return null;
        }
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView(view.getType(), viewIdentifier);
    }

    private String getIconColor() {
        String iconColor = WebComponentUtil.getIconColor(getModelObject().getDisplay());
        if (StringUtils.isNotEmpty(iconColor)) {
            return iconColor.startsWith("bg-") ? iconColor : "bg-" + iconColor + " ";
        }
        return "";
    }

    private boolean isExternalLink() {
//        return getModelObject().getTargetUrl() != null && new UrlValidator().isValid(getModelObject().getTargetUrl());
        return false;
    }
}
