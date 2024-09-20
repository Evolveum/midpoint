/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.defaultOperationPolicies;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DefaultOperationPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowMarkingConfigurationType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class DefaultOperationPoliciesTable extends AbstractWizardTable<DefaultOperationPolicyConfigurationType, ResourceObjectTypeDefinitionType> {

    public DefaultOperationPoliciesTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config, DefaultOperationPolicyConfigurationType.class);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<DefaultOperationPolicyConfigurationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<DefaultOperationPolicyConfigurationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<DefaultOperationPolicyConfigurationType>> defaultOperationPolicyDef = getDefaultOperationPolicyDefinition();
        columns.add(new PrismReferenceWrapperColumn<>(
                defaultOperationPolicyDef,
                DefaultOperationPolicyConfigurationType.F_POLICY_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-6";
            }
        });

        columns.add(new LifecycleStateColumn<>(defaultOperationPolicyDef, getPageBase()));

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<DefaultOperationPolicyConfigurationType>> getDefaultOperationPolicyDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<DefaultOperationPolicyConfigurationType> load() {
                return getValueModel().getObject().getDefinition()
                        .findContainerDefinition(ResourceObjectTypeDefinitionType.F_DEFAULT_OPERATION_POLICY);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_DEFAULT_OPERATION_POLICIES_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "DefaultOperationPoliciesTable.newObject";
    }

    @Override
    protected IModel<PrismContainerWrapper<DefaultOperationPolicyConfigurationType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(getValueModel(), ResourceObjectTypeDefinitionType.F_DEFAULT_OPERATION_POLICY);
    }

    @Override
    protected boolean isDuplicationSupported() {
        return false;
    }
}
