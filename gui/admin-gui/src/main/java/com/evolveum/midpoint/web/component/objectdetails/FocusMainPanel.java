/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.assignment.GenericAbstractRoleAssignmentPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.StringValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class FocusMainPanel<F extends FocusType> extends AbstractObjectMainPanel<F> {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(FocusMainPanel.class);

	private LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel;
	private TaskDtoProvider taskDtoProvider;
    private FocusAssignmentsTabPanel assignmentsTabPanel = null;

	public FocusMainPanel(String id, LoadableModel<ObjectWrapper<F>> objectModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel,
			PageAdminFocus<F> parentPage) {
		super(id, objectModel, parentPage);
		Validate.notNull(projectionModel, "Null projection model");
		this.projectionModel = projectionModel;
		initLayout(parentPage);
	}

	private void initLayout(final PageAdminObjectDetails<F> parentPage) {
		getMainForm().setMultiPart(true);

		taskDtoProvider = new TaskDtoProvider(parentPage, TaskDtoProviderOptions.minimalOptions());
		taskDtoProvider.setQuery(createTaskQuery(null, parentPage));
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		StringValue oidValue = getPage().getPageParameters().get(OnePageParameterEncoder.PARAMETER);

		taskDtoProvider.setQuery(createTaskQuery(oidValue != null ? oidValue.toString() : null, (PageBase)getPage()));
	}

	private ObjectQuery createTaskQuery(String oid, PageBase page) {
		if (oid == null) {
			oid = "non-existent"; // TODO !!!!!!!!!!!!!!!!!!!!
		}
		return QueryBuilder.queryFor(TaskType.class, page.getPrismContext())
				.item(TaskType.F_OBJECT_REF).ref(oid)
				.and()
					.block()
						.not().item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.CLOSED)
					.endBlock()
				.and().item(TaskType.F_PARENT).isNull()
				.build();
	}

	@Override
	protected List<ITab> createTabs(final PageAdminObjectDetails<F> parentPage) {
		List<ITab> tabs = new ArrayList<>();

		List<ObjectFormType> objectFormTypes = parentPage.getObjectFormTypes();
		// default tabs are always added to component structure, visibility is decided later in
		// visible behavior based on adminGuiConfiguration
		addDefaultTabs(parentPage, tabs);
        addSpecificTabs(parentPage, tabs);
		if (objectFormTypes == null) {
			return tabs;
		}

		for (ObjectFormType objectFormType : objectFormTypes) {
			final FormSpecificationType formSpecificationType = objectFormType.getFormSpecification();
			if (formSpecificationType == null){
				continue;
			}
			String title = formSpecificationType.getTitle();
			if (title == null) {
				title = "pageAdminFocus.extended";
			}

			if (StringUtils.isEmpty(formSpecificationType.getPanelClass())) {
				continue;
			}

			tabs.add(
					new PanelTab(parentPage.createStringResource(title)) {
						private static final long serialVersionUID = 1L;

						@Override
						public WebMarkupContainer createPanel(String panelId) {
							return createTabPanel(panelId, formSpecificationType, parentPage);
						}
					});
		}

		return tabs;
	}

	protected WebMarkupContainer createTabPanel(String panelId, FormSpecificationType formSpecificationType,
			PageAdminObjectDetails<F> parentPage) {
		String panelClassName = formSpecificationType.getPanelClass();

		Class<?> panelClass;
		try {
			panelClass = Class.forName(panelClassName);
		} catch (ClassNotFoundException e) {
			throw new SystemException("Panel class '"+panelClassName+"' as specified in admin GUI configuration was not found", e);
		}
		if (AbstractFocusTabPanel.class.isAssignableFrom(panelClass)) {
			Constructor<?> constructor;
			try {
				constructor = panelClass.getConstructor(String.class, Form.class, LoadableModel.class, LoadableModel.class, PageBase.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SystemException("Unable to locate constructor (String,Form,LoadableModel,LoadableModel,LoadableModel,PageBase) in "+panelClass+": "+e.getMessage(), e);
			}
			AbstractFocusTabPanel<F> tabPanel;
			try {
				tabPanel = (AbstractFocusTabPanel<F>) constructor.newInstance(panelId, getMainForm(), getObjectModel(),
						projectionModel ,parentPage);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Error instantiating "+panelClass+": "+e.getMessage(), e);
			}
			return tabPanel;
		} else if (AbstractObjectTabPanel.class.isAssignableFrom(panelClass)) {
			Constructor<?> constructor;
			try {
				constructor = panelClass.getConstructor(String.class, Form.class, LoadableModel.class, PageBase.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SystemException("Unable to locate constructor (String,Form,LoadableModel,PageBase) in "+panelClass+": "+e.getMessage(), e);
			}
			AbstractObjectTabPanel<F> tabPanel;
			try {
				tabPanel = (AbstractObjectTabPanel<F>) constructor.newInstance(panelId, getMainForm(), getObjectModel(), parentPage);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Error instantiating "+panelClass+": "+e.getMessage(), e);
			}
			return tabPanel;

		} else {
			throw new UnsupportedOperationException("Tab panels that are not subclasses of AbstractObjectTabPanel or AbstractFocusTabPanel are not supported yet (got "+panelClass+")");
		}
	}

	protected WebMarkupContainer createFocusDetailsTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new FocusDetailsTabPanel<F>(panelId, getMainForm(), getObjectModel(), projectionModel, parentPage);
	}

	protected WebMarkupContainer createFocusProjectionsTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new FocusProjectionsTabPanel<F>(panelId, getMainForm(), getObjectModel(), projectionModel, parentPage);
	}

	protected WebMarkupContainer createFocusAssignmentsTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		assignmentsTabPanel = new FocusAssignmentsTabPanel<F>(panelId, getMainForm(), getObjectModel(), parentPage);
        return assignmentsTabPanel;
	}
	
	protected WebMarkupContainer createFocusDataProtectionTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		assignmentsTabPanel = new FocusAssignmentsTabPanel<F>(panelId, getMainForm(), getObjectModel(), parentPage) {
			
			 @Override
			protected AssignmentPanel createPanel(String panelId, ContainerWrapperFromObjectWrapperModel<AssignmentType, F> model) {
				return new GenericAbstractRoleAssignmentPanel(panelId, model);
			}
			
		};
        return assignmentsTabPanel;
	}

	protected WebMarkupContainer createObjectHistoryTabPanel(String panelId, PageAdminObjectDetails<F> parentPage) {
		return new ObjectHistoryTabPanel<>(panelId, getMainForm(), getObjectModel(), parentPage);
	}

	protected IModel<PrismObject<F>> unwrapModel() {
		return new AbstractReadOnlyModel<PrismObject<F>>() {

				@Override
			public PrismObject<F> getObject() {
				return getObjectWrapper().getObject();
			}
		};
	}

	protected void addSpecificTabs(final PageAdminObjectDetails<F> parentPage, List<ITab> tabs) {
    }

    protected void addDefaultTabs(final PageAdminObjectDetails<F> parentPage, List<ITab> tabs) {
		FocusTabVisibleBehavior authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_BASIC_URL);

		tabs.add(
				new PanelTab(parentPage.createStringResource("pageAdminFocus.basic"), authorization){

					private static final long serialVersionUID = 1L;

					@Override
					public WebMarkupContainer createPanel(String panelId) {
						return createFocusDetailsTabPanel(panelId, parentPage);
					}
				});

		authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_PROJECTIONS_URL);
		tabs.add(
                new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.projections"), authorization){

                	private static final long serialVersionUID = 1L;

					@Override
					public WebMarkupContainer createPanel(String panelId) {
						return createFocusProjectionsTabPanel(panelId, parentPage);
					}

					@Override
					public String getCount() {
						return Integer.toString(projectionModel.getObject() == null ? 0 : projectionModel.getObject().size());
					}
				});

		authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_PERSONAS_URL);
		tabs.add(
                new PanelTab(parentPage.createStringResource("pageAdminFocus.personas"), authorization){

                	private static final long serialVersionUID = 1L;

					@Override
					public WebMarkupContainer createPanel(String panelId) {
                        return new FocusPersonasTabPanel<F>(panelId, getMainForm(), getObjectModel(), parentPage);
					}

				});

		authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_ASSIGNMENTS_URL);
		tabs.add(
				new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.assignments"), authorization) {

					private static final long serialVersionUID = 1L;

					@Override
					public WebMarkupContainer createPanel(String panelId) {
						return createFocusAssignmentsTabPanel(panelId, parentPage);
					}

					@Override
					public String getCount() {
						return Integer.toString(countAssignments());
					}
				});
		
		authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_ASSIGNMENTS_URL);
		tabs.add(
				new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.dataProtection"), authorization) {

					private static final long serialVersionUID = 1L;

					@Override
					public WebMarkupContainer createPanel(String panelId) {
						return createFocusDataProtectionTabPanel(panelId, parentPage);
					}

					@Override
					public String getCount() {
						PrismObject<F> focus = getObjectModel().getObject().getObject();
						List<AssignmentType> assignments = focus.asObjectable().getAssignment();
						int count = 0;
						for (AssignmentType assignment : assignments) {
							if (assignment.getTargetRef() == null) {
								continue;
							}
							if (QNameUtil.match(assignment.getTargetRef().getType(), OrgType.COMPLEX_TYPE)) {
								Task task = parentPage.createSimpleTask("load data protection obejcts");
								PrismObject<OrgType> org = WebModelServiceUtils.loadObject(assignment.getTargetRef(), parentPage, task, task.getResult());
								
								if (org != null) {
									if (org.asObjectable().getOrgType().contains("access")) {
										count++;
									}
								}
							}
						}
						
						return String.valueOf(count);
					}
				});

		authorization = new FocusTabVisibleBehavior(unwrapModel(), ComponentConstants.UI_FOCUS_TAB_TASKS_URL);
		tabs.add(
				new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.tasks"), authorization) {

					private static final long serialVersionUID = 1L;

					@Override
					public WebMarkupContainer createPanel(String panelId) {
						return new FocusTasksTabPanel<F>(panelId, getMainForm(), getObjectModel(), taskDtoProvider, parentPage);
					}

					@Override
					public String getCount() {
						return Long.toString(taskDtoProvider == null ? 0L : taskDtoProvider.size());
					}
				});

	}

	@Override
    protected boolean areSavePreviewButtonsEnabled(){
        return isAssignmentsModelChanged();
    }

    private boolean isAssignmentsModelChanged(){
		ObjectWrapper<F> focusWrapper = getObjectModel().getObject();
		ContainerWrapper<AssignmentType> assignmentsWrapper =
				focusWrapper.findContainerWrapper(new ItemPath(FocusType.F_ASSIGNMENT));
		if (assignmentsWrapper != null) {
			for (ContainerValueWrapper assignmentWrapper : assignmentsWrapper.getValues()) {
				if (ValueStatus.DELETED.equals(assignmentWrapper.getStatus()) ||
						ValueStatus.ADDED.equals(assignmentWrapper.getStatus())) {
					return true;
				}
			}
		}
		return false;
	}

	protected int countPolicyRules() {
		int policyRuleCounter = 0;
		PrismObject<F> focus = getObjectModel().getObject().getObject();
		List<AssignmentType> assignments = focus.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {
			if (AssignmentsUtil.isPolicyRuleAssignment(assignment)) {
				policyRuleCounter++;
			}
		}
		return policyRuleCounter;
	}

	protected int countAssignments() {

		int rv = 0;
		PrismObject<F> focus = getObjectModel().getObject().getObject();
		List<AssignmentType> assignments = focus.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {
			if (!AssignmentsUtil.isPolicyRuleAssignment(assignment) && !AssignmentsUtil.isConsentAssignment(assignment)
					&& AssignmentsUtil.isAssignmentRelevant(assignment)) {
				rv++;
			}
		}
		return rv;
	}
}
