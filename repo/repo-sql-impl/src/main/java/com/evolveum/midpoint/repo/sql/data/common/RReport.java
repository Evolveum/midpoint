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
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExportType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROrientationType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.SchemaDefinitionType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_report",
        indexes = {@Index(name = "iReportName", columnNames = "name_orig")})
@ForeignKey(name = "fk_report")
public class RReport extends RObject<ReportType> {

    private RPolyString name;
    private String template;
    private String templateStyle;
    private ROrientationType orientation;
    private RExportType export;
    private String field;
    private Boolean parent;
    private String subreport;
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
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getTemplateStyle() {
        return templateStyle;
    }

    public void setTemplateStyle(String templateStyle) {
        this.templateStyle = templateStyle;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public ROrientationType getOrientation() {
        return orientation;
    }

    public void setOrientation(ROrientationType orientation) {
        this.orientation = orientation;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RExportType getExport() {
        return export;
    }

    public void setExport(RExportType export) {
        this.export = export;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @Index(name = "iReportParent")
    public Boolean getParent() {
        return parent;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getSubreport() {
        return subreport;
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

    public void setSubreport(String subreport) {
        this.subreport = subreport;
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
        if (template != null ? !template.equals(rReport.template) : rReport.template != null)
            return false;
        if (templateStyle != null ? !templateStyle.equals(rReport.templateStyle) : rReport.templateStyle != null)
            return false;
        if (orientation != null ? !orientation.equals(rReport.orientation) : rReport.orientation != null)
            return false;
        if (export != null ? !export.equals(rReport.export) : rReport.export != null)
            return false;
        if (subreport != null ? !subreport.equals(rReport.subreport) : rReport.subreport != null)
            return false;
        if (dataSource != null ? !dataSource.equals(rReport.dataSource) : rReport.dataSource != null)
            return false;
        if (field != null ? !field.equals(rReport.field) : rReport.field != null)
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
        result = 31 * result + (template != null ? template.hashCode() : 0);
        result = 31 * result + (templateStyle != null ? templateStyle.hashCode() : 0);
        result = 31 * result + (orientation != null ? orientation.hashCode() : 0);
        result = 31 * result + (export != null ? export.hashCode() : 0);
        result = 31 * result + (field != null ? field.hashCode() : 0);
        result = 31 * result + (subreport != null ? subreport.hashCode() : 0);
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
        repo.setOrientation(RUtil.getRepoEnumValue(jaxb.getOrientation(), ROrientationType.class));
        repo.setExport(RUtil.getRepoEnumValue(jaxb.getExport(), RExportType.class));
        repo.setParent(jaxb.isParent());
        repo.setUseHibernateSession(jaxb.isUseHibernateSession());
        if (jaxb.getDataSource() != null) {
            RDataSource source = new RDataSource();
            RDataSource.copyFromJAXB(jaxb.getDataSource(), source, prismContext);
            repo.setDataSource(source);
        }

        PrismObjectDefinition<ReportType> reportDef = jaxb.asPrismObject().getDefinition();
        
        try {
//            repo.setTemplate(RUtil.toRepo(reportDef, ReportType.F_TEMPLATE, jaxb.getTemplate(), prismContext));
//            repo.setTemplateStyle(RUtil.toRepo(reportDef, ReportType.F_TEMPLATE_STYLE, jaxb.getTemplateStyle(), prismContext));
        	repo.setTemplate(jaxb.getTemplate());
        	repo.setTemplateStyle(jaxb.getTemplateStyle());
            repo.setField(RUtil.toRepo(reportDef, ReportType.F_FIELD, jaxb.getField(), prismContext));

            repo.setConfiguration(RUtil.toRepo(jaxb.getConfiguration(), prismContext));
            repo.setConfigurationSchema(RUtil.toRepo(jaxb.getConfigurationSchema(), prismContext));
            repo.setSubreport(RUtil.toRepo(reportDef, ReportType.F_SUBREPORT, jaxb.getSubreport(), prismContext));
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
        if (repo.getOrientation() != null) {
            jaxb.setOrientation(repo.getOrientation().getSchemaValue());
        }
        if (repo.getExport() != null) {
            jaxb.setExport(repo.getExport().getSchemaValue());
        }

        try {
            if (StringUtils.isNotEmpty(repo.getTemplate())) {
//                jaxb.setTemplate(RUtil.toJAXB(ReportType.class, ReportType.F_TEMPLATE,
//                        repo.getTemplate(), SchemaDefinitionType.class, prismContext));
            	jaxb.setTemplate(repo.getTemplate());
            }
            if (StringUtils.isNotEmpty(repo.getTemplateStyle())) {
//                jaxb.setTemplateStyle(RUtil.toJAXB(ReportType.class, ReportType.F_TEMPLATE_STYLE,
//                        repo.getTemplateStyle(), SchemaDefinitionType.class, prismContext));
            	jaxb.setTemplateStyle(repo.getTemplateStyle());
            }
            if (StringUtils.isNotEmpty(repo.getField())) {
                List<ReportFieldConfigurationType> reportField = RUtil.toJAXB(ReportType.class, ReportType.F_FIELD, repo.getField(), List.class,
                        prismContext);
                jaxb.getField().addAll(reportField);
            }

            if (StringUtils.isNotEmpty(repo.getConfigurationSchema())) {
                jaxb.setConfigurationSchema(RUtil.toJAXB(ReportType.class, ReportType.F_CONFIGURATION_SCHEMA,
                        repo.getConfigurationSchema(), XmlSchemaType.class, prismContext));
            }

            if (StringUtils.isNotEmpty(repo.getConfiguration())) {
                jaxb.setConfiguration(RUtil.toJAXB(ReportType.class, ReportType.F_CONFIGURATION,
                        repo.getConfiguration(), ReportConfigurationType.class, prismContext));
            }

            if (StringUtils.isNotEmpty(repo.getSubreport())) {
                ReportType reportParameter = RUtil.toJAXB(ReportType.class, ReportType.F_SUBREPORT, repo.getSubreport(), ReportType.class,
                		prismContext);
                jaxb.getSubreport().addAll(reportParameter.getSubreport());
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