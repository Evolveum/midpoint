/*
 * Copyright (c) 2010-2022 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class RichHyperlinkListContentPanel extends MultivalueContainerListPanelWithDetailsPanel<RichHyperlinkType> {

    private IModel<PrismContainerWrapper<RichHyperlinkType>> model;

    public RichHyperlinkListContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType, ItemPath containerRealPath) {
        super(id, RichHyperlinkType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), containerRealPath);
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<RichHyperlinkType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<RichHyperlinkType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), RichHyperlinkType.F_LABEL,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<RichHyperlinkType>> model) {
                        RichHyperlinkListContentPanel.this.itemDetailsPerformed(target, model);
                    }
                }
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<RichHyperlinkType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<RichHyperlinkType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<RichHyperlinkType>> item) {

        return new RichHyperlinkDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }
}
