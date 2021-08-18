/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.assignmentType.AbstractAssignmentTypePanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.List;

public class AbstractInducementPanel<AR extends AbstractRoleType> extends AbstractAssignmentTypePanel {

    public AbstractInducementPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, PrismContainerWrapperModel.fromContainerWrapper(model, AbstractRoleType.F_INDUCEMENT), config);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return null;
    }

    @Override
    protected IModel<AssignmentPopupDto> createAssignmentPopupModel() {
        return null;
    }

    @Override
    protected String getAssignmentsTabStorageKey() {
        return SessionStorage.KEY_INDUCEMENTS_TAB;
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        return null;
    }

    @Override
    protected void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef, List<SearchItemDefinition> defs) {

    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCEMENTS_TAB_TABLE;
    }
}
