/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryQueryDiagRequest;
import com.evolveum.midpoint.schema.RepositoryQueryDiagResponse;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.RepoQueryDto;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author lazyman
 * @author mederly
 */
@PageDescriptor(url = "/admin/config/repositoryQuery", action = {
		@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
				label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_REPOSITORY_QUERY_URL,
                label = "PageRepositoryQuery.auth.query.label", description = "PageRepositoryQuery.auth.query.description")
})
public class PageRepositoryQuery extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageRepositoryQuery.class);

    private static final String DOT_CLASS = PageRepositoryQuery.class.getName() + ".";

    private static final String OPERATION_TRANSLATE_QUERY = DOT_CLASS + "translateQuery";
    private static final String OPERATION_EXECUTE_QUERY = DOT_CLASS + "executeQuery";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_EXECUTE_MIDPOINT = "executeMidPoint";
    private static final String ID_COMPILE_MIDPOINT = "compileMidPoint";
    private static final String ID_EXECUTE_HIBERNATE = "executeHibernate";
    private static final String ID_EDITOR_MIDPOINT = "editorMidPoint";
    private static final String ID_EDITOR_HIBERNATE = "editorHibernate";
    private static final String ID_HIBERNATE_PARAMETERS = "hibernateParameters";
    private static final String ID_RESULT_LABEL = "resultLabel";
    private static final String ID_RESULT_TEXT = "resultText";
	private static final String ID_QUERY_SAMPLE = "querySample";
	private static final String ID_OBJECT_TYPE = "objectType";
	private static final String ID_HIBERNATE_PARAMETERS_NOTE = "hibernateParametersNote";
	private static final String ID_INCOMPLETE_RESULTS_NOTE = "incompleteResultsNote";

	private static final String SAMPLES_DIR = "query-samples";
	private static final List<String> SAMPLES = Arrays.asList(
			"UserType_AllUsers",
			"UserType_UsersStartingWithA",
			"UserType_UsersContainingJack",
			"UserType_UsersNamedJack",
			"UserType_First10UsersStartingWithA",
			"UserType_UsersWithAGivenMailDomain",
			"UserType_SpecifiedCostCenters",
			"UserType_UsersThatHaveAssignedRole",
			"UserType_UsersThatHaveARole",
			"OrgType_AllRootOrgs",
			"OrgType_OrgOfType1",
			"ObjectType_AllObjectsInASubtree",
			"ObjectType_AllObjectsInAnOrg",
			"ShadowType_ShadowsOnGivenResource",
			"UserType_UsersWithShadowOnGivenResource"
	);

	private final IModel<RepoQueryDto> model = new Model<>(new RepoQueryDto());
	private final boolean isAdmin;

	enum Action {TRANSLATE_ONLY, EXECUTE_MIDPOINT, EXECUTE_HIBERNATE }

    public PageRepositoryQuery() {
		boolean admin;
		try {
			admin = getSecurityEnforcer().isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null);
		} catch (SchemaException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine admin authorization -- continuing as non-admin", e);
			admin = false;
		}
		isAdmin = admin;

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

		List<QName> objectTypeList = WebComponentUtil.createObjectTypeList();
		Collections.sort(objectTypeList, new Comparator<QName>() {
			@Override
			public int compare(QName o1, QName o2) {
				return String.CASE_INSENSITIVE_ORDER.compare(o1.getLocalPart(), o2.getLocalPart());
			}
		});
		DropDownChoice<QName> objectTypeChoice = new DropDownChoice<>(ID_OBJECT_TYPE,
				new PropertyModel<QName>(model, RepoQueryDto.F_OBJECT_TYPE),
				new ListModel<>(objectTypeList),
				new QNameChoiceRenderer());
		objectTypeChoice.setOutputMarkupId(true);
		objectTypeChoice.setNullValid(true);
		mainForm.add(objectTypeChoice);

		AceEditor editorMidPoint = new AceEditor(ID_EDITOR_MIDPOINT, new PropertyModel<String>(model, RepoQueryDto.F_MIDPOINT_QUERY));
		editorMidPoint.setHeight(400);
		editorMidPoint.setResizeToMaxHeight(false);
        mainForm.add(editorMidPoint);

		AceEditor editorHibernate = new AceEditor(ID_EDITOR_HIBERNATE, new PropertyModel<String>(model, RepoQueryDto.F_HIBERNATE_QUERY));
		editorHibernate.setHeight(300);
		editorHibernate.setResizeToMaxHeight(false);
		editorHibernate.setReadonly(!isAdmin);
		editorHibernate.setMode(null);
		mainForm.add(editorHibernate);

		AceEditor hibernateParameters = new AceEditor(ID_HIBERNATE_PARAMETERS, new PropertyModel<String>(model, RepoQueryDto.F_HIBERNATE_PARAMETERS));
		hibernateParameters.setReadonly(true);
		hibernateParameters.setHeight(100);
		hibernateParameters.setResizeToMaxHeight(false);
		hibernateParameters.setMode(null);
		mainForm.add(hibernateParameters);

		WebMarkupContainer hibernateParametersNote = new WebMarkupContainer(ID_HIBERNATE_PARAMETERS_NOTE);
		hibernateParametersNote.setVisible(isAdmin);
		mainForm.add(hibernateParametersNote);

		AjaxSubmitButton executeMidPoint = new AjaxSubmitButton(ID_EXECUTE_MIDPOINT, createStringResource("PageRepositoryQuery.button.translateAndExecute")) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                queryPerformed(Action.EXECUTE_MIDPOINT, target);
            }
        };
        mainForm.add(executeMidPoint);

        AjaxSubmitButton compileMidPoint = new AjaxSubmitButton(ID_COMPILE_MIDPOINT, createStringResource("PageRepositoryQuery.button.translate")) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                queryPerformed(Action.TRANSLATE_ONLY, target);
            }
        };
        mainForm.add(compileMidPoint);

		final DropDownChoice<String> sampleChoice = new DropDownChoice<>(ID_QUERY_SAMPLE,
				Model.of(""),
				new AbstractReadOnlyModel<List<String>>() {
					@Override
					public List<String> getObject() {
						return SAMPLES;
					}
				},
				new StringResourceChoiceRenderer("PageRepositoryQuery.sample"));
		sampleChoice.setNullValid(true);
		sampleChoice.add(new OnChangeAjaxBehavior() {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				String sampleName = sampleChoice.getModelObject();
				if (StringUtils.isEmpty(sampleName)) {
					return;
				}
				String resourceName = SAMPLES_DIR + "/" + sampleName + ".xml.data";
				InputStream is = PageRepositoryQuery.class.getResourceAsStream(resourceName);
				if (is != null) {
					try {
						String localTypeName = StringUtils.substringBefore(sampleName, "_");
						model.getObject().setObjectType(new QName(SchemaConstants.NS_C, localTypeName));
						model.getObject().setMidPointQuery(IOUtils.toString(is, "UTF-8"));
						model.getObject().setHibernateQuery("");
						model.getObject().setHibernateParameters("");
						model.getObject().setQueryResultObject(null);
						model.getObject().resetQueryResultText();
						target.add(PageRepositoryQuery.this);
					} catch (IOException e) {
						LoggingUtils.logUnexpectedException(LOGGER, "Couldn't read sample from resource {}", e, resourceName);
					}
				} else {
					LOGGER.warn("Resource {} containing sample couldn't be found", resourceName);
				}
			}
		});
		mainForm.add(sampleChoice);

        AjaxSubmitButton executeHibernate = new AjaxSubmitButton(ID_EXECUTE_HIBERNATE, createStringResource("PageRepositoryQuery.button.execute")) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                queryPerformed(Action.EXECUTE_HIBERNATE, target);
            }
        };
		executeHibernate.setVisible(isAdmin);
        mainForm.add(executeHibernate);

		Label resultLabel = new Label(ID_RESULT_LABEL, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				if (model.getObject().getQueryResultText() == null) {
					return "";
				}
				Object queryResult = model.getObject().getQueryResultObject();
				if (queryResult instanceof List) {
					return getString("PageRepositoryQuery.resultObjects", ((List) queryResult).size());
				} else if (queryResult instanceof Throwable) {
					return getString("PageRepositoryQuery.resultException", queryResult.getClass().getName());
				} else {
					// including null
					return getString("PageRepositoryQuery.result");
				}
			}
		});
		mainForm.add(resultLabel);

		WebMarkupContainer incompleteResultsNote = new WebMarkupContainer(ID_INCOMPLETE_RESULTS_NOTE);
		incompleteResultsNote.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !isAdmin && model.getObject().getQueryResultText() != null;
			}
		});
		mainForm.add(incompleteResultsNote);

		AceEditor resultText = new AceEditor(ID_RESULT_TEXT, new PropertyModel<String>(model, RepoQueryDto.F_QUERY_RESULT_TEXT));
		resultText.setReadonly(true);
		resultText.setHeight(300);
		resultText.setResizeToMaxHeight(false);
		resultText.setMode(null);
		resultText.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return model.getObject().getQueryResultText() != null;
			}
		});
		mainForm.add(resultText);

	}

    private void queryPerformed(Action action, AjaxRequestTarget target) {
    	String opName = action == Action.TRANSLATE_ONLY ? OPERATION_TRANSLATE_QUERY : OPERATION_EXECUTE_QUERY;
		Task task = createSimpleTask(opName);
		OperationResult result = new OperationResult(opName);

		RepoQueryDto dto = model.getObject();
		PrismContext prismContext = getPrismContext();
        try {
			boolean queryPresent;
			RepositoryQueryDiagRequest request = new RepositoryQueryDiagRequest();

			switch (action) {
				case EXECUTE_HIBERNATE:
					String hqlText = dto.getHibernateQuery();
					queryPresent = StringUtils.isNotBlank(hqlText);
					request.setImplementationLevelQuery(hqlText);
					break;
				case TRANSLATE_ONLY:
					request.setTranslateOnly(true);
				case EXECUTE_MIDPOINT:
					String queryText = dto.getMidPointQuery();
					queryPresent = StringUtils.isNotBlank(queryText);
					if (queryPresent) {
						QName objectType = dto.getObjectType() != null ? dto.getObjectType() : ObjectType.COMPLEX_TYPE;
						@SuppressWarnings("unchecked")
						Class<? extends ObjectType> clazz = (Class<? extends ObjectType>) prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectType);
						if (clazz == null) {
							throw new SchemaException("Couldn't find compile-time class for object type of " + objectType);
						}
						QueryType queryType = prismContext.parseAtomicValue(queryText, QueryType.COMPLEX_TYPE, PrismContext.LANG_XML);
						request.setType(clazz);
						ObjectQuery objectQuery = QueryJaxbConvertor.createObjectQuery(clazz, queryType, prismContext);
						ObjectQuery queryWithExprEvaluated = ExpressionUtil.evaluateQueryExpressions(objectQuery, new ExpressionVariables(),
								getExpressionFactory(), getPrismContext(), "evaluate query expressions", task, result);
						request.setQuery(queryWithExprEvaluated);
					}
					break;
				default:
					throw new IllegalArgumentException("Invalid action: " + action);
			}

			if (!queryPresent) {
				warn(getString("PageRepositoryQuery.message.emptyString"));
				target.add(getFeedbackPanel());
				return;
			}

			RepositoryQueryDiagResponse response;
			List<?> queryResult;

			if (isAdmin) {
				response = getModelDiagnosticService().executeRepositoryQuery(request, task, result);
				queryResult = response.getQueryResult();
			} else {
				request.setTranslateOnly(true);
				request.setImplementationLevelQuery(null);	// just to be sure
				response = getModelDiagnosticService().executeRepositoryQuery(request, task, result);

				if (action != Action.TRANSLATE_ONLY) {
					// not an admin, so have to fetch objects via model
					queryResult = getModelService().searchObjects(request.getType(), request.getQuery(),
							GetOperationOptions.createRawCollection(), task, result);
				} else {
					queryResult = null;
				}
			}

			if (action != Action.EXECUTE_HIBERNATE) {
				dto.setHibernateQuery(String.valueOf(response.getImplementationLevelQuery()));
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, RepositoryQueryDiagResponse.ParameterValue> entry : response.getImplementationLevelQueryParameters().entrySet()) {
					sb.append(entry.getKey()).append(" = ").append(entry.getValue().displayValue).append("\n");
				}
				dto.setHibernateParameters(sb.toString());
			} else {
				dto.setHibernateParameters("");
			}
			if (action != Action.TRANSLATE_ONLY) {
				dto.setQueryResultText(formatQueryResult(queryResult));
				dto.setQueryResultObject(queryResult);
			} else {
				dto.resetQueryResultText();
				dto.setQueryResultObject(null);
			}
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError("Couldn't execute query", e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute query", e);
            dto.setQueryResultText(e.getMessage());
			dto.setQueryResultObject(e);
        } finally {
            result.computeStatus();
        }

        showResult(result);
        target.add(this);
    }

	private String formatQueryResult(List<?> objects) {
		StringBuilder sb = new StringBuilder();
		if (objects != null) {
			for (Object item : objects) {
				if (item instanceof Object[]) {
					boolean first = true;
					for (Object item1 : (Object[]) item) {
						if (first) {
							first = false;
						} else {
							sb.append(",");
						}
						sb.append(item1);
					}
				} else {
					sb.append(item);
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}
}
