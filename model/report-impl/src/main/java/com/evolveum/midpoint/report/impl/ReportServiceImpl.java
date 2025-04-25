/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import static com.evolveum.midpoint.schema.GetOperationOptions.createAllowNotFoundCollection;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.ExpressionProfileManager;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.*;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.google.common.base.Preconditions;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptEvaluator;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.ReportOutputCreatedListener;
import com.evolveum.midpoint.repo.common.commandline.CommandLineScriptExecutor;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.subscription.SubscriptionUtil;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Trace LOGGER = TraceManager.getTrace(ReportServiceImpl.class);

    @Autowired private ModelService model;
    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private AuditService auditService;
    @Autowired private ModelAuditService modelAuditService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private ExpressionProfileManager expressionProfileManager;

    @Autowired private Clock clock;
    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private DashboardService dashboardService;
    @Autowired private LocalizationService localizationService;
    @Autowired private CommandLineScriptExecutor commandLineScriptExecutor;
    @Autowired private BulkActionsService bulkActionsService;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ReportFunctions reportFunctions;

    @Autowired(required = false) private List<ReportOutputCreatedListener> reportOutputCreatedListeners;

    @Override
    public Collection<? extends PrismValue> evaluateScript(
            @NotNull PrismObject<ReportType> report,
            @NotNull ExpressionType expression,
            VariablesMap variables,
            String shortDesc,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        Preconditions.checkNotNull(report, "Report must not be null.");

        // TODO why do we circumvent the standard expression evaluation mechanism here (for scripts)?
        List<JAXBElement<?>> evaluators = expression.getExpressionEvaluator();
        if (evaluators.size() == 1
                && evaluators.get(0).getValue() instanceof ScriptExpressionEvaluatorType scriptExpressionBean) {
            ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext();
            context.setVariables(variables);
            context.setContextDescription(shortDesc);
            context.setTask(task);
            context.setResult(result);
            setupExpressionProfiles(context, report);

            if (scriptExpressionBean.getObjectVariableMode() == null) {
                var defaultScriptConfiguration = report.asObjectable().getDefaultScriptConfiguration();
                scriptExpressionBean.setObjectVariableMode(
                        defaultScriptConfiguration == null
                                ? ObjectVariableModeType.OBJECT
                                : defaultScriptConfiguration.getObjectVariableMode());
            }
            context.setScriptBean(scriptExpressionBean);
            context.setObjectResolver(objectResolver);

            ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(
                    scriptExpressionBean, context.getOutputDefinition(), context.getExpressionProfile(),
                    context.getContextDescription(), context.getResult());

            scriptExpression.setFunctionLibraryBindings(createFunctionLibraries(scriptExpression.getFunctionLibraryBindings()));

            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                    new ExpressionEnvironment(context.getTask(), context.getResult()));
            try {
                return scriptExpression.evaluate(context);
            } finally {
                ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            }
        } else {
            return ExpressionUtil.evaluateExpressionNative(null, variables, null, expression,
                    determineExpressionProfile(report, result), expressionFactory, shortDesc, task, result);
        }
    }

    private Collection<FunctionLibraryBinding> createFunctionLibraries(Collection<FunctionLibraryBinding> originalFunctions) {
        FunctionLibraryBinding reportLib = new FunctionLibraryBinding(
                MidPointConstants.FUNCTION_LIBRARY_REPORT_VARIABLE_NAME,
                reportFunctions);

        Collection<FunctionLibraryBinding> functions = new ArrayList<>(originalFunctions);
        functions.add(reportLib);
        return functions;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    private ExpressionProfile determineExpressionProfile(
            @NotNull PrismObject<ReportType> report, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        return expressionProfileManager.determineExpressionProfile(report, result);
    }

    private void setupExpressionProfiles(ScriptExpressionEvaluationContext context, PrismObject<ReportType> report)
            throws SchemaException, ConfigurationException {
        ExpressionProfile expressionProfile = determineExpressionProfile(report, context.getResult());
        LOGGER.trace("Using expression profile {} for report evaluation, determined from: '{}'",
                expressionProfile == null ? null : expressionProfile.getIdentifier(), report);
        context.setExpressionProfile(expressionProfile);
        context.setScriptExpressionProfile(
                findScriptExpressionProfile(expressionProfile, report));
    }

    private @Nullable ScriptLanguageExpressionProfile findScriptExpressionProfile(
            ExpressionProfile expressionProfile, PrismObject<ReportType> report) {
        if (expressionProfile == null) {
            return null;
        }
        ExpressionEvaluatorsProfile evaluatorsProfile = expressionProfile.getEvaluatorsProfile();
        ExpressionEvaluatorProfile scriptEvaluatorProfile =
                evaluatorsProfile.getEvaluatorProfile(ScriptExpressionEvaluatorFactory.ELEMENT_NAME);
        if (scriptEvaluatorProfile == null) {
            return null;
        }
        return scriptEvaluatorProfile.getScriptExpressionProfile(getScriptLanguageName(report));
    }

    private String getScriptLanguageName(PrismObject<ReportType> report) {
        // Hardcoded for now
        return GroovyScriptEvaluator.LANGUAGE_NAME;
    }

    @Override
    public PrismObject<ReportType> getReportDefinition(String reportOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return model.getObject(ReportType.class, reportOid, null, task, result);
    }

    @Override
    public boolean isAuthorizedToRunReport(PrismObject<ReportType> report, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        AuthorizationParameters<ReportType, ObjectType> params = AuthorizationParameters.Builder.buildObject(report);
        return securityEnforcer.isAuthorized(
                ModelAuthorizationAction.RUN_REPORT.getUrl(), null, params, SecurityEnforcer.Options.create(), task, result);
    }

    @Override
    public boolean isAuthorizedToImportReport(PrismObject<ReportType> report, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        AuthorizationParameters<ReportType, ObjectType> params = AuthorizationParameters.Builder.buildObject(report);
        return securityEnforcer.isAuthorized(
                ModelAuthorizationAction.IMPORT_REPORT.getUrl(), null, params, SecurityEnforcer.Options.create(), task, result);
    }

    public CompiledObjectCollectionView createCompiledView(DashboardReportEngineConfigurationType dashboardConfig,
            DashboardWidgetType widget, Task task, OperationResult result) throws CommonException {
        MiscUtil.stateCheck(dashboardConfig != null, "Dashboard engine in report couldn't be null.");

        DashboardWidgetPresentationType presentation = widget.getPresentation();
        MiscUtil.stateCheck(!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation),
                "DataField of presentation couldn't be null.");
        DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(widget);
        MiscUtil.stateCheck(sourceType != null, "No source type specified in " + widget);

        CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
        if (widget.getPresentation() != null && widget.getPresentation().getView() != null) {
            getModelInteractionService().applyView(compiledCollection, widget.getPresentation().getView());
        }
        CollectionRefSpecificationType collectionRefSpecification =
                getDashboardService().getCollectionRefSpecificationType(widget, task, result);
        if (collectionRefSpecification != null) {
            @NotNull CompiledObjectCollectionView compiledCollectionRefSpec =
                    getModelInteractionService().compileObjectCollectionView(
                            collectionRefSpecification, compiledCollection.getTargetClass(), task, result);
            getModelInteractionService().applyView(compiledCollectionRefSpec, compiledCollection.toGuiObjectListViewType());
            compiledCollection = compiledCollectionRefSpec;
        }

        GuiObjectListViewType reportView = getReportViewByType(
                dashboardConfig, ObjectUtils.defaultIfNull(compiledCollection.getContainerType(), ObjectType.COMPLEX_TYPE));
        if (reportView != null) {
            getModelInteractionService().applyView(compiledCollection, reportView);
        }

        if (compiledCollection.getColumns().isEmpty()) {
            Class<?> type = resolveTypeForReport(compiledCollection);
            getModelInteractionService().applyView(
                    compiledCollection, DefaultColumnUtils.getDefaultView(ObjectUtils.defaultIfNull(type, ObjectType.class)));
        }
        return compiledCollection;
    }

    private GuiObjectListViewType getReportViewByType(DashboardReportEngineConfigurationType dashboardConfig, QName type) {
        for (GuiObjectListViewType view : dashboardConfig.getView()) {
            if (QNameUtil.match(view.getType(), type)) {
                return view;
            }
        }
        return null;
    }

    public CompiledObjectCollectionView createCompiledView(
            ObjectCollectionReportEngineConfigurationType collectionConfig,
            boolean useDefaultView, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        Validate.notNull(collectionConfig, "Collection engine in report couldn't be null.");

        CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
        GuiObjectListViewType reportView = collectionConfig.getView();
        if (reportView != null) {
            getModelInteractionService().applyView(compiledCollection, reportView);
        }

        CollectionRefSpecificationType collectionRefSpecification = collectionConfig.getCollection();
        if (collectionRefSpecification != null) {
            @NotNull CompiledObjectCollectionView compiledCollectionRefSpec =
                    getModelInteractionService().compileObjectCollectionView(
                            collectionRefSpecification, compiledCollection.getTargetClass(), task, result);

            if (Boolean.TRUE.equals(collectionConfig.isUseOnlyReportView())) {
                compiledCollectionRefSpec.getColumns().clear();
            }
            getModelInteractionService().applyView(compiledCollectionRefSpec, compiledCollection.toGuiObjectListViewType());
            compiledCollection = compiledCollectionRefSpec;
        }

        if (compiledCollection.getColumns().isEmpty()) {
            if (useDefaultView) {
                Class<?> type = resolveTypeForReport(compiledCollection);
                getModelInteractionService().applyView(
                        compiledCollection, DefaultColumnUtils.getDefaultView(ObjectUtils.defaultIfNull(type, ObjectType.class)));
            } else {
                return null;
            }
        }
        return compiledCollection;
    }

    public Class<?> resolveTypeForReport(CompiledObjectCollectionView compiledCollection) {
        QName type = compiledCollection.getContainerType();
        ComplexTypeDefinition def = getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByType(type);
        if (def != null) {
            Class<?> clazz = def.getCompileTimeClass();
            if (clazz != null &&
                    (Containerable.class.isAssignableFrom(clazz)
                            || ObjectReferenceType.class.isAssignableFrom(clazz))) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("Couldn't define type for QName " + type);
    }

    public <O extends ObjectType> PrismObject<O> getObjectFromReference(Referencable ref, Task task, OperationResult result) {
        Class<O> type = getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());

        PrismObject<O> embedded = ref.asReferenceValue().getObject();
        if (embedded != null) {
            return embedded;
        }

        PrismObject<O> object = null;
        try {
            object = getModelService().getObject(type, ref.getOid(), createAllowNotFoundCollection(), task, result);
        } catch (Exception e) {
            LOGGER.debug("Couldn't get object from objectRef " + ref, e);
        }
        return object;
    }

    // FIXME this does not take "asRow" (joined) subreports into account
    VariablesMap evaluateSubreports(
            PrismObject<ReportType> reportObject, VariablesMap variables, Task task, OperationResult result) {
        VariablesMap subreportVariable = new VariablesMap();
        if (reportObject == null) {
            return subreportVariable;
        }

        ReportType report = reportObject.asObjectable();
        ObjectCollectionReportEngineConfigurationType collection = report.getObjectCollection();
        if (collection == null || collection.getSubreport().isEmpty()) {
            return subreportVariable;
        }

        Collection<SubreportParameterType> subreports = collection.getSubreport();

        List<SubreportParameterType> sortedSubreports = new ArrayList<>(subreports);
        sortedSubreports.sort(Comparator.comparingInt(s -> ObjectUtils.defaultIfNull(s.getOrder(), Integer.MAX_VALUE)));

        VariablesMap inputVariables = new VariablesMap();
        inputVariables.putAll(variables);
        for (SubreportParameterType subreport : sortedSubreports) {
            VariablesMap resultVariables = evaluateSubreport(reportObject, inputVariables, subreport, task, result);

            inputVariables.putAll(resultVariables);
            subreportVariable.putAll(resultVariables);
        }

        return subreportVariable;
    }

    /** Returns zero- or single-entry map with the value of given (evaluated) sub-report parameter. */
    public @NotNull VariablesMap evaluateSubreport(
            PrismObject<ReportType> reportObject, VariablesMap variables, SubreportParameterType subReportDef,
            Task task, OperationResult result) {
        VariablesMap resultMap = new VariablesMap();

        String name = subReportDef.getName();
        ExpressionType expression = subReportDef.getExpression();
        if (expression == null || name == null) {
            LOGGER.warn("No expression or no name for sub-report in {}: {}", reportObject, subReportDef);
            return resultMap;
        }

        try {
            Collection<? extends PrismValue> values =
                    evaluateScript(
                            reportObject, expression, variables, "subreport '" + name + '\'', task, result);
            resultMap.put(name, TypedValue.of(values, subReportDef.getType()));
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't execute expression {} in {}", e, expression, reportObject);
            resultMap.put(name, null, Object.class);
        }

        return resultMap;
    }

    public Clock getClock() {
        return clock;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public ModelService getModelService() {
        return modelService;
    }

    public ModelAuditService getModelAuditService() {
        return modelAuditService;
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public CommandLineScriptExecutor getCommandLineScriptExecutor() {
        return commandLineScriptExecutor;
    }

    public SchemaService getSchemaService() {
        return schemaService;
    }

    public BulkActionsService getBulkActionsService() {
        return bulkActionsService;
    }

    public @NotNull List<ReportOutputCreatedListener> getReportCreatedListeners() {
        return emptyIfNull(reportOutputCreatedListeners);
    }

    @Nullable
    public String missingSubscriptionFooter() {
        return SubscriptionUtil.missingSubscriptionAppeal(localizationService, Locale.getDefault());
    }
}
