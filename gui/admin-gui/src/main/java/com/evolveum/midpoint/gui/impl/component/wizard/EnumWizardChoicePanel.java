/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public abstract class EnumWizardChoicePanel<T extends TileEnum, AHD extends AssignmentHolderDetailsModel>
        extends WizardChoicePanel<T, AHD> {

    private final Class<T> tileTypeClass;

    /**
    * @param tileTypeClass have to be Enum class
    **/
    public EnumWizardChoicePanel(String id, AHD resourceModel, Class<T> tileTypeClass) {
        super(id, resourceModel);
        Validate.isAssignableFrom(Enum.class, tileTypeClass);
        this.tileTypeClass = tileTypeClass;
    }

    protected LoadableModel<List<Tile<T>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<T>> load() {
                List<Tile<T>> list = new ArrayList<>();

                for (T type : tileTypeClass.getEnumConstants()) {
                    Tile tile = new Tile(type.getIcon(), getString((Enum) type));
                    tile.setValue(type);
                    list.add(tile);
                }
                addDefaultTile(list);

                return list;
            }
        };
    }

    protected void addDefaultTile(List<Tile<T>> list) {
        if (addDefaultTile()) {
            list.add(createDefaultTile(getObjectType()));
        }
    }

    protected boolean addDefaultTile() {
        return true;
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<T>> tileModel) {
        return new TilePanel(id, tileModel) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                Tile<T> tile = tileModel.getObject();
                onTileClick(tile.getValue(), target);
            }
        };
    }

    private void onTileClick(T value, AjaxRequestTarget target) {
        if (value == null) {
            goToObjectPerformed(getObjectType());
            return;
        }
        this.onTileClickPerformed(value, target);
    }

    protected abstract QName getObjectType();

    protected abstract void onTileClickPerformed(T value, AjaxRequestTarget target);

    private Tile<T> createDefaultTile(QName type) {
        String icon = IconAndStylesUtil.createDefaultBlackIcon(type);
        if (StringUtils.isEmpty(icon)) {
            icon = "fa fa-server";
        }
        return new Tile<>(
                icon,
                getPageBase().createStringResource(
                        "WizardChoicePanel.toObject" ,
                        WebComponentUtil.translateMessage(
                                ObjectTypeUtil.createTypeDisplayInformation(type.getLocalPart(), false))
                ).getString());
    }

    protected void goToObjectPerformed(QName type) {
        Class<? extends ObjectType> typeClass = WebComponentUtil.qnameToClass(PrismContext.get(), type, ObjectType.class);
        if (typeClass == null) {
            return;
        }

        Class<? extends PageBase> detailPage = DetailsPageUtil.getObjectDetailsPage(typeClass);
        if (detailPage == null) {
            return;
        }

        getPageBase().removeLastBreadcrumb();
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getAssignmentHolderDetailsModel().getObjectType().getOid());
        getPageBase().navigateToNext(detailPage, parameters);
    }
}
