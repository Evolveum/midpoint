package com.evolveum.midpoint.report.impl;


import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.base.JRBasePen;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.engine.util.JRReportUtils;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlTemplateLoader;
import net.sf.jasperreports.olap.JRMondrianQueryExecuterFactory;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;


public class ReportUtils {

	private static String MIDPOINT_HOME = System.getProperty("midpoint.home"); 
    private static String EXPORT_DIR = MIDPOINT_HOME + "export/";
    
    // parameters define objectQuery
    private static String PARAMETER_OBJECT_TYPE = "type";
    private static String PARAMETER_QUERY_FILTER = "filter";

    // parameters define datasource
    private static String PARAMETER_REPORT_OID = "reportOid";
    private static String PARAMETER_OPERATION_RESULT = "operationResult";
    
    // parameter for design
    private static String PARAMETER_LOGO = "logoPath";
    private static String PARAMETER_TEMPLATE_STYLES = "baseTemplateStyles";
    private static String TEMPLATE_STYLE_SCHEMA = "<!DOCTYPE jasperTemplate  PUBLIC \"-//JasperReports//DTD Template//EN\" \"http://jasperreports.sourceforge.net/dtds/jaspertemplate.dtd\">";
    
    private static final Trace LOGGER = TraceManager
			.getTrace(ReportUtils.class);

    
    //new
    
//    public static JasperDesign loadJasperDesign(byte[] template) throws SchemaException{
//    	try	 {
//    	byte[] reportTemplate = Base64.decodeBase64(template);
//	 	
//	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate);
//	 	JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML);
//	 	LOGGER.trace("load jasper design : {}", jasperDesign);
//	 	return jasperDesign;
//    	} catch (JRException ex){
//    		throw new SchemaException(ex.getMessage(), ex.getCause());
//    	}
//    }
//    
//public static JasperReport loadJasperReport(ReportType reportType) throws SchemaException{
//		
//		if (reportType.getTemplate() == null) {
//			throw new IllegalStateException("Could not create report. No jasper template defined.");
//		}
//		try	 {
////	    	 	byte[] reportTemplate = Base64.decodeBase64(reportType.getTemplate());
////	    	 	
////	    	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate);
//	    	 	JasperDesign jasperDesign = loadJasperDesign(reportType.getTemplate());//JRXmlLoader.load(inputStreamJRXML);
////	    	 	LOGGER.trace("load jasper design : {}", jasperDesign);
//			 
//			 if (reportType.getTemplateStyle() != null){
//				JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + PARAMETER_TEMPLATE_STYLES + "}"));
//				jasperDesign.addTemplate(templateStyle);
//				JRDesignParameter parameter = new JRDesignParameter();
//				parameter.setName(PARAMETER_TEMPLATE_STYLES);
//				parameter.setValueClass(JRTemplate.class);
//				parameter.setForPrompting(false);
//				jasperDesign.addParameter(parameter);
//			 } 
//			 JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
//			 return jasperReport;
//		 } catch (JRException ex){ 
//			 LOGGER.error("Couldn't create jasper report design {}", ex.getMessage());
//			 throw new SchemaException(ex.getMessage(), ex.getCause());
//		 }
//		 
//		 
//}

public static List<PrismObject<? extends ObjectType>> getReportData(PrismContext prismContext, Task task, ReportFunctions reportFunctions, String script, ExpressionVariables variables, ObjectResolver objectResolver) throws ExpressionSyntaxException, ExpressionEvaluationException, ObjectNotFoundException{
	List<PrismObject<? extends ObjectType>> results = new ArrayList<>();
	FunctionLibrary functionLib = ExpressionUtil.createBasicFunctionLibrary(prismContext, prismContext.getDefaultProtector());
	FunctionLibrary midPointLib = new FunctionLibrary();
	midPointLib.setVariableName("report");
	midPointLib.setNamespace("http://midpoint.evolveum.com/xml/ns/public/function/report-3");
//	ReportFunctions reportFunctions = new ReportFunctions(prismContext, model, taskManager, auditService);
	midPointLib.setGenericFunctions(reportFunctions);
	
	Collection<FunctionLibrary> functions = new ArrayList<>();
	functions.add(functionLib);
	
	
	functions.add(midPointLib);
	Jsr223ScriptEvaluator scripts = new Jsr223ScriptEvaluator("Groovy", prismContext, prismContext.getDefaultProtector());
	Object o = scripts.evaluateReportScript(script, variables, objectResolver, functions, "desc", task.getResult());
	if (o != null){

		if (Collection.class.isAssignableFrom(o.getClass())) {
			Collection resultSet = (Collection) o;
			if (resultSet != null && !resultSet.isEmpty()){
				if (resultSet.iterator().next() instanceof PrismObject){
					results.addAll((Collection<? extends PrismObject<? extends ObjectType>>) o);
				} else {
//					return new JRBeanCollectionDataSource(resultSet);
				}
			}
			
		} else {
			results.add((PrismObject) o);
		}
	}
	
	return results;

}

public static List<PrismObject<? extends ObjectType>> getReportData(ModelService model, Class type, ObjectQuery query, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException{
	List<PrismObject<? extends ObjectType>> results = new ArrayList<>();
		
		GetOperationOptions options = GetOperationOptions.createRaw();
		options.setResolveNames(true);
	results = model.searchObjects(type, query, SelectorOptions.createCollection(options), task, parentResult);;
	return results;
}
    //end of new
    
	public static Class<?> getClassType(QName clazz, String namespace)
    {
		Class<?> classType = String.class; 
    	try
    	{
    		classType = XsdTypeMapper.getXsdToJavaMapping(clazz);
    		if (classType == XMLGregorianCalendar.class) {
    			classType = Timestamp.class;
    		}
    		if (clazz.getNamespaceURI().equals(namespace))
    		{
    			classType = Integer.class;
    		}
    		classType = (classType == null) ? String.class : classType ;
    	} catch (Exception ex){
    		classType = String.class;
    	}
    	return classType;
    	
    }

    public static Element getParametersXsdSchema(ReportType reportType) {
		XmlSchemaType xmlSchemaType = reportType.getConfigurationSchema();
		if (xmlSchemaType == null) {
			return null;
		}
		return ObjectTypeUtil.findXsdElement(xmlSchemaType);
	}
	
	public static PrismSchema getParametersSchema(ReportType reportType, PrismContext prismContext) throws SchemaException {
		Element parametersSchemaElement = getParametersXsdSchema(reportType);
		if (parametersSchemaElement == null) {
			return null;
		}

		/*LOGGER.trace("Parameters schema element : {}", parametersSchemaElement.getElementsByTagName("simpleType"));
		LOGGER.trace("Parameters schema attribut : {}", parametersSchemaElement.getAttributeNode("simpleType"));
		NodeList childNodes = parametersSchemaElement.getChildNodes();
		for (int i=0; i< childNodes.getLength(); i++)
		{
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equals("xsd:simpleType"))
			{
				LOGGER.trace("Parameters schema child node : {}", childNode.toString());
				LOGGER.trace("Parameters schema child node name : {}", childNode.getNodeName());
				LOGGER.trace("Parameters schema child node local name : {}", childNode.getLocalName());
				LOGGER.trace("Parameters schema child node class : {}", childNode.getClass());
				LOGGER.trace("Parameters schema child node type : {}", childNode.getNodeType());
				LOGGER.trace("Parameters schema child node value : {}", childNode.getNodeValue());
				LOGGER.trace("Parameters schema child node text content : {}", childNode.getTextContent().split("\t"));
				String enumValue = childNode.getTextContent().replaceAll("\t","");
				LOGGER.trace("Parameters schema enum value : {}", enumValue); 
				
				LOGGER.trace("---------------------------------------------------------------------------------------");
				NamedNodeMap attributes = childNode.getAttributes();
				for (int j=0; j<attributes.getLength(); j++)
				{
					Node attribute = attributes.item(j);
					LOGGER.trace("child node attribut : {}", attribute);
					LOGGER.trace("child node attribut text content : {}", attribute.getTextContent());
				}
			}
		}
		*/
		PrismSchema parametersSchema = PrismSchema.parse(parametersSchemaElement, true, "schema for " + reportType, prismContext);
		if (parametersSchema == null) {
			throw new SchemaException("No parameters schema in "+ reportType);
		}
		LOGGER.trace("Parameters schema : {}", parametersSchema.debugDump());
		
		return parametersSchema;
	}
	
    public static PrismContainer<Containerable> getParametersContainer(ReportType reportType, PrismSchema schema)
			throws SchemaException, ObjectNotFoundException {
		
		PrismContainer<Containerable> configuration = reportType.asPrismObject().findContainer(ReportType.F_CONFIGURATION);
		if (configuration == null) {
			return null;
//			throw new SchemaException("No configuration container in " + reportType);
		}
		
		LOGGER.trace("Parameters container : {}", configuration.debugDump());
		if (schema == null){
			return null;
		}
		
		QName configContainerQName = new QName(schema.getNamespace(), ReportType.F_CONFIGURATION.getLocalPart());
		PrismContainerDefinition<ReportConfigurationType> configurationContainerDefinition = schema.findContainerDefinitionByElementName(configContainerQName);
		
		if (configurationContainerDefinition == null) {
			throw new SchemaException("No configuration container definition in " + reportType);
		}
		
		LOGGER.trace("Parameters configuration definition: {}", configurationContainerDefinition.debugDump());
		/*for(ItemDefinition item : configurationContainerDefinition.getDefinitions())
		{
			LOGGER.trace("Item definition: {}", item.dump());
			LOGGER.trace("Display Name : {}", item.getDisplayName());
			LOGGER.trace("Name : {}", item.getName());
			LOGGER.trace("Definition - type class : {}", item.getTypeClass());
			LOGGER.trace("Definition - type name: {}", item.getTypeName());
	
		}
		
		LOGGER.trace("---------------------------------------------------------------------------------------");
>>>>>>> 48eec5fce88a117d5ce08aa60a36b9d09045780d
		
		for(PrismPropertyDefinition property : configurationContainerDefinition.getPropertyDefinitions())
		{
			LOGGER.trace("PrismProperty definition: {}", property.dump());
			LOGGER.trace("Display Name : {}", property.getDisplayName());
			LOGGER.trace("Name : {}", property.getName());
			LOGGER.trace("Definition - type class : {}", property.getTypeClass());
			LOGGER.trace("Definition - type name: {}", property.getTypeName());
			LOGGER.trace("Values: {}", property.getAllowedValues());
		}
		LOGGER.trace("---------------------------------------------------------------------------------------");*/
		configuration.applyDefinition(configurationContainerDefinition, true);
		
		LOGGER.trace("Parameters container with definitions : {}", configuration.debugDump());
		 
		return configuration;
	}
    
 public static JasperReport getJasperReport(ReportType reportType, PrismContainer<Containerable> parameterConfiguration, PrismSchema reportSchema) throws JRException
 {
	 JasperDesign jasperDesign;
	 JasperReport jasperReport;
	 try
	 {
		 if (reportType.getTemplate() == null)
		 {
			jasperDesign = createJasperDesign(reportType, parameterConfiguration, reportSchema);
    	 	LOGGER.trace("create jasper design : {}", jasperDesign);
		 }
		 else
		 {
    	 	byte[] reportTemplatebase64 = reportType.getTemplate();
    	 	byte[] reportTemplate = Base64.decodeBase64(reportTemplatebase64);
    	 	
    	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate);
    	 	jasperDesign = JRXmlLoader.load(inputStreamJRXML);
    	 	LOGGER.trace("load jasper design : {}", jasperDesign);
		 }
		 
		 if (reportType.getTemplateStyle() != null)
		 {
			JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + PARAMETER_TEMPLATE_STYLES + "}"));
			jasperDesign.addTemplate(templateStyle);
			JRDesignParameter parameter = new JRDesignParameter();
			parameter.setName(PARAMETER_TEMPLATE_STYLES);
			parameter.setValueClass(JRTemplate.class);
			parameter.setForPrompting(false);
			jasperDesign.addParameter(parameter);
		 } 
		 jasperReport = JasperCompileManager.compileReport(jasperDesign);
		 
		 
	 } catch (JRException ex){ 
		 LOGGER.error("Couldn't create jasper report design {}", ex.getMessage());
		 throw ex;
	 }
	 
	 return jasperReport;
 }
    		
    
    public static Map<String, Object> getReportParameters(ReportType reportType, PrismContainer<Containerable> parameterConfiguration, PrismSchema reportSchema, OperationResult parentResult)
	{
		Map<String, Object> params = new HashMap<String, Object>();
		if (reportType.getTemplateStyle() != null)
		{	 
			byte[] reportTemplateStyleBase64 = reportType.getTemplateStyle();
			byte[] reportTemplateStyle = Base64.decodeBase64(reportTemplateStyleBase64);
			//TODO must be changed
			//without replace strings, without xmlns namespace, with insert into schema special xml element DOCTYPE
//			int first = reportTemplateStyle.indexOf(">");
//			int last = reportTemplateStyle.lastIndexOf("<");
//			reportTemplateStyle = "<jasperTemplate>" + reportTemplateStyle.substring(first+1, last) + "</jasperTemplate>";
//			StringBuilder templateStyleSb = new StringBuilder(TEMPLATE_STYLE_SCHEMA);
			try{
//				templateStyleSb.append("\n");
//				templateStyleSb.append(new String(reportTemplateStyle, "utf-8"));
	////			
	//			reportTemplateStyle = TEMPLATE_STYLE_SCHEMA + "\n" + reportTemplateStyle;  
				LOGGER.trace("Style template string {}", new String(reportTemplateStyle));
		    	InputStream inputStreamJRTX = new ByteArrayInputStream(reportTemplateStyle);
	    		JRTemplate templateStyle = JRXmlTemplateLoader.load(inputStreamJRTX);
				params.put(PARAMETER_TEMPLATE_STYLES, templateStyle);
				LOGGER.trace("Style template parameter {}", templateStyle);
				
			} catch (Exception ex) {
				LOGGER.error("Error create style template parameter {}", ex.getMessage());
				throw new SystemException(ex);
			}
			
		 } 
		OperationResult subResult = parentResult.createSubresult("get report parameters");
		if (parameterConfiguration != null) 	
		{
			for(PrismProperty<?> parameter : parameterConfiguration.getValue().getProperties())
			{
				LOGGER.trace("parameter {}, {}, {} ", new Object[]{parameter.getElementName().getLocalPart(), parameter.getRealValue(), parameter.getValues()});
					
					if (parameter.getDefinition().getTypeName().getNamespaceURI().equals(reportSchema.getNamespace()))
					{			
						com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl ccc = (com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl)parameter.getRealValue(com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl.class);
						LOGGER.trace("Parameter simple type, text content : {}, {}", parameter.getDefinition().getTypeName().getLocalPart(), ccc.getTextContent());
						params.put(parameter.getElementName().getLocalPart(), Integer.decode(ccc.getTextContent()));
					} else {
						
						Class<?> classType = ReportUtils.getClassType(parameter.getDefinition().getTypeName(), reportSchema.getNamespace());
						if (classType == java.sql.Timestamp.class) {
							params.put(parameter.getElementName().getLocalPart(), ReportUtils.convertDateTime((XMLGregorianCalendar)parameter.getRealValue(XMLGregorianCalendar.class)));
						} else {
							params.put(parameter.getElementName().getLocalPart(), parameter.getRealValue());
						}
					}
					
					//LOGGER.trace("--------------------------------------------------------------------------------");
				//}
			}
		}
		// for our special datasource
		subResult.computeStatus();	
		 
		return params;
	}
    
    public static String resolveRefName(ObjectReferenceType ref){
		if (ref == null){
			return null;
		}
		PrismReferenceValue refValue = ref.asReferenceValue();
		Object name = refValue.getUserData(XNodeSerializer.USER_DATA_KEY_COMMENT);
		if (!(name instanceof String)){
			LOGGER.error("Couldn't resolve object name");
		}
		
		return (String) name;
	}
    
    /*
	public static Class getObjectTypeClass(PrismContainer<Containerable> parameterConfiguration, String namespace)
	{
		
		PrismProperty objectTypeProp = getParameter(PARAMETER_OBJECT_TYPE, parameterConfiguration, namespace);
		return getObjectTypeClass(objectTypeProp.getRealValue());
	}
	*/
	public static Class<?> getObjectTypeClass(Map<?, ?> params){
		
		Object parameterClass = params.get(PARAMETER_OBJECT_TYPE);
		return getObjectTypeClass(parameterClass);
	}
	
	public static Class<?> getObjectTypeClass(Object objectClass)
	{
		Class<?> clazz = ObjectType.class;
		try
		{
			QName objectType = (QName) objectClass;			
			LOGGER.trace("Parameter object type : {}", objectType);
			clazz = ObjectTypes.getObjectTypeClass(objectType.getLocalPart());
			LOGGER.trace("Parameter class of object type : {}", clazz);
		} catch (Exception ex){
			LOGGER.trace("Couldn't load object type parameter : {}", ex.getMessage());
		}
		return clazz;
	}

	
	public static PrismProperty<?> getParameter(String parameterName, PrismContainer<Containerable> parameterConfiguration, String namespace)
	{
		if (parameterConfiguration == null){
			return null;
		}
		PrismProperty<?> property = parameterConfiguration.findProperty(new QName(namespace, parameterName));	
		/*for(PrismProperty parameter : parameterConfiguration.getValue().getProperties())
		{
			LOGGER.trace("Parameter : {} ", parameter.debugDump());
			LOGGER.trace("Display Name : {}", parameter.getDisplayName());
			LOGGER.trace("Real value : {}", parameter.getRealValue());
			LOGGER.trace("Element Name : {}", parameter.getElementName());
			LOGGER.trace("Definition - type name: {}", parameter.getDefinition().getTypeName());
			LOGGER.trace("--------------------------------------------------------------------------------");
		}*/
		return property;
	}
	
	public static Timestamp convertDateTime(XMLGregorianCalendar dateTime)
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis()); 
		try {
			timestamp = new Timestamp(XmlTypeConverter.toDate(dateTime).getTime()); 
		}
		catch (Exception ex)
		{
			LOGGER.trace("Incorrect date time value {}", dateTime);
		}
		
		return timestamp;
	}
	
	private static JRDesignParameter createParameter(PrismProperty<?> parameterConfig, PrismSchema reportSchema)
	{
		JRDesignParameter parameter = new JRDesignParameter();
		parameter.setName(parameterConfig.getElementName().getLocalPart());
		parameter.setValueClass(getClassType(parameterConfig.getDefinition().getTypeName(), reportSchema.getNamespace()));
		parameter.setForPrompting(false);
		return parameter;
	}
    
	private static JRDesignTextField createField(ReportFieldConfigurationType fieldRepo, int x, int width, int frameWidth)
	{
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(x);
		textField.setY(1);		
		textField.setWidth(width);
		textField.setHeight(18);
		textField.setStretchWithOverflow(true);
		textField.setBlankWhenNull(true);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Detail");
		textField.setExpression(new JRDesignExpression("$F{" + fieldRepo.getNameReport() + "}"));
		return textField;
	}
	
	private static void setOrientation(JasperDesign jasperDesign, OrientationEnum orientation, int pageWidth, int pageHeight, int columnWidth)
	{
		jasperDesign.setOrientation(orientation);
		jasperDesign.setPageWidth(pageWidth);
		jasperDesign.setPageHeight(pageHeight);
		jasperDesign.setColumnWidth(columnWidth);
	}
    
	private static JRDesignStyle createStyle(String name, boolean isDefault, boolean isBold, JRStyle parentStyle, Color backcolor, Color forecolor, ModeEnum mode, HorizontalAlignEnum hAlign, VerticalAlignEnum vAlign, int fontSize, String pdfFontName, String pdfEncoding, boolean isPdfEmbedded)
	{
		JRDesignStyle style = new JRDesignStyle();
		style.setName(name);
		style.setDefault(isDefault);
		style.setBold(isBold);
		if (parentStyle != null) style.setParentStyle(parentStyle);
		style.setBackcolor(backcolor);
		style.setForecolor(forecolor);
		if (hAlign != null) style.setHorizontalAlignment(hAlign);
		if (vAlign != null) style.setVerticalAlignment(vAlign);
		if (fontSize != 0) style.setFontSize(fontSize);
		if (mode != null) style.setMode(mode);
		if (!pdfFontName.isEmpty())
		{
			style.setPdfFontName(pdfFontName);
			style.setPdfEncoding(pdfEncoding);
			style.setPdfEmbedded(isPdfEmbedded);
		}
		return style;
	}
	
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle, Color backcolor, Color forecolor, ModeEnum mode, HorizontalAlignEnum hAlign, int fontSize)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, backcolor, forecolor, mode, hAlign, null, fontSize, "", "", false);
		return style;
	}
	
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle, Color backcolor, Color forecolor, ModeEnum mode, int fontSize)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, backcolor, forecolor, mode, null, null, fontSize, "", "", false);
		return style;
	}
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle, int fontSize)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, null, null, null, null, null, fontSize, "", "", false);
		return style;
	}
	
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, null, null, null, null, null, 0, "", "", false);
		return style;
	}
	
	private static JRDesignBand createBand(int height, SplitTypeEnum split)
	{
		JRDesignBand band = new JRDesignBand();
		band.setHeight(height);
		band.setSplitType(split);
		return band;
	}
	
	private static JRDesignBand createBand(int height)
	{
		return createBand(height, SplitTypeEnum.STRETCH);
	}
	
	private static JRDesignFrame createFrame(int x, int y, int height, int width, String styleName, ModeEnum mode)
	{
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(x);
		frame.setY(y);
		frame.setHeight(height);
		frame.setWidth(width);
		frame.setStyleNameReference(styleName);
		if (mode != null) frame.setMode(mode);
		return frame;
	}
	
	private static JRDesignFrame createFrame(int x, int y, int height, int width, String styleName)
	{
		return createFrame(x, y, height, width, styleName, null);
	}
	private static JRDesignStaticText createStaticText(int x, int y, int height, int width, String styleName, VerticalAlignEnum vAlign, String text)
	{
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(x);
		staticText.setY(y);
		staticText.setHeight(height);
		staticText.setWidth(width);
		staticText.setStyleNameReference(styleName);
		staticText.setVerticalAlignment(vAlign);
		staticText.setText(text);
		return staticText;
	}
	
	private static JRDesignImage createImage(int x, int y, int height, int width, String styleName, JRExpression expression)
	{
		JRDesignImage image = new JRDesignImage(new JRDesignStyle().getDefaultStyleProvider());
		image.setX(x);
		image.setY(y);
		image.setHeight(height);
		image.setWidth(width);
		image.setStyleNameReference(styleName);
		image.setExpression(expression);
		return image;
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, HorizontalAlignEnum hAlign, VerticalAlignEnum vAlign, String styleName, Boolean isBold, EvaluationTimeEnum evalution, Boolean blankWhenNull, JRExpression expression)
	{
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(x);
		textField.setY(y);
		textField.setHeight(height);
		textField.setWidth(width);
		textField.setHorizontalAlignment(hAlign);
		textField.setVerticalAlignment(vAlign);
		textField.setStyleNameReference(styleName);
		if (isBold != null) textField.setBold(isBold);
		textField.setEvaluationTime(evalution);
		if (blankWhenNull != null) textField.setBlankWhenNull(blankWhenNull);
		textField.setExpression(expression);
		return textField;
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, Boolean isBold, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, isBold, EvaluationTimeEnum.NOW, null, expression);
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, null, EvaluationTimeEnum.NOW, null, expression);
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, Boolean isBold, EvaluationTimeEnum evalution, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, isBold, evalution, null, expression);
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, EvaluationTimeEnum evalution, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, null, evalution, null, expression);
	}
	
	private static JRDesignLine createLine(int x, int y, int height, int width, PositionTypeEnum position, float penLineWidth)
	{
		JRDesignLine line = new JRDesignLine();
		line.setX(x);
		line.setY(y);
		line.setHeight(height);
		line.setWidth(width);
		line.setPositionType(position);
		JRBasePen pen = new JRBasePen(line);
		pen.setLineWidth(penLineWidth);
		pen.setLineColor(Color.decode("#999999"));
		return line;
	}
	
	private static void createStyles(JasperDesign jasperDesign) throws JRException
	{
		JRDesignStyle baseStyle = createStyle("Base", true, true, null, Color.decode("#FFFFFF"), Color.decode("#000000"), null, HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE, 10, "Helvetica", "Cp1252", false);
		jasperDesign.addStyle(baseStyle);
			
		JRDesignStyle titleStyle = createStyle("Title", true, baseStyle, Color.decode("#267994"), Color.decode("#FFFFFF"), ModeEnum.OPAQUE, 26);
		jasperDesign.addStyle(titleStyle);
		
		JRDesignStyle pageHeaderStyle = createStyle("Page header", true, baseStyle, 12);
		jasperDesign.addStyle(pageHeaderStyle);
		
		JRDesignStyle columnHeaderStyle = createStyle ("Column header", true, baseStyle, Color.decode("#333333"), Color.decode("#FFFFFF"), ModeEnum.OPAQUE, HorizontalAlignEnum.CENTER, 12);
		jasperDesign.addStyle(columnHeaderStyle);
		
		JRDesignStyle detailStyle = createStyle("Detail", false, baseStyle);
		jasperDesign.addStyle(detailStyle);
		
		JRDesignStyle pageFooterStyle = createStyle("Page footer", true, baseStyle, 9);
		jasperDesign.addStyle(pageFooterStyle);
	}

	private static JRDesignBand createTitleBand(int height, int reportColumn, int secondColumn, PrismContainer<Containerable> parameterConfiguration, PrismSchema reportSchema)
	{
		JRDesignBand titleBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 0, 70, reportColumn, "Title");
		titleBand.addElement(frame);
	
		JRDesignStaticText staticText = createStaticText(10, 15, 40, 266, "Title", VerticalAlignEnum.MIDDLE, "DataSource Report");
		frame.addElement(staticText);
		if (getParameter(PARAMETER_LOGO, parameterConfiguration, reportSchema.getNamespace()) != null) 
		{
			JRDesignImage image = createImage(589, 15, 40, 203, "Title", new JRDesignExpression("$P{" + PARAMETER_LOGO + "}"));
			frame.addElement(image);
		}
		staticText = createStaticText(secondColumn, 70, 20, 150, "Page header", VerticalAlignEnum.MIDDLE, "Report generated on:");
		titleBand.addElement(staticText);
	
		JRDesignTextField textField = createTextField(secondColumn + 150, 70, 20, 250, "Page header", false, new JRDesignExpression("new java.util.Date()"));
		titleBand.addElement(textField);
	
		staticText = createStaticText(secondColumn, 90, 20, 150, "Page header", VerticalAlignEnum.MIDDLE, "Number of records:");
		titleBand.addElement(staticText);
	
		textField = createTextField(secondColumn + 150, 90, 20, 250, "Page header", false, EvaluationTimeEnum.REPORT, new JRDesignExpression("$V{REPORT_COUNT}"));
		titleBand.addElement(textField);
	
	    //remove parameters, which are not special for data
		if (parameterConfiguration != null)
		{
			int y = 70;
			for(PrismProperty<?> parameter : parameterConfiguration.getValue().getProperties())
			{/*
				LOGGER.trace("Parameter : {} ", parameter);
				LOGGER.trace("Display Name : {}", parameter.getDisplayName());
				LOGGER.trace("Real value : {}", parameter.getRealValue());
				LOGGER.trace("Element Name : {}", parameter.getElementName());
				LOGGER.trace("Definition : {}", parameter.getDefinition());
				LOGGER.trace("--------------------------------------------------------------------------------");
			*/
				if (parameter.getDisplayName() != null)
				{
					staticText = createStaticText(2, y, 20, 150, "Page header", VerticalAlignEnum.MIDDLE, parameter.getDisplayName() + ":");
					titleBand.addElement(staticText);
		
					textField = createTextField(160, y, 20, 240, "Page header", false, new JRDesignExpression("$P{"+ parameter.getElementName().getLocalPart() + "}"));
					titleBand.addElement(textField);

					y = y + 20;
				}
			}
		}
		return titleBand;
	}
	
	private static JRDesignBand createColumnHeaderBand(int height, int reportColumn, List<ReportFieldConfigurationType> reportFields)
	{
		JRDesignBand columnHeaderBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 5, 19, reportColumn, "Column header");
	
		int x = 0;
		int width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportFields)
		{
			width =  Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidth()));	
			JRDesignStaticText staticText = createStaticText(x, 0, 18, width, "Column header", VerticalAlignEnum.MIDDLE, fieldRepo.getNameHeader());
			frame.addElement(staticText);
			x = x + width;
		}
	
		columnHeaderBand.addElement(frame);
		return columnHeaderBand;
	}
	
	private static JRDesignBand createDetailBand(int height, int reportColumn, List<ReportFieldConfigurationType> reportFields)
	{ 
		JRDesignBand detailBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 1, 19, reportColumn, "Detail");
	
		int x = 0;
		int width = 0;
		int frameWidth = frame.getWidth();
		for(ReportFieldConfigurationType fieldRepo : reportFields)
		{
			width = Math.round((float) ((frameWidth/100) * fieldRepo.getWidth())); 
			JRDesignTextField textField = createField(fieldRepo, x, width, frameWidth);
			frame.addElement(textField);
			x = x + width;
		}
	
		JRDesignLine line = createLine(0, 3, 1, reportColumn, PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM, (float) 0.5);
		frame.addElement(line);
	
		detailBand.addElement(frame);
		return detailBand;
	}
	
	private static JRDesignBand createColumnFooterBand(int height, int reportColumn)
	{
		JRDesignBand columnFooterBand = createBand(height);
		
		JRDesignLine line = createLine(0, 3, 1, reportColumn, PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM, (float) 0.5);
		columnFooterBand.addElement(line);
		return columnFooterBand;
	}
	
	private static JRDesignBand createPageFooterBand(int height, int reportColumn)
	{
		JRDesignBand pageFooterBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 1, 24, reportColumn, "Page footer", ModeEnum.TRANSPARENT);
		JRDesignTextField textField = createTextField(2, 1, 20, 197, "Page footer", new JRDesignExpression("new java.util.Date()"));
		frame.addElement(textField);

		textField = createTextField(680, 1, 20, 80, "Page footer", new JRDesignExpression("\"Page \" + String.valueOf($V{PAGE_NUMBER}) + \" of\""));
		frame.addElement(textField);
	
		textField = createTextField(760, 1, 20, 40,"Page footer", EvaluationTimeEnum.REPORT, new JRDesignExpression("$V{PAGE_NUMBER}")); 
		frame.addElement(textField);
	
		pageFooterBand.addElement(frame);
		return pageFooterBand;
	}
	
    public static JasperDesign createJasperDesign(ReportType reportType, PrismContainer<Containerable> parameterConfiguration, PrismSchema reportSchema) throws JRException
	{
    	
		//JasperDesign
		JasperDesign jasperDesign = new JasperDesign();
		String reportName = reportType.getName().getOrig(); 
		jasperDesign.setName(reportName.replace("\\s", ""));
		
		switch (reportType.getOrientation())
		{
			case LANDSCAPE :
			default: setOrientation(jasperDesign, OrientationEnum.LANDSCAPE, 842, 595, 802);
				break;
			case PORTRAIT :	setOrientation(jasperDesign, OrientationEnum.PORTRAIT, 595, 842, 555);
				break;
		}
		
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(20);
		jasperDesign.setBottomMargin(20);
		
		//Parameters
		if (parameterConfiguration != null)
		{
			for(PrismProperty<?> parameterConfig : parameterConfiguration.getValue().getProperties())
			{
				JRDesignParameter parameter = createParameter(parameterConfig, reportSchema);
				jasperDesign.addParameter(parameter);
				
			}
//			jasperDesign.setLanguage(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION);
//			jasperDesign.setProperty(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT, parameterConfiguration.getPrismContext());
//			jasperDesign.addParameter(parameter);
//			.PARAMETER_MIDPOINT_CONNECTION, model);
		}	 
		//Template Style or Styles
		if (getParameter(PARAMETER_TEMPLATE_STYLES, parameterConfiguration, reportSchema.getNamespace()) != null)
		{
			JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + PARAMETER_TEMPLATE_STYLES + "}"));
			jasperDesign.addTemplate(templateStyle);
		}
		else createStyles(jasperDesign);
		
		//Fields
		for(ReportFieldConfigurationType fieldRepo : reportType.getField())
		{
			JRDesignField field = new JRDesignField();
			field.setName(fieldRepo.getNameReport());
			field.setValueClass(getClassType(fieldRepo.getClassType(), reportSchema.getNamespace()));	
			jasperDesign.addField(field);
		}

		//Background
		JRDesignBand bandBackground = createBand(30);
		jasperDesign.setBackground(bandBackground);
		
		//Title
		//band size depends on the number of parameters
		int reportColumn = jasperDesign.getColumnWidth() - 2;
		int secondColumn = Math.round(jasperDesign.getColumnWidth()/2 - 1);
		//int height = 70 + Math.max(40, parameters.size()*20);
		int height = 70 + Math.max(40, 20);
		
		JRDesignBand titleBand = createTitleBand(height, reportColumn, secondColumn, parameterConfiguration, reportSchema);
		jasperDesign.setTitle(titleBand);
	
		//Column header
		JRDesignBand columnHeaderBand = createColumnHeaderBand(24, reportColumn, reportType.getField());
		jasperDesign.setColumnHeader(columnHeaderBand);
		
		//Detail
		JRDesignBand detailBand = createDetailBand(20, reportColumn, reportType.getField());
		((JRDesignSection)jasperDesign.getDetailSection()).addBand(detailBand);		
		
		//Column footer
		JRDesignBand columnFooterBand = createColumnFooterBand(7, reportColumn);
		jasperDesign.setColumnFooter(columnFooterBand);

		//Page footer
		JRDesignBand pageFooterBand = createPageFooterBand(32, reportColumn);
		jasperDesign.setPageFooter(pageFooterBand);

		return jasperDesign;
	}

    public static String getDateTime()
    {
    	Date createDate = new Date(System.currentTimeMillis());
    	SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss");
        return formatDate.format(createDate);
    }
       
    public static String getReportOutputFilePath(ReportType reportType){
        File exportFolder = new File(EXPORT_DIR);
        if (!exportFolder.exists() || !exportFolder.isDirectory()) {
            exportFolder.mkdir();
        }
    	
    	String output = EXPORT_DIR +  reportType.getName().getOrig() + " " + getDateTime();
    	
    	switch (reportType.getExport())
        {
        	case PDF : output = output + ".pdf";
        		break;
          	case CSV : output = output + ".csv";
      			break;
          	case XML : output = output + ".xml";
          		break;
          	case XML_EMBED : output = output + "_embed.xml";
          		break;
          	case HTML : output = output + ".html";
          		break;
          	case RTF : output = output + ".rtf";
          		break;
          	case XLS : output = output + ".xls";
  				break;
          	case ODT : output = output + ".odt";
          		break;
          	case ODS : output = output + ".ods";
  				break;
          	case DOCX : output = output + ".docx";
  				break;
          	case XLSX : output = output + ".xlsx";
  				break;
          	case PPTX : output = output + ".pptx";
          		break;
          	case XHTML : output = output + ".x.html";
  				break;
          	case JXL : output = output + ".jxl.xls";
          		break; 	
			default:
				break;
        }
    	
    	return output;
    }
    
    // TODO is this used? YES :) But it needs re-implementation (I hope, for now this one is quite good)..
    public static String getDeltaAudit(String delta)
    {
    	String deltaAudit = "fixed value";
    	try
    	{
//    		SchemaRegistry schemaRegistry = new SchemaRegistry();
//    		PrismContext prismContext = PrismContext.createEmptyContext(schemaRegistry);
//    		ObjectDeltaType xmlDelta = prismContext.getPrismJaxbProcessor().unmarshalObject(delta, ObjectDeltaType.class);
//    		deltaAudit = xmlDelta.getChangeType().toString() + " - " + xmlDelta.getObjectType().getLocalPart().toString();
    		
    		DomParser domParser = new DomParser(null);
    		XNode xnode = domParser.parse(delta);
    		
    		MapXNode deltaXnode = null; 
    		if (xnode instanceof RootXNode){
    			RootXNode root = (RootXNode) xnode;
    			if (root.getSubnode() instanceof MapXNode){
    				deltaXnode = (MapXNode) root.getSubnode();
    			} else{
    				throw new IllegalStateException("Error parsing delta for audit report. Expected map after parsing, but was: " + root.getSubnode());
    			}
    		} else if (xnode instanceof MapXNode){
    			deltaXnode = (MapXNode) xnode;
    		} else {
    			throw new IllegalStateException("Error parsing delta for audit report " + xnode);
    		}
    		
//    		System.out.println("delta xnode : " + xnode.debugDump());
    		
    		QName objectTypeXnode = deltaXnode.getParsedPrimitiveValue(ObjectDeltaType.F_OBJECT_TYPE, DOMUtil.XSD_QNAME);
    		String changeTypeXnode = deltaXnode.getParsedPrimitiveValue(ObjectDeltaType.F_CHANGE_TYPE, DOMUtil.XSD_STRING);
    		StringBuilder sb = new StringBuilder(changeTypeXnode);
    		sb.append("-");
    		sb.append(objectTypeXnode.getLocalPart());
    		
    		deltaAudit = sb.toString();
    		
    	} catch (Exception ex) {
    		return ex.getMessage();
    	}

    	return deltaAudit;
    }
   /* 
    public static ObjectQuery getObjectQuery(PrismContainer<Containerable> parameterConfiguration, String namespace, PrismContext prismContext)
    {/*
    	PrismProperty<FilterType> filterProp = getParameter(PARAMETER_QUERY_FILTER, parameterConfiguration, namespace);
    	
    	PrismContainerDefinition def = parameterConfiguration.getDefinition();
    	ObjectQuery objectQuery = new ObjectQuery();
    	if (filterProp != null)
    	{
    		try
        	{
    			ObjectFilter objectFilter = QueryConvertor.parseFilter(def, (Node)filterProp.getRealValue());
        	    objectQuery = ObjectQuery.createObjectQuery(objectFilter);
        	} catch(SchemaException ex){
        		LOGGER.error("Couldn't create object query : {}", ex.getMessage());
        		throw ex;
        	}
    	}
    *//*
    	PrismProperty<QueryType> filterProp = getParameter(PARAMETER_QUERY_FILTER, parameterConfiguration, namespace);
    	
    	ObjectQuery objectQuery = new ObjectQuery();
    	try
        {
    		Class<?> clazz = getObjectTypeClass(parameterConfiguration, namespace);
        	QueryType queryType = filterProp.getRealValue();
    		LOGGER.info(DOMUtil.serializeDOMToString(filterProp.asDomElements().get(0)));
    		
    		objectQuery = QueryConvertor.createObjectQuery(clazz, queryType, prismContext);
        } catch(Exception ex){
        	LOGGER.error("Couldn't create object query : {}", ex.getMessage());
        }
   
    	return objectQuery;
    }
    */
    public static ObjectQuery getObjectQuery(Map<?, ?> params, Class<?> clazz, PrismContext prismContext){
		ObjectQuery objectQuery = new ObjectQuery();
    	try
        {
            // TODO is this ok?
        	SearchFilterType filterType = (SearchFilterType) params.get(PARAMETER_QUERY_FILTER);
    		LOGGER.info("DataSource Query type : {}", filterType);
            if (filterType != null) {
                objectQuery.setFilter(QueryConvertor.parseFilter(filterType, (Class) clazz, prismContext));
            }
        } catch(Exception ex){
        	LOGGER.error("Couldn't create object query : {}", ex.getMessage());
        }
   
    	return objectQuery;
	}
	
  
    public static ReportType getReport(String reportOid, OperationResult parentResult, ModelService modelService) throws Exception
    {
		parentResult.addContext("reportOid", reportOid);

		if (reportOid == null) {
			throw new IllegalArgumentException("Report OID is missing in task extension");
		}
        ReportType reportType = null;
        try {
        	LOGGER.trace("get report : {}", reportOid);
        	reportType = modelService.getObject(ReportType.class, reportOid, null, null, parentResult).asObjectable();; 
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Report does not exist: {}", ex.getMessage(), ex);
			parentResult.recordFatalError("Report does not exist: " + ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Create Report: {}", ex.getMessage(), ex);
			parentResult.recordFatalError("Report: " + ex.getMessage(), ex);
			throw ex;
		}
        
        return reportType;
    }

    
    public static ReportType getReport(Map<?, ?> params, ModelService modelService, PrismContext prismContext) throws Exception
    {
    	OperationResult parentResult = getOperationResult(params);
    	String reportOid = params.get(PARAMETER_REPORT_OID).toString();
		parentResult.addContext("reportOid", reportOid);

		if (reportOid == null) {
			throw new IllegalArgumentException("Report OID is missing in datasource");
		}
        ReportType reportType = null;
        try {
        	LOGGER.trace("get report : {}", reportOid);
        	reportType = modelService.getObject(ReportType.class, reportOid, null, null, parentResult).asObjectable();; 
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Report does not exist: {}", ex.getMessage(), ex);
			parentResult.recordFatalError("Report does not exist: " + ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Create Report: {}", ex.getMessage(), ex);
			parentResult.recordFatalError("Report: " + ex.getMessage(), ex);
			throw ex;
		}
        
        return reportType;
    }
    
    public static OperationResult getOperationResult(Map<?, ?> params){
		OperationResult result = new OperationResult("DataSource - create");
    	try
        {
        	result = (OperationResult) params.get(PARAMETER_OPERATION_RESULT);
    		LOGGER.info("Datasource Operation result : {}", result);
    		
        } catch(Exception ex){
        	LOGGER.error("Couldn't create operation result : {}", ex.getMessage());
        }
   
    	return result;
	}
	

   
	
}
