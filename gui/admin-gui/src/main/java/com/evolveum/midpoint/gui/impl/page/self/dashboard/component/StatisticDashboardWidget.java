/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import java.util.List;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PreviewContainerPanelConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@PanelType(name = "statisticWidget")
public class StatisticDashboardWidget extends BasePanel<PreviewContainerPanelConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticDashboardWidget.class);
    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
    private static final String ID_STATISTIC_DATA = "statisticData";
    private static final String ICON_DEFAULT_CSS_CLASS = "fa fa-angle-double-right";

    public StatisticDashboardWidget(String id, IModel<PreviewContainerPanelConfigurationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private GuiActionType getDefinedAction() {
        PreviewContainerPanelConfigurationType previewConfig = getModelObject();
        List<GuiActionType> actions = previewConfig.getAction();
        if (actions.size() > 1) {
            LOGGER.debug("More than one action defined. Check your configuration");
        }

        return actions.iterator().next();
    }

    private void initLayout() {
        Link<Void> linkItem = new Link<>(ID_LINK) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                DetailsPageUtil.redirectFromDashboardWidget(getDefinedAction(),
                        StatisticDashboardWidget.this.getPageBase());
            }

        };
        add(linkItem);

        Label icon = new Label(ID_IMAGE);
        icon.add(AttributeAppender.append("class", getIconClassModel()));
        icon.add(AttributeAppender.append("style", getIconStyleModel()));
        linkItem.add(icon);

        linkItem.add(new Label(ID_LABEL, () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            PolyStringType label = WebComponentUtil.getCollectionLabel(panel.getDisplay());
            return WebComponentUtil.getTranslatedPolyString(label);
        }));

        Label description = new Label(ID_DESCRIPTION, () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            return GuiDisplayTypeUtil.getHelp(panel.getDisplay());
        });
        description.setEnabled(false);
        linkItem.add(description);

        Label statisticData = new Label(ID_STATISTIC_DATA, getCollectionViewCountLabelModel());
        linkItem.add(statisticData);
    }

    private IModel<String> getIconClassModel() {
        return () -> {
            ContainerPanelConfigurationType panel = StatisticDashboardWidget.this.getModelObject();
            String cssClass = GuiDisplayTypeUtil.getIconCssClass(panel.getDisplay());
            if (StringUtils.isEmpty(cssClass)) {
                cssClass = ICON_DEFAULT_CSS_CLASS;
            }
            return cssClass;
        };
    }

    private IModel<String> getCollectionViewCountLabelModel() {
        return () -> {
            CompiledObjectCollectionView view = getObjectCollectionView();
            if (view == null) {
                return "";
            }
            ObjectFilter filter = view.getFilter();
            Class<? extends Containerable> type = (Class<? extends Containerable>) WebComponentUtil.qnameToClass(view.getContainerType());
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

    private IModel<String> getIconStyleModel() {
        return () -> {
            String iconColor = GuiDisplayTypeUtil.getIconColor(getModelObject().getDisplay());
            if (StringUtils.isEmpty(iconColor)) {
                return null;
            }

            return "background-color: " + iconColor + " !important";
        };
    }
}
