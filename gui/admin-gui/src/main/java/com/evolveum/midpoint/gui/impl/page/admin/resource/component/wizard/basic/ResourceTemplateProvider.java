/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * @author lskublik
 */
public class ResourceTemplateProvider
        extends ObjectDataProvider<TemplateTile<ResourceTemplateProvider.ResourceTemplate>, AssignmentHolderType> {

    private static final String DOT_CLASS = ResourceTemplateProvider.class.getName() + ".";
    private static final String OPERATION_GET_DISPLAY = DOT_CLASS + "getDisplay";

    private final IModel<TemplateType> type;

    public ResourceTemplateProvider(Component component, IModel<Search<? extends AssignmentHolderType>> search, IModel<TemplateType> type) {
        super(component, (IModel) search);
        this.type = type;
    }

    public enum TemplateType {
        TEMPLATE(ResourceType.class),
        CONNECTOR(ConnectorType.class);

        private final Class<? extends AssignmentHolderType> type;

        TemplateType(Class<? extends AssignmentHolderType> type) {
            this.type = type;
        }

        public Class<? extends AssignmentHolderType> getType() {
            return type;
        }
    }

    // Here we apply the distinct option. It is easier and more reliable to apply it here than to do at all the places
    // where options for this provider are defined.
    protected Collection<SelectorOptions<GetOperationOptions>> getOptionsToUse() {
        @NotNull Collection<SelectorOptions<GetOperationOptions>> rawOption = getOperationOptionsBuilder().raw().build();
        return GetOperationOptions.merge(getPrismContext(), getOptions(), getDistinctRelatedOptions(), rawOption);
    }

    @Override
    protected ObjectQuery getCustomizeContentQuery() {
        if (TemplateType.TEMPLATE == type.getObject()) {
            return PrismContext.get().queryFor(ResourceType.class)
                    .item(ResourceType.F_TEMPLATE)
                    .eq(true)
                    .build();
        }
        if (TemplateType.CONNECTOR == type.getObject()) {
            return PrismContext.get().queryFor(ConnectorType.class)
                    .item(ConnectorType.F_AVAILABLE)
                    .eq(true)
                    .build();
        }
        return null;
    }

    public TemplateTile<ResourceTemplate> createDataObjectWrapper(PrismObject<AssignmentHolderType> obj) {
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
                    new ResourceTemplate(obj.getOid(), ConnectorType.COMPLEX_TYPE))
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
                new ResourceTemplate(obj.getOid(), ResourceType.COMPLEX_TYPE))
                .description(obj.asObjectable().getDescription())
                .addTag(new DisplayType().label(tag));

    }

    private String getDescriptionForConnectorType(@NotNull ConnectorType connectorObject) {
        if (connectorObject.getDescription() == null) {
            return connectorObject.getName().getOrig();
        }
        return connectorObject.getDescription();
    }

    protected class ResourceTemplate implements Serializable {

        private final String oid;
        private final QName type;

        ResourceTemplate(String oid, QName type) {
            this.oid = oid;
            this.type = type;
        }

        public QName getType() {
            return type;
        }

        public String getOid() {
            return oid;
        }
    }
}
