package com.evolveum.midpoint.report;

import java.util.LinkedHashMap;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;


public class DataSourceReport implements JRDataSource
{
	private ModelService modelService;
	private PrismContext prismContext;
	private ReportType reportType;
	
	private LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	
	
	private List<PrismObject<ObjectType>> data;
	private OperationResult result = null;
	private OperationResult subResult = null;
	private ObjectQuery query = null;
	private ObjectPaging paging = null;
	private ItemPath fieldPath = null;
	private int rowCounter = -1;
	private int pageOffset = 0;
	private int rowCount = 0;
	private Class objectClass = null;
	
	
	private LinkedHashMap<String, ItemPath> getFieldsPair(List<ReportFieldConfigurationType> fieldsRepo)
    {
    	LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
        // pair fields in the report with fields in repo
    	int i=1;
    	for (ReportFieldConfigurationType fieldRepo : fieldsRepo)
    	{
    		fieldsPair.put("Field_"+ String.valueOf(i), new XPathHolder(fieldRepo.getItemPathField()).toItemPath());
    		i++;
    	}	
    	return fieldsPair;
    }
	
	public DataSourceReport(ModelService modelService, PrismContext prismContext, ReportType reportType, OperationResult result)
	{
		this.modelService = modelService;
		this.prismContext = prismContext;
		this.reportType = reportType;
		this.result = result;
		initialize();
	}
	
	@Override
	public boolean next() throws JRException {
		try 
		{	
			if (rowCounter == rowCount - 1)			 
			{ 
				subResult = result.createSubresult("Paging");				
				data = modelService.searchObjects(objectClass, query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), null, subResult);
				subResult.computeStatus();
				
				pageOffset += paging.getMaxSize();
				paging.setOffset(pageOffset);
				query.setPaging(paging);
				rowCounter = 0;
				rowCount  = Math.min(paging.getMaxSize(), data.size());
			}
			else rowCounter++; 
				
			return !data.isEmpty();
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	@Override
	public Object getFieldValue(JRField jrField) throws JRException {
		fieldPath = null;
		if (fieldsPair.containsKey(jrField.getName()))
			fieldPath = fieldsPair.get(jrField.getName());	
		
		if (fieldPath != null)
		{
			PrismObject<ObjectType> record = data.get(rowCounter);
			PrismProperty<?> fieldValue = record.findProperty(fieldPath);
			return  fieldValue != null ? fieldValue.getRealValue().toString() : "";
		}
		else return "";
	}
	
	private void initialize()
	{
		objectClass = ObjectTypes.getObjectTypeFromTypeQName(reportType.getObjectClass()).getClassDefinition();
		
		try
		{
			query = QueryConvertor.createObjectQuery(objectClass, reportType.getQuery(), prismContext);	
		}
		catch (Exception ex)
		{
			
		}		
		paging = query.getPaging();
		rowCount = paging.getMaxSize();
		rowCounter = rowCount - 1;
		fieldsPair = getFieldsPair(reportType.getReportFields());
	}
}
