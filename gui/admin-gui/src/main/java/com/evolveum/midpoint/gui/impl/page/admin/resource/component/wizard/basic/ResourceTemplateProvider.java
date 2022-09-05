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
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSearchDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TypedCacheKey;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lazyman
 */
public class ResourceTemplateProvider
        extends ObjectDataProvider<TemplateTile<ResourceTemplateProvider.ResourceTemplate>, AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceTemplateProvider.class);
    private static final String DOT_CLASS = ResourceTemplateProvider.class.getName() + ".";
    private static final String OPERATION_GET_DISPLAY = DOT_CLASS + "getDisplay";

    private final IModel<TemplateType> type;

    public ResourceTemplateProvider(Component component, IModel<Search<AssignmentHolderType>> search, IModel<TemplateType> type) {
        super(component, search);
        this.type = type;
    }

    public enum TemplateType {
        TEMPLATE(ResourceType.class),
        CONNECTOR(ConnectorType.class);

        private final Class<AssignmentHolderType> type;

        private TemplateType(Class<? extends AssignmentHolderType> type) {
            this.type = (Class<AssignmentHolderType>) type;
        }

        public Class<AssignmentHolderType> getType() {
            return type;
        }
    }

    // Here we apply the distinct option. It is easier and more reliable to apply it here than to do at all the places
    // where options for this provider are defined.
    protected Collection<SelectorOptions<GetOperationOptions>> getOptionsToUse() {
        @NotNull Collection<SelectorOptions<GetOperationOptions>> rawOption = getOperationOptionsBuilder().raw().build();
        return GetOperationOptions.merge(getPrismContext(), getOptions(), getDistinctRelatedOptions(), rawOption);
    }

//    @Override
//    public Iterator<TemplateTile<ResourceTemplate>> internalIterator(long first, long count) {
//        LOGGER.trace("begin::iterator() from {} count {}.", first, count);
//
//        getAvailableData().clear();
//
//        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
//        try {
//            Task task = getPageBase().createSimpleTask(OPERATION_SEARCH_OBJECTS);
//
//            ObjectQuery query = getQuery();
//            if (query == null) {
//                query = getPrismContext().queryFactory().createQuery();
//            }
//
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Query {} with {}", getType().getSimpleName(), query.debugDump());
//            }
//
//            List<PrismObject<AssignmentHolderType>> list = getModelService().searchObjects(getType(), query, getOptionsToUse(), task, result);
//
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Query {} resulted in {} objects", getType().getSimpleName(), list.size());
//            }
//
//            List<TemplateTile<ResourceTemplate>> tiles = new ArrayList<>();
//            for (PrismObject<AssignmentHolderType> object : list) {
//                tiles.add(createDataObjectWrapper(object, result));
//            }
//            Collections.sort(tiles);
//            internalSize = tiles.size();
//            getAvailableData().addAll(
//                    tiles.stream()
//                            .skip(first)
//                            .limit(count)
//                            .collect(Collectors.toList()));
//        } catch (Exception ex) {
//            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list objects", ex);
//        } finally {
//            result.computeStatusIfUnknown();
//        }
//
//        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
//            handleNotSuccessOrHandledErrorInIterator(result);
//        }
//
//        LOGGER.trace("end::iterator()");
//        return getAvailableData().iterator();
//    }

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
                    .tag(connectorObject.getConnectorVersion());
        }

        String title = WebComponentUtil.getDisplayNameOrName(obj);

        OperationResult result = new OperationResult(OPERATION_GET_DISPLAY);

        DisplayType display =
                GuiDisplayTypeUtil.getDisplayTypeForObject(obj, result, getPageBase());
        return new TemplateTile(
                WebComponentUtil.getIconCssClass(display),
                title,
                new ResourceTemplate(obj.getOid(), ResourceType.COMPLEX_TYPE))
                .description(obj.asObjectable().getDescription())
                .tag(getPageBase().createStringResource("CreateResourceTemplatePanel.template").getString());
    }

    private String getDescriptionForConnectorType(@NotNull ConnectorType connectorObject) {
        if (connectorObject.getDescription() == null) {
            return connectorObject.getName().getOrig();
        }
        return connectorObject.getDescription();
    }

    protected class ResourceTemplate implements Serializable {

        private String oid;
        private QName type;

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
