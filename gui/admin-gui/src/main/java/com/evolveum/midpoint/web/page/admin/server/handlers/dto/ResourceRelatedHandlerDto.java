package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
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
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
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

	private static final transient Trace LOGGER = TraceManager.getTrace(ResourceRelatedHandlerDto.class);

	private boolean dryRun;
	private ShadowKindType kind;
	private String intent;
	private String objectClass;
	private List<QName> objectClassList;
	private TaskAddResourcesDto resourceRef;
        private boolean retryUnhandledErr;

	private ResourceRelatedHandlerDto(TaskDto taskDto) {
		super(taskDto);
	}

	public ResourceRelatedHandlerDto(TaskDto taskDto, PageBase pageBase, Task opTask, OperationResult thisOpResult) {
		super(taskDto);
		TaskType taskType = taskDto.getTaskType();
		fillInResourceReference(taskType, pageBase, opTask, thisOpResult);
		fillFromExtension(taskType);
	}

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

		PrismProperty<QName> objectClassItem = task.getExtension().findProperty(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
		if(objectClassItem != null && objectClassItem.getRealValue() != null){
			objectClass = objectClassItem.getRealValue().getLocalPart();
		}

                PrismProperty<Boolean> retrySyncItem = task.getExtension().findProperty(SchemaConstants.SYNC_TOKEN_RETRY_UNHANDLED);
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
		if (ref != null && ResourceType.COMPLEX_TYPE.equals(ref.getType())){
			resourceRef = new TaskAddResourcesDto(ref.getOid(), taskDto.getTaskObjectName(task, pageBase, opTask, result));
		}
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
		taskDto.setObjectRef(resource != null ? resource.asObjectReferenceType() : null);
	}

	@NotNull
	@Override
	public Collection<ItemDelta<?, ?>> getDeltasToExecute(HandlerDtoEditableState origState, HandlerDtoEditableState currState, PrismContext prismContext)
			throws SchemaException {

		List<ItemDelta<?, ?>> rv = new ArrayList<>();

		// we can safely assume this; also that both are non-null
		ResourceRelatedHandlerDto orig = (ResourceRelatedHandlerDto) origState;
		ResourceRelatedHandlerDto curr = (ResourceRelatedHandlerDto) currState;

		String origResourceOid = orig.getResourceRef() != null ? orig.getResourceRef().getOid() : null;
		String currResourceOid = curr.getResourceRef() != null ? curr.getResourceRef().getOid() : null;
		if (!StringUtils.equals(origResourceOid, currResourceOid)) {
			ObjectReferenceType resourceObjectRef = new ObjectReferenceType();
			resourceObjectRef.setOid(curr.getResourceRef().getOid());
			resourceObjectRef.setType(ResourceType.COMPLEX_TYPE);
			rv.add(DeltaBuilder.deltaFor(TaskType.class, prismContext)
					.item(TaskType.F_OBJECT_REF).replace(resourceObjectRef.asReferenceValue()).asItemDelta());
		}

		if (orig.isDryRun() != curr.isDryRun()) {
			addExtensionDelta(rv, SchemaConstants.MODEL_EXTENSION_DRY_RUN, curr.isDryRun(), prismContext);
		}

                if (orig.isRetryUnhandledErr() != curr.isRetryUnhandledErr()) {
			addExtensionDelta(rv, SchemaConstants.SYNC_TOKEN_RETRY_UNHANDLED, curr.isRetryUnhandledErr(), prismContext);
		}

		if (orig.getKind() != curr.getKind()) {
			addExtensionDelta(rv, SchemaConstants.MODEL_EXTENSION_KIND, curr.getKind(), prismContext);
		}

		if (!StringUtils.equals(orig.getIntent(), curr.getIntent())) {
			addExtensionDelta(rv, SchemaConstants.MODEL_EXTENSION_INTENT, curr.getIntent(), prismContext);
		}

		if (!StringUtils.equals(orig.getObjectClass(), curr.getObjectClass())) {
			QName objectClassQName = null;
			for (QName q: getObjectClassList()) {
				if (q.getLocalPart().equals(objectClass)) {
					objectClassQName = q;
				}
			}
			addExtensionDelta(rv, SchemaConstants.OBJECTCLASS_PROPERTY_NAME, objectClassQName, prismContext);
		}
		return rv;
	}

	private void addExtensionDelta(List<ItemDelta<?, ?>> rv, QName itemName, Object realValue, PrismContext prismContext)
			throws SchemaException {
		PrismPropertyDefinition def = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(itemName);
		rv.add(DeltaBuilder.deltaFor(TaskType.class, prismContext)
				.item(new ItemPath(TaskType.F_EXTENSION, itemName), def).replace(realValue).asItemDelta());
	}

	@Override
	public HandlerDtoEditableState getEditableState() {
		return this;
	}

	// TODO implement seriously
	public ResourceRelatedHandlerDto clone() {
		ResourceRelatedHandlerDto clone = new ResourceRelatedHandlerDto(taskDto);
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
