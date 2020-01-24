package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.server.TaskOperationStatisticsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class TaskOperationStatisticsPanelFactory implements GuiComponentFactory<PrismContainerPanelContext<OperationStatsType>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return QNameUtil.match(OperationStatsType.COMPLEX_TYPE, wrapper.getTypeName());
    }

    @Override
    public Panel createPanel(PrismContainerPanelContext<OperationStatsType> panelCtx) {
        return new TaskOperationStatisticsPanel(panelCtx.getComponentId(), panelCtx.getValueWrapper());
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
