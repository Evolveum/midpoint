/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.TitleWithMarks;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShadowAssociationObjectsColumn extends PrismContainerWrapperColumn<ShadowAssociationValueType> {

    public ShadowAssociationObjectsColumn(IModel<? extends PrismContainerDefinition<ShadowAssociationValueType>> rowModel, PageBase pageBase) {
        super(rowModel, ShadowAssociationValueType.F_OBJECTS, pageBase);
    }

    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new PrismContainerWrapperColumnPanel<>(componentId, (IModel<PrismContainerWrapper<ShadowAttributesType>>) rowModel, getColumnType()){
            @Override
            protected void populate(String id, ListItem<PrismContainerValueWrapper<ShadowAttributesType>> item) {
                if (item.getModelObject() != null && item.getModelObject().getParent() != null) {
                    item.getModelObject().getParent().revive(getPageBase().getPrismContext());
                }

                populateTitle(id, item);

            }
        };
    }

    private String populateTitle(String id, ListItem<PrismContainerValueWrapper<ShadowAttributesType>> item) {
        PrismContainerValueWrapper<ShadowAttributesType> object = item.getModelObject();

        RepeatingView repeatingView = new RepeatingView(id);
        item.add(repeatingView);

        List<String> stringValues = new ArrayList<>();
        Iterator<? extends ItemWrapper<?, ?>> iterator = object.getItems().iterator();
        while (iterator.hasNext()) {
            ItemWrapper<?, ?> itemWrapper = iterator.next();
            if (itemWrapper.getValues().isEmpty()) {
                continue;
            }

            for (PrismValueWrapper value : itemWrapper.getValues()) {
                Object realValue = value.getRealValue();

                if (realValue != null) {
                    if (realValue instanceof Referencable reference) {
                        if (reference.getOid() != null) {
                            Model<String> name = Model.of(WebComponentUtil.getReferencedObjectDisplayNamesAndNames(reference, false));
                            repeatingView.add(new TitleWithMarks(repeatingView.newChildId(), name, createRealMarksList(reference)){
                                @Override
                                protected boolean isTitleLinkEnabled() {
                                    return false;
                                }
                            });
                        }
                    } else {
                        repeatingView.add(new TitleWithMarks(repeatingView.newChildId(), Model.of(realValue.toString()), Model.of()){
                            @Override
                            protected boolean isTitleLinkEnabled() {
                                return false;
                            }
                        });
                    }
                }
            }
        }

        return StringUtils.join(stringValues, ", ");
    }

    private IModel<String> createRealMarksList(Referencable reference) {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                Task task = getPageBase().createSimpleTask(ShadowAssociationObjectsColumn.class.getName() + ".resolveReference");
                PrismObject<ObjectType> shadow = WebModelServiceUtils.resolveReferenceNoFetch(reference, getPageBase(), task, task.getResult());
                if (shadow == null) {
                    return "";
                }

                ObjectType shadowBean = shadow.getRealValue();
                return WebComponentUtil.createMarkList(shadowBean, getPageBase());
            }
        };
    }
}
