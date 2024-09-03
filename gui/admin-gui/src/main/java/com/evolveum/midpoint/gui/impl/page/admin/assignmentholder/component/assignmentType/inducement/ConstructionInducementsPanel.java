/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleInducementPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

@PanelType(name = "constructionInducements")
@PanelInstance(identifier = "constructionInducements",
        applicableForType = AbstractRoleType.class,
        childOf = AbstractRoleInducementPanel.class,
        display = @PanelDisplay(label = "ObjectType.ResourceType", icon = GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON, order = 50))
public class ConstructionInducementsPanel<AR extends AbstractRoleType> extends AbstractInducementPanel<AR> {

    public ConstructionInducementsPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected QName getAssignmentType() {
        return ResourceType.COMPLEX_TYPE;
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION).build();
    }

    @Override
    protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
        if (getPageBase() instanceof PageAbstractRole) {
            ((PageAbstractRole) getPageBase()).showConstructionWizard(target);
            return;
        }
        super.newAssignmentClickPerformed(target);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return ColumnUtils.createInducedAssociationsColumns(getContainerModel(), getPageBase());
    }
}
