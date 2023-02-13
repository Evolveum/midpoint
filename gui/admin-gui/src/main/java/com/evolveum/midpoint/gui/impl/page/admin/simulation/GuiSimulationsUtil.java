/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class GuiSimulationsUtil {

    //todo move elsewhere
    @Deprecated
    public StringResourceModel createStringResource(String resourceKey, IModel<?> model, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(model)
                .setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    //todo move elsewhere
    @Deprecated
    public static String getString(Component component, String key, Object... params) {
        return new StringResourceModel(key, component)
                .setDefaultValue(key)
                .setParameters(params).getString();
    }

    public static Label createProcessedObjectStateLabel(String id, IModel<SimulationResultProcessedObjectType> model) {
        Label label = new Label(id, () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            return getString(null, WebComponentUtil.createEnumResourceKey(state));
        });
        label.add(AttributeModifier.append("class", () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            switch (state) {
                case ADDED:
                    return Badge.State.SUCCESS.getCss();
                case DELETED:
                    return Badge.State.DANGER.getCss();
                case MODIFIED:
                    return Badge.State.INFO.getCss();
                case UNMODIFIED:
                default:
                    return Badge.State.SECONDARY.getCss();
            }
        }));

        return label;
    }

    public static String getProcessedObjectType(@NotNull IModel<SimulationResultProcessedObjectType> model) {
        SimulationResultProcessedObjectType object = model.getObject();
        if (object == null || object.getType() == null) {
            return null;
        }

        QName type = object.getType();
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);
        String key = WebComponentUtil.createEnumResourceKey(ot);

        return getString(null, key);
    }

    public static IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createProcessedObjectIconColumn() {
        return new RoundedIconColumn<>(null) {

            @Override
            protected DisplayType createDisplayType(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                SimulationResultProcessedObjectType object = model.getObject().getValue();
                ObjectType obj = object.getBefore() != null ? object.getBefore() : object.getAfter();
                if (obj == null || obj.asPrismObject() == null) {
                    return new DisplayType()
                            .icon(new IconType().cssClass(WebComponentUtil.createDefaultColoredIcon(object.getType())));
                }

                return new DisplayType()
                        .icon(new IconType().cssClass(WebComponentUtil.createDefaultIcon(obj.asPrismObject())));
            }
        };
    }
}
