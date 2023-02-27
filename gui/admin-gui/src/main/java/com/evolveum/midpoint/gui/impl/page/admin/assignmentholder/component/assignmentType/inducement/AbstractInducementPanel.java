/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement;

import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.AbstractAssignmentTypePanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AbstractInducementPanel<AR extends AbstractRoleType> extends AbstractAssignmentTypePanel {

    public AbstractInducementPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, null, config, model.getObject().getTypeClass(), model.getObject().getOid());

        setModel(PrismContainerWrapperModel.fromContainerWrapper(model, AbstractRoleType.F_INDUCEMENT, (Supplier<PageBase> & Serializable) () -> getPageBase()));
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return new ArrayList<>();
    }

    @NotNull
    protected IModel<AssignmentPopupDto> createAssignmentPopupModel() {
        return new LoadableModel<>(false) {

            @Override
            protected AssignmentPopupDto load() {
                return new AssignmentPopupDto(null);
            }
        };
    }

    @Override
    protected QName getAssignmentType() {
        return null;
    }

    @Override
    protected String getStorageKey() {
        return SessionStorage.KEY_INDUCEMENTS_TAB;
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        RefFilter targetRefFilter = getTargetTypeFilter();
        if (targetRefFilter != null) {
            return getPrismContext().queryFactory().createQuery(targetRefFilter);
        }
        return null;
    }

    @Override
    protected void addSpecificSearchableItemWrappers(PrismContainerDefinition<AssignmentType> containerDef, List<? super FilterableSearchItemWrapper> defs) {

    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCEMENTS_TAB_TABLE;
    }
}
