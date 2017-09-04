package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRParameter;
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

	private static final long serialVersionUID = 1L;
	private String query;
	private List<JasperReportParameterDto> parameters;
	private List<JasperReportFieldDto> fields;
	private String detail;
	private JasperDesign design;

	private byte[] jasperReportXml;

	public JasperReportDto(byte[] jasperReportxml, boolean onlyForPromptingParams) {
		this.jasperReportXml = jasperReportxml;

		initFileds(onlyForPromptingParams);
	}

	public JasperReportDto(byte[] jasperReportxml) {
		this(jasperReportxml, false);
	}

	private void initFileds(boolean onlyForPromptingParams){
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
				if (onlyForPromptingParams && !parameter.isForPrompting()){
					continue;

				}
				JasperReportParameterDto p = new JasperReportParameterDto(parameter);
				parameters.add(p);
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


	public String getQuery() {
		return query;
	}

	public byte[] getTemplate(){
		try{
//			design.remadgetFields().
			design.getFieldsList().clear();
			design.getParametersList().clear();
            design.getFieldsMap().clear();
            design.getParametersMap().clear();
            for (JasperReportFieldDto field : fields) {
				if (field.isEmpty()){
					continue;
				}
				JRDesignField f = new JRDesignField();
				f.setValueClassName(field.getTypeAsString());
				f.setValueClass(Class.forName(field.getTypeAsString()));
				f.setName(field.getName());
				design.addField(f);
			}

			for (JasperReportParameterDto param : parameters) {
				if (param.isEmpty()) {
					continue;
				}
				JRDesignParameter p = new JRDesignParameter();
				p.setValueClassName(param.getTypeAsString());
				p.setValueClass(Class.forName(param.getTypeAsString()));
				p.setName(param.getName());
				p.setForPrompting(param.isForPrompting());
				p.setDescription(param.getDescription());
				p.setNestedTypeName(param.getNestedTypeAsString());
				p.setNestedType(param.getNestedType());
				p.getPropertiesMap().setBaseProperties(param.getJRProperties());
				//			p.getPropertiesMap().setProperty(propName, value);
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
			return Base64.encodeBase64(reportAsString.getBytes("UTF-8"));

		} catch (JRException | ClassNotFoundException | SchemaException | UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex.getMessage(), ex.getCause());
		}

	}


}
