/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.getObjectClassName;

@PanelType(name = "resourceTasks")
@PanelInstance(identifier = "resourceTasks", applicableForType = ResourceType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "PageResource.tab.content.tasks", icon = GuiStyleConstants.CLASS_OBJECT_TASK_ICON, order = 40))
public class ResourceTasksPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ResourceTasksPanel.class.getName() + ".";

    private static final String OPERATION_LOAD_TASKS = DOT_CLASS + "loadTasks";

    private static final String ID_TASKS_TABLE = "taskTable";

    private static final String ID_RUN_NOW = "runNow";
    private static final String ID_RESUME = "resume";
    private static final String ID_SUSPEND = "suspend";

    //    private IModel<PrismObject<ResourceType>> resourceModel;
    String[] resourceTaskArchetypeOids = new String[] { SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value(),
            SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value(),
            SystemObjectsType.ARCHETYPE_IMPORT_TASK.value(),
            SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value() };

    public ResourceTasksPanel(String id, final ResourceDetailsModel resourceModel, ContainerPanelConfigurationType config) {
        super(id, resourceModel, config);
    }

    protected void initLayout() {
        final MainObjectListPanel<TaskType> tasksPanel =
                new MainObjectListPanel<>(ID_TASKS_TABLE, TaskType.class, null) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_RESOURCE_TASKS_PANEL;
                    }

                    @Override
                    protected ISelectableDataProvider<SelectableBean<TaskType>> createProvider() {
                        return createSelectableBeanObjectDataProvider(() -> createResourceTasksQuery(), null);
                    }

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        return null;
                    }

                    @Override
                    protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                        if (collectionView == null) {
                            collectionView = getObjectCollectionView();
                        }

                        List<ObjectReferenceType> archetypeRef = ObjectCollectionViewUtil.getArchetypeReferencesList(collectionView);
                        try {
                            TaskType newTask = createResourceTask(getPrismContext(), ResourceTasksPanel.this.getObjectWrapper().getObject(), archetypeRef);

                            WebComponentUtil.initNewObjectWithReference(getPageBase(), newTask, archetypeRef);
                        } catch (SchemaException ex) {
                            getPageBase().getFeedbackMessages().error(ResourceTasksPanel.this, ex.getUserFriendlyMessage());
                            target.add(getPageBase().getFeedbackPanel());
                        }
                    }

                    @Override
                    protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
                        return ColumnUtils.getDefaultTaskColumns();
                    }

                    @Override
                    protected List<CompiledObjectCollectionView> getNewObjectInfluencesList() {
                        List<CompiledObjectCollectionView> newObjectInfluencesList = super.getNewObjectInfluencesList();
                        List<CompiledObjectCollectionView> filteredInfluencesList = new ArrayList<>();
                        if (newObjectInfluencesList != null) {
                            newObjectInfluencesList.forEach(influence -> {
                                if (influence.getCollection() != null && influence.getCollection().getCollectionRef() != null &&
                                        ArrayUtils.contains(resourceTaskArchetypeOids, influence.getCollection().getCollectionRef().getOid())) {
                                    filteredInfluencesList.add(influence);
                                }
                            });
                        }
                        return filteredInfluencesList;
                    }
                };
        tasksPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_TASK_BOX_CSS_CLASSES);
        add(tasksPanel);

        AjaxButton runNow = new AjaxButton(ID_RUN_NOW, getPageBase().createStringResource("pageTaskEdit.button.runNow")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<String> oids = createOidList(getTaskListPanel().getSelectedRealObjects());
                if (!oids.isEmpty()) {
                    OperationResult result = TaskOperationUtils.runNowPerformed(oids, getPageBase());
                    getPageBase().showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        add(runNow);

        AjaxButton resume = new AjaxButton(ID_RESUME, getPageBase().createStringResource("pageTaskEdit.button.resume")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<TaskType> tasks = getTaskListPanel().getSelectedRealObjects();
                if (!tasks.isEmpty()) {
                    OperationResult result = TaskOperationUtils.resumeTasks(tasks, getPageBase());
                    getPageBase().showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        add(resume);

        AjaxButton suspend = new AjaxButton(ID_SUSPEND, getPageBase().createStringResource("pageTaskEdit.button.suspend")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<TaskType> tasks = getTaskListPanel().getSelectedRealObjects();
                if (!tasks.isEmpty()) {
                    OperationResult result = TaskOperationUtils.suspendTasks(tasks, getPageBase());
                    getPageBase().showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        add(suspend);
    }

    public static TaskType createResourceTask(PrismContext prismContext, PrismObject<ResourceType> resource,
            List<ObjectReferenceType> archetypeRefs) throws SchemaException {

        PrismObjectDefinition<TaskType> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(TaskType.COMPLEX_TYPE);
        PrismObject<TaskType> obj = def.instantiate();
        TaskType newTask = obj.asObjectable();

        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resource.getOid());
        resourceRef.setType(ResourceType.COMPLEX_TYPE);
        resourceRef.setTargetName(new PolyStringType(resource.getName()));
        newTask.setObjectRef(resourceRef);

        prepopulateTask(newTask, resource, archetypeRefs);

        return newTask;
    }

    private static void prepopulateTask(TaskType task, PrismObject<ResourceType> resource, List<ObjectReferenceType> archetypeRefs) {
        SchemaHandlingType schemaHandling = resource.asObjectable().getSchemaHandling();
        List<ResourceObjectTypeDefinitionType> objectTypes = Collections.emptyList();
        if (schemaHandling != null) {
            objectTypes = schemaHandling.getObjectType();
        }

        if (task.getObjectRef() != null && ResourceType.COMPLEX_TYPE.equals(task.getObjectRef().getType())) {
            if (hasArchetype(archetypeRefs, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value())) {
                WorkDefinitionsType work = findWork(task);

                ReconciliationWorkDefinitionType recon = work.getReconciliation();
                if (recon == null) {
                    recon = new ReconciliationWorkDefinitionType();
                    work.setReconciliation(recon);
                }

                ResourceObjectSetType set = updateResourceObjectsSet(recon.getResourceObjects(), task.getObjectRef(), objectTypes);
                recon.setResourceObjects(set);
            } else if (hasArchetype(archetypeRefs, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value())) {
                WorkDefinitionsType work = findWork(task);

                ImportWorkDefinitionType imp = work.getImport();
                if (imp == null) {
                    imp = new ImportWorkDefinitionType();
                    work.setImport(imp);
                }

                ResourceObjectSetType set = updateResourceObjectsSet(imp.getResourceObjects(), task.getObjectRef(), objectTypes);
                imp.setResourceObjects(set);
            } else if (hasArchetype(archetypeRefs, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())) {
                WorkDefinitionsType work = findWork(task);

                LiveSyncWorkDefinitionType live = work.getLiveSynchronization();
                if (live == null) {
                    live = new LiveSyncWorkDefinitionType();
                    work.setLiveSynchronization(live);
                }

                ResourceObjectSetType set = updateResourceObjectsSet(live.getResourceObjects(), task.getObjectRef(), objectTypes);
                live.setResourceObjects(set);
            } else if (hasArchetype(archetypeRefs, SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value())) {
                WorkDefinitionsType work = findWork(task);

                AsyncUpdateWorkDefinitionType async = work.getAsynchronousUpdate();
                if (async == null) {
                    async = new AsyncUpdateWorkDefinitionType();
                    work.setAsynchronousUpdate(async);
                }

                ResourceObjectSetType set = updateResourceObjectsSet(async.getUpdatedResourceObjects(), task.getObjectRef(), objectTypes);
                async.setUpdatedResourceObjects(set);
            }
        }
    }

    private static WorkDefinitionsType findWork(TaskType task) {
        ActivityDefinitionType activity = task.getActivity();
        if (activity == null) {
            activity = new ActivityDefinitionType();
            task.setActivity(activity);
        }
        WorkDefinitionsType work = activity.getWork();
        if (work == null) {
            work = new WorkDefinitionsType();
            activity.setWork(work);
        }
        return work;
    }

    private static ResourceObjectSetType updateResourceObjectsSet(ResourceObjectSetType set, ObjectReferenceType objectRef, List<ResourceObjectTypeDefinitionType> objectTypes) {
        if (set == null) {
            set = new ResourceObjectSetType();
        }

        if (set.getResourceRef() == null) {
            set.setResourceRef(objectRef);
        }

        if (objectTypes.size() == 1) {
            ResourceObjectTypeDefinitionType def = objectTypes.get(0);
            set.setKind(def.getKind());
            set.setIntent(def.getIntent());
            set.setObjectclass(getObjectClassName(def));
        }

        return set;
    }

    private static boolean hasArchetype(List<ObjectReferenceType> archetypeRefs, String oid) {
        if (oid == null) {
            return false;
        }

        for (ObjectReferenceType ref : archetypeRefs) {
            if (Objects.equals(oid, ref.getOid())) {
                return true;
            }
        }

        return false;
    }

    private ObjectQuery createResourceTasksQuery() {
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(getObjectWrapper().getOid())
                .build();
    }

    private void noTasksSelected() {
        warn(getString("ResourceTasksPanel.noTasksSelected"));
    }

    private ObjectListPanel<TaskType> getTaskListPanel() {
        //noinspection unchecked
        return (ObjectListPanel<TaskType>) get(ID_TASKS_TABLE);
    }

    private List<String> createOidList(List<TaskType> tasks) {
        List<String> oids = new ArrayList<>();
        for (TaskType task : tasks) {
            oids.add(task.getOid());
        }
        return oids;
    }

    @Override
    public int getWidth() {
        return 900;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return getPageBase().createStringResource("ResourceTasksPanel.definedTasks");
    }

}
