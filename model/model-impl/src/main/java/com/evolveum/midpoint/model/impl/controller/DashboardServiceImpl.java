/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.model.api.util.DashboardUtils.*;

import java.util.*;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.CollectionStats;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */
@Component("dashboardService")
public class DashboardServiceImpl implements DashboardService {

    private static final Trace LOGGER = TraceManager.getTrace(DashboardServiceImpl.class);

    private static final String VAR_PROPORTIONAL = "proportional";
    private static final String VAR_POLICY_SITUATIONS = "policySituations";

    @Autowired private TaskManager taskManager;
    @Autowired private AuditService auditService;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private ModelService modelService;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private CollectionProcessor collectionProcessor;
    @Autowired private SchemaHelper schemaHelper;

    @Override
    public DashboardWidget createWidgetData(DashboardWidgetType widget, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException {

        Validate.notNull(widget, "Widget is null");

        DashboardWidget data = new DashboardWidget();
        getNumberMessage(widget, data, task, result);
        data.setWidget(widget);
        if(data.getDisplay() == null) {
            data.setDisplay(widget.getDisplay());
        }
        LOGGER.debug("Widget Data: {}", data);
        return data;
    }

    private DisplayType combineDisplay(DisplayType display, DisplayType variationDisplay) {
        DisplayType combinedDisplay = new DisplayType();
        if (variationDisplay == null) {
            return display;
        }
        if (display == null) {
            return variationDisplay;
        }
        if (StringUtils.isBlank(variationDisplay.getColor())) {
            combinedDisplay.setColor(display.getColor());
        } else {
            combinedDisplay.setColor(variationDisplay.getColor());
        }
        if (StringUtils.isBlank(variationDisplay.getCssClass())) {
            combinedDisplay.setCssClass(display.getCssClass());
        } else {
            combinedDisplay.setCssClass(variationDisplay.getCssClass());
        }
        if (StringUtils.isBlank(variationDisplay.getCssStyle())) {
            combinedDisplay.setCssStyle(display.getCssStyle());
        } else {
            combinedDisplay.setCssStyle(variationDisplay.getCssStyle());
        }
        if (variationDisplay.getHelp() == null) {
            combinedDisplay.setHelp(display.getHelp());
        } else {
            combinedDisplay.setHelp(variationDisplay.getHelp());
        }
        if (variationDisplay.getLabel() == null) {
            combinedDisplay.setLabel(display.getLabel());
        } else {
            combinedDisplay.setLabel(variationDisplay.getLabel());
        }
        if (variationDisplay.getSingularLabel() == null) {
            combinedDisplay.setSingularLabel(display.getSingularLabel());
        } else {
            combinedDisplay.setSingularLabel(variationDisplay.getSingularLabel());
        }
        if (variationDisplay.getPluralLabel() == null) {
            combinedDisplay.setPluralLabel(display.getPluralLabel());
        } else {
            combinedDisplay.setPluralLabel(variationDisplay.getPluralLabel());
        }
        if (variationDisplay.getTooltip() == null) {
            combinedDisplay.setTooltip(display.getTooltip());
        } else {
            combinedDisplay.setTooltip(variationDisplay.getTooltip());
        }
        if (variationDisplay.getIcon() == null) {
            combinedDisplay.setIcon(display.getIcon());
        } else if (display.getIcon() != null) {
            IconType icon = new IconType();
            if (StringUtils.isBlank(variationDisplay.getIcon().getCssClass())) {
                icon.setCssClass(display.getIcon().getCssClass());
            } else {
                icon.setCssClass(variationDisplay.getIcon().getCssClass());
            }
            if (StringUtils.isBlank(variationDisplay.getIcon().getColor())) {
                icon.setColor(display.getIcon().getColor());
            } else {
                icon.setColor(variationDisplay.getIcon().getColor());
            }
            if (StringUtils.isBlank(variationDisplay.getIcon().getImageUrl())) {
                icon.setImageUrl(display.getIcon().getImageUrl());
            } else {
                icon.setImageUrl(variationDisplay.getIcon().getImageUrl());
            }
            combinedDisplay.setIcon(icon);
        }

        return combinedDisplay;
    }

    public DashboardWidgetSourceTypeType getSourceType(DashboardWidgetType widget) {
        if(isSourceTypeOfDataNull(widget)) {
            return null;
        }
        return widget.getData().getSourceType();
    }

    private String getNumberMessage(DashboardWidgetType widget, DashboardWidget data, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException {
        DashboardWidgetSourceTypeType sourceType = getSourceType(widget);
        DashboardWidgetPresentationType presentation = widget.getPresentation();
        switch (sourceType) {
        case OBJECT_COLLECTION:
            if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                return generateNumberMessageForCollection(widget, data, task, result);
            }
            break;
        case AUDIT_SEARCH:
            if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                return generateNumberMessageForAuditSearch(widget, data, task, result);
            }
            break;
        case OBJECT:
            if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                return generateNumberMessageForObject(widget, data, task, result);
            }
            break;
        }
        return null;
    }

    private String generateNumberMessageForObject(DashboardWidgetType widget, DashboardWidget data, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectType object = getObjectFromObjectRef(widget, task, result);
        if(object == null) {
            return null;
        }
        return generateNumberMessage(widget, createVariables(object.asPrismObject(), null, null), data);
    }

    private String generateNumberMessageForAuditSearch(DashboardWidgetType widget, DashboardWidget data, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectCollectionType collection = getObjectCollectionType(widget, task, result);
        CollectionRefSpecificationType collectionRef = getCollectionRefSpecificationType(widget, task, result);
        if(collection == null && collectionRef.getFilter() == null) {
            return null;
        }
        AuditSearchType auditSearch = collection != null ? collection.getAuditSearch() : null;
        SearchFilterType filter = collectionRef.getFilter() != null ? collectionRef.getFilter() : null;
        filter = collection != null ? collection.getFilter() : filter;
        Integer value = 0;
        Integer domainValue = null;
        if (filter != null) {
            value = countAuditEvents(collectionRef, collection, task, result);
            if (value == null) {
                return null;
            }
            if (collection != null && collection.getDomain() != null && collection.getDomain().getCollectionRef() != null
                    && collection.getDomain().getCollectionRef().getOid() != null) {
                @NotNull PrismObject<ObjectCollectionType> domainCollection = modelService.getObject(ObjectCollectionType.class, collection.getDomain().getCollectionRef().getOid(),
                        null, task, result);
                domainValue = countAuditEvents(collection.getDomain(), domainCollection.asObjectable(), task, result);
            }
        } else if (auditSearch != null) {
            if (auditSearch.getRecordQuery() == null) {
                LOGGER.error("RecordQuery of auditSearch is not defined in widget " +
                        widget.getIdentifier());
                return null;
            }

            Map<String, Object> parameters = new HashMap<>();
            String query = getQueryForCount(createQuery(collection,
                    parameters, false, clock));
            LOGGER.debug("Parameters for select: " + parameters);
            value = (int) auditService.countObjects(
                    query, parameters);
            domainValue = null;
            if (auditSearch.getDomainQuery() == null) {
                LOGGER.error("DomainQuery of auditSearch is not defined");
            } else {
                parameters = new HashMap<>();
                query = getQueryForCount(createQuery(collection,
                        parameters, true, clock));
                LOGGER.debug("Parameters for select: " + parameters);
                domainValue = (int) auditService.countObjects(
                        query, parameters);
            }
        } else {
            LOGGER.error("Filter or auditSearch of ObjectCollection is not found in widget " +
                    widget.getIdentifier());
            return null;
        }
        LOGGER.debug("Value: {}, Domain value: {}", value, domainValue);
        IntegerStatType statType = generateIntegerStat(value, domainValue);
        return generateNumberMessage(widget, createVariables(null, statType, null), data);
    }

    public Integer countAuditEvents(CollectionRefSpecificationType collectionRef, ObjectCollectionType collection, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        if (collectionRef == null ||
                ((collectionRef.getCollectionRef() == null || collectionRef.getCollectionRef().getOid() == null)
                        && collectionRef.getFilter() == null)) {
            return null;
        }

        if (!DashboardUtils.isAuditCollection(collectionRef, modelService, task, result)) {
            LOGGER.error("Unsupported type for audit object collection");
            return null;
        }

        SearchFilterType filter;
        if (collection != null) {
            filter = collection.getFilter();
        } else {
            filter = collectionRef.getFilter();
        }
        ObjectFilter objectFilter = combineAuditFilter(collectionRef, filter, task, result);
        ObjectQuery query;
        if (objectFilter == null) {
            query = prismContext.queryFor(AuditEventRecordType.class).build();
        } else {
            query = prismContext.queryFactory().createQuery();
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(objectFilter, new ExpressionVariables(), MiscSchemaUtil.getExpressionProfile(),
                    expressionFactory, prismContext, "collection filter", task, result);
            query.setFilter(evaluatedFilter);
        }
        @NotNull Collection<SelectorOptions<GetOperationOptions>> option = combineAuditOption(collectionRef, collection, task, result);

        return auditService.countObjects(query, option, result);
    }

    private @NotNull Collection<SelectorOptions<GetOperationOptions>> combineAuditOption(CollectionRefSpecificationType collectionRef, ObjectCollectionType collection, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        List<SelectorOptions<GetOperationOptions>> collectionOptions = null;
        if (collection != null) {
            collectionOptions = MiscSchemaUtil.optionsTypeToOptions(collection.getGetOptions(), prismContext);
        } else if (collectionRef.getCollectionRef() != null) {
            @NotNull PrismObject<ObjectCollectionType> collectionFromRef = modelService.getObject(ObjectCollectionType.class, collectionRef.getCollectionRef().getOid(), null, task, result);
            collectionOptions = MiscSchemaUtil.optionsTypeToOptions(collectionFromRef.asObjectable().getGetOptions(), prismContext);
        }
        GetOperationOptionsBuilder optionsBuilder = schemaHelper.getOperationOptionsBuilder().setFrom(collectionOptions);
        if (collectionRef.getBaseCollectionRef() != null && collectionRef.getBaseCollectionRef().getCollectionRef() != null
                && collectionRef.getBaseCollectionRef().getCollectionRef().getOid() != null) {
            @NotNull PrismObject<ObjectCollectionType> baseCollection = modelService.getObject(ObjectCollectionType.class, collectionRef.getCollectionRef().getOid(), null, task, result);
            List<SelectorOptions<GetOperationOptions>> baseCollectionOptions = MiscSchemaUtil.optionsTypeToOptions(baseCollection.asObjectable().getGetOptions(), prismContext);
            optionsBuilder.mergeFrom(baseCollectionOptions);
        }
        return optionsBuilder.build();

    }

    private ObjectFilter combineAuditFilter(CollectionRefSpecificationType collectionRef, SearchFilterType baseFilter, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        SearchFilterType filter = baseFilter;
        if (filter == null) {
            if (collectionRef.getCollectionRef() != null) {
                @NotNull PrismObject<ObjectCollectionType> collection = modelService.getObject(ObjectCollectionType.class, collectionRef.getCollectionRef().getOid(), null, task, result);
                filter = collection.asObjectable().getFilter();
            } else {
                filter = collectionRef.getFilter();
            }
        }
        if (collectionRef.getBaseCollectionRef() != null && collectionRef.getBaseCollectionRef().getCollectionRef() != null
                && collectionRef.getBaseCollectionRef().getCollectionRef().getOid() != null) {
            @NotNull PrismObject<ObjectCollectionType> baseCollection = modelService.getObject(ObjectCollectionType.class, collectionRef.getCollectionRef().getOid(), null, task, result);
            if (filter == null && baseCollection.asObjectable().getFilter() == null) {
                return null;
            } else if (filter == null) {
                return prismContext.getQueryConverter().parseFilter(baseCollection.asObjectable().getFilter(), AuditEventRecordType.class);
            } else if (baseCollection.asObjectable().getFilter() == null) {
                return prismContext.getQueryConverter().parseFilter(filter, AuditEventRecordType.class);
            } else {
                ObjectFilter baseFilterFromCollection = prismContext.getQueryConverter().parseFilter(baseCollection.asObjectable().getFilter(), AuditEventRecordType.class);
                ObjectFilter baseObjectFilter = prismContext.getQueryConverter().parseFilter(filter, AuditEventRecordType.class);
                ObjectQueryUtil.filterAnd(baseFilterFromCollection, baseObjectFilter, prismContext);
                return prismContext.getQueryConverter().parseFilter(filter, AuditEventRecordType.class);
            }
        }
        if (filter == null) {
            return null;
        }
        return prismContext.getQueryConverter().parseFilter(filter, AuditEventRecordType.class);
    }

    private String getQueryForCount(String query) {
        int index = query.toLowerCase().indexOf("from");
        query = "select count(*) " + query.substring(index);
        query = query.split("order")[0];
        LOGGER.debug("Query for select: " + query);
        return query;
    }

    private String generateNumberMessageForCollection(DashboardWidgetType widget, DashboardWidget data, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException {
        CollectionRefSpecificationType collectionSpec = getCollectionRefSpecificationType(widget, task, result);
        if(collectionSpec != null) {

            CompiledObjectCollectionView compiledCollection = modelInteractionService.compileObjectCollectionView(
                    collectionSpec, null, task, task.getResult());
            CollectionStats collStats = modelInteractionService.determineCollectionStats(compiledCollection, task, result);

            Integer value = collStats.getObjectCount();//getObjectCount(valueCollection, true, task, result);
            Integer domainValue = collStats.getDomainCount();
            IntegerStatType statType = generateIntegerStat(value, domainValue);

            Collection<EvaluatedPolicyRule> evalPolicyRules = new ArrayList<>();
            if (collectionSpec.getCollectionRef() != null
                    && QNameUtil.match(ObjectCollectionType.COMPLEX_TYPE, collectionSpec.getCollectionRef().getType())) {
                ObjectCollectionType valueCollection = getObjectCollectionType(widget, task, result);
                evalPolicyRules = modelInteractionService.evaluateCollectionPolicyRules(
                        valueCollection.asPrismObject(), compiledCollection, null, task, task.getResult());
            }
            Collection<String> policySituations = new ArrayList<>();
            for(EvaluatedPolicyRule evalPolicyRule : evalPolicyRules) {
                if(!evalPolicyRule.getAllTriggers().isEmpty()) {
                    policySituations.add(evalPolicyRule.getPolicySituation());
                }
            }
            return generateNumberMessage(widget, createVariables(null, statType, policySituations), data);

        }  else {
            LOGGER.error("CollectionRefSpecificationType is null in widget " + widget.getIdentifier());
        }
        return null;
    }

    private static ExpressionVariables createVariables(PrismObject<? extends ObjectType> object,
            IntegerStatType statType, Collection<String> policySituations) {
        ExpressionVariables variables = new ExpressionVariables();
        if (statType != null || policySituations != null) {
            VariablesMap variablesMap = new VariablesMap();
            if (statType != null) {
                variablesMap.put(ExpressionConstants.VAR_INPUT, statType, statType.getClass());
                variablesMap.put(VAR_PROPORTIONAL, statType, statType.getClass());
                variablesMap.registerAlias(VAR_PROPORTIONAL, ExpressionConstants.VAR_INPUT);
            }
            if (policySituations != null) {
                variablesMap.put(VAR_POLICY_SITUATIONS, policySituations, EvaluatedPolicyRule.class);
            }
            variables.addVariableDefinitions(variablesMap);
        }
        if (object != null) {
            variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object, object.getDefinition());
        }

        return variables;
    }

    private static IntegerStatType generateIntegerStat(Integer value, Integer domainValue){
        IntegerStatType statType = new IntegerStatType();
        statType.setValue(value);
        statType.setDomain(domainValue);
        return statType;
    }

    private String generateNumberMessage(DashboardWidgetType widget, ExpressionVariables variables, DashboardWidget data) {
        Map<DashboardWidgetDataFieldTypeType, String> numberMessagesParts = new HashMap<>();
        widget.getPresentation().getDataField().forEach(dataField -> {
            switch(dataField.getFieldType()) {

            case VALUE:
                Task task = taskManager.createTaskInstance("Search domain collection");
                try {
                String valueMessage = getStringExpressionMessage(variables,
                        dataField.getExpression(), "Get value message", task, task.getResult());
                if(valueMessage != null) {
                    numberMessagesParts.put(DashboardWidgetDataFieldTypeType.VALUE, valueMessage);
                }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                break;

            case UNIT:
                task = taskManager.createTaskInstance("Get unit");
                String unit = getStringExpressionMessage(new ExpressionVariables(), dataField.getExpression(), "Unit",
                        task, task.getResult());
                numberMessagesParts.put(DashboardWidgetDataFieldTypeType.UNIT, unit);
                break;
            }
        });
        if(!numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.VALUE)) {
            LOGGER.error("Value message is not generate from widget " + widget.getIdentifier());
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.VALUE));
        if(numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.UNIT)) {
            sb.append(" ").append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.UNIT));
        }

        try {
            evaluateVariation(widget, variables, data);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        data.setNumberMessage(sb.toString());
        return sb.toString();
    }

    private void evaluateVariation(DashboardWidgetType widget, ExpressionVariables variables, DashboardWidget data) {
        if (widget.getPresentation() != null) {
            if (widget.getPresentation().getVariation() != null) {
                for (DashboardWidgetVariationType variation : widget.getPresentation().getVariation()) {
                    Task task = taskManager.createTaskInstance("Evaluate variation");
                    try {
                        boolean usingVariation = ExpressionUtil.evaluateConditionDefaultFalse(variables, variation.getCondition(),
                                MiscSchemaUtil.getExpressionProfile(), expressionFactory, "Variation", task, task.getResult());
                        if (usingVariation) {
                            data.setDisplay(combineDisplay(widget.getDisplay(), variation.getDisplay()));
                        } else {
                            data.setDisplay(widget.getDisplay());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Couldn't evaluate condition " + variation.toString(), e);
                    }
                }
            }  else {
                LOGGER.error("Variation of presentation is not found in widget " + widget.getIdentifier());
            }
        }  else {
            LOGGER.error("Presentation is not found in widget " + widget.getIdentifier());
        }
    }

    @Override
    public List<PrismObject<ObjectType>> searchObjectFromCollection(CollectionRefSpecificationType collectionConfig, QName typeForFilter,
            Collection<SelectorOptions<GetOperationOptions>> defaultOptions, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Class<ObjectType> type = null;

        if (collectionConfig.getCollectionRef() != null && collectionConfig.getFilter() != null) {
            LOGGER.error("CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
            throw new IllegalArgumentException("CollectionRefSpecificationType contains CollectionRef and Filter, please define only one");
        }
        if (typeForFilter != null) {
            type = prismContext.getSchemaRegistry().determineClassForType(typeForFilter);
        }
        CompiledObjectCollectionView compiledCollection = modelInteractionService.compileObjectCollectionView(
                collectionConfig, type, task, task.getResult());

        ExpressionVariables variables = new ExpressionVariables();
        ObjectFilter filter = ExpressionUtil.evaluateFilterExpressions(compiledCollection.getFilter(), variables, MiscSchemaUtil.getExpressionProfile(),
                expressionFactory, prismContext, "collection filter", task, result);
        if (filter == null ) {
            LOGGER.error("Couldn't find filter");
            throw new ConfigurationException("Couldn't find filter");
        }

        filter = collectionProcessor.evaluateExpressionsInFilter(filter, result, task);
        ObjectQuery query = prismContext.queryFactory().createQuery();
        query.setFilter(filter);

        if (compiledCollection.getTargetClass() == null) {
            if (typeForFilter == null) {
                LOGGER.error("Type of objects is null");
                throw new ConfigurationException("Type of objects is null");
            }
            type = prismContext.getSchemaRegistry().determineClassForType(typeForFilter);
        } else {
            type = compiledCollection.getTargetClass();
        }

        Collection<SelectorOptions<GetOperationOptions>> options;
        if (compiledCollection.getOptions() == null) {
            options = defaultOptions;
        } else {
            options = compiledCollection.getOptions();
        }

        List<PrismObject<ObjectType>> values;
        values = modelService.searchObjects(type, query, options, task, result);
        return values;
    }

    @Override
    public List<AuditEventRecordType> searchObjectFromCollection(CollectionRefSpecificationType collectionConfig, ObjectPaging paging, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<AuditEventRecordType> auditRecords = new ArrayList<>();
        if (collectionConfig.getCollectionRef() != null) {
            ObjectReferenceType ref = collectionConfig.getCollectionRef();
            Class<ObjectType> refType = prismContext.getSchemaRegistry().determineClassForType(ref.getType());
            ObjectCollectionType collection = (ObjectCollectionType) modelService
                    .getObject(refType, ref.getOid(), null, task, result).asObjectable();
            if ((collection != null && collection.getFilter() != null) || collectionConfig.getFilter() != null) {
                SearchFilterType filter;
                if (collection != null) {
                    filter = collection.getFilter();
                } else {
                    filter = collectionConfig.getFilter();
                }
                ObjectFilter objectFilter = combineAuditFilter(collectionConfig, filter, task, result);
                ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(objectFilter, new ExpressionVariables(), MiscSchemaUtil.getExpressionProfile(),
                        expressionFactory, prismContext, "collection filter", task, result);
                ObjectQuery query = prismContext.queryFactory().createQuery();
                query.setFilter(evaluatedFilter);
                query.setPaging(paging);
                @NotNull Collection<SelectorOptions<GetOperationOptions>> option = combineAuditOption(collectionConfig, collection, task, result);
                auditRecords.addAll(auditService.searchObjects(query, option, result));
                if (auditRecords == null) {
                    auditRecords = new ArrayList<>();
                }
            } else if (collection != null && collection.getAuditSearch() != null) {
                Map<String, Object> parameters = new HashMap<>();
                String query = DashboardUtils.getQueryForListRecords(DashboardUtils.createQuery(collection, parameters, false, clock));
                List<AuditEventRecord> oldAuditRecords = auditService.listRecords(query, parameters, result);
                if (oldAuditRecords == null) {
                    oldAuditRecords = new ArrayList<>();
                }
                for (AuditEventRecord auditRecord : oldAuditRecords) {
                    auditRecords.add(auditRecord.createAuditEventRecordType(true));
                }
            }
        }
        return auditRecords;
    }

    @Override
    public ObjectCollectionType getObjectCollectionType(DashboardWidgetType widget, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (isCollectionRefOfCollectionNull(widget)) {
            return null;
        }
        ObjectReferenceType ref = widget.getData().getCollection().getCollectionRef();
        return objectResolver.resolve(ref, ObjectCollectionType.class, null, "resolving collection from "+widget, task, result);
    }

    @Override
    public CollectionRefSpecificationType getCollectionRefSpecificationType(DashboardWidgetType widget, Task task, OperationResult result) {
        if (isCollectionRefSpecOfCollectionNull(widget)) {
            return null;
        }
        return widget.getData().getCollection();
    }

    private ObjectType getObjectFromObjectRef(DashboardWidgetType widget, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if(isDataNull(widget)) {
            return null;
        }
        ObjectReferenceType ref = widget.getData().getObjectRef();
        if(ref == null) {
            LOGGER.error("ObjectRef of data is not found in widget " + widget.getIdentifier());
            return null;
        }
        ObjectType object = objectResolver.resolve(ref, ObjectType.class, null, "resolving data object reference in "+widget, task, result);
        if(object == null) {
            LOGGER.error("Object from ObjectRef " + ref + " is null in widget " + widget.getIdentifier());
        }
        return object;
    }

    private String getStringExpressionMessage(ExpressionVariables variables,
            ExpressionType expression, String shortDes, Task task, OperationResult result) {
        if (expression != null) {
            Collection<String> contentTypeList = null;
            try {
                contentTypeList = ExpressionUtil.evaluateStringExpression(variables, prismContext,
                        expression, null, expressionFactory, shortDes, task, result);
            } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                    | ConfigurationException | SecurityViolationException e) {
                LOGGER.error("Couldn't evaluate Expression " + expression.toString(), e);
            }
            if (contentTypeList == null || contentTypeList.isEmpty()) {
                LOGGER.error("Expression " + expression + " returned nothing");
                return null;
            }
            if (contentTypeList.size() > 1) {
                LOGGER.error("Expression returned more than 1 item. First item is used.");
            }
            return contentTypeList.iterator().next();
        } else {
            return null;
        }
    }

}
