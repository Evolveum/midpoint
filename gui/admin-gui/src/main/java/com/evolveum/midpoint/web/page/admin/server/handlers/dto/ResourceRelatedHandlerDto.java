/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ResourceRelatedHandlerDto extends HandlerDto implements HandlerDtoEditableState {

    public static final String F_DRY_RUN = "dryRun";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_RESOURCE_REFERENCE = "resourceRef";
    public static final String F_TOKEN_RETRY_UNHANDLED_ERR = "retryUnhandledErr";

    private static final String CLASS_DOT = ResourceRelatedHandlerDto.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = CLASS_DOT + "loadResource";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceRelatedHandlerDto.class);

    private boolean dryRun;
    private ShadowKindType kind;
    private String intent;
    private String objectClass;
    private List<QName> objectClassList;
    private TaskAddResourcesDto resourceRef;
        private boolean retryUnhandledErr;


    private void fillFromExtension(TaskType taskType) {
        PrismObject<TaskType> task = taskType.asPrismObject();
        if (task.getExtension() == null) {
            dryRun = false;
            return;
        }

        PrismProperty<Boolean> item = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
        if (item == null || item.getRealValue() == null) {
            dryRun = false;
        } else {
            dryRun = item.getRealValue();
        }

        PrismProperty<ShadowKindType> kindItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_KIND);
        if(kindItem != null && kindItem.getRealValue() != null){
            kind = kindItem.getRealValue();
        }

        PrismProperty<String> intentItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_INTENT);
        if(intentItem != null && intentItem.getRealValue() != null){
            intent = intentItem.getRealValue();
        }

        PrismProperty<QName> objectClassItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_OBJECTCLASS);
        if(objectClassItem != null && objectClassItem.getRealValue() != null){
            objectClass = objectClassItem.getRealValue().getLocalPart();
        }

        PrismProperty<Boolean> retrySyncItem = task.getExtension().findProperty(SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS);
        if (retrySyncItem == null || retrySyncItem.getRealValue() == null) {
            retryUnhandledErr = true;
        } else {
            retryUnhandledErr = retrySyncItem.getRealValue();
        }
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

        public boolean isRetryUnhandledErr() {
        return retryUnhandledErr;
    }

    public void setRetryUnhandledErr(boolean retryUnhandledErr) {
        this.retryUnhandledErr = retryUnhandledErr;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public List<QName> getObjectClassList() {
        if (objectClassList == null) {
            objectClassList = new ArrayList<>();
        }
        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
    }

    private void fillInResourceReference(TaskType task, PageBase pageBase, Task opTask, OperationResult result) {
        ObjectReferenceType ref = task.getObjectRef();
//        if (ref != null && ResourceType.COMPLEX_TYPE.equals(ref.getType())){
//            resourceRef = new TaskAddResourcesDto(ref.getOid(), taskDto.getTaskObjectName(task, pageBase, opTask, result));
//        }
        updateObjectClassList(pageBase);
    }

    private void updateObjectClassList(PageBase pageBase){
        Task task = pageBase.createSimpleTask(OPERATION_LOAD_RESOURCE);
        OperationResult result = task.getResult();
        List<QName> objectClassList = new ArrayList<>();

        if(resourceRef != null){
            PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceRef.getOid(),
                    pageBase, task, result);

            try {
                ResourceSchema schema = RefinedResourceSchemaImpl.getResourceSchema(resource, pageBase.getPrismContext());
                schema.getObjectClassDefinitions();

                for(Definition def: schema.getDefinitions()){
                    objectClassList.add(def.getTypeName());
                }

                setObjectClassList(objectClassList);
            } catch (Exception e){
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object class list from resource.", e);
            }
        }
    }

    public TaskAddResourcesDto getResourceRef() {
        return resourceRef;
    }

    public void setResource(TaskAddResourcesDto resource) {
        this.resourceRef = resource;
//        taskDto.setObjectRef(resource != null ? resource.asObjectReferenceType() : null);
    }



    private void addExtensionDelta(List<ItemDelta<?, ?>> rv, QName itemName, Object realValue, PrismContext prismContext)
            throws SchemaException {
        PrismPropertyDefinition def = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(itemName);
        rv.add(prismContext.deltaFor(TaskType.class)
                .item(ItemPath.create(TaskType.F_EXTENSION, itemName), def).replace(realValue).asItemDelta());
    }


    // TODO implement seriously
    public ResourceRelatedHandlerDto clone() {
        ResourceRelatedHandlerDto clone = new ResourceRelatedHandlerDto();
        clone.dryRun = dryRun;
                clone.retryUnhandledErr = retryUnhandledErr;
        clone.kind = kind;
        clone.intent = intent;
        clone.objectClass = objectClass;
        clone.objectClassList = CloneUtil.clone(objectClassList);
        clone.resourceRef = CloneUtil.clone(resourceRef);
        return clone;
    }
}
