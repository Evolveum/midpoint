package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Collection;
import java.util.List;

import javax.persistence.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RDataSource;
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
    private String reportFields;
    private Boolean parent;
    private String subReport;
    private Boolean useHibernateSession;
    private RDataSource dataSource;
    private String configuration;
    private String configurationSchema;

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
    public String getReportFields() {
        return reportFields;
    }

    public void setReportFields(String reportFields) {
        this.reportFields = reportFields;
    }

    @Index(name = "iReportParent")
    public Boolean getParent() {
        return parent;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getSubReport() {
        return subReport;
    }

    public Boolean getUseHibernateSession() {
        return useHibernateSession;
    }

    @Embedded
    public RDataSource getDataSource() {
        return dataSource;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getConfiguration() {
        return configuration;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getConfigurationSchema() {
        return configurationSchema;
    }

    public void setParent(Boolean parent) {
        this.parent = parent;
    }

    public void setSubReport(String subReport) {
        this.subReport = subReport;
    }

    public void setUseHibernateSession(Boolean useHibernateSession) {
        this.useHibernateSession = useHibernateSession;
    }

    public void setDataSource(RDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public void setConfigurationSchema(String configurationSchema) {
        this.configurationSchema = configurationSchema;
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
        if (subReport != null ? !subReport.equals(rReport.subReport) : rReport.subReport != null)
            return false;
        if (dataSource != null ? !dataSource.equals(rReport.dataSource) : rReport.dataSource != null)
            return false;
        if (reportFields != null ? !reportFields.equals(rReport.reportFields) : rReport.reportFields != null)
            return false;
        if (configuration != null ? !configuration.equals(rReport.configuration) : rReport.configuration != null)
            return false;
        if (configurationSchema != null ? !configurationSchema.equals(rReport.configurationSchema) : rReport.configurationSchema != null)
            return false;
        if (parent != null ? !parent.equals(rReport.parent) : rReport.parent != null)
            return false;
        if (useHibernateSession != null ? !useHibernateSession.equals(rReport.useHibernateSession) : rReport.useHibernateSession != null)
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
        result = 31 * result + (reportFields != null ? reportFields.hashCode() : 0);
        result = 31 * result + (subReport != null ? subReport.hashCode() : 0);
        result = 31 * result + (dataSource != null ? dataSource.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        result = 31 * result + (configurationSchema != null ? configurationSchema.hashCode() : 0);
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        result = 31 * result + (useHibernateSession != null ? useHibernateSession.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(ReportType jaxb, RReport repo, PrismContext prismContext)
            throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setReportOrientation(RUtil.getRepoEnumValue(jaxb.getOrientation(), ROrientationType.class));
        repo.setReportExport(RUtil.getRepoEnumValue(jaxb.getExport(), RExportType.class));
        repo.setParent(jaxb.isParent());
        repo.setUseHibernateSession(jaxb.isUseHibernateSession());
        if (jaxb.getDataSource() != null) {
            RDataSource source = new RDataSource();
            RDataSource.copyFromJAXB(jaxb.getDataSource(), source, prismContext);
            repo.setDataSource(source);
        }

        try {
            repo.setReportTemplate(RUtil.toRepo(jaxb.getTemplate(), prismContext));
            repo.setReportTemplateStyle(RUtil.toRepo(jaxb.getTemplateStyle(), prismContext));
            repo.setReportFields(RUtil.toRepo(jaxb.getReportField(), prismContext));

            repo.setConfiguration(RUtil.toRepo(jaxb.getConfiguration(), prismContext));
            repo.setConfigurationSchema(RUtil.toRepo(jaxb.getConfigurationSchema(), prismContext));
            repo.setSubReport(RUtil.toRepo(jaxb.getSubReport(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyToJAXB(RReport repo, ReportType jaxb, PrismContext prismContext,
                                  Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        RObject.copyToJAXB(repo, jaxb, prismContext, options);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setParent(repo.getParent());
        jaxb.setUseHibernateSession(repo.getUseHibernateSession());
        if (repo.getDataSource() != null) {
            jaxb.setDataSource(repo.getDataSource().toJAXB(prismContext));
        }
        if (repo.getReportOrientation() != null) {
            jaxb.setOrientation(repo.getReportOrientation().getSchemaValue());
        }
        if (repo.getReportExport() != null) {
            jaxb.setExport(repo.getReportExport().getSchemaValue());
        }

        try {
            if (StringUtils.isNotEmpty(repo.getReportTemplate())) {
                jaxb.setTemplate(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_TEMPLATE),
                        repo.getReportTemplate(), ReportTemplateType.class, prismContext));
            }
            if (StringUtils.isNotEmpty(repo.getReportTemplateStyle())) {
                jaxb.setTemplateStyle(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_TEMPLATE_STYLE),
                        repo.getReportTemplateStyle(), ReportTemplateStyleType.class, prismContext));
            }
            if (StringUtils.isNotEmpty(repo.getReportFields())) {
                List<ReportFieldConfigurationType> reportField = RUtil.toJAXB(ReportType.class, null, repo.getReportFields(), List.class, null,
                        prismContext);
                jaxb.getReportField().addAll(reportField);
            }

            if (StringUtils.isNotEmpty(repo.getConfigurationSchema())) {
                jaxb.setConfigurationSchema(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_CONFIGURATION_SCHEMA),
                        repo.getConfigurationSchema(), XmlSchemaType.class, prismContext));
            }

            if (StringUtils.isNotEmpty(repo.getConfiguration())) {
                jaxb.setConfiguration(RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_CONFIGURATION),
                        repo.getConfiguration(), ReportConfigurationType.class, prismContext));
            }

            if (StringUtils.isNotEmpty(repo.getSubReport())) {
                List<SubReportType> reportParameter = RUtil.toJAXB(ReportType.class, null, repo.getSubReport(), List.class, null,
                        prismContext);
                jaxb.getSubReport().addAll(reportParameter);
            }
        } catch (Exception ex) {
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