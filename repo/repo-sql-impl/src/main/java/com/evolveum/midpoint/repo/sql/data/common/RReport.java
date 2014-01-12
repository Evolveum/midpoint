package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Collection;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExportType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROrientationType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_report",
        indexes = {@Index(name = "iReportName", columnNames = "name_orig")})
@ForeignKey(name = "fk_report")
public class RReport extends RObject<ReportType> {

	private RPolyString name;
	private String reportTemplate;
	private String reportTemplateStyle;
	private ROrientationType reportOrientation;
	private RExportType reportExport;
	private String query;
	private QName objectClass;
	private String reportFields;
	private String reportParameters;

	public RPolyString getName() {
		return name;
	}

	public void setName(RPolyString name) {
		this.name = name;
	}
	
	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getReportTemplate() {
		return reportTemplate;
	}

	public void setReportTemplate(String reportTemplate) {
		this.reportTemplate = reportTemplate;
	}

	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getReportTemplateStyle() {
		return reportTemplateStyle;
	}

	public void setReportTemplateStyle(String reportTemplateStyle) {
		this.reportTemplateStyle = reportTemplateStyle;
	}
	
	@Enumerated(EnumType.ORDINAL)
	@Column(nullable = true)
	public ROrientationType getReportOrientation() {
		return reportOrientation;
	}

	public void setReportOrientation(ROrientationType reportOrientation) {
		this.reportOrientation = reportOrientation;
	}

	@Enumerated(EnumType.ORDINAL)
	@Column(nullable = true)
	public RExportType getReportExport() {
		return reportExport;
	}

	public void setReportExport(RExportType reportExport) {
		this.reportExport = reportExport;
	}

	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getQuery()
	{
		return query;	
	}
	
	public void setQuery(String query)
	{
		this.query = query;
	}
	@Columns(columns = {
			@Column(name = "class_namespace"),
	        @Column(name = "class_localPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
	})
	public QName getObjectClass()
	{
		return objectClass;
	}
	
	public void setObjectClass(QName objectClass)
	{
		this.objectClass = objectClass;
	}
	
	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getReportFields()
	{
		return reportFields;
	}
		
	public void setReportFields(String reportFields)
	{
		this.reportFields = reportFields;
	}
	
	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getReportParameters() {
		return reportParameters;
	}

	public void setReportParameters(String reportParameters) {
		this.reportParameters = reportParameters;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		RReport rReport = (RReport) o;

		if (name != null ? !name.equals(rReport.name) : rReport.name != null) 
			return false;
	    if (reportTemplate != null ? !reportTemplate.equals(rReport.reportTemplate) : rReport.reportTemplate != null) 
	    	return false;
	    if (reportTemplateStyle != null ? !reportTemplateStyle.equals(rReport.reportTemplateStyle) : rReport.reportTemplateStyle != null) 
	    	return false;
	    if (reportOrientation != null ? !reportOrientation.equals(rReport.reportOrientation) : rReport.reportOrientation != null) 
	    	return false;
	    if (reportExport != null ? !reportExport.equals(rReport.reportExport) : rReport.reportExport != null) 
	    	return false;
	    if (query != null ? !query.equals(rReport.query) : rReport.query != null) 
	    	return false;
	    if (objectClass != null ? !objectClass.equals(rReport.objectClass) : rReport.objectClass != null) 
	    	return false;      
	    if (reportFields != null ? !reportFields.equals(rReport.reportFields) : rReport.reportFields != null) 
	    	return false;
	    if (reportParameters != null ? !reportParameters.equals(rReport.reportParameters) : rReport.reportParameters != null) 
	    	return false;
	    return true;
	}
	
	@Override
	public int hashCode() {
		int result = super.hashCode();
	      	result = 31 * result + (name != null ? name.hashCode() : 0);
	        result = 31 * result + (reportTemplate != null ? reportTemplate.hashCode() : 0);
	        result = 31 * result + (reportTemplateStyle != null ? reportTemplateStyle.hashCode() : 0);
	        result = 31 * result + (reportOrientation != null ? reportOrientation.hashCode() : 0);
	        result = 31 * result + (reportExport != null ? reportExport.hashCode() : 0);
	        result = 31 * result + (query != null ? query.hashCode() : 0);
	        result = 31 * result + (objectClass != null ? objectClass.hashCode() : 0);
	        result = 31 * result + (reportFields != null ? reportFields.hashCode() : 0);
	        result = 31 * result + (reportParameters != null ? reportParameters.hashCode() : 0);
	        
	    return result;
	}
	
    public static void copyFromJAXB(ReportType jaxb, RReport repo, PrismContext prismContext) throws
    DtoTranslationException {
    	RObject.copyFromJAXB(jaxb, repo, prismContext);

    	repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
    	repo.setReportOrientation(RUtil.getRepoEnumValue(jaxb.getReportOrientation(), ROrientationType.class));
    	repo.setReportExport(RUtil.getRepoEnumValue(jaxb.getReportExport(), RExportType.class));
    	repo.setObjectClass(jaxb.getObjectClass());
    	try
    	{
    		repo.setReportTemplate(RUtil.toRepo(jaxb.getReportTemplate(), prismContext));
        	repo.setReportTemplateStyle(RUtil.toRepo(jaxb.getReportTemplateStyle(), prismContext));
    		repo.setReportFields(RUtil.toRepo(jaxb.getReportField(),prismContext));
    		repo.setReportParameters(RUtil.toRepo(jaxb.getReportParameter(), prismContext));
    		repo.setQuery(RUtil.toRepo(jaxb.getQuery(), prismContext));
    	}
    	catch (Exception ex) {
    		throw new DtoTranslationException(ex.getMessage(), ex);
    	}
    }

    public static void copyToJAXB(RReport repo, ReportType jaxb, PrismContext prismContext,
                          Collection<SelectorOptions<GetOperationOptions>> options) throws
    DtoTranslationException {
    	RObject.copyToJAXB(repo, jaxb, prismContext, options);

    	jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
    	if (repo.getReportOrientation() != null) {
             jaxb.setReportOrientation(repo.getReportOrientation().getSchemaValue());
         }
    	if (repo.getReportExport() != null) {
            jaxb.setReportExport(repo.getReportExport().getSchemaValue());
        }
    	
    	jaxb.setObjectClass(repo.getObjectClass());
    	try
    	{
    		if (StringUtils.isNotEmpty(repo.getReportTemplate())) {
    			jaxb.setReportTemplate(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_REPORT_TEMPLATE),
    					repo.getReportTemplate(), ReportTemplateType.class, prismContext));
    		}
    		if (StringUtils.isNotEmpty(repo.getReportTemplateStyle())) {
                jaxb.setReportTemplateStyle(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_REPORT_TEMPLATE_STYLE),
                        repo.getReportTemplateStyle(), ReportTemplateStyleType.class, prismContext));
    		}
    		 if (StringUtils.isNotEmpty(repo.getReportFields())) {
    			 List<ReportFieldConfigurationType> reportField = RUtil.toJAXB(ReportType.class, null, repo.getReportFields(), List.class, null,
                         prismContext);
                 jaxb.getReportField().addAll(reportField);
             }
    		 
    		 if (StringUtils.isNotEmpty(repo.getReportParameters())) {
    			 List<ReportParameterConfigurationType> reportParameter = RUtil.toJAXB(ReportType.class, null, repo.getReportParameters(), List.class, null,
                         prismContext);
                 jaxb.getReportParameter().addAll(reportParameter);
             }
    		 
    		 jaxb.setQuery(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_QUERY), repo.getQuery(), QueryType.class, prismContext));
    	}
    	catch (Exception ex)
    	{
    		throw new DtoTranslationException(ex.getMessage(), ex);
    	}
    }	 

	@Override
	public ReportType toJAXB(PrismContext prismContext,
			Collection<SelectorOptions<GetOperationOptions>> options)
			throws DtoTranslationException {
		
		 ReportType object = new ReportType();
	     RUtil.revive(object, prismContext);
	     RReport.copyToJAXB(this, object, prismContext, options);

	     return object;
	}
	
	
}