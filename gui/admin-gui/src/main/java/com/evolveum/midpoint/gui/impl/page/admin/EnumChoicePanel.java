/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public abstract class EnumChoicePanel<T extends TileEnum> extends AbstractTemplateChoicePanel<T> {

    private final Class<T> tileTypeClass;

    public EnumChoicePanel(String id, Class<T> tileTypeClass) {
        super(id);
        Validate.isAssignableFrom(Enum.class, tileTypeClass);
        this.tileTypeClass = tileTypeClass;
    }

    @Override
    protected LoadableModel<List<Tile<T>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<T>> load() {
                List<Tile<T>> list = new ArrayList<>();

                for (T type : tileTypeClass.getEnumConstants()) {
                    Tile tile = new Tile(type.getIcon(), getTitleOfEnum(type));
                    tile.setValue(type);
                    tile.setDescription(getDescriptionForTile(type));
                    list.add(tile);
                }
                return list;
            }
        };
    }

    protected String getTitleOfEnum(T type) {
        return getString((Enum) type);
    }

    protected String getDescriptionForTile(T type) {
        return type.getDescription();
    }

    @Override
    protected WebMarkupContainer createTemplateIconPanel(IModel<Tile<T>> tileModel, String idIcon) {
        WebMarkupContainer icon = new WebMarkupContainer(idIcon);
        icon.add(AttributeAppender.append("class", () -> tileModel.getObject().getIcon()));
        return icon;
    }

    @Override
    protected QName getType() {
        return ObjectType.COMPLEX_TYPE;
    }

    @Override
    protected abstract IModel<String> getTextModel();

    @Override
    protected abstract IModel<String> getSubTextModel();
}
