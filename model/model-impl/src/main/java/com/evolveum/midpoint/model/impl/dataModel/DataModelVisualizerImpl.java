/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.model.api.DataModelVisualizer;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.dataModel.dot.DotModel;
import com.evolveum.midpoint.model.impl.dataModel.model.AdHocDataItem;
import com.evolveum.midpoint.model.impl.dataModel.model.DataItem;
import com.evolveum.midpoint.model.impl.dataModel.model.ResourceDataItem;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

@Component
public class DataModelVisualizerImpl implements DataModelVisualizer {

    private static final Trace LOGGER = TraceManager.getTrace(DataModelVisualizerImpl.class);
    public static final QName ACTIVATION_EXISTENCE = new QName(SchemaConstants.NS_C, "existence");

    @Autowired
    private ModelService modelService;

    @Autowired
    private PrismContext prismContext;

    @Override
    public String visualize(Collection<String> resourceOids, Target target, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        LOGGER.debug("Starting data model visualization");

        DataModel model = new DataModel(prismContext);

        ObjectQuery resourceQuery;
        if (resourceOids != null) {
            resourceQuery = prismContext.queryFor(ResourceType.class)
                    .id(resourceOids.toArray(new String[0]))
                    .build();
        } else {
            resourceQuery = null;
        }
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, resourceQuery, null, task, result);

        createDataItems(model, resources);
        processResourceMappings(model, resources);

        return export(model, target);
    }

    private String export(DataModel model, Target target) {
        if (target == null || target == Target.DOT) {
            return new DotModel(model).exportDot();
        } else {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String visualize(ResourceType resource, Target target, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException {

        LOGGER.debug("Starting data model visualization for {}", ObjectTypeUtil.toShortString(resource));

        DataModel model = new DataModel(prismContext);

        List<PrismObject<ResourceType>> resources = new ArrayList<>();
        resources.add(resource.clone().asPrismObject());

        createDataItems(model, resources);
        processResourceMappings(model, resources);

        return export(model, target);
    }

    private void processResourceMappings(DataModel model, List<PrismObject<ResourceType>> resources)
            throws SchemaException, ConfigurationException {
        for (PrismObject<ResourceType> resource : resources) {
            LOGGER.debug("Processing {}", ObjectTypeUtil.toShortString(resource));
            ResourceSchema refinedResourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
            if (refinedResourceSchema == null) {
                LOGGER.debug("Refined resource schema is null, skipping the resource.");
                continue;
            }
            Collection<? extends ResourceObjectTypeDefinition> refinedDefinitions = refinedResourceSchema.getObjectTypeDefinitions();
            for (ResourceObjectTypeDefinition refinedDefinition : refinedDefinitions) {
                LOGGER.debug("Processing refined definition {}", refinedDefinition);
                Collection<? extends ShadowSimpleAttributeDefinition<?>> attributeDefinitions = refinedDefinition.getSimpleAttributeDefinitions();
                final ShadowKindType kind = def(refinedDefinition.getKind());
                final String intent = def(refinedDefinition.getIntent());
                for (ShadowSimpleAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
                    if (attributeDefinition.isIgnored()) {
                        continue;
                    }
                    LOGGER.debug("Processing refined attribute definition for {}", attributeDefinition.getItemName());
                    ResourceDataItem attrItem = model.findResourceItem(resource.getOid(), kind, intent, getObjectClassName(refinedDefinition),
                            ItemPath.create(attributeDefinition.getItemName()));
                    if (attributeDefinition.getOutboundMappingBean() != null) {
                        processOutboundMapping(model, attrItem, attributeDefinition.getOutboundMappingBean(), null);
                    }
                    processInboundMappings(model, attrItem, attributeDefinition.getInboundMappingBeans());
                }
                for (ShadowReferenceAttributeDefinition associationDefinition : refinedDefinition.getReferenceAttributeDefinitions()) {
                    if (associationDefinition.isIgnored()) {
                        continue;
                    }
                    LOGGER.debug("Processing refined association definition for {}", associationDefinition.getItemName());
                    ResourceDataItem assocItem = model.findResourceItem(resource.getOid(), kind, intent, getObjectClassName(refinedDefinition),
                            ItemPath.create(associationDefinition.getItemName()));
                    var outboundMapping = associationDefinition.getOutboundMappingBean();
                    if (outboundMapping != null) {
                        processOutboundMapping(model, assocItem, outboundMapping, null);
                    }
//                    if (associationDefinition.getAssociationTarget() != null) {
//                        ResourceObjectTypeDefinition target = associationDefinition.getAssociationTarget();
//                        boolean objectToSubject = associationDefinition.getResourceObjectAssociationType().getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;
//                        associationDefinition.getResourceObjectAssociationType().getAssociationAttribute()
//                    }
                }
                ResourceActivationDefinitionType actMapping = refinedDefinition.getActivationSchemaHandling();
                if (actMapping != null) {
                    QName objectClassName = getObjectClassName(refinedDefinition);
                    processBidirectionalMapping(model, resource.getOid(), kind, intent, objectClassName, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, actMapping.getAdministrativeStatus());
                    processBidirectionalMapping(model, resource.getOid(), kind, intent, objectClassName, PATH_ACTIVATION_VALID_FROM, actMapping.getValidFrom());
                    processBidirectionalMapping(model, resource.getOid(), kind, intent, objectClassName, PATH_ACTIVATION_VALID_TO, actMapping.getValidTo());
                    processBidirectionalMapping(model, resource.getOid(), kind, intent, objectClassName, PATH_ACTIVATION_LOCKOUT_STATUS, actMapping.getLockoutStatus());
                    processBidirectionalMapping(model, resource.getOid(), kind, intent, objectClassName, ItemPath.create(FocusType.F_ACTIVATION, ACTIVATION_EXISTENCE), actMapping.getExistence());
                }
                ResourcePasswordDefinitionType pwdDef = refinedDefinition.getPasswordDefinition();
                if (pwdDef != null) {
                    ResourceDataItem resourceDataItem = model.findResourceItem(resource.getOid(), kind, intent,
                            getObjectClassName(refinedDefinition), PATH_CREDENTIALS_PASSWORD);
                    if (resourceDataItem == null) {
                        throw new IllegalStateException("No resource item for " + resource.getOid() + ":" + kind + ":" + intent + ":" + PATH_CREDENTIALS_PASSWORD);
                    }
                    if (pwdDef.getOutbound() != null) {
                        for (MappingType outbound : pwdDef.getOutbound()) {
                            processOutboundMapping(model, resourceDataItem, outbound, PATH_CREDENTIALS_PASSWORD);
                        }
                    }
                    for (MappingType inbound : pwdDef.getInbound()) {
                        processInboundMapping(model, resourceDataItem, inbound, PATH_CREDENTIALS_PASSWORD);
                    }
                }
            }
        }
    }

    private void processBidirectionalMapping(DataModel model, String oid, ShadowKindType kind, String intent, QName objectClassName, ItemPath itemPath,
            ResourceBidirectionalMappingType mapping) {
        if (mapping == null) {
            return;
        }
        ResourceDataItem resourceDataItem = model.findResourceItem(oid, kind, intent, objectClassName, itemPath);
        if (resourceDataItem == null) {
            throw new IllegalStateException("No resource item for " + oid + ":" + kind + ":" + intent + ":" + objectClassName + ":" + itemPath);
        }
        for (MappingType outbound : mapping.getOutbound()) {
            processOutboundMapping(model, resourceDataItem, outbound, itemPath);
        }
        for (MappingType inbound : mapping.getInbound()) {
            processInboundMapping(model, resourceDataItem, inbound, itemPath);
        }
    }

    private void createDataItems(DataModel model, List<PrismObject<ResourceType>> resources)
            throws SchemaException, ConfigurationException {
        LOGGER.debug("createDataItems starting");
        for (PrismObject<ResourceType> resource : resources) {
            final ResourceSchema resourceSchema = ResourceSchemaFactory.getBareSchema(resource);
            if (resourceSchema == null) {
                LOGGER.debug("Resource schema is null, skipping the resource.");
                continue;
            }
            ResourceSchema refinedResourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
            if (refinedResourceSchema == null) {
                LOGGER.debug("Refined resource schema is null, skipping the resource.");        // actually shouldn't be null if resource schema exists
                continue;
            }

            model.registerResource(resource);

            Collection<? extends ResourceObjectTypeDefinition> refinedDefinitions = refinedResourceSchema.getObjectTypeDefinitions();
            for (ResourceObjectTypeDefinition refinedDefinition : refinedDefinitions) {
                LOGGER.debug("Processing refined definition {} in {}", refinedDefinition, resource);
                Collection<? extends ShadowSimpleAttributeDefinition<?>> attributeDefinitions = refinedDefinition.getSimpleAttributeDefinitions();
                //Collection<? extends ResourceAttributeDefinition> rawAttributeDefinitions = refinedDefinition.getObjectClassDefinition().getAttributeDefinitions();
                final ShadowKindType kind = def(refinedDefinition.getKind());
                final String intent = def(refinedDefinition.getIntent());
                for (ShadowSimpleAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
                    if (attributeDefinition.isIgnored()) {
                        continue;
                    }
                    LOGGER.debug("Registering refined attribute definition for {}", attributeDefinition.getItemName());
                    ResourceDataItem attrItem = new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, attributeDefinition.getItemName());
                    attrItem.setAttributeDefinition(attributeDefinition);
                    // TODO check the name
                    model.registerDataItem(attrItem);
                }
                // TODO check attributes not mentioned in schema handling
                for (ShadowReferenceAttributeDefinition associationDefinition : refinedDefinition.getReferenceAttributeDefinitions()) {
                    if (associationDefinition.isIgnored()) {
                        continue;
                    }
                    LOGGER.debug("Registering refined association definition for {}", associationDefinition.getItemName());
                    ResourceDataItem assocItem = new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, associationDefinition.getItemName());
                    model.registerDataItem(assocItem);
                }
                model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, PATH_ACTIVATION_ADMINISTRATIVE_STATUS));
                model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, PATH_ACTIVATION_LOCKOUT_STATUS));
                model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, PATH_ACTIVATION_VALID_FROM));
                model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, PATH_ACTIVATION_VALID_TO));
                model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, ItemPath.create(ShadowType.F_ACTIVATION, ACTIVATION_EXISTENCE)));
                model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, refinedResourceSchema, refinedDefinition, PATH_CREDENTIALS_PASSWORD));
            }
        }
//        createRepoDataItems(UserType.class);
//        createRepoDataItems(RoleType.class);
//        createRepoDataItems(OrgType.class);
//        createRepoDataItems(ServiceType.class);

        LOGGER.debug("createDataItems finished");
    }

    static ShadowKindType def(ShadowKindType kind) {
        return kind != null ? kind : ShadowKindType.ACCOUNT;
    }

    static String def(String intent) {
        return intent != null ? intent : "default";
    }

    private void processInboundMappings(DataModel model, ResourceDataItem item, List<? extends MappingType> mappings) {
        if (mappings == null) {
            return;
        }
        for (MappingType mapping : mappings) {
            processInboundMapping(model, item, mapping, null);
        }
    }

    private void processInboundMapping(@NotNull DataModel model, @NotNull ResourceDataItem sourceItem, @NotNull MappingType mapping,
            @Nullable ItemPath defaultTargetItemPath) {
        LOGGER.debug("Processing inbound mapping: {} for {}", mapping, sourceItem);
        List<DataItem> sources = new ArrayList<>();
        for (VariableBindingDefinitionType sourceDecl : mapping.getSource()) {
            LOGGER.debug(" - src: {}", sourceDecl.getPath());
            DataItem explicitSourceItem = resolveSourceItem(model, sourceItem, mapping, sourceDecl, null);
            sources.add(explicitSourceItem);
        }
        if (!sources.contains(sourceItem)) {
            sources.add(sourceItem);
        }
        DataItem targetItem = null;
        VariableBindingDefinitionType targetDecl = mapping.getTarget();
        if (mapping.getTarget() != null) {
            LOGGER.debug(" - target: {}", targetDecl.getPath());
            targetItem = resolveTargetItem(model, sourceItem, mapping, targetDecl, ExpressionConstants.VAR_FOCUS);
        } else if (defaultTargetItemPath != null) {
            targetItem = resolveTargetItem(model, sourceItem, mapping, defaultTargetItemPath, ExpressionConstants.VAR_FOCUS);
        }
        model.registerMappingRelation(sources, targetItem, mapping);
    }

    private DataItem resolveSourceItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
            @NotNull MappingType mapping, @NotNull VariableBindingDefinitionType sourceDecl, @Nullable String defaultVariable) {
        // todo from the description
        return resolveSourceItem(model, currentItem, mapping, sourceDecl.getPath().getItemPath(), defaultVariable);
    }

    // for outbound (but sometimes also inbound) mappings
    @NotNull
    private DataItem resolveSourceItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
            @NotNull MappingType mapping, @NotNull ItemPath path, @Nullable String defaultVariable) {
        if (!path.startsWithName() && !path.startsWithVariable()) {
            LOGGER.warn("Probably incorrect path ({}) - does not start with a name - skipping", path);
            return createAdHocDataItem(model, path);
        }
        String varName;
        ItemPath itemPath;
        Object first = path.first();
        if (ItemPath.isVariable(first)) {
            varName = ItemPath.toVariableName(first).getLocalPart();
            itemPath = path.rest();
        } else {
            if (defaultVariable == null) {
                LOGGER.warn("No default variable for mapping source");
                return createAdHocDataItem(model, path);
            }
            varName = defaultVariable;
            itemPath = path;
        }

        if (ExpressionConstants.VAR_PROJECTION.equals(varName) || ExpressionConstants.VAR_SHADOW.equals(varName) || ExpressionConstants.VAR_ACCOUNT.equals(varName)) {
            return resolveResourceItem(model, currentItem, itemPath);
        } else if (ExpressionConstants.VAR_USER.equals(varName)) {
            return model.resolveRepositoryItem(UserType.class, itemPath);
        } else if (ExpressionConstants.VAR_ACTOR.equals(varName)) {
            return model.resolveRepositoryItem(FocusType.class, itemPath);            // TODO
        } else if (ExpressionConstants.VAR_FOCUS.equals(varName)) {
            Class<? extends ObjectType> guessedClass = guessFocusClass(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent());
            DataItem item = model.resolveRepositoryItem(guessedClass, itemPath);
            if (item != null) {
                return item;
            }
            // TODO guess e.g. by item existence in schema
            LOGGER.warn("Couldn't resolve {} in $focus", path);
        } else if (ExpressionConstants.VAR_INPUT.equals(varName)) {
            return currentItem;
        } else {
            LOGGER.warn("Unsupported variable {} in {}", varName, path);
        }
        return createAdHocDataItem(model, path);
    }

    private DataItem createAdHocDataItem(DataModel model, ItemPath path) {
        return new AdHocDataItem(path);
    }

    // currently for inbounds only
    @NotNull
    private DataItem resolveTargetItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
            @NotNull MappingType mapping, @NotNull VariableBindingDefinitionType targetDecl, @Nullable String defaultVariable) {
        // todo from the description
        return resolveTargetItem(model, currentItem, mapping, targetDecl.getPath().getItemPath(), defaultVariable);
    }

    // currently for inbounds only
    @NotNull
    private DataItem resolveTargetItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
            @NotNull MappingType mapping, @NotNull ItemPath path, @Nullable String defaultVariable) {
        if (!path.startsWithName() && !path.startsWithVariable()) {
            LOGGER.warn("Probably incorrect path ({}) - does not start with a name - skipping", path);
            return createAdHocDataItem(model, path);
        }
        String varName;
        ItemPath itemPath;
        Object first = path.first();
        if (ItemPath.isVariable(first)) {
            varName = ItemPath.toVariableName(first).getLocalPart();
            itemPath = path.rest();
        } else {
            if (defaultVariable == null) {
                LOGGER.warn("No default variable for mapping target");
                return createAdHocDataItem(model, path);
            }
            varName = defaultVariable;
            itemPath = path;
        }

        if (ExpressionConstants.VAR_PROJECTION.equals(varName) || ExpressionConstants.VAR_SHADOW.equals(varName) || ExpressionConstants.VAR_ACCOUNT.equals(varName)) {
            return resolveResourceItem(model, currentItem, itemPath);                // does make sense?
        } else if (ExpressionConstants.VAR_USER.equals(varName)) {
            return model.resolveRepositoryItem(UserType.class, itemPath);
        } else if (ExpressionConstants.VAR_ACTOR.equals(varName)) {
            return model.resolveRepositoryItem(FocusType.class, itemPath);            // TODO
        } else if (ExpressionConstants.VAR_FOCUS.equals(varName)) {
            Class<? extends ObjectType> guessedClass = guessFocusClass(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent());
            DataItem item = model.resolveRepositoryItem(guessedClass, itemPath);
            if (item != null) {
                return item;
            }
            // TODO guess e.g. by item existence in schema
            LOGGER.warn("Couldn't resolve {} in $focus", path);
        } else if (ExpressionConstants.VAR_INPUT.equals(varName)) {
            return currentItem;                                                        // does make sense?
        } else {
            LOGGER.warn("Unsupported variable {} in {}", varName, path);
        }
        return createAdHocDataItem(model, path);
    }

    private Class<? extends ObjectType> guessFocusClass(@NotNull String resourceOid, @NotNull ShadowKindType kind, @NotNull String intent) {
        // TODO use synchronization as well
        switch (kind) {
            case ACCOUNT: return UserType.class;
            case ENTITLEMENT: return RoleType.class;
            case GENERIC: return OrgType.class;
        }
        throw new IllegalStateException();
    }

    private ResourceDataItem resolveResourceItem(DataModel model, ResourceDataItem currentItem, ItemPath path) {
        return model.findResourceItem(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent(),
                currentItem.getObjectClassName(), path);
    }

    private void processOutboundMapping(
            @NotNull DataModel model, @NotNull ResourceDataItem targetItem, @NotNull MappingType mapping,
            @Nullable ItemPath defaultSourceItemPath) {
        LOGGER.debug("Processing outbound mapping: {} for {}", mapping, targetItem);
        List<DataItem> sources = new ArrayList<>();
        for (VariableBindingDefinitionType sourceDecl : mapping.getSource()) {
            LOGGER.debug(" - src: {}", sourceDecl.getPath());
            DataItem sourceItem = resolveSourceItem(model, targetItem, mapping, sourceDecl, ExpressionConstants.VAR_FOCUS);
            sources.add(sourceItem);
        }
        if (defaultSourceItemPath != null) {
            DataItem defaultSource = resolveSourceItem(model, targetItem, mapping, defaultSourceItemPath, ExpressionConstants.VAR_FOCUS);
            if (!sources.contains(defaultSource)) {
                sources.add(defaultSource);
            }
        }
        VariableBindingDefinitionType targetDecl = mapping.getTarget();
        if (targetDecl != null) {
            LOGGER.warn(" - ignoring target (mapping is outbound): {}; using {} instead", targetDecl.getPath(), targetItem);
        }
        model.registerMappingRelation(sources, targetItem, mapping);
    }

    // TODO move to appropriate place
    private QName getObjectClassName(ResourceObjectTypeDefinition def) {
        return def != null ? def.getTypeName() : null;
    }

}
