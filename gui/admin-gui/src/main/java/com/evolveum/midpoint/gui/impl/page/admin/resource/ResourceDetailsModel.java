/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceDetailsModel extends AssignmentHolderDetailsModel<ResourceType> {

    private static final String DOT_CLASS = ResourceDetailsModel.class.getName() + ".";
    private static final String OPERATION_FETCH_SCHEMA = DOT_CLASS + "fetchSchema";
    private static final Trace LOGGER = TraceManager.getTrace(ResourceDetailsModel.class);

    private final LoadableModel<List<ObjectClassWrapper>> objectClassesModel;

    public ResourceDetailsModel(LoadableDetachableModel<PrismObject<ResourceType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        this.objectClassesModel = new LoadableModel<>() {
            @Override
            protected List<ObjectClassWrapper> load() {
                List<ObjectClassWrapper> list = new ArrayList<>();

                ResourceSchema schema = getResourceSchema(getObjectWrapper(), getPageBase());

                if (schema == null) {
                    return list;
                }

                for(ResourceObjectClassDefinition definition: schema.getObjectClassDefinitions()){
                    list.add(new ObjectClassWrapper(definition));
                }

                Collections.sort(list);

                return list;
            }
        };
    }

    public static ResourceSchema getResourceSchema(PrismObjectWrapper<ResourceType> objectWrapper, PageAdminLTE pageBase) {
        ResourceSchema schema = null;
        OperationResult result = new OperationResult(OPERATION_FETCH_SCHEMA);
        try {
            schema = pageBase.getModelService().fetchSchema(objectWrapper.getObjectApplyDelta(), result);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError("Cannot load schema object classes, " + e.getMessage());
            result.computeStatus();
            pageBase.showResult(result);
        }
        return schema;
    }

    public ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(getObjectWrapperModel().getObject().getObjectOld().asObjectable());
    }

    @Override
    public void reset() {
        super.reset();
        objectClassesModel.reset();
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        PrismObject<ResourceType> resource = getPrismObject();
        PrismReference connector = resource.findReference(ResourceType.F_CONNECTOR_REF);
        String connectorOid = connector != null ? connector.getOid() : null;
        GuiResourceDetailsPageType resourceDetailsConfiguration = getModelServiceLocator().getCompiledGuiProfile().findResourceDetailsConfiguration(connectorOid);
        return applyArchetypePolicy(resourceDetailsConfiguration);
    }

    public IModel<List<ObjectClassWrapper>> getObjectClassesModel() {
        return objectClassesModel;
    }

    public PageResource getPageResource() {
        return (PageResource) super.getPageBase();
    }

    @Override
    protected WrapperContext createWrapperContext(Task task, OperationResult result) {
        WrapperContext ctx = super.createWrapperContext(task, result);
        ctx.setConfigureMappingType(true);
        return ctx;
    }

    private ResourceSchema getResourceSchemaOrNothing() {
        try {
            return getRefinedSchema();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot load resource schema", e);
            return null;
        }

    }

    public ResourceObjectTypeDefinition getDefaultObjectType(ShadowKindType kind) {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();
        if (resourceSchema == null) {
            return null;
        }
        ResourceObjectDefinition defaultObjectType = resourceSchema.findDefaultDefinitionForKind(kind);
        if (defaultObjectType instanceof ResourceObjectTypeDefinition) {
            return (ResourceObjectTypeDefinition) defaultObjectType;
        }
        List<? extends ResourceObjectTypeDefinition> objectTypes = getResourceObjectTypesDefinitions(kind);
        if (objectTypes.isEmpty()) {
            return null;
        }
        return objectTypes.iterator().next();
    }

    public ResourceObjectTypeDefinition getObjectTypeDefinition(ShadowKindType kind, String intent) {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();
        if (resourceSchema == null) {
            return null;
        }

        if (StringUtils.isEmpty(intent)) {
            return getDefaultObjectType(kind);
        }

        ResourceObjectTypeDefinition defaultObjectType = resourceSchema.getObjectTypeDefinition(kind, intent);
        if (defaultObjectType == null) {
            return getDefaultObjectType(kind);
        }

        return defaultObjectType;
    }

    public List<? extends ResourceObjectTypeDefinition> getResourceObjectTypesDefinitions(ShadowKindType kind) {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();

        if (resourceSchema == null) {
            return null;
        }

        return resourceSchema.getObjectTypeDefinitions(kind);
//        return resourceSchema.getObjectTypeDefinitions(kind)
//                .stream()
//                .map(ResourceObjectDefinition::getDefinitionBean)
//                .collect(Collectors.toList());
    }

    public QName getDefaultObjectClass() {
        List<QName> objectClassDefinitions = getResourceObjectClassesDefinitions();
        if (objectClassDefinitions == null) {
            return null;
        }
        return objectClassDefinitions.iterator().next();
    }

    public List<QName> getResourceObjectClassesDefinitions() {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();

        if (resourceSchema == null) {
            return null;
        }
        return resourceSchema.getObjectClassDefinitions()
                .stream()
                .map(ResourceObjectDefinition::getObjectClassName)
                .collect(Collectors.toList());
    }

    public ResourceObjectDefinition findResourceObjectClassDefinition(QName objectClass) {
        ResourceSchema schema = getResourceSchemaOrNothing();
        if (schema == null) {
            return null;
        }
        return schema.findObjectClassDefinition(objectClass);
    }
}
