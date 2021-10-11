/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.input.DataLanguagePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.RepoQueryDto;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

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

    private static final String OPERATION_CHECK_QUERY = DOT_CLASS + "checkQuery";
    private static final String OPERATION_TRANSLATE_QUERY = DOT_CLASS + "translateQuery";
    private static final String OPERATION_EXECUTE_QUERY = DOT_CLASS + "executeQuery";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_REPOSITORY_QUERY_LABEL = "repositoryQueryLabel";
    private static final String ID_QUERY_VS_FILTER_NOTE = "queryVsFilterNote";
    private static final String ID_MIDPOINT_QUERY_BUTTON_BAR = "midPointQueryButtonBar";
    private static final String ID_EXECUTE_MIDPOINT = "executeMidPoint";
    private static final String ID_COMPILE_MIDPOINT = "compileMidPoint";
    private static final String ID_USE_IN_OBJECT_LIST = "useInObjectList";
    private static final String ID_EXECUTE_HIBERNATE = "executeHibernate";
    private static final String ID_EDITOR_MIDPOINT = "editorMidPoint";
    private static final String ID_EDITOR_HIBERNATE = "editorHibernate";
    private static final String ID_HIBERNATE_PARAMETERS = "hibernateParameters";
    private static final String ID_RESULT_LABEL = "resultLabel";
    private static final String ID_RESULT_TEXT = "resultText";
    private static final String ID_QUERY_SAMPLE = "querySample";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_DISTINCT = "distinct";
    private static final String ID_HIBERNATE_PARAMETERS_NOTE = "hibernateParametersNote";
    private static final String ID_INCOMPLETE_RESULTS_NOTE = "incompleteResultsNote";
    private static final String ID_VIEW_BUTTON_PANEL = "viewButtonPanel";

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
    private static final Set<QName> USE_IN_OBJECT_LIST_AVAILABLE_FOR = new HashSet<>(Arrays.asList(
            UserType.COMPLEX_TYPE,
            RoleType.COMPLEX_TYPE,
            ServiceType.COMPLEX_TYPE,
            ResourceType.COMPLEX_TYPE
    ));

    private final NonEmptyModel<RepoQueryDto> model = new NonEmptyWrapperModel<>(new Model<>(new RepoQueryDto()));
    private final boolean isAdmin;

    private String dataLanguage;

    enum Action {TRANSLATE_ONLY, EXECUTE_MIDPOINT, EXECUTE_HIBERNATE }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (dataLanguage == null) {
            dataLanguage = determineDataLanguage();
        }
        initLayout();
    }

    public PageRepositoryQuery() {
        this(null, null);
    }

    public PageRepositoryQuery(QName objectType, String queryText) {
        model.getObject().setObjectType(objectType);
        model.getObject().setMidPointQuery(queryText);

        boolean admin;
        try {
            admin = isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null, null);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | RuntimeException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine admin authorization -- continuing as non-admin", e);
            admin = false;
        }
        isAdmin = admin;
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        Label repositoryQueryLabel = new Label(ID_REPOSITORY_QUERY_LABEL, createStringResource("PageRepositoryQuery.midPoint",
                WebComponentUtil.getMidpointCustomSystemName(PageRepositoryQuery.this, "MidPoint")));
        repositoryQueryLabel.setOutputMarkupId(true);
        mainForm.add(repositoryQueryLabel);

        DropDownChoicePanel<QName> objectTypeChoice = new DropDownChoicePanel<>(ID_OBJECT_TYPE,
            new PropertyModel<>(model, RepoQueryDto.F_OBJECT_TYPE),
                new ListModel<>(WebComponentUtil.createObjectTypeList()),
                new QNameChoiceRenderer());
        objectTypeChoice.setOutputMarkupId(true);
        objectTypeChoice.getBaseFormComponent().setNullValid(true);
        objectTypeChoice.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_MAIN_FORM).get(ID_MIDPOINT_QUERY_BUTTON_BAR));
            }
        });
        mainForm.add(objectTypeChoice);

        CheckFormGroup distinctCheck = new CheckFormGroup(ID_DISTINCT, new PropertyModel<>(model, RepoQueryDto.F_DISTINCT), createStringResource("PageRepositoryQuery.checkBox.distinct"), "col-xs-3", "col-xs-1");
        mainForm.add(distinctCheck);

        AceEditor editorMidPoint = new AceEditor(ID_EDITOR_MIDPOINT, new PropertyModel<>(model, RepoQueryDto.F_MIDPOINT_QUERY));
        editorMidPoint.setHeight(400);
        editorMidPoint.setResizeToMaxHeight(false);
        mainForm.add(editorMidPoint);

        AceEditor editorHibernate = new AceEditor(ID_EDITOR_HIBERNATE, new PropertyModel<>(model, RepoQueryDto.F_HIBERNATE_QUERY));
        editorHibernate.setHeight(300);
        editorHibernate.setResizeToMaxHeight(false);
        editorHibernate.setReadonly(!isAdmin);
        editorHibernate.setMode(null);
        mainForm.add(editorHibernate);

        AceEditor hibernateParameters = new AceEditor(ID_HIBERNATE_PARAMETERS, new PropertyModel<>(model, RepoQueryDto.F_HIBERNATE_PARAMETERS));
        hibernateParameters.setReadonly(true);
        hibernateParameters.setHeight(100);
        hibernateParameters.setResizeToMaxHeight(false);
        hibernateParameters.setMode(null);
        mainForm.add(hibernateParameters);

        Label hibernateParametersNote = new Label(ID_HIBERNATE_PARAMETERS_NOTE, createStringResource("PageRepositoryQuery.hibernateParametersNote",
                WebComponentUtil.getMidpointCustomSystemName(PageRepositoryQuery.this, "midPoint")));
        hibernateParametersNote.setVisible(isAdmin);
        mainForm.add(hibernateParametersNote);

        Label queryVsFilterNote = new Label(ID_QUERY_VS_FILTER_NOTE, createStringResource("PageRepositoryQuery.queryVsFilterNote",
                WebComponentUtil.getMidpointCustomSystemName(PageRepositoryQuery.this, "midPoint")));
        queryVsFilterNote.setOutputMarkupId(true);
        mainForm.add(queryVsFilterNote);

        WebMarkupContainer midPointQueryButtonBar = new WebMarkupContainer(ID_MIDPOINT_QUERY_BUTTON_BAR);
        midPointQueryButtonBar.setOutputMarkupId(true);
        mainForm.add(midPointQueryButtonBar);

        DataLanguagePanel<QueryType> dataLanguagePanel =
                new DataLanguagePanel<QueryType>(ID_VIEW_BUTTON_PANEL, dataLanguage, QueryType.class, PageRepositoryQuery.this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onLanguageSwitched(AjaxRequestTarget target, int updatedIndex, String updatedLanguage,
                            String objectString) {
                        model.getObject().setMidPointQuery(objectString);
                        dataLanguage = updatedLanguage;
                        target.add(mainForm);
                    }
                    @Override
                    protected String getObjectStringRepresentation() {
                        return model.getObject().getMidPointQuery();
                    }
                };
        dataLanguagePanel.setOutputMarkupId(true);
        mainForm.add(dataLanguagePanel);

        AjaxSubmitButton executeMidPoint = new AjaxSubmitButton(ID_EXECUTE_MIDPOINT, createStringResource("PageRepositoryQuery.button.translateAndExecute")) {
            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                queryPerformed(Action.EXECUTE_MIDPOINT, target);
            }
        };
        midPointQueryButtonBar.add(executeMidPoint);

        AjaxSubmitButton compileMidPoint = new AjaxSubmitButton(ID_COMPILE_MIDPOINT, createStringResource("PageRepositoryQuery.button.translate")) {
            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                queryPerformed(Action.TRANSLATE_ONLY, target);
            }
        };
        midPointQueryButtonBar.add(compileMidPoint);

        AjaxSubmitButton useInObjectList = new AjaxSubmitButton(ID_USE_IN_OBJECT_LIST, createStringResource("PageRepositoryQuery.button.useInObjectList")) {
            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                useInObjectListPerformed(target);
            }
        };
        useInObjectList.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return USE_IN_OBJECT_LIST_AVAILABLE_FOR.contains(model.getObject().getObjectType());
            }
        });
        midPointQueryButtonBar.add(useInObjectList);

        final DropDownChoicePanel<String> sampleChoice = new DropDownChoicePanel<>(ID_QUERY_SAMPLE,
                Model.of(""), Model.ofList(SAMPLES),
                new StringResourceChoiceRenderer("PageRepositoryQuery.sample"), true);
        sampleChoice.getBaseFormComponent().setNullValid(true);
        sampleChoice.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String sampleName = sampleChoice.getModel().getObject();
                if (StringUtils.isEmpty(sampleName)) {
                    return;
                }
                String resourceName = SAMPLES_DIR + "/" + sampleName + ".xml.data";
                InputStream is = PageRepositoryQuery.class.getResourceAsStream(resourceName);
                if (is != null) {
                    try {
                        String localTypeName = StringUtils.substringBefore(sampleName, "_");
                        model.getObject().setObjectType(new QName(SchemaConstants.NS_C, localTypeName));
                        String xml = IOUtils.toString(is, StandardCharsets.UTF_8);
                        String serialization;
                        if (PrismContext.LANG_XML.equals(dataLanguage)) {
                            serialization = xml;
                        } else {
                            PrismContext prismContext = getPrismContext();
                            try {
                                QueryType parsed = prismContext.parserFor(xml).xml().parseRealValue(QueryType.class);
                                serialization = prismContext.serializerFor(dataLanguage).serializeRealValue(parsed);
                            } catch (Throwable t) {
                                serialization = "Couldn't serialize sample: " + t.getMessage();
                            }
                        }
                        model.getObject().setMidPointQuery(serialization);
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
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                queryPerformed(Action.EXECUTE_HIBERNATE, target);
            }
        };
        executeHibernate.setVisible(isAdmin);
        mainForm.add(executeHibernate);

        Label resultLabel = new Label(ID_RESULT_LABEL, new IModel<String>() {
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

        AceEditor resultText = new AceEditor(ID_RESULT_TEXT, new PropertyModel<>(model, RepoQueryDto.F_QUERY_RESULT_TEXT));
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

    private void useInObjectListPerformed(AjaxRequestTarget target) {
        final RepoQueryDto dto = model.getObject();
        String queryText = dto.getMidPointQuery();
        if (StringUtils.isBlank(queryText)) {
            queryText = "<query/>";
        }
        RepositoryQueryDiagRequest request = new RepositoryQueryDiagRequest();
        Task task = createSimpleTask(OPERATION_CHECK_QUERY);
        OperationResult result = task.getResult();
        try {
            updateRequestWithMidpointQuery(request, dto.getObjectType(), queryText, dto.isDistinct(), task, result);            // just to parse the query

            ObjectFilter parsedFilter = request.getQuery().getFilter();
            String filterAsString;
            if (parsedFilter != null) {
                SearchFilterType filterType = getQueryConverter().createSearchFilterType(parsedFilter);
                filterAsString = getPrismContext().xmlSerializer().serializeRealValue(filterType, SchemaConstantsGenerated.Q_FILTER);
                // TODO remove extra xmlns from serialized value
            } else {
                filterAsString = "";
            }

            Class<? extends PageBase> listPageClass = WebComponentUtil.getObjectListPage(request.getType());
            String storageKey = listPageClass != null ? WebComponentUtil.getObjectListPageStorageKey(dto.getObjectType().getLocalPart()) : null;
            if (storageKey == null) {
                // shouldn't occur because of button visibility
                error("No page to redirect for " + dto.getObjectType());
                target.add(getFeedbackPanel());
                return;
            }
            Search search = SearchFactory.createSearch(request.getType(), this);
            search.setAdvancedQuery(filterAsString);
            search.setSearchType(SearchBoxModeType.ADVANCED);
            if (!search.isAdvancedQueryValid(getPrismContext())) {
                // shouldn't occur because the query was already parsed
                error("Query is not valid: " + search.getAdvancedError());
                target.add(getFeedbackPanel());
                return;
            }

            SessionStorage sessionStorage = ((MidPointAuthWebSession) getSession()).getSessionStorage();
            PageStorage storage = sessionStorage.getPageStorageMap().get(storageKey);
            if (storage == null) {
                storage = sessionStorage.initPageStorage(storageKey);
            }
            storage.setSearch(search);
            setResponsePage(listPageClass);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError(getString("PageRepositoryQuery.message.couldNotParseQuery", e.getMessage()), e);
            showResult(result);
            target.add(this);
        }
    }

    private void queryPerformed(Action action, AjaxRequestTarget target) {
        String opName = action == Action.TRANSLATE_ONLY ? OPERATION_TRANSLATE_QUERY : OPERATION_EXECUTE_QUERY;
        Task task = createSimpleTask(opName);
        OperationResult result = new OperationResult(opName);

        RepoQueryDto dto = model.getObject();
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
                    queryPresent = StringUtils.isNotBlank(dto.getMidPointQuery());
                    if (queryPresent) {
                        updateRequestWithMidpointQuery(request, dto.getObjectType(), dto.getMidPointQuery(), dto.isDistinct(), task, result);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid action: " + action);
            }

            if (!queryPresent) {
                warnNoQuery(target);
                return;
            }

            RepositoryQueryDiagResponse response;
            List<?> queryResult;

            if (isAdmin) {
                response = getModelDiagnosticService().executeRepositoryQuery(request, task, result);
                queryResult = response.getQueryResult();
            } else {
                request.setTranslateOnly(true);
                request.setImplementationLevelQuery(null);    // just to be sure
                response = getModelDiagnosticService().executeRepositoryQuery(request, task, result);

                if (action != Action.TRANSLATE_ONLY) {
                    // not an admin, so have to fetch objects via model
                    queryResult = getModelService().searchObjects(request.getType(), request.getQuery(),
                            createRawCollection(), task, result);
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
            result.recordFatalError(getString("PageRepositoryQuery.message.couldNotExecuteQuery"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute query", e);
            dto.setQueryResultText(e.getMessage());
            dto.setQueryResultObject(e);
        } finally {
            result.computeStatus();
        }

        showResult(result);
        target.add(this);
    }

    private void warnNoQuery(AjaxRequestTarget target) {
        warn(getString("PageRepositoryQuery.message.emptyString"));
        target.add(getFeedbackPanel());
    }

    private void updateRequestWithMidpointQuery(RepositoryQueryDiagRequest request, QName objectType, String queryText,
            boolean distinct, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismContext prismContext = getPrismContext();
        if (objectType == null) {
            objectType = ObjectType.COMPLEX_TYPE;
        }
        @SuppressWarnings("unchecked")
        Class<? extends ObjectType> clazz = (Class<? extends ObjectType>) prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectType);
        if (clazz == null) {
            throw new SchemaException("Couldn't find compile-time class for object type of " + objectType);
        }
        QueryType queryType = prismContext.parserFor(queryText).language(dataLanguage).parseRealValue(QueryType.class);
        request.setType(clazz);
        ObjectQuery objectQuery = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
        ObjectQuery queryWithExprEvaluated = ExpressionUtil.evaluateQueryExpressions(objectQuery, new ExpressionVariables(),
                MiscSchemaUtil.getExpressionProfile(), getExpressionFactory(), getPrismContext(), "evaluate query expressions", task, result);
        request.setQuery(queryWithExprEvaluated);

        Collection<SelectorOptions<GetOperationOptions>> options = distinct ? createCollection(createDistinct()) : null;
        request.setOptions(options);
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
