/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyEnumValuesModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshPanel;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.page.admin.server.dto.*;
import com.evolveum.midpoint.web.session.TasksStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tasks", matchUrlForSecurity = "/admin/tasks")
        },
        action = {
        @AuthorizationAction(actionUri = PageAdminTasks.AUTHORIZATION_TASKS_ALL,
                label = PageAdminTasks.AUTH_TASKS_ALL_LABEL,
                description = PageAdminTasks.AUTH_TASKS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_URL,
                label = "PageTasks.auth.tasks.label",
                description = "PageTasks.auth.tasks.description")})
public class PageTasks extends PageAdminTasks implements Refreshable {

	private static final Trace LOGGER = TraceManager.getTrace(PageTasks.class);
    private static final String DOT_CLASS = PageTasks.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTasks";
    private static final String OPERATION_RESUME_TASKS = DOT_CLASS + "resumeTasks";
    private static final String OPERATION_RESUME_TASK = DOT_CLASS + "resumeTask";
    private static final String OPERATION_DELETE_TASKS = DOT_CLASS + "deleteTasks";
    private static final String OPERATION_RECONCILE_WORKERS = DOT_CLASS + "reconcileWorkers";
    private static final String OPERATION_DELETE_ALL_CLOSED_TASKS = DOT_CLASS + "deleteAllClosedTasks";
    private static final String OPERATION_SCHEDULE_TASKS = DOT_CLASS + "scheduleTasks";
    private static final String OPERATION_DELETE_NODES = DOT_CLASS + "deleteNodes";
    private static final String OPERATION_START_SCHEDULERS = DOT_CLASS + "startSchedulers";
    private static final String OPERATION_STOP_SCHEDULERS_AND_TASKS = DOT_CLASS + "stopSchedulersAndTasks";
    private static final String OPERATION_STOP_SCHEDULERS = DOT_CLASS + "stopSchedulers";
    private static final String OPERATION_DEACTIVATE_SERVICE_THREADS = DOT_CLASS + "deactivateServiceThreads";
    private static final String OPERATION_REACTIVATE_SERVICE_THREADS = DOT_CLASS + "reactivateServiceThreads";
    private static final String OPERATION_SYNCHRONIZE_TASKS = DOT_CLASS + "synchronizeTasks";
    private static final String OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS = DOT_CLASS + "synchronizeWorkflowRequests";
    private static final String OPERATION_REFRESH_TASKS = DOT_CLASS + "refreshTasks";
    private static final String ALL_CATEGORIES = "";

    public static final long WAIT_FOR_TASK_STOP = 2000L;

    private static final String ID_REFRESH_PANEL = "refreshPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_STATE = "state";
    private static final String ID_CATEGORY = "category";
    private static final String ID_SHOW_SUBTASKS = "showSubtasks";
    private static final String ID_TASK_TABLE = "taskTable";
    private static final String ID_NODE_TABLE = "nodeTable";
    private static final String ID_SEARCH_CLEAR = "searchClear";
    private static final String ID_TABLE_HEADER = "tableHeader";
	public static final String ID_SYNCHRONIZE_WORKFLOW_REQUESTS = "synchronizeWorkflowRequests";

    public static final String SELECTED_CATEGORY = "category";
	private static final int REFRESH_INTERVAL = 60000;				// don't set too low to prevent refreshing open inline menus (TODO skip refresh if a menu is open)

	private IModel<TasksSearchDto> searchModel;
    private String searchText = "";

	private IModel<AutoRefreshDto> refreshModel;
	private AutoRefreshPanel refreshPanel;

    public PageTasks() {
		this("", null);
    }

    public PageTasks(String searchText) {
        this(searchText, null);
    }

    public PageTasks(PageParameters parameters) {
        this("", parameters);
    }

    // TODO clean the mess with constructors
    public PageTasks(String searchText, PageParameters parameters) {
        if (parameters != null) {
            getPageParameters().overwriteWith(parameters);
        }

        this.searchText = searchText;
        searchModel = LoadableModel.create(this::loadTasksSearchDto, false);
		refreshModel = new Model<>(new AutoRefreshDto(REFRESH_INTERVAL));

        initLayout();

		refreshPanel.startRefreshing(this, null);
    }

    private TasksSearchDto loadTasksSearchDto() {
        TasksStorage storage = getSessionStorage().getTasks();
        TasksSearchDto dto = storage.getTasksSearch();

        if (dto == null) {
            dto = new TasksSearchDto();
            dto.setShowSubtasks(false);
        }

        if (getPageParameters() != null) {
            StringValue category = getPageParameters().get(SELECTED_CATEGORY);
            if (category != null && category.toString() != null && !category.toString().isEmpty()) {
                dto.setCategory(category.toString());
            }
        }

        if (dto.getStatus() == null) {
            dto.setStatus(TaskDtoExecutionStatusFilter.ALL);
        }

        return dto;
    }

    private void initLayout() {

		refreshPanel = new AutoRefreshPanel(ID_REFRESH_PANEL, refreshModel, this, false);
		add(refreshPanel);

        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

		List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();

        TaskDtoProviderOptions options = TaskDtoProviderOptions.minimalOptions();
		options.setGetNextRunStartTime(true);
		options.setUseClusterInformation(true);
		options.setResolveObjectRef(true);
        TaskDtoProvider provider = new TaskDtoProvider(PageTasks.this, options) {
        	private static final long serialVersionUID = 1L;
            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                TasksStorage storage = getSessionStorage().getTasks();
                storage.setPaging(paging);
            }

            @Override
            public TaskDto createTaskDto(PrismObject<TaskType> task, boolean subtasksLoaded, Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
                TaskDto dto = super.createTaskDto(task, subtasksLoaded, opTask, result);
                addInlineMenuToTaskRow(dto);

                return dto;
            }
            
            @Override
            public IModel<TaskDto> model(TaskDto object) {
            	return new LoadableDetachableModel<TaskDto>(object) {
            		
            		private static final long serialVersionUID = 1L;
					private String oid;
            		
            		protected void onDetach() {
            			this.oid = getObject().getOid();
            		}
            		
					@Override
					protected TaskDto load() {
						Task task = createSimpleTask("load task");
						OperationResult result = task.getResult();
						PrismObject<TaskType> taskType = WebModelServiceUtils.loadObject(TaskType.class, oid, PageTasks.this, task, result);
						if (taskType == null) {
							return null;
						}
						
						TaskDto taskDto = null;
						try {
							taskDto = new TaskDto(taskType.asObjectable(), null, getModel(), getTaskService(),
									getModelInteractionService(), getTaskManager(), getWorkflowManager(), options,
									false, task, result, PageTasks.this);
							taskDto.setSelected(object.isSelected());
						} catch (SchemaException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return taskDto;
					};
            		
				};
            }
        };

        provider.setQuery(createTaskQuery());
        BoxedTablePanel<TaskDto> taskTable = new BoxedTablePanel<TaskDto>(ID_TASK_TABLE, provider, taskColumns,
                UserProfileStorage.TableId.PAGE_TASKS_PANEL,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_TASKS_PANEL)) {

          	private static final long serialVersionUID = 1L;

			@Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageTasks.this, searchModel);
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
            	CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(id) {

            		private static final long serialVersionUID = 1L;
            		@Override
            		protected DataTable<?, ?> getDataTable() {
            			return getTaskTable().getDataTable();
            		}

            		@Override
            		protected String getFilename() {
            			return "TaskType_" + createStringResource("MainObjectListPanel.exportFileName").getString();
            		}

				};
                return exportDataLink;
            }
        };
        taskTable.setOutputMarkupId(true);

        TasksStorage storage = getSessionStorage().getTasks();
        taskTable.setCurrentPage(storage.getPaging());

        mainForm.add(taskTable);

        List<IColumn<NodeDto, String>> nodeColumns = initNodeColumns();
        BoxedTablePanel<NodeDto> nodeTable = new BoxedTablePanel<>(ID_NODE_TABLE, new NodeDtoProvider(PageTasks.this) {

            private static final long serialVersionUID = 1L;

            @Override
            public NodeDto createNodeDto(PrismObject<NodeType> node) {
                NodeDto dto = super.createNodeDto(node);
                addInlineMenuToNodeRow(dto);

                return dto;
            }
        }, nodeColumns,
            UserProfileStorage.TableId.PAGE_TASKS_NODES_PANEL,
            (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_TASKS_NODES_PANEL));
        nodeTable.setOutputMarkupId(true);
        nodeTable.setShowPaging(false);
        mainForm.add(nodeTable);

        initDiagnosticButtons();
    }

	@Override
	public void refresh(AjaxRequestTarget target) {
		refreshTasks(target);
	}

	@Override
	public Component getRefreshingBehaviorParent() {
		return refreshPanel;
	}

	@Override
	public int getRefreshInterval() {
		return REFRESH_INTERVAL;
	}

	private List<IColumn<NodeDto, String>> initNodeColumns() {
        List<IColumn<NodeDto, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<>();
        columns.add(column);

		column = new PropertyColumn<>(createStringResource("pageTasks.node.name"), "name", "name");
        columns.add(column);

        columns.add(new EnumPropertyColumn<NodeDto>(createStringResource("pageTasks.node.executionStatus"),
                "executionStatus") {

            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        });

        columns.add(new PropertyColumn(createStringResource("pageTasks.node.managementPort"), "managementPort"));
        columns.add(new AbstractColumn<NodeDto, String>(createStringResource("pageTasks.node.lastCheckInTime")) {

            @Override
            public void populateItem(Item<ICellPopulator<NodeDto>> item, String componentId,
                                     final IModel<NodeDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return getLastCheckInTime(rowModel);
                    }
                }));
            }
        });
        CheckBoxColumn check = new CheckBoxColumn(createStringResource("pageTasks.node.clustered"), "clustered");
        check.setEnabled(false);
        columns.add(check);
        columns.add(new PropertyColumn(createStringResource("pageTasks.node.statusMessage"), "statusMessage"));

        IColumn<NodeDto, String> menuColumn = new InlineMenuButtonColumn<NodeDto>(createNodesInlineMenu(false), 2, PageTasks.this){
            @Override
            protected int getHeaderNumberOfButtons() {
                return 2;
            }

            @Override
            protected List<InlineMenuItem> getHeaderMenuItems() {
                return createNodesInlineMenu(true);
            }
        };
        columns.add(menuColumn);

        return columns;
    }

    private List<InlineMenuItem> createNodesInlineMenu(boolean isHeader) {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.stopScheduler"),
            new Model<>(false),
            new Model<>(false),
                false,
                new ColumnMenuAction<NodeDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            stopSchedulersPerformed(target);
                        } else {
                            NodeDto rowDto = getRowModel().getObject();
                            stopSchedulersPerformed(target, rowDto);
                        }
                    }
                }, InlineMenuItem.TASKS_INLINE_MENU_ITEM_ID.NODE_STOP_SCHEDULER.getMenuItemId(),
                GuiStyleConstants.CLASS_STOP_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO.toString()){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isNodeShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.stopSchedulerAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        });

        items.add(new InlineMenuItem(createStringResource("pageTasks.button.stopSchedulerAndTasks"), false,
                new ColumnMenuAction<NodeDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        stopSchedulersAndTasksPerformed(target, getRowModel() != null ? getRowModel().getObject() : null);
                    }
                }){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isNodeShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.stopSchedulerTasksAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });


        items.add(new InlineMenuItem(createStringResource("pageTasks.button.startScheduler"),
            new Model<>(false),
            new Model<>(false),
                false,
                new ColumnMenuAction<NodeDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            startSchedulersPerformed(target);
                        } else {
                            NodeDto rowDto = getRowModel().getObject();
                            startSchedulersPerformed(target, rowDto);
                        }
                    }
                }, InlineMenuItem.TASKS_INLINE_MENU_ITEM_ID.NODE_START.getMenuItemId(),
                GuiStyleConstants.CLASS_START_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO.toString()){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isNodeShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.startSchedulerAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteNode"), false,
                new ColumnMenuAction<NodeDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            deleteNodesPerformed(target);
                        } else {
                            NodeDto rowDto = getRowModel().getObject();
                            deleteNodesPerformed(target, rowDto);
                        }
                    }
                }){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isNodeShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return PageTasks.this.getNodeConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });

        return items;
    }

    private List<IColumn<TaskDto, String>> initTaskColumns() {
        List<IColumn<TaskDto, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<TaskDto>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<TaskDto> rowModel) {
                TaskDtoProvider taskTableProvider = (TaskDtoProvider) table.getDataProvider();
                List<TaskDto> objects = taskTableProvider.getAvailableData();
                if (objects == null || objects.isEmpty()) {
                    return;
                }
                objects.forEach(taskDto -> {
                    if (taskDto.getOid().equals(rowModel.getObject().getOid())){
                        boolean selected = rowModel.getObject().isSelected();
                        taskDto.setSelected(selected);
                    }
                });
                super.onUpdateRow(target, table, rowModel);
            }
        };
        columns.add(column);

        column = createTaskNameColumn(this, "pageTasks.task.name");
        columns.add(column);

        columns.add(createTaskCategoryColumn(this, "pageTasks.task.category"));

		columns.add(new IconColumn<TaskDto>(createStringResource("")) {
			@Override
			protected IModel<String> createIconModel(IModel<TaskDto> rowModel) {
				ObjectReferenceType ref = rowModel.getObject().getObjectRef();
				if (ref == null || ref.getType() == null) {
					return Model.of("");
				}
				ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(ref.getType());
				String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
				return new Model<>(icon);
			}

			private ObjectTypeGuiDescriptor getObjectTypeDescriptor(QName type) {
				return ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(type));
			}

			@Override
			public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId, IModel<TaskDto> rowModel) {
				super.populateItem(item, componentId, rowModel);
				ObjectReferenceType ref = rowModel.getObject().getObjectRef();
				if (ref != null && ref.getType() != null) {
					ObjectTypeGuiDescriptor guiDescriptor = getObjectTypeDescriptor(ref.getType());
					if (guiDescriptor != null) {
						item.add(AttributeModifier.replace("title", createStringResource(guiDescriptor.getLocalizationKey())));
						item.add(new TooltipBehavior());
					}
				}
			}
		});

        columns.add(new AbstractExportableColumn<TaskDto, String>(createStringResource("pageTasks.task.objectRef")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createObjectRef(rowModel);
                    }
                }));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return Model.of(createObjectRef(rowModel));
            }

        });
        columns.add(createTaskExecutionStatusColumn(this, "pageTasks.task.execution"));
        columns.add(new PropertyColumn<>(createStringResource("pageTasks.task.executingAt"), "executingAt"));
        columns.add(createProgressColumn(this, "pageTasks.task.progress"));
        columns.add(new AbstractExportableColumn<TaskDto, String>(createStringResource("pageTasks.task.currentRunTime")) {

            @Override
            public void populateItem(final Item<ICellPopulator<TaskDto>> item, final String componentId,
                                     final IModel<TaskDto> rowModel) {

                DateLabelComponent dateLabel = new DateLabelComponent(componentId, new AbstractReadOnlyModel<Date>() {

                    @Override
                    public Date getObject() {
                        Date date = getCurrentRuntime(rowModel);
                        TaskDto task = rowModel.getObject();
                        if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED) {
                            ((DateLabelComponent) item.get(componentId)).setBefore("closed at ");
                        } else if (date != null){
                            ((DateLabelComponent) item.get(componentId)).setBefore(DurationFormatUtils.formatDurationWords(date.getTime(), true, true));
                        }
                        return date;
                    }
                }, DateLabelComponent.MEDIUM_MEDIUM_STYLE);
                item.add(dateLabel);
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                TaskDto task = rowModel.getObject();
                Date date = getCurrentRuntime(rowModel);
                String displayValue = "";
                if (date != null) {
                    if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED) {
                        displayValue = "closed at " + WebComponentUtil.getLocalizedDate(date, DateLabelComponent.LONG_MEDIUM_STYLE);
                    } else {
                        displayValue = DurationFormatUtils.formatDurationWords(date.getTime(), true, true);
                    }
                }
                return Model.of(displayValue);
            }
        });
        columns.add(new AbstractExportableColumn<TaskDto, String>(createStringResource("pageTasks.task.scheduledToRunAgain")) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        return createScheduledToRunAgain(rowModel);
                    }
                }));
            }
            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return Model.of(createScheduledToRunAgain(rowModel));
            }
         });

        columns.add(new IconColumn<TaskDto>(createStringResource("pageTasks.task.status")) {

            @Override
            protected IModel<String> createTitleModel(final IModel<TaskDto> rowModel) {

                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        TaskDto dto = rowModel.getObject();

                        if (dto != null && dto.getStatus() != null) {
                            return createStringResourceStatic(PageTasks.this, dto.getStatus()).getString();
                        } else {
                            return createStringResourceStatic(PageTasks.this, OperationResultStatus.UNKNOWN).getString();
                        }
                    }
                };
            }

            @Override
            protected IModel<String> createIconModel(final IModel<TaskDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getStatus() != null) {
                            return OperationResultStatusPresentationProperties.parseOperationalResultStatus(rowModel.getObject().getStatus().createStatusType()).getIcon() + " fa-lg";
                        } else
                            return OperationResultStatusPresentationProperties.UNKNOWN.getIcon() + " fa-lg";
                    }
                };
            }
        });

        IColumn<TaskDto, String> menuColumn = new InlineMenuButtonColumn<TaskDto>(createTasksInlineMenu(false, null), 2, PageTasks.this){
            @Override
            protected int getHeaderNumberOfButtons() {
                return 2;
            }

            @Override
            protected List<InlineMenuItem> getHeaderMenuItems() {
                return createTasksInlineMenu(true, null);
            }
        };
        columns.add(menuColumn);

        return columns;
    }

	@NotNull
	public static AbstractExportableColumn<TaskDto, String> createProgressColumn(PageBase pageBase, final String titleKey) {
		return new AbstractExportableColumn<TaskDto, String>(pageBase.createStringResource(titleKey)) {

		    @Override
		    public void populateItem(Item<ICellPopulator<TaskDto>> cellItem, String componentId, final IModel<TaskDto> rowModel) {
		        cellItem.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {
		            @Override
		            public Object getObject() {
			            rowModel.getObject().ensureSubtasksLoaded(pageBase);
		                return rowModel.getObject().getProgressDescription(pageBase);
		            }
		        }));
		    }

		    @Override
		    public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
			    rowModel.getObject().ensureSubtasksLoaded(pageBase);
		        return Model.of(rowModel.getObject().getProgressDescription(pageBase));
		    }
		};
	}

	private List<InlineMenuItem> createTasksInlineMenu(boolean isHeader, TaskDto dto) {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.suspendTask"),
            new Model<>(false),
            new Model<>(false),
                false,
                new ColumnMenuAction<TaskDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            suspendTasksPerformed(target);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            suspendTaskPerformed(target, rowDto);
                        }
                    }
                }, InlineMenuItem.TASKS_INLINE_MENU_ITEM_ID.SUSPEND.getMenuItemId(),
                GuiStyleConstants.CLASS_SUSPEND_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO.toString()){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isTaskShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.suspendAction").getString();
                return PageTasks.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.resumeTask"),
            new Model<>(false),
            new Model<>(false),
                false,
                new ColumnMenuAction<TaskDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            resumeTasksPerformed(target);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            resumeTaskPerformed(target, rowDto);
                        }
                    }
                }, InlineMenuItem.TASKS_INLINE_MENU_ITEM_ID.RESUME.getMenuItemId(),
                GuiStyleConstants.CLASS_RESUME_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO.toString()){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isTaskShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.resumeAction").getString();
                return PageTasks.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.scheduleTask"), false,
                new ColumnMenuAction<TaskDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            scheduleTasksPerformed(target);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            scheduleTaskPerformed(target, rowDto);
                        }
                    }
                }){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isTaskShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.runNowAction").getString();
                return PageTasks.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        });
        items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteTask"), false,
                new ColumnMenuAction<TaskDto>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null){
                            deleteTaskConfirmedPerformed(target, null);
                        } else {
                            TaskDto rowDto = getRowModel().getObject();
                            deleteTaskConfirmedPerformed(target, rowDto);
                        }
                    }
                }){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isShowConfirmationDialog() {
                return PageTasks.this.isTaskShowConfirmationDialog((ColumnMenuAction) getAction());
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                String actionName = createStringResource("pageTasks.message.deleteAction").getString();
                return PageTasks.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }

        });
        if (!isHeader && dto != null) {
        	if (dto.getTaskType().getWorkManagement() != null && dto.getTaskType().getWorkManagement().getTaskKind() == TaskKindType.COORDINATOR) {
		        items.add(new InlineMenuItem(createStringResource("pageTasks.button.reconcileWorkers"), false,
				        new ColumnMenuAction<TaskDto>() {

					        @Override
					        public void onClick(AjaxRequestTarget target) {
						        if (getRowModel() == null) {
							        throw new UnsupportedOperationException();
						        } else {
							        TaskDto rowDto = getRowModel().getObject();
							        reconcileWorkersConfirmedPerformed(target, rowDto);
						        }
					        }
				        }) {

			        private static final long serialVersionUID = 1L;

			        @Override
			        public boolean isShowConfirmationDialog() {
				        return PageTasks.this.isTaskShowConfirmationDialog((ColumnMenuAction) getAction());
			        }

			        @Override
			        public IModel<String> getConfirmationMessageModel() {
				        String actionName = createStringResource("pageTasks.message.reconcileWorkersAction").getString();
				        return PageTasks.this.getTaskConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
			        }
		        });
	        }
        }
        if (isHeader) {
            items.add(new InlineMenuItem(createStringResource("pageTasks.button.deleteAllClosedTasks"), false,
                    new ColumnMenuAction<TaskDto>() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteAllClosedTasksConfirmedPerformed(target);
                        }
                    }){

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isShowConfirmationDialog() {
                    return true;
                }

                @Override
                public IModel<String> getConfirmationMessageModel(){
                    return createStringResource("pageTasks.message.deleteAllClosedTasksConfirm");
                }

            });
        }
        return items;
    }


    // used in SubtasksPanel as well
    public static IColumn createTaskNameColumn(final Component component, String label) {
        LinkColumn<TaskDto> column = new LinkColumn<TaskDto>(createStringResourceStatic(component, label), TaskDto.F_NAME, TaskDto.F_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<TaskDto> rowModel) {
                TaskDto task = rowModel.getObject();
                taskDetailsPerformed(target, task.getOid());
            }

            private void taskDetailsPerformed(AjaxRequestTarget target, String oid) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, oid);

                PageBase page = (PageBase) component.getPage();
                page.navigateToNext(PageTaskEdit.class, parameters);
            }

            @Override
            public boolean isEnabled(IModel<TaskDto> rowModel) {
                return super.isEnabled(rowModel) && rowModel.getObject().getOid() != null;
            }
        };
        return column;
    }

    public static AbstractColumn<TaskDto, String> createTaskCategoryColumn(final Component component, String label) {
        return new AbstractExportableColumn<TaskDto, String>(createStringResourceStatic(component, label)) {

            @Override
            public void populateItem(Item<ICellPopulator<TaskDto>> item, String componentId,
                                     final IModel<TaskDto> rowModel) {
                item.add(new Label(componentId, WebComponentUtil.createCategoryNameModel(component, new PropertyModel<>(rowModel, TaskDto.F_CATEGORY))));
            }

            @Override
            public IModel<String> getDataModel(IModel<TaskDto> rowModel) {
                return WebComponentUtil.createCategoryNameModel(component, new PropertyModel<>(rowModel, TaskDto.F_CATEGORY));
            }
        };
    }

    public static EnumPropertyColumn createTaskResultStatusColumn(final Component component, String label) {
        return new EnumPropertyColumn(createStringResourceStatic(component, label), "status") {

            @Override
            protected String translate(Enum en) {
                return createStringResourceStatic(component, en).getString();
            }
        };
    }

    public static EnumPropertyColumn<TaskDto> createTaskExecutionStatusColumn(final Component component, String label) {
        return new EnumPropertyColumn<TaskDto>(createStringResourceStatic(component, label), "execution") {

            @Override
            protected String translate(Enum en) {
                return createStringResourceStatic(component, en).getString();
            }
        };
    }

//    public static IColumn createTaskDetailColumn(final Component component, String label, boolean workflowsEnabled) {
//
//        if (workflowsEnabled) {
//
//            return new LinkColumn<TaskDto>(createStringResourceStatic(component, label), TaskDto.F_WORKFLOW_LAST_DETAILS) {
//
//                @Override
//                public void onClick(AjaxRequestTarget target, IModel<TaskDto> rowModel) {
//                    TaskDto task = rowModel.getObject();
//                    taskDetailsPerformed(target, task);
//                }
//
//                // todo display a message if process instance cannot be found
//                private void taskDetailsPerformed(AjaxRequestTarget target, TaskDto task) {
//                    if (task.getWorkflowProcessInstanceId() != null) {
//                        PageParameters parameters = new PageParameters();
//                        parameters.add(OnePageParameterEncoder.PARAMETER, task.getWorkflowProcessInstanceId());
//                        component.setResponsePage(new PageProcessInstance(parameters, (PageBase) component.getPage()));
//                    }
//                }
//
//            };
//        } else {
//            return new PropertyColumn(createStringResourceStatic(component, label), TaskDto.F_WORKFLOW_LAST_DETAILS);
//        }
//    }

    private String createObjectRef(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
        if (task.getObjectRef() == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(task.getObjectRefName())) {
            return task.getObjectRefName();
        } else {
            return task.getObjectRef().getOid();
        }
    }

    private String createScheduledToRunAgain(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();
		boolean runnable = task.getRawExecutionStatus() == TaskExecutionStatus.RUNNABLE;
        Long scheduledAfter = task.getScheduledToStartAgain();
		Long retryAfter = runnable ? task.getRetryAfter() : null;

        if (scheduledAfter == null) {
            if (retryAfter == null || retryAfter <= 0) {
                return "";
            }
        } else if (scheduledAfter == TaskDto.NOW) { // TODO what about retryTime?
            return getString(runnable ? "pageTasks.now" : "pageTasks.nowForNotRunningTasks");
        } else if (scheduledAfter == TaskDto.RUNS_CONTINUALLY) {	// retryTime is probably null here
            return getString("pageTasks.runsContinually");
        } else if (scheduledAfter == TaskDto.ALREADY_PASSED && retryAfter == null) {
            return getString(runnable ? "pageTasks.alreadyPassed" : "pageTasks.alreadyPassedForNotRunningTasks");
        }

        long displayTime;
        boolean displayAsRetry;
        if (retryAfter != null && retryAfter > 0 && (scheduledAfter == null || scheduledAfter < 0 || retryAfter < scheduledAfter)) {
        	displayTime = retryAfter;
        	displayAsRetry = true;
		} else {
        	displayTime = scheduledAfter;
        	displayAsRetry = false;
		}

		String key;
        if (runnable) {
        	key = displayAsRetry ? "pageTasks.retryIn" : "pageTasks.in";
		} else {
        	key = "pageTasks.inForNotRunningTasks";
		}

        //todo i18n
        return PageBase.createStringResourceStatic(this, key, DurationFormatUtils.formatDurationWords(displayTime, true, true)).getString();
    }

    private Date getCurrentRuntime(IModel<TaskDto> taskModel) {
        TaskDto task = taskModel.getObject();

        if (task.getRawExecutionStatus() == TaskExecutionStatus.CLOSED) {

            //todo i18n and proper date/time formatting
            Long time = task.getCompletionTimestamp();
            if (time == null) {
                return null;
            }
            return new Date(time);

        } else {
            Long time = task.getCurrentRuntime();
            if (time == null) {
                return null;
            }
            //todo i18n
            return null;
        }
    }

    private String getLastCheckInTime(IModel<NodeDto> nodeModel) {
        NodeDto node = nodeModel.getObject();
        Long time = node.getLastCheckInTime();
        if (time == null || time == 0) {
            return "";
        }

        //todo i18n
        return DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - time, true, true)
                + " ago";
    }

    private void initDiagnosticButtons() {
        AjaxButton deactivate = new AjaxButton("deactivateServiceThreads",
                createStringResource("pageTasks.button.deactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deactivateServiceThreadsPerformed(target);
            }
        };
        add(deactivate);

        AjaxButton reactivate = new AjaxButton("reactivateServiceThreads",
                createStringResource("pageTasks.button.reactivateServiceThreads")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reactivateServiceThreadsPerformed(target);
            }
        };
        add(reactivate);

        AjaxButton synchronize = new AjaxButton("synchronizeTasks",
                createStringResource("pageTasks.button.synchronizeTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                synchronizeTasksPerformed(target);
            }
        };
        add(synchronize);

		AjaxButton synchronizeWorkflowRequests = new AjaxButton(ID_SYNCHRONIZE_WORKFLOW_REQUESTS,
				createStringResource("pageTasks.button.synchronizeWorkflowRequests")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				synchronizeWorkflowRequestsPerformed(target);
			}
		};
		add(synchronizeWorkflowRequests);

//      adding Refresh button
        AjaxButton refresh = new AjaxButton("refreshTasks",
                createStringResource("pageTasks.button.refreshTasks")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshTasks(target);
            }
        };

        add(refresh);
    }

    private Table getTaskTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_TASK_TABLE));
    }

    private Table getNodeTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_NODE_TABLE));
    }

    private boolean isSomeTaskSelected(List<TaskDto> tasks, AjaxRequestTarget target) {
        if (!tasks.isEmpty()) {
            return true;
        }

        warn(getString("pageTasks.message.noTaskSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private boolean isSomeNodeSelected(List<NodeDto> nodes, AjaxRequestTarget target) {
        if (!nodes.isEmpty()) {
            return true;
        }

        warn(getString("pageTasks.message.noNodeSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private void suspendTasksPerformed(AjaxRequestTarget target, List<String> oidList) {
    	Task opTask = createSimpleTask(OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            boolean suspended = getTaskService().suspendTasks(oidList, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully suspended.");    // todo i18n
                } else {
                    result.recordWarning("Task(s) suspension has been successfully requested; please check for its completion using task list.");   // todo i18n
                }
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't suspend the task(s)", e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void suspendTaskPerformed(AjaxRequestTarget target, TaskDto dto) {
        suspendTasksPerformed(target, Arrays.asList(dto.getOid()));
    }

    //region Task-level actions
    private void suspendTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskTypeList = WebComponentUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskTypeList, target)) {
            return;
        }

        suspendTasksPerformed(target, TaskDto.getOids(taskTypeList));
    }

    private void resumeTasksPerformed(AjaxRequestTarget target, List<String> oids) {
    	Task opTask = createSimpleTask(OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().resumeTasks(oids, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully resumed.");
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't resume the task(s)", e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void resumeTaskPerformed(AjaxRequestTarget target, TaskDto dto) {
        resumeTasksPerformed(target, Arrays.asList(dto.getOid()));
    }

    private void resumeTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebComponentUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        resumeTasksPerformed(target, TaskDto.getOids(taskDtoList));
    }

    private void scheduleTasksPerformed(AjaxRequestTarget target, List<String> oids) {
    	Task opTask = createSimpleTask(OPERATION_SCHEDULE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().scheduleTasksNow(oids, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully scheduled.");    // todo i18n
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't schedule the task(s)", e);        // todo i18n
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void scheduleTaskPerformed(AjaxRequestTarget target, TaskDto dto) {
        scheduleTasksPerformed(target, Arrays.asList(dto.getOid()));
    }

    private void scheduleTasksPerformed(AjaxRequestTarget target) {
        List<TaskDto> taskDtoList = WebComponentUtil.getSelectedData(getTaskTable());
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        scheduleTasksPerformed(target, TaskDto.getOids(taskDtoList));
    }
    //endregion

    //region Node-level actions
    private void nodeDetailsPerformed(AjaxRequestTarget target, String oid) {

    }

    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target, List<String> identifiers) {
    	Task opTask = createSimpleTask(OPERATION_STOP_SCHEDULERS_AND_TASKS);
        OperationResult result = opTask.getResult();
        try {
            boolean suspended = getTaskService().stopSchedulersAndTasks(identifiers, WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (suspended) {
                    // todo i18n
                    result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been " +
                            "successfully stopped, including tasks that were running on them.");
                } else {
                    // todo i18n
                    result.recordWarning("Selected node scheduler(s) have been successfully paused; however, " +
                            "some of the tasks they were executing are still running on them. Please check " +
                            "their completion using task list.");
                }
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't stop schedulers due", e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void stopSchedulersAndTasksPerformed(AjaxRequestTarget target,  NodeDto dto) {
        List<NodeDto> nodeDtoList = new ArrayList<>();
        if (dto != null){
            nodeDtoList.add(dto);
        } else {
            nodeDtoList.addAll(WebComponentUtil.getSelectedData(getNodeTable()));
        }
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        stopSchedulersAndTasksPerformed(target, NodeDto.getNodeIdentifiers(nodeDtoList));
    }

    private void startSchedulersPerformed(AjaxRequestTarget target, List<String> identifiers) {
    	Task opTask = createSimpleTask(OPERATION_START_SCHEDULERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().startSchedulers(identifiers, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully started.");   // todo i18n
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't start the scheduler(s)", e);
        }

        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void startSchedulersPerformed(AjaxRequestTarget target, NodeDto dto) {
        startSchedulersPerformed(target, Arrays.asList(dto.getNodeIdentifier()));
    }

    private void startSchedulersPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebComponentUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

       startSchedulersPerformed(target, NodeDto.getNodeIdentifiers(nodeDtoList));
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target, List<String> identifiers) {
    	Task opTask = createSimpleTask(OPERATION_STOP_SCHEDULERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().stopSchedulers(identifiers, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Selected node scheduler(s) have been successfully stopped.");   // todo i18n
            }
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't stop the scheduler(s)", e);   // todo i18n
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target, NodeDto dto) {
        stopSchedulersPerformed(target, Arrays.asList(dto.getNodeIdentifier()));
    }

    private void stopSchedulersPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebComponentUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        stopSchedulersPerformed(target, NodeDto.getNodeIdentifiers(nodeDtoList));
    }

    private void deleteNodesPerformed(AjaxRequestTarget target, List<NodeDto> nodes) {
        OperationResult result = new OperationResult(OPERATION_DELETE_NODES);

        Task task = createSimpleTask(OPERATION_DELETE_NODES);

        for (NodeDto nodeDto : nodes) {
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            deltas.add(ObjectDelta.createDeleteDelta(NodeType.class, nodeDto.getOid(), getPrismContext()));
            try {
                getModelService().executeChanges(deltas, null, task, result);
            } catch (Exception e) {     // until java 7 we do it in this way
                result.recordFatalError("Couldn't delete the node " + nodeDto.getNodeIdentifier(), e);
            }
        }

        result.computeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected node(s) have been successfully deleted."); // todo i18n
        }
        showResult(result);

        NodeDtoProvider provider = (NodeDtoProvider) getNodeTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        refreshTables(target);
    }

    private void deleteNodesPerformed(AjaxRequestTarget target, NodeDto dto) {
        deleteNodesPerformed(target, Arrays.asList(dto));
    }

    private void deleteNodesPerformed(AjaxRequestTarget target) {
        List<NodeDto> nodeDtoList = WebComponentUtil.getSelectedData(getNodeTable());
        if (!isSomeNodeSelected(nodeDtoList, target)) {
            return;
        }

        deleteNodesPerformed(target, nodeDtoList);
    }
    //endregion

    //region Diagnostics actions
    private void deactivateServiceThreadsPerformed(AjaxRequestTarget target) {
    	Task opTask = createSimpleTask(OPERATION_DEACTIVATE_SERVICE_THREADS);
        OperationResult result = opTask.getResult();

        try {
            boolean stopped = getTaskService().deactivateServiceThreads(WAIT_FOR_TASK_STOP, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                if (stopped) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Service threads on local node have been successfully deactivated.");    // todo i18n
                } else {
                    // todo i18n
                    result.recordWarning("Deactivation of service threads on local node have been successfully requested; however, some of the tasks are still running. Please check their completion using task list.");
                }
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't deactivate service threads on this node", e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void reactivateServiceThreadsPerformed(AjaxRequestTarget target) {
    	Task opTask = createSimpleTask(OPERATION_REACTIVATE_SERVICE_THREADS);
        OperationResult result = opTask.getResult();

        try {
            getTaskService().reactivateServiceThreads(opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "Service threads on local node have been successfully reactivated.");    // todo i18n
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't reactivate service threads on local node", e);    // todo i18n
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

    private void refreshTables(AjaxRequestTarget target) {
        clearTablesCache();
        target.add(getFeedbackPanel());
        target.add((Component) getTaskTable());
        target.add((Component) getNodeTable());
    }

    private void clearTablesCache(){
        if (getTaskTable() != null && getTaskTable().getDataTable() != null){
            WebComponentUtil.clearProviderCache(getTaskTable().getDataTable().getDataProvider());
        }
        if (getNodeTable() != null && getNodeTable().getDataTable() != null){
            WebComponentUtil.clearProviderCache(getNodeTable().getDataTable().getDataProvider());
        }
    }

    private void synchronizeTasksPerformed(AjaxRequestTarget target) {
    	Task opTask = createSimpleTask(OPERATION_SYNCHRONIZE_TASKS);
        OperationResult result = opTask.getResult();

        try {
            getTaskService().synchronizeTasks(opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {       // brutal hack - the subresult's message contains statistics
                result.recordStatus(OperationResultStatus.SUCCESS, result.getLastSubresult().getMessage());
            }
        } catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't synchronize tasks", e);
        }
        showResult(result);

        //refresh feedback and table
        refreshTables(target);
    }

	private void synchronizeWorkflowRequestsPerformed(AjaxRequestTarget target) {
		Task opTask = createSimpleTask(OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS);
        OperationResult result = opTask.getResult();

		try {
			getTaskService().synchronizeWorkflowRequests(opTask, result);
			result.computeStatusIfUnknown();
			if (result.isSuccess()) {       // brutal hack - the subresult's message contains statistics
				result.recordStatus(OperationResultStatus.SUCCESS, result.getLastSubresult().getMessage());
			}
		} catch (RuntimeException | SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
			result.recordFatalError("Couldn't synchronize tasks", e);
		}
		showResult(result);

		//refresh feedback and table
		refreshTables(target);
	}
    //endregion

    private void refreshTasks(AjaxRequestTarget target) {
//        searchModel = new LoadableModel<TasksSearchDto>(false) {
//
//            @Override
//            protected TasksSearchDto load() {
//                return loadTasksSearchDto();
//            }
//        };

		target.add(refreshPanel);

        //refresh feedback and table
        refreshTables(target);

		if (refreshModel.getObject().isEnabled()) {
			refreshPanel.startRefreshing(this, target);
		}
    }

    private void searchFilterPerformed(AjaxRequestTarget target) {
        TasksSearchDto dto = searchModel.getObject();

//        ObjectQuery query = createTaskQuery(dto.getStatus(), dto.getCategory(), dto.isShowSubtasks());
        ObjectQuery query = createTaskQuery();

        Table panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        TasksStorage storage = getSessionStorage().getTasks();
        storage.setTasksSearch(dto);

        target.add(getFeedbackPanel());
        target.add((Component) getTaskTable());
    }

    private ObjectQuery createTaskQuery() {
        TasksSearchDto dto = searchModel.getObject();
        TaskDtoExecutionStatusFilter status = dto.getStatus();
        String category = dto.getCategory();
        boolean showSubtasks = dto.isShowSubtasks();

        S_AtomicFilterEntry q = QueryBuilder.queryFor(TaskType.class, getPrismContext());
        if (status != null) {
            q = status.appendFilter(q);
        }
        if (category != null && !ALL_CATEGORIES.equals(category)) {
            q = q.item(TaskType.F_CATEGORY).eq(category).and();
        }
        if (StringUtils.isNotBlank(searchText)) {
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(searchText);
            q = q.item(TaskType.F_NAME).containsPoly(normalizedString, normalizedString).matchingNorm().and();
            searchText = "";        // ???
        }
        if (!Boolean.TRUE.equals(showSubtasks)) {
            q = q.item(TaskType.F_PARENT).isNull().and();
        }
        return q.all().build();
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        TasksSearchDto tasksSearchDto = new TasksSearchDto();
        tasksSearchDto.setCategory(ALL_CATEGORIES);
        tasksSearchDto.setStatus(TaskDtoExecutionStatusFilter.ALL);
        searchModel.setObject(tasksSearchDto);

        Table panel = getTaskTable();
        DataTable table = panel.getDataTable();
        TaskDtoProvider provider = (TaskDtoProvider) table.getDataProvider();
        provider.setQuery(null);

        TasksStorage storage = getSessionStorage().getTasks();
        storage.setTasksSearch(searchModel.getObject());
        panel.setCurrentPage(storage.getPaging());

        target.add((Component) panel);
    }

    private void deleteTaskConfirmedPerformed(AjaxRequestTarget target, TaskDto task) {
        List<TaskDto> taskDtoList = new ArrayList<>();
        if (task != null){
            taskDtoList.add(task);
        } else {
            taskDtoList.addAll(WebComponentUtil.getSelectedData(getTaskTable()));
        }
        if (!isSomeTaskSelected(taskDtoList, target)) {
            return;
        }

        Task opTask = createSimpleTask(OPERATION_DELETE_TASKS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().suspendAndDeleteTasks(TaskDto.getOids(taskDtoList), WAIT_FOR_TASK_STOP, true, opTask, result);
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, "The task(s) have been successfully deleted."); // todo i18n
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't delete the task(s)", e);  // todo i18n
        }
        showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        refreshTables(target);
    }

    private void reconcileWorkersConfirmedPerformed(AjaxRequestTarget target, @NotNull TaskDto task) {
        Task opTask = createSimpleTask(OPERATION_RECONCILE_WORKERS);
        OperationResult result = opTask.getResult();
        try {
            getTaskService().reconcileWorkers(task.getOid(), opTask, result);
            result.computeStatus();
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError("Couldn't reconcile the workers", e);  // todo i18n
        }
	    showResult(result);

        TaskDtoProvider provider = (TaskDtoProvider) getTaskTable().getDataTable().getDataProvider();
        provider.clearCache();

        //refresh feedback and table
        refreshTables(target);
    }

    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<TasksSearchDto> model) {
            super(id, markupId, markupProvider, model);

            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<TasksSearchDto> searchModel = (IModel) getDefaultModel();

            DropDownChoice listSelect = new DropDownChoice<>(ID_STATE,
                    new PropertyModel<>(searchModel, TasksSearchDto.F_STATUS),
                    new ReadOnlyEnumValuesModel<>(TaskDtoExecutionStatusFilter.class),
                    new EnumChoiceRenderer<>(this));
            listSelect.add(createFilterAjaxBehaviour());
            listSelect.setOutputMarkupId(true);
            listSelect.setNullValid(false);
            if (listSelect.getModel().getObject() == null) {
                listSelect.getModel().setObject(TaskDtoExecutionStatusFilter.ALL);
            }
            searchForm.add(listSelect);

            DropDownChoice categorySelect = new DropDownChoice(ID_CATEGORY,
                    new PropertyModel(searchModel, TasksSearchDto.F_CATEGORY),
                    new AbstractReadOnlyModel<List<String>>() {

                        @Override
                        public List<String> getObject() {
                            return createCategoryList();
                        }
                    },
                    new StringChoiceRenderer.Prefixed("pageTasks.category.") {

                        @Override
                        public String getDisplayValue(String object) {
                            if (ALL_CATEGORIES.equals(object)) {
                                object = "AllCategories";
                            }
                            return getPage().getString("pageTasks.category." + object);
                        }

                  }
            );
            categorySelect.setOutputMarkupId(true);
            categorySelect.setNullValid(false);
            categorySelect.add(createFilterAjaxBehaviour());
            if (categorySelect.getModel().getObject() == null) {
                categorySelect.getModel().setObject(ALL_CATEGORIES);
            }
            searchForm.add(categorySelect);

            CheckBox showSubtasks = new CheckBox(ID_SHOW_SUBTASKS,
                    new PropertyModel(searchModel, TasksSearchDto.F_SHOW_SUBTASKS));
            showSubtasks.add(createFilterAjaxBehaviour());
            searchForm.add(showSubtasks);

            AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_SEARCH_CLEAR) {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    PageTasks page = (PageTasks) getPage();
                    page.clearSearchPerformed(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    PageTasks page = (PageTasks) getPage();
                    target.add(page.getFeedbackPanel());
                }
            };
            searchForm.add(clearButton);
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("change") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    PageTasks page = (PageTasks) getPage();
                    page.searchFilterPerformed(target);
                }
            };
        }

        private List<String> createCategoryList() {
            List<String> categories = new ArrayList<>();
            categories.add(ALL_CATEGORIES);

            PageTasks page = (PageTasks) getPage();
            List<String> list = page.getTaskService().getAllTaskCategories();
            if (list != null) {
                categories.addAll(list);
                Collections.sort(categories);
            }

            return categories;
        }
    }

    private void deleteAllClosedTasksConfirmedPerformed(AjaxRequestTarget target) {
        OperationResult launchResult = new OperationResult(OPERATION_DELETE_ALL_CLOSED_TASKS);
        Task task = createSimpleTask(OPERATION_DELETE_ALL_CLOSED_TASKS);

        task.setHandlerUri(ModelPublicConstants.CLEANUP_TASK_HANDLER_URI);
        task.setName("Closed tasks cleanup");

        try {
            CleanupPolicyType policy = new CleanupPolicyType();
            policy.setMaxAge(XmlTypeConverter.createDuration(0));

            CleanupPoliciesType policies = new CleanupPoliciesType();
            policies.setClosedTasks(policy);

            PrismProperty<CleanupPoliciesType> policiesProperty = getPrismContext().getSchemaRegistry()
                    .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES).instantiate();
            policiesProperty.setRealValue(policies);
            task.setExtensionProperty(policiesProperty);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            launchResult.recordFatalError("Error dealing with schema", e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        getTaskManager().switchToBackground(task, launchResult);
		launchResult.setBackgroundTaskOid(task.getOid());

        showResult(launchResult);
        target.add(getFeedbackPanel());
    }

    private void addInlineMenuToTaskRow(TaskDto dto) {
        addInlineMenuToTaskDto(dto);

        List<TaskDto> list = new ArrayList<>();
        if (dto.getSubtasks() != null) {
            list.addAll(dto.getTransientSubtasks());
        }
        if (dto.getTransientSubtasks() != null) {
            list.addAll(dto.getSubtasks());
        }

        for (TaskDto task : list) {
            addInlineMenuToTaskDto(task);
        }
    }

    private void addInlineMenuToTaskDto(final TaskDto dto) {
        List<InlineMenuItem> items = dto.getMenuItems();
        if (!items.isEmpty()) {
            //menu was already added
            return;
        }

        items.addAll(createTasksInlineMenu(false, dto));
    }

    private void addInlineMenuToNodeRow(final NodeDto dto) {
        List<InlineMenuItem> items = dto.getMenuItems();
        if (!items.isEmpty()) {
            //menu already added
            return;
        }

        items.addAll(createNodesInlineMenu(false));
    }

    private IModel<String> getTaskConfirmationMessageModel(ColumnMenuAction action, String actionName){
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleTaskObject",
                    actionName, WebComponentUtil.getSelectedData(getTaskTable()).size());
        } else {
            String objectName = ((TaskDto)(action.getRowModel().getObject())).getName();
           return createStringResource("pageTasks.message.confirmationMessageForSingleTaskObject",
                    actionName, objectName);
        }

    }

    private boolean isTaskShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                WebComponentUtil.getSelectedData(getTaskTable()).size() > 0;
    }

    private IModel<String> getNodeConfirmationMessageModel(ColumnMenuAction action, String actionName){
        if (action.getRowModel() == null) {
            return createStringResource("pageTasks.message.confirmationMessageForMultipleNodeObject",
                    actionName, WebComponentUtil.getSelectedData(getNodeTable()).size());
        } else {
            String objectName = ((NodeDto)(action.getRowModel().getObject())).getName();
            return createStringResource("pageTasks.message.confirmationMessageForSingleNodeObject",
                    actionName, objectName);
        }

    }

    private boolean isNodeShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                WebComponentUtil.getSelectedData(getNodeTable()).size() > 0;
    }
}
