package com.evolveum.midpoint.report;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.base.JRBasePen;
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
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXhtmlExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

@Component
public class ModelReport {
	
	@Autowired
	private ModelService modelService;
	
	@Autowired
	private PrismContext prismContext;
	
	public String addReportType(ReportType reportType, Task task, OperationResult result) 
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException
	{	
		String oid = null;
		
		try {

			ObjectDelta<ReportType> delta = ObjectDelta.createAddDelta(reportType.asPrismObject());
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
			ModelExecuteOptions options = new ModelExecuteOptions();
			options.setRaw(true);
			
			modelService.executeChanges(deltas, options, task, result);
			
			oid = delta.getOid();

			result.computeStatus();
			result.cleanupResult();

		} 
		catch (ObjectAlreadyExistsException ex) 
		{
			result.recordFatalError(ex);
			throw ex;
		} 
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (SchemaException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ExpressionEvaluationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (CommunicationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 		
		catch (ConfigurationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (PolicyViolationException ex)
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		}
		catch (SecurityViolationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		finally
		{
			RepositoryCache.exit();
		}

		return oid;
	}
	
	public ReportType getReportType(String oid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, 
			CommunicationException, ConfigurationException
	{  
		ReportType reportType = null;
		try
		{
			reportType = modelService.getObject(ReportType.class, oid, null, task, result).asObjectable();
		}
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (SchemaException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (CommunicationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ConfigurationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (SecurityViolationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		}
		finally 
		{
			RepositoryCache.exit();
		}
		
		return reportType;
	}
	
	
	public void deleteReportType(String oid, Task task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException

	{	
		try 
		{
			ObjectDelta<ReportType> delta = ObjectDelta.createDeleteDelta(ReportType.class, oid, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
			ModelExecuteOptions options = new ModelExecuteOptions();
			options.setRaw(true);
			
			modelService.executeChanges(deltas, options, task, result);
		
			result.computeStatus();

		} 
		catch (ObjectAlreadyExistsException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (SchemaException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ExpressionEvaluationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (CommunicationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 		
		catch (ConfigurationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (PolicyViolationException ex)
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		}
		catch (SecurityViolationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		finally
		{
			RepositoryCache.exit();
		}
		
	}
	
	public void modifyReportType(String oid, ReportType reportType, Task task, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
			PolicyViolationException, SecurityViolationException

	{
		try 
		{	 
			Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
			ReportType reportTypeOld = getReportType(oid, task, result);
			
			for(int i=0; i<reportTypeOld.asPrismContainerValue().getItems().size(); i++)
			{
				PrismProperty<?> obj = (PrismProperty<?>)reportType.asPrismContainerValue().getItems().get(i);
				PrismProperty<?> objOld = (PrismProperty<?>)reportTypeOld.asPrismContainerValue().getItems().get(i);
				
				if (!obj.equalsRealValue(objOld))
				{	
					ObjectDelta<ReportType> userDelta = ObjectDelta.createModificationReplaceProperty(ReportType.class,
		        		oid, obj.getPath(), prismContext, obj.getRealValue());
					deltas.add(userDelta);
				}

			}
			modelService.executeChanges(deltas, null, task, result);

            result.computeStatus();
		}
		catch (ObjectAlreadyExistsException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (SchemaException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ExpressionEvaluationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (CommunicationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 		
		catch (ConfigurationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (PolicyViolationException ex)
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		}
		catch (SecurityViolationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		finally
		{
			RepositoryCache.exit();
		}
		
	}
	
	public <T extends ObjectType> List<PrismObject<T>> searchReportObjects(ReportType reportType, OperationResult result) 
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, 
			CommunicationException, ConfigurationException
	{
		List<PrismObject<T>> listReportObjects = null;
		try
		{
			Class<T> clazz = (Class<T>) ObjectTypes.getObjectTypeFromTypeQName(reportType.getObjectClass()).getClassDefinition();
			ObjectQuery objectQuery = QueryConvertor.createObjectQuery(clazz, reportType.getQuery(), prismContext);
			listReportObjects = modelService.searchObjects(clazz, objectQuery, SelectorOptions.createCollection(GetOperationOptions.createRaw()), null, result);
		}
		catch (SchemaException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
        }
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
        }
		catch (SecurityViolationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
        }
		catch (CommunicationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
        }
		catch (ConfigurationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
        }
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
        }
		return listReportObjects;
	}
	
	public List<ReportType> searchReportTypes(ObjectQuery query, OperationResult parentResult) 
	{
		// TODO get ReportType with input OID from database  
		List<ReportType> listReportType = new ArrayList<ReportType>();
				
		return listReportType;
	}
	
	public Map<String, Object> getReportParams(ReportType reportType)
	{
	 	Map<String, Object> params = new HashMap<String, Object>();
	  	for(ReportParameterConfigurationType parameterRepo : reportType.getReportParameters())
		{
    		params.put(parameterRepo.getNameParameter(), parameterRepo.getValueParameter());			
    	}
	    	
	    return params;
	}
	
	public LinkedHashMap<String, ItemPath> getFieldsPair(ReportType reportType)
	{
		LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	    // pair fields in the report with fields in repo
	    for (ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
	   	{
	   		fieldsPair.put(fieldRepo.getNameReportField(), new XPathHolder(fieldRepo.getItemPathField()).toItemPath());
	   	}	
	   	return fieldsPair;
	}
	
	private Class<?> getClassType(QName classtType)
  	{
	   	// TODO compare with ordinal types        
	    return java.lang.String.class;
	}
	
	public QName getObjectClassForResource(String resourceOid, Task task, OperationResult result) 
		throws Exception
	{
        try {
            PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class,
                    resourceOid, null, task, result);
            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                    LayerType.PRESENTATION, prismContext);

            RefinedObjectClassDefinition def = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
            return def.getTypeName();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get default object class qname for resource oid '"
                    + resourceOid + "'.", ex);
            throw ex;
        }
    }
	
	public ReportType createReportType(JasperDesign jasperDesign) throws JRException
	{
	   	// TODO generate object ReportType from JasperDesign object  
		ReportType reportType = new ReportType();
		
		return reportType;
	}
	
	
	public JasperDesign createJasperDesign(ReportType reportType) throws JRException
	{
		//JasperDesign
		JasperDesign jasperDesign = new JasperDesign();
		jasperDesign.setName("reportDataSource");
		
		switch (reportType.getReportOrientation())
		{
			case LANDSCAPE :
			default:
			{
				jasperDesign.setOrientation(OrientationEnum.LANDSCAPE);
				jasperDesign.setPageWidth(842);
				jasperDesign.setPageHeight(595);
				jasperDesign.setColumnWidth(802);
			}
			break;
			case PORTRAIT :
			{
				jasperDesign.setOrientation(OrientationEnum.LANDSCAPE);
				jasperDesign.setPageWidth(595);
				jasperDesign.setPageHeight(842);
				jasperDesign.setColumnWidth(555);
			}
			break;
		}
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(20);
		jasperDesign.setBottomMargin(20);
		
	
		//Templates
		JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{BaseTemplateStyles}"));
		jasperDesign.addTemplate(templateStyle);
		
		//Parameters
		//two parameters are there every time - template styles and logo image and will be excluded
		List<ReportParameterConfigurationType> parameters = new ArrayList<ReportParameterConfigurationType>();
		parameters.addAll(reportType.getReportParameters());
		
		for(ReportParameterConfigurationType parameterRepo : parameters)
		{
			JRDesignParameter parameter = new JRDesignParameter();
			parameter.setName(parameterRepo.getNameParameter());
			parameter.setValueClass(getClassType(parameterRepo.getClassTypeParameter()));
			jasperDesign.addParameter(parameter);
			/*if(parameterRepo.getNameParameter().equals("LOGO_PATH") || parameterRepo.getNameParameter().equals("BaseTemplateStyles")) 
			{
				parameters.remove(parameterRepo);
			}
			*/
		}
				
		//Fields
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			JRDesignField field = new JRDesignField();
			field.setName(fieldRepo.getNameReportField());
			field.setValueClass(getClassType(fieldRepo.getClassTypeField()));	
			jasperDesign.addField(field);
		}

		//Background
		JRDesignBand bandBackground = new JRDesignBand();
		bandBackground.setHeight(30);
		bandBackground.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setBackground(bandBackground);
		
		//Title
		//band size depends on the number of parameters
		//two pre-defined parameters were excluded
		int secondColumn = Math.round(jasperDesign.getColumnWidth()/2 - 1);
		JRDesignBand titleBand = new JRDesignBand();
		int height = 70 + Math.max(40, parameters.size()*20);
		titleBand.setHeight(height);
		titleBand.setSplitType(SplitTypeEnum.STRETCH);
		
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(0);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(70);
		frame.setStyleNameReference("Title");
		titleBand.addElement(frame);
		
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(10);
		staticText.setY(15);
		staticText.setWidth(266);
		staticText.setHeight(40);
		staticText.setStyleNameReference("Title");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("DataSource Report");
		frame.addElement(staticText);
		
		JRDesignImage image = new JRDesignImage(new JRDesignStyle().getDefaultStyleProvider());
		image.setX(589);
		image.setY(15);
		image.setWidth(203);
		image.setHeight(40);
		image.setStyleNameReference("Title");
		image.setExpression(new JRDesignExpression("$P{LOGO_PATH}"));
		frame.addElement(image);
		
		 
		staticText = new JRDesignStaticText();
		staticText.setX(secondColumn);
		staticText.setY(70);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Report generated on:");
		titleBand.addElement(staticText);
		
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(secondColumn + 150);
		textField.setY(70);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		titleBand.addElement(textField);
		
		staticText = new JRDesignStaticText();
		staticText.setX(secondColumn);
		staticText.setY(90);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Number of records:");
		titleBand.addElement(staticText);
		
		textField = new JRDesignTextField();
		textField.setX(secondColumn + 150);
		textField.setY(90);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setExpression(new JRDesignExpression("$V{REPORT_COUNT}"));
		titleBand.addElement(textField);
		
		parameters.remove(0);
		parameters.remove(0);
		int y = 70;
		for(ReportParameterConfigurationType parameterRepo : parameters)
		{
			staticText = new JRDesignStaticText();
			staticText.setX(2);
			staticText.setY(y);
			staticText.setWidth(150);
			staticText.setHeight(20);
			staticText.setStyleNameReference("Page header");
			staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			staticText.setText(parameterRepo.getDescriptionParameter() + ":");
			titleBand.addElement(staticText);
			
			textField = new JRDesignTextField();
			textField.setX(160);
			textField.setY(y);
			textField.setWidth(240);
			textField.setHeight(20);
			textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
			textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			textField.setStyleNameReference("Page header");
			textField.setBold(false);
			textField.setExpression(new JRDesignExpression("$P{"+ parameterRepo.getNameParameter() + "}"));
			titleBand.addElement(textField);
			
			y = y + 20;
		}
		
		jasperDesign.setTitle(titleBand);
	
		//Page header
		JRDesignBand pageHeaderBand = new JRDesignBand();
		pageHeaderBand.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setPageHeader(pageHeaderBand);
		
		//Column header
		JRDesignBand columnHeaderBand = new JRDesignBand();
		columnHeaderBand.setSplitType(SplitTypeEnum.STRETCH);
		columnHeaderBand.setHeight(24);

		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(5);
		//frame.setWidth(799);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(19);
		frame.setStyleNameReference("Column header");
		//frame.setRemoveLineWhenBlank(true);
		
		int x = 0;
		int width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			staticText = new JRDesignStaticText();
			staticText.setX(x);
			staticText.setY(0);
			width =  Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField()));
			staticText.setWidth(width);
			staticText.setHeight(18);
			staticText.setStyleNameReference("Column header");
			staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			staticText.setText(fieldRepo.getNameHeaderField());
			frame.addElement(staticText);
			x = x + width;
		}
		
		columnHeaderBand.addElement(frame);
		jasperDesign.setColumnHeader(columnHeaderBand);
		
		//Detail
		JRDesignBand detailBand = new JRDesignBand();
		detailBand.setHeight(20);
		detailBand.setSplitType(SplitTypeEnum.PREVENT);
		
		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(1);
		//frame.setWidth(799);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(19);
		frame.setStyleNameReference("Detail");
		
		x = 0;
		width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			textField = new JRDesignTextField();
			textField.setX(x);
			textField.setY(1);
			width = Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField())); 
			textField.setWidth(width);
			textField.setHeight(18);
			textField.setStretchWithOverflow(true);
			textField.setBlankWhenNull(true);
			textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			textField.setStyleNameReference("Detail");
			textField.setExpression(new JRDesignExpression("$F{" + fieldRepo.getNameReportField() + "}"));
			frame.addElement(textField);
			x = x + width;
		}
		
		JRDesignLine line = new JRDesignLine();
		line.setX(0);
		line.setY(3);
		//line.setWidth(800);
		line.setWidth(jasperDesign.getColumnWidth()-2);
		line.setHeight(1);
		line.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM);
		JRBasePen pen = new JRBasePen(line);
		pen.setLineWidth((float) 0.5);
		pen.setLineColor(Color.decode("#999999"));
		frame.addElement(line);
		
		detailBand.addElement(frame);
		((JRDesignSection)jasperDesign.getDetailSection()).addBand(detailBand);		
		
		//Column footer
		JRDesignBand columnFooterBand = new JRDesignBand();
		columnFooterBand.setHeight(7);
		columnFooterBand.setSplitType(SplitTypeEnum.STRETCH);		
		
		line = new JRDesignLine();
		line.setX(0);
		line.setY(3);
		//line.setWidth(800);
		line.setWidth(jasperDesign.getColumnWidth()-2);
		line.setHeight(1);
		line.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM);
		pen = new JRBasePen(line);
		pen.setLineWidth((float) 0.5);
		pen.setLineColor(Color.decode("#999999"));
		columnFooterBand.addElement(line);
		jasperDesign.setColumnFooter(columnFooterBand);

		//Page footer
		JRDesignBand pageFooterBand = new JRDesignBand();
		pageFooterBand.setHeight(32);
		pageFooterBand.setSplitType(SplitTypeEnum.STRETCH);
		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(1);
		//frame.setWidth(800);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(24);
		frame.setStyleNameReference("Page footer");
		frame.setMode(ModeEnum.TRANSPARENT);
		
		textField = new JRDesignTextField();
		textField.setX(2);
		textField.setY(1);
		textField.setWidth(197);
		textField.setHeight(20);
		textField.setPattern("EEEEE dd MMMMM yyyy");
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		frame.addElement(textField);
		
		textField = new JRDesignTextField();
		textField.setX(680);
		textField.setY(1);
		textField.setWidth(80);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		//textField.setExpression(new JRDesignExpression("''Page '' + $V{PAGE_NUMBER} + '' of''"));
		textField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
		frame.addElement(textField);
		
		textField = new JRDesignTextField();
		textField.setX(760);
		textField.setY(1);
		textField.setWidth(40);
		textField.setHeight(20);
		textField.setEvaluationTime(EvaluationTimeEnum.REPORT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		//textField.setExpression(new JRDesignExpression("'' '' + $V{PAGE_NUMBER}"));
		textField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
		frame.addElement(textField);
		
		pageFooterBand.addElement(frame);
		
		jasperDesign.setPageFooter(pageFooterBand);

		return jasperDesign;
	}
	//export report
    private void pdf(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(jasperPrint, output + ".pdf");
    }
    
    private static void csv(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".csv");
		
		JRCsvExporter exporter = new JRCsvExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void xml(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToXmlFile(jasperPrint, output + ".xml", false);
    }
    
    private void xmlEmbed(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToXmlFile(jasperPrint, output + "_embed.xml", true);
    }
    
    private void html(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToHtmlFile(jasperPrint, output + ".html");
    }
    
    private void rtf(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".rtf");
		
		JRRtfExporter exporter = new JRRtfExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void xls(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".xls");
		
		JRXlsExporter exporter = new JRXlsExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
		
		exporter.exportReport();
    }
    
    private void odt(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".odt");
		
		JROdtExporter exporter = new JROdtExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void ods(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".ods");
		
		JROdsExporter exporter = new JROdsExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
		
		exporter.exportReport();
    }
    
    private void docx(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".docx");
		
		JRDocxExporter exporter = new JRDocxExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void xlsx(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".xlsx");
		
		JRXlsxExporter exporter = new JRXlsxExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
		
		exporter.exportReport();
    }
    
    private void pptx(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".pptx");
		
		JRPptxExporter exporter = new JRPptxExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());

		exporter.exportReport();
    }
    
    private void xhtml(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output + ".x.html");
		
		JRXhtmlExporter exporter = new JRXhtmlExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();

    }
    
    private void jxl(JasperPrint jasperPrint, String output) throws JRException
    {
		/*File destFile = new File(output + ".jxl.xls");

		JExcelApiExporter exporter = new JExcelApiExporter();

		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);

		exporter.exportReport();*/
    }
    
    
    // generate report - export
    public void generateReport(ReportType reportType, JasperPrint jasperPrint, String output) throws JRException
    {
    	  switch (reportType.getReportExport())
          {
          	case PDF : pdf(jasperPrint, output);
          		break;
          	case CSV : csv(jasperPrint, output);
      			break;
          	case XML : xml(jasperPrint, output);
      			break;
          	case XML_EMBED : xmlEmbed(jasperPrint, output);
          		break;
          	case HTML : html(jasperPrint, output);
  				break;
          	case RTF : rtf(jasperPrint, output);
  				break;
          	case XLS : xls(jasperPrint, output);
  				break;
          	case ODT : odt(jasperPrint, output);
  				break;
          	case ODS : ods(jasperPrint, output);
  				break;
          	case DOCX : docx(jasperPrint, output);
  				break;
          	case XLSX : xlsx(jasperPrint, output);
  				break;
          	case PPTX : pptx(jasperPrint, output);
  				break;
          	case XHTML : xhtml(jasperPrint, output);
  				break;
          	case JXL : jxl(jasperPrint, output);
          		break; 	
			default:
				break;
          }
    }

}
