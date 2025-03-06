/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import static com.evolveum.midpoint.schema.GetOperationOptions.createDistinct;
import static com.evolveum.midpoint.schema.GetOperationOptions.createRawCollection;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.impl.query.lang.AxiomQueryContentAssistImpl;
import com.evolveum.midpoint.prism.query.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.expression.ScriptExpressionPanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryQueryDiagRequest;
import com.evolveum.midpoint.schema.RepositoryQueryDiagResponse;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.page.admin.configuration.dto.RepoQueryDto;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;

public class QueryPlaygroundPanel extends BasePanel<RepoQueryDto> {

    private static final Trace LOGGER = TraceManager.getTrace(QueryPlaygroundPanel.class);
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_REPOSITORY_QUERY_LABEL = "repositoryQueryLabel";
    private static final String ID_QUERY_VS_FILTER_NOTE = "queryVsFilterNote";
    private static final String ID_MIDPOINT_QUERY_BUTTON_BAR = "midPointQueryButtonBar";
    private static final String ID_EXECUTE_MIDPOINT = "executeMidPoint";
    private static final String ID_COMPILE_MIDPOINT = "compileMidPoint";
    private static final String ID_USE_IN_OBJECT_LIST = "useInObjectList";
    private static final String ID_EDITOR_MIDPOINT = "editorMidPoint";
    private static final String ID_EDITOR_MIDPOINT_SCRIPT = "editorMidPointScript";
    private static final String ID_QUERY_EDITOR = "queryEditor";
    private static final String ID_QUERY_LABEL = "queryLabel";
    private static final String ID_PARAMETERS = "parameters";
    private static final String ID_RESULT_LABEL = "resultLabel";
    private static final String ID_RESULT_TEXT = "resultText";
    private static final String ID_QUERY_SAMPLE = "querySample";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_DISTINCT = "distinct";
    private static final String ID_INCOMPLETE_RESULTS_NOTE = "incompleteResultsNote";
    private static final String ID_SCRIPT_ENABLED = "scriptEnabled";

    private static final String DOT_CLASS = QueryPlaygroundPanel.class.getName() + ".";

    private static final String OPERATION_CHECK_QUERY = DOT_CLASS + "checkQuery";
    private static final String OPERATION_TRANSLATE_QUERY = DOT_CLASS + "translateQuery";
    private static final String OPERATION_EXECUTE_QUERY = DOT_CLASS + "executeQuery";

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
            "UserType_UsersWithShadowOnGivenResource",
            "ObjectReferenceType_RoleMembershipRefsTargetingSuperuser"
    );
    private static final Set<QName> USE_IN_OBJECT_LIST_AVAILABLE_FOR = new HashSet<>(Arrays.asList(
            UserType.COMPLEX_TYPE,
            RoleType.COMPLEX_TYPE,
            ServiceType.COMPLEX_TYPE,
            ResourceType.COMPLEX_TYPE
    ));

    private boolean isAdmin;

    enum Action {TRANSLATE_ONLY, EXECUTE_MIDPOINT}

    public QueryPlaygroundPanel(String id, IModel<RepoQueryDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        boolean admin;
        try {
            admin = getPageBase()
                    .isAuthorized(AuthorizationConstants.AUTZ_ALL_URL, null, null, null, null);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | RuntimeException
                | CommunicationException | ConfigurationException | SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine admin authorization -- continuing as non-admin", e);
            admin = false;
        }
        isAdmin = admin;
        initLayout();
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);
        Label repositoryQueryLabel = new Label(ID_REPOSITORY_QUERY_LABEL, createStringResource("PageRepositoryQuery.midPoint",
                WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "MidPoint")));
        repositoryQueryLabel.setOutputMarkupId(true);
        mainForm.add(repositoryQueryLabel);

        DropDownChoicePanel<QName> objectTypeChoice = new DropDownChoicePanel<>(ID_OBJECT_TYPE,
                new PropertyModel<>(getModel(), RepoQueryDto.F_OBJECT_TYPE),
                new ListModel<>(ObjectTypeListUtil.createSearchableTypeList()),
                new QNameObjectTypeChoiceRenderer());
        objectTypeChoice.setOutputMarkupId(true);
        objectTypeChoice.getBaseFormComponent().setNullValid(true);
        objectTypeChoice.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_MAIN_FORM).get(ID_MIDPOINT_QUERY_BUTTON_BAR));
            }
        });
        mainForm.add(objectTypeChoice);

        CheckFormGroup distinctCheck = new CheckFormGroup(
                ID_DISTINCT, new PropertyModel<>(getModel(), RepoQueryDto.F_DISTINCT),
                createStringResource(
                        "PageRepositoryQuery.checkBox.distinct"), "col-xs-3", "col-xs-1");
        mainForm.add(distinctCheck);

        AceEditor editorMidPoint = new AceEditor(ID_EDITOR_MIDPOINT, new PropertyModel<>(getModel(),
                RepoQueryDto.F_MIDPOINT_QUERY));
        editorMidPoint.setHeight(400);
        editorMidPoint.setResizeToMaxHeight(false);
        mainForm.add(editorMidPoint);

        CheckFormGroup scriptCheck = new CheckFormGroup(
                ID_SCRIPT_ENABLED, new PropertyModel<>(getModel(), RepoQueryDto.F_SCRIPT_ENABLED),
                createStringResource("PageRepositoryQuery.checkBox.script"), "col-xs-3", "col-xs-1");
        scriptCheck.getCheck().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                ajaxRequestTarget.add(QueryPlaygroundPanel.this);
            }
        });
        scriptCheck.setOutputMarkupId(true);
        mainForm.add(scriptCheck);

        ScriptExpressionPanel scriptExpressionPanel = new ScriptExpressionPanel(ID_EDITOR_MIDPOINT_SCRIPT,
                new PropertyModel<>(getModel(), RepoQueryDto.F_MIDPOINT_QUERY_SCRIPT));
        scriptExpressionPanel.setOutputMarkupId(true);
        scriptExpressionPanel.setOutputMarkupPlaceholderTag(true);
        scriptExpressionPanel.add(new VisibleBehaviour(() -> getModel().getObject().isScriptEnabled()));
        mainForm.add(scriptExpressionPanel);

        Label queryLabel = new Label(ID_QUERY_LABEL, createQueryLabelModel());
        mainForm.add(queryLabel);

        AceEditor editorHibernate = new AceEditor(ID_QUERY_EDITOR, new PropertyModel<>(getModel(), RepoQueryDto.F_HIBERNATE_QUERY));
        editorHibernate.setHeight(300);
        editorHibernate.setResizeToMaxHeight(false);
        editorHibernate.setReadonly(!isAdmin);
        editorHibernate.setMode(null);
        mainForm.add(editorHibernate);

        AceEditor hibernateParameters = new AceEditor(ID_PARAMETERS, new PropertyModel<>(getModel(), RepoQueryDto.F_HIBERNATE_PARAMETERS));
        hibernateParameters.setReadonly(true);
        hibernateParameters.setHeight(100);
        hibernateParameters.setResizeToMaxHeight(false);
        hibernateParameters.setMode(null);
        mainForm.add(hibernateParameters);

        Label queryVsFilterNote = new Label(ID_QUERY_VS_FILTER_NOTE, createStringResource("PageRepositoryQuery.queryVsFilterNote",
                WebComponentUtil.getMidpointCustomSystemName(getPageBase(), "midPoint")));
        queryVsFilterNote.setOutputMarkupId(true);
        mainForm.add(queryVsFilterNote);

        WebMarkupContainer midPointQueryButtonBar = new WebMarkupContainer(ID_MIDPOINT_QUERY_BUTTON_BAR);
        midPointQueryButtonBar.setOutputMarkupId(true);
        mainForm.add(midPointQueryButtonBar);

        AjaxSubmitButton executeMidPoint = new AjaxSubmitButton(ID_EXECUTE_MIDPOINT,
                createStringResource("PageRepositoryQuery.button.translateAndExecute")) {
            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                queryPerformed(QueryPlaygroundPanel.Action.EXECUTE_MIDPOINT, target);
            }
        };
        midPointQueryButtonBar.add(executeMidPoint);

        AjaxSubmitButton compileMidPoint = new AjaxSubmitButton(ID_COMPILE_MIDPOINT, createCompileMidpointLabelModel()) {
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

        AjaxSubmitButton useInObjectList = new AjaxSubmitButton(ID_USE_IN_OBJECT_LIST,
                createStringResource("PageRepositoryQuery.button.useInObjectList")) {
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
                return USE_IN_OBJECT_LIST_AVAILABLE_FOR.contains(getModel().getObject().getObjectType());
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
                        getModel().getObject().setObjectType(new QName(SchemaConstants.NS_C, localTypeName));
                        String xml = IOUtils.toString(is, StandardCharsets.UTF_8);
                        String serialization = "";
                        try {
                            QueryType parsed = getPrismContext().parserFor(xml).xml().parseRealValue(QueryType.class);
                            SearchFilterType filter = parsed.getFilter();
                            if (filter != null && filter.getText() != null) {
                                serialization = filter.getText();
                            }

                        } catch (Throwable t) {
                            serialization = "Couldn't serialize sample: " + t.getMessage();
                        }
                        getModel().getObject().setMidPointQuery(serialization);
                        getModel().getObject().setHibernateQuery("");
                        getModel().getObject().setHibernateParameters("");
                        getModel().getObject().setQueryResultObject(null);
                        getModel().getObject().resetQueryResultText();
                        target.add(QueryPlaygroundPanel.this);
                    } catch (IOException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't read sample from resource {}", e, resourceName);
                    }
                } else {
                    LOGGER.warn("Resource {} containing sample couldn't be found", resourceName);
                }
            }
        });
        mainForm.add(sampleChoice);

        Label resultLabel = new Label(ID_RESULT_LABEL, (IModel<String>) () -> {
            if (getModel().getObject().getQueryResultText() == null) {
                return "";
            }
            Object queryResult = getModel().getObject().getQueryResultObject();
            if (queryResult instanceof List) {
                return getString("PageRepositoryQuery.resultObjects", ((List<?>) queryResult).size());
            } else if (queryResult instanceof Throwable) {
                return getString("PageRepositoryQuery.resultException", queryResult.getClass().getName());
            } else {
                // including null
                return getString("PageRepositoryQuery.result");
            }
        });
        mainForm.add(resultLabel);

        WebMarkupContainer incompleteResultsNote = new WebMarkupContainer(ID_INCOMPLETE_RESULTS_NOTE);
        incompleteResultsNote.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !isAdmin && getModel().getObject().getQueryResultText() != null;
            }
        });
        mainForm.add(incompleteResultsNote);

        AceEditor resultText = new AceEditor(ID_RESULT_TEXT, new PropertyModel<>(getModel(), RepoQueryDto.F_QUERY_RESULT_TEXT));
        resultText.setReadonly(true);
        resultText.setHeight(300);
        resultText.setResizeToMaxHeight(false);
        resultText.setMode(null);
        resultText.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModel().getObject().getQueryResultText() != null;
            }
        });

        ObjectMapper mapper = new ObjectMapper();

        editorMidPoint.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(
                        new ThrottlingSettings(ID_EDITOR_MIDPOINT, Duration.ofMillis(300), true)
                );

                attributes.getDynamicExtraParameters().add(
                        "return {'cursorPosition': window.MidPointAceEditor.cursorPosition || 0};"
                );
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                RepoQueryDto repo = getModelObject();

                if (repo != null) {
                    IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();
                    ItemDefinition<?> rootDef = repo.getObjectType() == null ?
                            getPrismContext().getSchemaRegistry().findItemDefinitionByType(objectTypeChoice.getFirstChoice()) :
                            getPrismContext().getSchemaRegistry().findItemDefinitionByType(repo.getObjectType());

                    var contentAssist = new AxiomQueryContentAssistImpl(getPrismContext()).process(
                            rootDef,
                            repo.getMidPointQuery() == null ? "" : repo.getMidPointQuery(),
                            params.getParameterValue("cursorPosition").toInt() + 1
                    );

                    var suggestions = contentAssist.autocomplete();

                    try {
                        // Content assist for AXQ lang
                        target.appendJavaScript("window.MidPointAceEditor.syncContentAssist(" +
                            mapper.writeValueAsString(suggestions.isEmpty()
                                    ? List.of(new Suggestion("", createStringResource("QueryLanguage.contentAssist.codeCompletions.noSuggestion").getString(), 0)) // If list is empty, add noSuggestion item
                                    : suggestions.stream()
                                    .map(suggestion -> !suggestion.alias().isEmpty()
                                            ? new Suggestion(suggestion.name(), createStringResource(suggestion.alias()).getString(), suggestion.priority()) // translate alias
                                            : suggestion)
//                                    .sorted(Comparator.comparingInt(Suggestion::priority).reversed()) // sorted suggestions by priority from max to min weigh
                                    .toList()
                            ) + ", " + mapper.writeValueAsString(contentAssist.validate()) + ", '" + editorMidPoint.getMarkupId() + "');"
                        );
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        });

        mainForm.add(resultText);
    }

    private IModel<String> createCompileMidpointLabelModel() {
        return getPageBase().isNativeRepo()
                ? createStringResource("PageRepositoryQuery.button.translate.SQL")
                : createStringResource("PageRepositoryQuery.button.translate");
    }

    private IModel<String> createQueryLabelModel() {
        return getPageBase().isNativeRepo()
                ? createStringResource("PageRepositoryQuery.sqlQuery")
                : createStringResource("PageRepositoryQuery.hibernateQuery");
    }

    private void useInObjectListPerformed(AjaxRequestTarget target) {
        final RepoQueryDto dto = getModelObject();
        String queryText = dto.getMidPointQuery();
        if (StringUtils.isEmpty(queryText)) {
            queryText = "";
        }
        RepositoryQueryDiagRequest request = new RepositoryQueryDiagRequest();
        Task task = getPageBase().createSimpleTask(OPERATION_CHECK_QUERY);
        OperationResult result = task.getResult();
        try {

            ExpressionType scriptQuery = null;
            if (dto.isScriptEnabled()) {
                scriptQuery = dto.getMidPointQueryScript();
            }

            updateRequestWithMidpointQuery(request, dto.getObjectType(), dto.getMidPointQuery(),
                    dto.isDistinct(), scriptQuery, task, result);
            //noinspection unchecked
            Class<? extends PageBase> listPageClass = DetailsPageUtil.getObjectListPage((Class<? extends ObjectType>) request.getType());
            String storageKey = listPageClass != null
                    ? WebComponentUtil.getObjectListPageStorageKey(dto.getObjectType().getLocalPart())
                    : null;
            if (storageKey == null) {
                // shouldn't occur because of button visibility
                error("No page to redirect for " + dto.getObjectType());
                target.add(getFeedbackPanel());
                return;
            }

            SessionStorage sessionStorage = getSession().getSessionStorage();
            PageStorage storage = sessionStorage.getPageStorageMap().get(storageKey);
            if (storage == null) {
                storage = sessionStorage.initPageStorage(storageKey);
            }
            Search<?> search = storage.getSearch() != null ? storage.getSearch()
                    : new SearchBuilder<>(request.getType()).modelServiceLocator(getPageBase()).build();
            search.addAllowedModelType(SearchBoxModeType.AXIOM_QUERY);
            search.setSearchMode(SearchBoxModeType.AXIOM_QUERY);
            // Use query from model object, call of updateRequestWithMidpointQuery may updated it with new Query Language text.
            search.setDslQuery(getModelObject().getMidPointQuery());

            if (!search.isAdvancedQueryValid(getPageBase())) {
                // shouldn't occur because the query was already parsed
                error("Query is not valid: " + search.getAdvancedError());
                target.add(getFeedbackPanel());
                return;
            }
            storage.setSearch(search);
            setResponsePage(listPageClass);
        } catch (Exception e) {
            result.recordFatalError(getString("PageRepositoryQuery.message.couldNotParseQuery", e.getMessage()), e);
            showResult(result);
            target.add(getFeedbackPanel());
            target.add(this);
        }
    }

    private void queryPerformed(QueryPlaygroundPanel.Action action, AjaxRequestTarget target) {
        String opName = action == QueryPlaygroundPanel.Action.TRANSLATE_ONLY ? OPERATION_TRANSLATE_QUERY : OPERATION_EXECUTE_QUERY;
        Task task = getPageBase().createSimpleTask(opName);
        OperationResult result = new OperationResult(opName);

        RepoQueryDto dto = getModelObject();
        try {
            RepositoryQueryDiagRequest request = new RepositoryQueryDiagRequest();

            switch (action) {
                case TRANSLATE_ONLY:
                    request.setTranslateOnly(true);
                    // Falls through to the next section, we want this.
                case EXECUTE_MIDPOINT:
                    ExpressionType scriptQuery = null;
                    if (dto.isScriptEnabled()) {
                        scriptQuery = dto.getMidPointQueryScript();
                    }
                    updateRequestWithMidpointQuery(request, dto.getObjectType(), dto.getMidPointQuery(),
                            dto.isDistinct(), scriptQuery, task, result);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid action: " + action);
            }

            RepositoryQueryDiagResponse response;
            List<?> queryResult;

            if (ObjectReferenceType.class.isAssignableFrom(request.getType()) && request.getQuery() == null) {
                warnNoQuery(target);
                return;
            }

            if (isAdmin) {
                response = getPageBase().getModelDiagnosticService().executeRepositoryQuery(request, task, result);
                queryResult = response.getQueryResult();
            } else {
                request.setTranslateOnly(true);
                response = getPageBase().getModelDiagnosticService().executeRepositoryQuery(request, task, result);

                if (action != Action.TRANSLATE_ONLY) {
                    // not an admin, so have to fetch objects via model
                    queryResult = performModelSearch(request, task, result);
                    //noinspection unchecked

                } else {
                    queryResult = null;
                }
            }

            dto.setHibernateQuery(String.valueOf(response.getImplementationLevelQuery()));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, RepositoryQueryDiagResponse.ParameterValue> entry
                    : response.getImplementationLevelQueryParameters().entrySet()) {
                sb.append(entry.getKey()).append(" = ").append(entry.getValue().displayValue).append("\n");
            }
            dto.setHibernateParameters(sb.toString());

            if (action != Action.TRANSLATE_ONLY) {
                dto.setQueryResultText(formatQueryResult(queryResult));
                dto.setQueryResultObject(queryResult);
            } else {
                dto.resetQueryResultText();
                dto.setQueryResultObject(null);
            }
        } catch (Exception e) {
            result.recordFatalError(getString("PageRepositoryQuery.message.couldNotExecuteQuery"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute query", e);
            dto.setQueryResultText(e.getMessage());
            dto.setQueryResultObject(e);
        } finally {
            result.computeStatus();
        }

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(this);
    }

    private List<?> performModelSearch(RepositoryQueryDiagRequest request, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException {
        if (ObjectType.class.isAssignableFrom(request.getType())) {
            return getPageBase().getModelService().searchObjects(
                    (Class<? extends ObjectType>) request.getType(), request.getQuery(), createRawCollection(), task, result);
        }
        if (Containerable.class.isAssignableFrom(request.getType())) {
            return getPageBase().getModelService().searchContainers(
                    (Class<? extends Containerable>) request.getType(), request.getQuery(), createRawCollection(), task, result);
        }
        if (ObjectReferenceType.class.isAssignableFrom(request.getType())) {
            return getPageBase().getModelService().searchReferences(request.getQuery(), createRawCollection(), task, result);
        }
        throw new SchemaException("Unknown type " + request.getType() + "for search.");
    }

    private void warnNoQuery(AjaxRequestTarget target) {
        warn(getString("PageRepositoryQuery.message.emptyString"));
        target.add(getFeedbackPanel());
    }

    private void updateRequestWithMidpointQuery(
            RepositoryQueryDiagRequest request,
            QName objectType, String queryText,
            boolean distinct,
            ExpressionType midPointQueryScript,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException, PrismQuerySerialization.NotSupportedException {
        PrismContext prismContext = getPrismContext();
        if (objectType == null) {
            objectType = ObjectType.COMPLEX_TYPE;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Containerable> clazz =
                prismContext.getSchemaRegistry().determineClassForTypeRequired(objectType);
        ObjectQuery queryWithExprEvaluated = null;
        if (midPointQueryScript != null) {
            PrismPropertyValue<?> filterValue = ExpressionUtil.evaluateExpression(
                    new VariablesMap(), null, midPointQueryScript, MiscSchemaUtil.getExpressionProfile(),
                    getPageBase().getExpressionFactory(), "", task, task.getResult());
            if (filterValue != null) {
                var realValue = filterValue.getRealValue();
                if (realValue instanceof ObjectQuery objQuery) {
                    queryWithExprEvaluated = objQuery;
                } else if (realValue instanceof TypedQuery<?> typed) {
                    queryWithExprEvaluated = typed.toObjectQuery();
                }
                if (queryWithExprEvaluated != null) {
                    queryText = prismContext.querySerializer().serialize(queryWithExprEvaluated.getFilter()).filterText();
                    getModelObject().setMidPointQuery(queryText);
                }
            }
        }
        if (queryWithExprEvaluated == null && StringUtils.isNotBlank(queryText)) {
            ObjectFilter filter = prismContext.createQueryParser().parseFilter(clazz, queryText);
            ObjectQuery objectQuery = prismContext.queryFactory().createQuery(filter);
            queryWithExprEvaluated = ExpressionUtil.evaluateQueryExpressions(
                    objectQuery, new VariablesMap(),
                    MiscSchemaUtil.getExpressionProfile(),
                    getPageBase().getExpressionFactory(),
                    "evaluate query expressions", task, result);
        }

        request.setType(clazz);
        if (ShadowType.class.isAssignableFrom(clazz)) {
            // We need to normalize / preprocess shadow type queries (this fixed difference between
            // string vs polystring)
            queryWithExprEvaluated = getPageBase().getResourceSchemaRegistry().tryToNormalizeQuery(queryWithExprEvaluated);
        }


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
