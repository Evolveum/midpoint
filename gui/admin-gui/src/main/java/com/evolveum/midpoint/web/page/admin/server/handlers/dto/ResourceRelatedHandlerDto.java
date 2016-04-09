package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddResourcesDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ResourceRelatedHandlerDto extends HandlerDto {

	public static final String F_DRY_RUN = "dryRun";
	public static final String F_KIND = "kind";
	public static final String F_INTENT = "intent";
	public static final String F_OBJECT_CLASS = "objectClass";
	public static final String F_RESOURCE_REFERENCE = "resourceRef";

	public static final String CLASS_DOT = ResourceRelatedHandlerDto.class.getName() + ".";
	public static final String OPERATION_LOAD_RESOURCE = CLASS_DOT + "loadResource";

	private static final transient Trace LOGGER = TraceManager.getTrace(ResourceRelatedHandlerDto.class);

	private boolean dryRun;
	private ShadowKindType kind;
	private String intent;
	private String objectClass;
	private List<QName> objectClassList;
	private TaskAddResourcesDto resourceRef;

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
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
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
				ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, pageBase.getPrismContext());
				schema.getObjectClassDefinitions();

				for(Definition def: schema.getDefinitions()){
					objectClassList.add(def.getTypeName());
				}

				setObjectClassList(objectClassList);
			} catch (Exception e){
				LoggingUtils.logException(LOGGER, "Couldn't load object class list from resource.", e);
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

	@Override
	public void updateTask(Task existingTask, PageTaskEdit parentPage) throws SchemaException {

		if (resourceRef != null) {
			ObjectReferenceType resourceObjectRef = new ObjectReferenceType();
			resourceObjectRef.setOid(resourceRef.getOid());
			resourceObjectRef.setType(ResourceType.COMPLEX_TYPE);
			existingTask.setObjectRef(resourceObjectRef);
		}

		SchemaRegistry registry = parentPage.getPrismContext().getSchemaRegistry();
		if (dryRun) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			PrismProperty dryRunProperty = new PrismProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			dryRunProperty.setDefinition(def);
			dryRunProperty.setRealValue(true);
			existingTask.addExtensionProperty(dryRunProperty);
		} else {
			PrismProperty dryRunProperty = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			if (dryRunProperty != null) {
				existingTask.deleteExtensionProperty(dryRunProperty);
			}
		}

		if (kind != null) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_KIND);
			PrismProperty kindProperty = new PrismProperty(SchemaConstants.MODEL_EXTENSION_KIND);
			kindProperty.setDefinition(def);
			kindProperty.setRealValue(kind);
			existingTask.addExtensionProperty(kindProperty);
		} else {
			PrismProperty kindProperty = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_KIND);
			if (kindProperty != null) {
				existingTask.deleteExtensionProperty(kindProperty);
			}
		}

		if (StringUtils.isNotEmpty(intent)) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_INTENT);
			PrismProperty intentProperty = new PrismProperty(SchemaConstants.MODEL_EXTENSION_INTENT);
			intentProperty.setDefinition(def);
			intentProperty.setRealValue(intent);
			existingTask.addExtensionProperty(intentProperty);
		} else {
			PrismProperty intentProperty = existingTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_INTENT);
			if (intentProperty != null) {
				existingTask.deleteExtensionProperty(intentProperty);
			}
		}

		if (StringUtils.isNotEmpty(objectClass)) {
			PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
			PrismProperty objectClassProperty = new PrismProperty(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
			objectClassProperty.setRealValue(def);

			QName objectClassQName = null;
			for (QName q: getObjectClassList()) {
				if (q.getLocalPart().equals(objectClass)) {
					objectClassQName = q;
				}
			}
			objectClassProperty.setRealValue(objectClassQName);
			existingTask.addExtensionProperty(objectClassProperty);
		} else {
			PrismProperty objectClassProperty = existingTask.getExtensionProperty(SchemaConstants.OBJECTCLASS_PROPERTY_NAME);
			if (objectClassProperty != null){
				existingTask.deleteExtensionProperty(objectClassProperty);
			}
		}
	}
}
