/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplate;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplate.TemplateType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author lskublik
 */
public class ResourceTemplateProvider
        extends TemplateTileProvider<ResourceTemplate, AssignmentHolderType> {

    private static final String DOT_CLASS = ResourceTemplateProvider.class.getName() + ".";
    private static final String OPERATION_GET_DISPLAY = DOT_CLASS + "getDisplay";

    private final IModel<TemplateType> type;

    public ResourceTemplateProvider(Component component, IModel<Search> search, IModel<TemplateType> type) {
        super(component, (IModel) search);
        this.type = type;
    }

    // Here we apply the distinct option. It is easier and more reliable to apply it here than to do at all the places
    // where options for this provider are defined.
    protected Collection<SelectorOptions<GetOperationOptions>> getOptionsToUse() {
        @NotNull Collection<SelectorOptions<GetOperationOptions>> rawOption = getOperationOptionsBuilder().raw().build();
        return GetOperationOptions.merge(getOptions(), getDistinctRelatedOptions(), rawOption);
    }

    @Override
    protected ObjectQuery getCustomizeContentQuery() {
        if (TemplateType.INHERIT_TEMPLATE == type.getObject()
                || TemplateType.COPY_FROM_TEMPLATE == type.getObject()) {
            return PrismContext.get().queryFor(ResourceType.class)
                    .item(ResourceType.F_TEMPLATE)
                    .eq(true)
                    .build();
        }
        if (TemplateType.CONNECTOR == type.getObject()) {
            return PrismContext.get().queryFor(ConnectorType.class)
                    .item(ConnectorType.F_AVAILABLE)
                    .eq(true)
                    .and()
                    .not()
                    .item(ConnectorType.F_CONNECTOR_TYPE)
                    .eq("AsyncUpdateConnector")
                    .and()
                    .not()
                    .item(ConnectorType.F_CONNECTOR_TYPE)
                    .eq("AsyncProvisioningConnector")
                    .build();
        }
        return null;
    }

    @Override
    public ObjectPaging createPaging(long offset, long pageSize) {
        if (type != null && TemplateType.CONNECTOR == type.getObject()) {
            setSort("displayName", getDefaultSortOrder());
        } else {
            setSort(getDefaultSortParam(), getDefaultSortOrder());
        }
        return super.createPaging(offset, pageSize);
    }

    @Override
    protected TemplateTile<ResourceTemplate> createTileObject(PrismObject<AssignmentHolderType> obj) {
        if (obj.getCompileTimeClass().isAssignableFrom(ConnectorType.class)) {
            @NotNull ConnectorType connectorObject = (ConnectorType) obj.asObjectable();
            String title;
            if (connectorObject.getDisplayName() == null || connectorObject.getDisplayName().isEmpty()) {
                title = connectorObject.getName().getOrig();
            } else {
                title = connectorObject.getDisplayName().getOrig();
            }
            return new TemplateTile(
                    GuiStyleConstants.CLASS_OBJECT_CONNECTOR_ICON,
                    title,
                    new ResourceTemplate(obj.getOid(), type.getObject()))
                    .description(getDescriptionForConnectorType(connectorObject))
                    .addTag(new DisplayType().label(connectorObject.getConnectorVersion()));
        }

        String title = WebComponentUtil.getDisplayNameOrName(obj);

        OperationResult result = new OperationResult(OPERATION_GET_DISPLAY);

        DisplayType display =
                GuiDisplayTypeUtil.getDisplayTypeForObject(obj, result, getPageBase());
        PolyStringType tag = new PolyStringType("template");
        tag.setTranslation(new PolyStringTranslationType().key("CreateResourceTemplatePanel.template"));
        return new TemplateTile(
                GuiDisplayTypeUtil.getIconCssClass(display),
                title,
                new ResourceTemplate(obj.getOid(), type.getObject()))
                .description(obj.asObjectable().getDescription())
                .addTag(new DisplayType().label(tag));
    }

    private String getDescriptionForConnectorType(@NotNull ConnectorType connectorObject) {
        if (connectorObject.getDescription() == null) {
            return connectorObject.getName().getOrig();
        }
        return connectorObject.getDescription();
    }

}
