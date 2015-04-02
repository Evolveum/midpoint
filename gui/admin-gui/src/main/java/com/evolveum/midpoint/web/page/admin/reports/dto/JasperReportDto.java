package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.base.JRBaseParameter;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignQuery;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

import org.apache.commons.codec.binary.Base64;


import com.evolveum.midpoint.schema.util.ReportTypeUtil;
//import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.util.exception.SchemaException;

public class JasperReportDto implements Serializable{
	
	private String query;
	private List<JasperReportParameterDto> parameters;
	private List<JasperReportFieldDto> fields;
	private String detail;
	private JasperDesign design;
	
	private byte[] jasperReportXml;
	
	public JasperReportDto(byte[] jasperReportxml) {
		this.jasperReportXml = jasperReportxml;
		
		initFileds();
	}
	
	private void initFileds(){
		if (jasperReportXml == null){
			return;
		}
		
		try {
			design = ReportTypeUtil.loadJasperDesign(jasperReportXml);
			query = design.getQuery().getText();
			
			fields = new ArrayList<JasperReportFieldDto>();
			for (JRField field : design.getFieldsList()){
				fields.add(new JasperReportFieldDto(field.getName(), field.getValueClass(), field.getValueClassName()));
			}
			
			for (JasperReportFieldDto field : fields){
				design.removeField(field.getName());
			}
			
			parameters = new ArrayList<JasperReportParameterDto>();
			for (JRParameter parameter : design.getParametersList()){
				if (parameter.isSystemDefined()){
					continue;
				}
				parameters.add(new JasperReportParameterDto(parameter.getName(), parameter.getValueClass(), parameter.getValueClassName()));
			}
			
			for (JasperReportParameterDto param : parameters){
				design.removeParameter(param.getName());
			}
			
			detail = new String(Base64.decodeBase64(jasperReportXml));
			
			
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			throw new IllegalArgumentException(e);
		}
		
		
	}
	
	public List<JasperReportParameterDto> getParameters() {
		if (parameters == null){
			parameters = new ArrayList<>();
		}
		return parameters;
	}
	
	public List<JasperReportFieldDto> getFields() {
		if (fields == null){
			fields = new ArrayList<>();
		}
		return fields;
	}
	
	public byte[] getTemplate(){
		try{
		for (JasperReportFieldDto field : fields){
			if (field.isEmpty()){
				continue;
			}
			JRDesignField f = new JRDesignField();
			f.setValueClassName(field.getTypeAsString());
			f.setValueClass(Class.forName(field.getTypeAsString()));
			f.setName(field.getName());
			design.addField(f);
		}
		
		for (JasperReportParameterDto param : parameters){
			if (param.isEmpty()){
				continue;
			}
			JRDesignParameter p = new JRDesignParameter();
			p.setValueClassName(param.getTypeAsString());
			p.setValueClass(Class.forName(param.getTypeAsString()));
			p.setName(param.getName());
			design.addParameter(p);
		}
		
		JasperDesign oldDesign = ReportTypeUtil.loadJasperDesign(jasperReportXml);
		oldDesign.getParametersList().clear();
		oldDesign.getParametersList().addAll(design.getParametersList());
		
		oldDesign.getFieldsList().clear();
		oldDesign.getFieldsList().addAll(design.getFieldsList());
		
		JRDesignQuery q = new JRDesignQuery();
		q.setLanguage("mql");
		q.setText(query);
		oldDesign.setQuery(q);
		
		String reportAsString = JRXmlWriter.writeReport(oldDesign, "UTF-8");
		return Base64.encodeBase64(reportAsString.getBytes());
		
		} catch (JRException | ClassNotFoundException | SchemaException ex){
			throw new IllegalStateException(ex.getMessage(), ex.getCause());
		}
		
	}
	
	
}
