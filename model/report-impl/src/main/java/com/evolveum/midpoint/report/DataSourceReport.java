package com.evolveum.midpoint.report;

import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;


public class DataSourceReport implements JRDataSource
{	
	@Autowired
	private ModelService modelService;
	
	@Autowired
    private PrismContext prismContext;
	
	private ReportType reportType;
	
	private LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	
	private List<PrismObject<ObjectType>> data;
	private OperationResult result = null;
	private OperationResult subResult = null;
	private PagingType paging = null;
	private ItemPath fieldPath = null;
	private int rowCounter = -1;
	private int pageOffset = 0;
	private int rowCount = 0;

	
	public DataSourceReport(PrismObject<ReportType> object, OperationResult result)
	{
		this.reportType = object.asObjectable();
		this.result = result;
		initialize();
	}
	
	private void initialize()
	{	
		subResult = result.createSubresult("Initialize");	
		paging = new PagingType();
		paging.setOffset(0);
		paging.setMaxSize(10);
		reportType.getQuery().setPaging(paging);
		rowCount = paging.getMaxSize();
		rowCounter = rowCount - 1;
		fieldsPair = getFieldsPair();
		subResult.computeStatus();
	}
	
	private LinkedHashMap<String, ItemPath> getFieldsPair()
	{
    	LinkedHashMap<String, ItemPath> fieldsPair = new LinkedHashMap<String, ItemPath>();
	    // pair fields in the report with fields in repo
	    for (ReportFieldConfigurationType fieldRepo : reportType.getReportField())
	   	{
	   		fieldsPair.put(fieldRepo.getNameReportField(), new XPathHolder(fieldRepo.getItemPathField()).toItemPath());
	   	}	
	   	return fieldsPair;
	}

	private <T extends ObjectType> List<PrismObject<T>> searchReportObjects() 
	{
		List<PrismObject<T>> listReportObjects = null;
		try
		{
			Class<T> clazz = (Class<T>) ObjectTypes.getObjectTypeFromTypeQName(reportType.getObjectClass()).getClassDefinition();
			ObjectQuery objectQuery = QueryConvertor.createObjectQuery(clazz, reportType.getQuery(), prismContext);
			listReportObjects = modelService.searchObjects(clazz, objectQuery, SelectorOptions.createCollection(GetOperationOptions.createRaw()), null, result);
			return listReportObjects;
		}
		catch (Exception ex) 
		{
			return null;
        }
		
	}
	@Override
	public boolean next() throws JRException {
		try 
		{	
			if (rowCounter == rowCount - 1)			 
			{ 
				subResult = result.createSubresult("Paging");				
				data = searchReportObjects();
				subResult.computeStatus();
				
				pageOffset += paging.getMaxSize();
				paging.setOffset(pageOffset);
				reportType.getQuery().setPaging(paging);
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
	
	
	
	
}
