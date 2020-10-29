/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by honchar
 */
public abstract class AbstractPopupTabPanel<O extends ObjectType> extends BasePanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_LIST_PANEL = "objectListPanel";
    protected static final String ID_PARAMETERS_PANEL = "parametersPanel";
    protected static final String ID_PARAMETERS_PANEL_FRAGMENT = "parametersPanelFragment";

    protected List<O> preSelectedObjects = new ArrayList<>();

    public AbstractPopupTabPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        add(initObjectListPanel());

        Fragment parametersPanelFragment = new Fragment(ID_PARAMETERS_PANEL, ID_PARAMETERS_PANEL_FRAGMENT, this);
        parametersPanelFragment.setOutputMarkupId(true);

        initParametersPanel(parametersPanelFragment);
        add(parametersPanelFragment);
    }

    protected Component initObjectListPanel(){
        PopupObjectListPanel<O> listPanel = new PopupObjectListPanel<O>(ID_OBJECT_LIST_PANEL, (Class)getObjectType().getClassDefinition(),
                true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<IColumn<SelectableBean<O>, String>> createDefaultColumns() {
                if (AbstractRoleType.class.isAssignableFrom(getType())){
                    List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<>();
                    columns.addAll((Collection)ColumnUtils.getDefaultAbstractRoleColumns(false));
                    return columns;
                } else {
                    return super.createDefaultColumns();
                }
            }

            @Override
            protected void onUpdateCheckbox(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                onSelectionPerformed(target, rowModel);
            }

            @Override
            protected List<O> getPreselectedObjectList() {
                return getPreselectedObjects();
            }

            @Override
            protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel) {
                return getObjectSelectCheckBoxEnableModel(rowModel);
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                ObjectQuery customQuery = AbstractPopupTabPanel.this.addFilterToContentQuery();
                if (customQuery == null) {
                    customQuery = AbstractPopupTabPanel.this.getPageBase().getPrismContext().queryFactory().createQuery();
                }
                List<ObjectReferenceType> archetypeRefList = getArchetypeRefList();
                if (!CollectionUtils.isEmpty(archetypeRefList)) {
                    List<ObjectFilter> archetypeRefFilterList = new ArrayList<>();

                    for (ObjectReferenceType archetypeRef : archetypeRefList) {
                        ObjectFilter filter = AbstractPopupTabPanel.this.getPageBase().getPrismContext().queryFor(AssignmentHolderType.class)
                                .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(archetypeRef.getOid())
                                .buildFilter();
                        ((RefFilter) filter).setTargetTypeNullAsAny(true);
                        ((RefFilter) filter).setRelationNullAsAny(true);
                        archetypeRefFilterList.add(filter);
                    }
                    if (!CollectionUtils.isEmpty(archetypeRefFilterList)) {
                        OrFilter archetypeRefOrFilter =
                                AbstractPopupTabPanel.this.getPageBase().getPrismContext().queryFactory().createOr(archetypeRefFilterList);
                        customQuery.addFilter(archetypeRefOrFilter);
                    }
                }

                ObjectFilter subTypeFilter = getSubtypeFilter();
                if (subTypeFilter != null) {
                    customQuery.addFilter(subTypeFilter);
                }
                return customQuery;
            }

        };
        listPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return isObjectListPanelVisible();
            }
        });
        listPanel.setOutputMarkupId(true);
        return listPanel;
    }

    protected abstract void initParametersPanel(Fragment parametersPanel);

    protected List<O> getPreselectedObjects() {
        return null;
    }

    protected List<O> getSelectedObjectsList() {
        PopupObjectListPanel objectListPanel = getObjectListPanel();
        if (objectListPanel == null) {
            return new ArrayList<>();
        }
        return objectListPanel.getSelectedRealObjects();
    }

    protected PopupObjectListPanel getObjectListPanel() {
        return (PopupObjectListPanel) get(ID_OBJECT_LIST_PANEL);
    }

    protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
    }

    protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel) {
        return Model.of(true);
    }

    protected ObjectQuery addFilterToContentQuery() {
        return null;
    }

    protected List<ObjectReferenceType> getArchetypeRefList() {
        return null;
    }

    protected ObjectFilter getSubtypeFilter() {
        return null;
    }

    protected boolean isObjectListPanelVisible() {
        return true;
    }

    protected abstract ObjectTypes getObjectType();

    protected boolean isInducement() {
        return false;
    }
}
