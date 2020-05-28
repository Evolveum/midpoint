/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.commandline.CommandLineScriptExecutor;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;

import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlTemplateLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.ExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.fill.JRAbstractLRUVirtualizer;
import net.sf.jasperreports.engine.fill.JRFileVirtualizer;
import net.sf.jasperreports.engine.fill.JRGzipVirtualizer;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.governors.MaxPagesGovernor;
import net.sf.jasperreports.governors.TimeoutGovernor;

@Component
public class ReportJasperCreateTaskHandler implements TaskHandler {

    static final String REPORT_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/jasper/create/handler-3";
    private static final Trace LOGGER = TraceManager.getTrace(ReportJasperCreateTaskHandler.class);

    private static final String JASPER_VIRTUALIZER_PKG = "net.sf.jasperreports.engine.fill";
    private static final String OP_CREATE_REPORT_OUTPUT = ReportJasperCreateTaskHandler.class.getName() + "createReportOutput";

    @Autowired private TaskManager taskManager;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private ReportService reportService;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired private CommandLineScriptExecutor commandLineScriptExecutor;

    @PostConstruct
    protected void initialize() {
        LOGGER.trace("Registering with taskManager as a handler for {}", REPORT_CREATE_TASK_URI);
        taskManager.registerHandler(REPORT_CREATE_TASK_URI, this);
    }

    private File getExportDir() {
        return new File(getMidPointHomeDirName(), "export");
    }

    private String getTempDirName() {
        return new File(getMidPointHomeDirName(), "tmp/").getPath();
    }

    private String getMidPointHomeDirName() {
        return System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult parentResult = task.getResult();
        OperationResult result = parentResult.createSubresult(ReportJasperCreateTaskHandler.class.getSimpleName() + ".run");

        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(result);

        recordProgress(task, 0, result);
        JRSwapFile swapFile = null;
        JRAbstractLRUVirtualizer virtualizer = null; // http://community.jaspersoft.com/wiki/virtualizers-jasperreports

        try {
            ReportType parentReport = objectResolver.resolve(task.getObjectRefOrClone(), ReportType.class, null, "resolving report", task, result);
            JasperReportEngineConfigurationType jasperConfig = parentReport.getJasper();

            if (!reportService.isAuthorizedToRunReport(parentReport.asPrismObject(), task, parentResult)) {
                LOGGER.error("Task {} is not authorized to run report {}", task, parentReport);
                throw new SecurityViolationException("Not authorized");
            }

            Map<String, Object> parameters = completeReport(parentReport, task, result);

            JasperReport jasperReport = loadJasperReport(jasperConfig);
            LOGGER.trace("compile jasper design, create jasper report : {}", jasperReport);

            //noinspection unchecked
            PrismContainer<ReportParameterType> reportParams = (PrismContainer) task.getExtensionItemOrClone(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
            if (reportParams != null) {
                PrismContainerValue<ReportParameterType> reportParamsValues = reportParams.getValue();
                Collection<Item<?, ?>> items = reportParamsValues.getItems();
                for (Item item : items) {
                    PrismProperty pp = (PrismProperty) item;
                    String paramName = pp.getPath().lastName().getLocalPart();
                    Object value;
                    if (isSingleValue(paramName, jasperReport.getParameters())) {
                        value = pp.getRealValues().iterator().next();
                    } else {
                        value = pp.getRealValues();
                    }
                    parameters.put(paramName, value);
                }
            }

            String virtualizerS;
            Integer virtualizerKickOn;
            Integer maxPages;
            Integer timeout;
            virtualizerS = jasperConfig.getVirtualizer();
            virtualizerKickOn = jasperConfig.getVirtualizerKickOn();
            maxPages = jasperConfig.getMaxPages();
            timeout = jasperConfig.getTimeout();

            if (maxPages != null && maxPages > 0) {
                LOGGER.trace("Setting hard limit on number of report pages: " + maxPages);
                jasperReport.setProperty(MaxPagesGovernor.PROPERTY_MAX_PAGES_ENABLED, Boolean.TRUE.toString());
                jasperReport.setProperty(MaxPagesGovernor.PROPERTY_MAX_PAGES, String.valueOf(maxPages));
            }

            if (timeout != null && timeout > 0) {
                LOGGER.trace("Setting timeout on report execution [ms]: " + timeout);
                jasperReport.setProperty(TimeoutGovernor.PROPERTY_TIMEOUT_ENABLED, Boolean.TRUE.toString());
                jasperReport.setProperty(TimeoutGovernor.PROPERTY_TIMEOUT, String.valueOf(timeout));
            }

            if (virtualizerS != null && virtualizerKickOn != null && virtualizerKickOn > 0) {

                String virtualizerClassName = JASPER_VIRTUALIZER_PKG + "." + virtualizerS;
                try {
                    Class<?> clazz = Class.forName(virtualizerClassName);

                    if (clazz.equals(JRSwapFileVirtualizer.class)) {
                        swapFile = new JRSwapFile(getTempDirName(), 4096, 200);
                        virtualizer = new JRSwapFileVirtualizer(virtualizerKickOn, swapFile);
                    } else if (clazz.equals(JRGzipVirtualizer.class)) {
                        virtualizer = new JRGzipVirtualizer(virtualizerKickOn);
                    } else if (clazz.equals(JRFileVirtualizer.class)) {
                        virtualizer = new JRFileVirtualizer(virtualizerKickOn, getTempDirName());
                    } else {
                        throw new ClassNotFoundException("No support for virtualizer class: " + clazz.getName());
                    }

                    LOGGER.trace("Setting explicit Jasper virtualizer: {}", virtualizer);
                    virtualizer.setReadOnly(false);
                    parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Cannot find Jasper virtualizer: " + e.getMessage());
                }
            }

            LOGGER.trace("All Report parameters:\n{}", DebugUtil.debugDumpLazily(parameters, 1));

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters);
            LOGGER.trace("fill report : {}", jasperPrint);

            String reportFilePath = generateReport(parentReport, jasperPrint);
            LOGGER.trace("generate report : {}", reportFilePath);

            saveReportOutputType(reportFilePath, parentReport, task, result);
            LOGGER.trace("create report output type : {}", reportFilePath);

            if (parentReport.getPostReportScript() != null) {
                processPostReportScript(parentReport, reportFilePath, task, result);
            }
            result.computeStatus();

        } catch (Exception ex) {
            LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
            result.recordFatalError(ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } finally {
            if (swapFile != null) {
                swapFile.dispose();
            }
            if (virtualizer != null) {
                virtualizer.cleanup();
            }
        }

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.trace("CreateReportTaskHandler.run stopping");
        return runResult;
    }

    private boolean isSingleValue(String paramName, JRParameter[] jrParams) {
        Optional<JRParameter> paramOptional = Arrays.stream(jrParams).filter(p -> p.getName().equals(paramName)).findAny();
        if (paramOptional.isPresent()) {
            JRParameter param = paramOptional.get();
            return !List.class.isAssignableFrom(param.getValueClass());
        } else {
            throw new IllegalArgumentException("Parameter " + paramName + " is not declared");
        }
    }

    private Map<String, Object> completeReport(ReportType reportType, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        return completeReport(reportType, null, null, task, result);
    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, Object> completeReport(ReportType reportType, JasperReport subReport, String subReportName,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();
        Map<String, Object> params = new HashMap<>();

        if (subReport != null && StringUtils.isNotBlank(subReportName)) {
            params.put(subReportName, subReport);
        }

        Map<String, Object> parameters = prepareReportParameters(reportType, task, result);
        params.putAll(parameters);

        Map<String, Object> subreportParameters = processSubreportParameters(jasperConfig, task, result);
        params.putAll(subreportParameters);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("create report params:\n{}", DebugUtil.debugDump(parameters, 1));
        }
        return params;
    }

    private Map<String, Object> prepareReportParameters(ReportType reportType, Task task, OperationResult parentResult) {
        JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();
        Map<String, Object> params = new HashMap<>();
         byte[] reportTemplateStyleBase64;
         reportTemplateStyleBase64 = jasperConfig.getTemplateStyle();
        if (reportTemplateStyleBase64 != null) {
            byte[] reportTemplateStyle = ReportUtils.decodeIfNeeded(reportTemplateStyleBase64);
            try {
                LOGGER.trace("Style template string {}", new String(reportTemplateStyle));
                InputStream inputStreamJRTX = new ByteArrayInputStream(reportTemplateStyle);
                JRTemplate jasperTemplateStyle = JRXmlTemplateLoader.load(inputStreamJRTX);
                params.put(ReportTypeUtil.PARAMETER_TEMPLATE_STYLES, jasperTemplateStyle);
                LOGGER.trace("Style template parameter {}", jasperTemplateStyle);

            } catch (Exception ex) {
                LOGGER.error("Error create style template parameter {}", ex.getMessage());
                throw new SystemException(ex);
            }

        }

        if (parentResult == null) {
            throw new IllegalArgumentException("No result");
        }

        // for our special datasource
        params.put(ReportTypeUtil.PARAMETER_REPORT_OID, reportType.getOid());
        params.put(ReportTypeUtil.PARAMETER_REPORT_OBJECT, reportType.asPrismObject());
        params.put(ReportTypeUtil.PARAMETER_TASK, task);
        params.put(ReportTypeUtil.PARAMETER_OPERATION_RESULT, parentResult);
        params.put(ReportService.PARAMETER_REPORT_SERVICE, reportService);

        return params;
    }

    private Map<String, Object> processSubreportParameters(JasperReportEngineConfigurationType jasperConfig, Task task, OperationResult subreportResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        List<SubreportType> subreports;
            subreports = jasperConfig.getSubreport();

        Map<String, Object> subreportParameters = new HashMap<>();
        for (SubreportType subreport : subreports) {
            Map<String, Object> subreportParam = getSubreportParameters(subreport, task, subreportResult);
            LOGGER.trace("create subreport params : {}", subreportParam);
            subreportParameters.putAll(subreportParam);

        }
        return subreportParameters;
    }

    private Map<String, Object> getSubreportParameters(SubreportType subreportType, Task task, OperationResult subResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ReportType reportType = objectResolver.resolve(subreportType.getReportRef(), ReportType.class, null,
                "resolve subreport", task, subResult);
        JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();

        Map<String, Object> parameters = prepareReportParameters(reportType, task, subResult);
        Map<String, Object> reportParams = new HashMap<>(parameters);

        JasperReport jasperReport = loadJasperReport(jasperConfig);
        reportParams.put(subreportType.getName(), jasperReport);

        Map<String, Object> subReportParams = processSubreportParameters(jasperConfig, task, subResult);
        reportParams.putAll(subReportParams);

        return reportParams;
    }

    private JasperReport loadJasperReport(JasperReportEngineConfigurationType jasperConfig) throws SchemaException {

        byte[] template;
        byte[] templateStyle;
        template = jasperConfig.getTemplate();
        templateStyle = jasperConfig.getTemplateStyle();
        if (template == null) {
            throw new IllegalStateException("Could not create report. No jasper template defined.");
        }

        LOGGER.trace("Loading Jasper report for {}", jasperConfig);
        try     {
                 JasperDesign jasperDesign = ReportTypeUtil.loadJasperDesign(template);
//                 LOGGER.trace("load jasper design : {}", jasperDesign);
                 jasperDesign.setLanguage(ReportTypeUtil.REPORT_LANGUAGE);

             if (templateStyle != null) {
                JRDesignReportTemplate jasperTemplateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + ReportTypeUtil.PARAMETER_TEMPLATE_STYLES + "}"));
                jasperDesign.addTemplate(jasperTemplateStyle);
                jasperDesign.addParameter(createParameter(ReportTypeUtil.PARAMETER_TEMPLATE_STYLES, JRTemplate.class));

             }

             jasperDesign.addParameter(createParameter("finalQuery", Object.class));
             jasperDesign.addParameter(createParameter(ReportTypeUtil.PARAMETER_REPORT_OID, String.class));
             //TODO is this right place, we don't see e.g. task
//             jasperDesign.addParameter(createParameter(PARAMETER_TASK, Object.class));
             jasperDesign.addParameter(createParameter(ReportTypeUtil.PARAMETER_OPERATION_RESULT, OperationResult.class));

             //TODO maybe other paramteres? sunch as PARAMETER_REPORT_OBJECT PARAMETER_REPORT_SERVICE ???

             JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

             LOGGER.trace("Loaded Jasper report for {}: {}", jasperConfig, jasperReport);

             return jasperReport;

         } catch (JRException ex) {
             LOGGER.error("Error loading Jasper report for {}: {}", jasperConfig, ex.getMessage(), ex);
             throw new SchemaException(ex.getMessage(), ex.getCause());
         }
    }

    private JRDesignParameter createParameter(String paramName, Class<?> valueClass) {
        JRDesignParameter param = new JRDesignParameter();
        param.setName(paramName);
        param.setValueClass(valueClass);
        param.setForPrompting(false);
        param.setSystemDefined(true);
        return param;

    }

    @SuppressWarnings("SameParameterValue")
    void recordProgress(Task task, long progress, OperationResult opResult) {
        try {
            task.setProgressImmediate(progress, opResult);
        } catch (ObjectNotFoundException e) {             // these exceptions are of so little probability and harmless, so we just log them and do not report higher
            LoggingUtils.logException(LOGGER, "Couldn't record progress to task {}, probably because the task does not exist anymore", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't record progress to task {}, due to unexpected schema exception", e, task);
        }
    }

    private String generateReport(ReportType reportType, JasperPrint jasperPrint) throws JRException {
        JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();
        String destinationFileName = getDestinationFileName(reportType);
        switch (getExport(jasperConfig)) {
            case PDF:
                JasperExportManager.exportReportToPdfFile(jasperPrint, destinationFileName);
                break;
            case XML:
            case XML_EMBED:
                JasperExportManager.exportReportToXmlFile(jasperPrint, destinationFileName, true);
                break;
            case XHTML:
            case HTML:
                JasperExportManager.exportReportToHtmlFile(jasperPrint, destinationFileName);
                break;
            case CSV:
            case RTF:
            case XLS:
            case ODT:
            case ODS:
            case DOCX:
            case XLSX:
            case PPTX:
                Exporter exporter = createExporter(getExport(jasperConfig), jasperPrint, destinationFileName);
                if (exporter != null) {
                    exporter.exportReport();
                }
            default:
                break;
        }
        return destinationFileName;
    }

    private Exporter createExporter(JasperExportType type, JasperPrint jasperPrint, String destinationFileName) {
        Exporter exporter;
        boolean writerOutput;
        switch (type) {
            case CSV:
                writerOutput = true;
                exporter = new JRCsvExporter();
                break;
            case RTF:
                writerOutput = true;
                exporter = new JRRtfExporter();
                break;
            case XLS:
                writerOutput = false;
                exporter = new JRXlsExporter();
                break;
            case ODT:
                writerOutput = false;
                exporter = new JROdtExporter();
                break;
            case ODS:
                writerOutput = false;
                exporter = new JROdsExporter();
                break;
            case DOCX:
                writerOutput = false;
                exporter = new JRDocxExporter();
                break;
            case XLSX:
                writerOutput = false;
                exporter = new JRXlsxExporter();
                break;
            case PPTX:
                writerOutput = false;
                exporter = new JRPptxExporter();
                break;
            default:
                return null;
        }
        //noinspection unchecked
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        ExporterOutput output = writerOutput
                ? new SimpleWriterExporterOutput(destinationFileName)
                : new SimpleOutputStreamExporterOutput(destinationFileName);
        //noinspection unchecked
        exporter.setExporterOutput(output);
        return exporter;
    }

    public static String getDateTime() {
        Date createDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss.SSS");
        return formatDate.format(createDate);
    }

    String getDestinationFileName(ReportType reportType) {
        File exportDir = getExportDir();
        if (!exportDir.exists() || !exportDir.isDirectory()) {
            if (!exportDir.mkdir()) {
                LOGGER.error("Couldn't create export dir {}", exportDir);
            }
        }

        String fileNamePrefix = reportType.getName().getOrig() + " " + getDateTime();
        String fileName;
        JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();
        JasperExportType export = getExport(jasperConfig);
        if (export == JasperExportType.XML_EMBED) {
            fileName = fileNamePrefix + "_embed.xml";
        } else {
            fileName = fileNamePrefix + "." + export.value();
        }
        return new File(getExportDir(), fileName).getPath();
    }

    void saveReportOutputType(String filePath, ReportType reportType, Task task, OperationResult parentResult) throws Exception {

        String fileName = FilenameUtils.getBaseName(filePath);
        JasperReportEngineConfigurationType jasperConfig = reportType.getJasper();
        JasperExportType export = getExport(jasperConfig);
        String reportOutputName = fileName + " - " + export.value();

        ReportOutputType reportOutputType = new ReportOutputType();
        prismContext.adopt(reportOutputType);

        reportOutputType.setFilePath(filePath);
        reportOutputType.setReportRef(MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE));
        reportOutputType.setName(new PolyStringType(reportOutputName));
        reportOutputType.setDescription(reportType.getDescription() + " - " + export.value());
//        reportOutputType.setExportType(export);


        SearchResultList<PrismObject<NodeType>> nodes = modelService.searchObjects(NodeType.class, prismContext
                .queryFor(NodeType.class).item(NodeType.F_NODE_IDENTIFIER).eq(task.getNode()).build(), null, task, parentResult);
        if (nodes == null || nodes.isEmpty()) {
            LOGGER.error("Could not found node for storing the report.");
            throw new ObjectNotFoundException("Could not find node where to save report");
        }

        if (nodes.size() > 1) {
            LOGGER.error("Found more than one node with ID {}.", task.getNode());
            throw new IllegalStateException("Found more than one node with ID " + task.getNode());
        }

        reportOutputType.setNodeRef(ObjectTypeUtil.createObjectRef(nodes.iterator().next(), prismContext));

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ReportOutputType> objectDelta = DeltaFactory.Object.createAddDelta(reportOutputType.asPrismObject());
        deltas.add(objectDelta);
        OperationResult subResult = parentResult.createSubresult(OP_CREATE_REPORT_OUTPUT);

        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = modelService.executeChanges(deltas, null, task, subResult);
        String reportOutputOid = ObjectDeltaOperation.findAddDeltaOid(executedDeltas, reportOutputType.asPrismObject());

        LOGGER.debug("Created report output with OID {}", reportOutputOid);
        //noinspection unchecked
        PrismProperty<String> outputOidProperty = prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME).instantiate();
        outputOidProperty.setRealValue(reportOutputOid);
        task.setExtensionPropertyImmediate(outputOidProperty, subResult);

        subResult.computeStatus();
    }

    void processPostReportScript(ReportType parentReport, String reportOutputFilePath, Task task, OperationResult parentResult) {
        CommandLineScriptType scriptType = parentReport.getPostReportScript();
        if (scriptType == null) {
            LOGGER.debug("No post report script found in {}, skipping", parentReport);
            return;
        }

        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_OBJECT, parentReport, parentReport.asPrismObject().getDefinition());
        PrismObject<TaskType> taskObject = task.getUpdatedOrClonedTaskObject();
        variables.put(ExpressionConstants.VAR_TASK, taskObject.asObjectable(), taskObject.getDefinition());
        variables.put(ExpressionConstants.VAR_FILE, commandLineScriptExecutor.getOsSpecificFilePath(reportOutputFilePath), String.class);

        try {
            commandLineScriptExecutor.executeScript(scriptType, variables, "post-report script in "+parentReport, task, parentResult);
        } catch (Exception e) {
            LOGGER.error("An exception has occurred during post report script execution {}", e.getLocalizedMessage(), e);
        }
    }

    @Override
    public Long heartbeat(Task task) {
        return null;
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.REPORT;
    }

    protected JasperExportType getExport(JasperReportEngineConfigurationType jasperConfig) {
        return ReportUtils.getExport(jasperConfig);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_REPORT_TASK.value();
    }
}
