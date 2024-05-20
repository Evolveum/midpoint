/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.menu.MultivalueDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.ComplexTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.PrismSchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

//@PanelType(name = "complex-type-definitions")
//@PanelInstance(
//        identifier = "complex-type-definitions",
//        applicableForType = SchemaType.class,
//        display = @PanelDisplay(label = "ComplexTypeDefinitionPanelList.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 20))
public class ComplexTypeDefinitionPanelList extends MultivalueContainerListPanel<ComplexTypeDefinitionType> implements MultivalueDetailsPanel<ComplexTypeDefinitionType> {

    private final IModel<PrismContainerWrapper<ComplexTypeDefinitionType>> model;
    private final AssignmentHolderDetailsModel detailsModel;

    public ComplexTypeDefinitionPanelList(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, ComplexTypeDefinitionType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                        ComplexTypeDefinitionPanel.PRISM_SCHEMA,
                        PrismSchemaType.F_COMPLEX_TYPE),
                (SerializableSupplier<PageBase>) () -> getPageBase());
        this.detailsModel = model;
    }

    @Override
    protected void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> rowModel, List<PrismContainerValueWrapper<ComplexTypeDefinitionType>> listItems) {
        showSubMenu(rowModel, detailsModel, getPanelConfiguration(), getPageBase(), target);
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<ComplexTypeDefinitionType>> getContainerModel() {
        return model;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<ComplexTypeDefinitionType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ComplexTypeDefinitionType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), ComplexTypeDefinitionType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> model) {
                        ComplexTypeDefinitionPanelList.this.editItemPerformed(target, model, getSelectedItems());
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), ComplexTypeDefinitionType.F_DISPLAY_NAME,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), ComplexTypeDefinitionType.F_DISPLAY_ORDER,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()),
                new LifecycleStateColumn<>(getContainerModel(), getPageBase())
        );
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
