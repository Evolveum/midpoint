/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

import java.io.Serializable;

public class StatisticBoxDto<T> implements Serializable {

    IModel<DisplayType> displayModel;
    IModel<IResource> messageImageResourceModel;

    IModel<DoughnutChartConfiguration> doughnutChartConfigurationIModel;

    public StatisticBoxDto(IModel<DisplayType> displayModel, IModel<IResource> messageImageResourceModel) {
        this.messageImageResourceModel = messageImageResourceModel;
        this.displayModel = displayModel;
    }

    public IResource getMessageImageResource() {
        return messageImageResourceModel != null ? messageImageResourceModel.getObject() : null;
    }

    public String getBoxTitle() {
        return displayModel != null ? GuiDisplayTypeUtil.getTranslatedLabel(displayModel.getObject()) : "";
    }

    public String getBoxDescription() {
        return displayModel != null ? GuiDisplayTypeUtil.getHelp(displayModel.getObject()) : "";
    }

    public String getBoxImageCss() {
        return displayModel != null ? GuiDisplayTypeUtil.getIconCssClass(displayModel.getObject()) : "";
    }

    public IModel<DoughnutChartConfiguration> getDoughnutChartConfigurationIModel() {
        return doughnutChartConfigurationIModel;
    }

    public void setDoughnutChartConfigurationIModel(IModel<DoughnutChartConfiguration> doughnutChartConfigurationIModel) {
        this.doughnutChartConfigurationIModel = doughnutChartConfigurationIModel;
    }

    public T getStatisticObject() {
        return null;
    }
}
