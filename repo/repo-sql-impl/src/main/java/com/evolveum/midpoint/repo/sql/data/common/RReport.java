package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExportType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROrientationType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Persister;

import javax.persistence.*;

import java.util.Collection;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_report_name", columnNames = {"name_norm"}))
@ForeignKey(name = "fk_report")
@Persister(impl = MidPointJoinedPersister.class)
public class RReport extends RObject<ReportType> {

    private RPolyString name;
    private ROrientationType orientation;
    private RExportType export;
    private Boolean parent;
    private Boolean useHibernateSession;

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
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

    @Index(name = "iReportParent")
    public Boolean getParent() {
        return parent;
    }

    public Boolean getUseHibernateSession() {
        return useHibernateSession;
    }

    public void setParent(Boolean parent) {
        this.parent = parent;
    }

    public void setUseHibernateSession(Boolean useHibernateSession) {
        this.useHibernateSession = useHibernateSession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RReport rReport = (RReport) o;

        if (name != null ? !name.equals(rReport.name) : rReport.name != null)
            return false;
        if (orientation != null ? !orientation.equals(rReport.orientation) : rReport.orientation != null)
            return false;
        if (export != null ? !export.equals(rReport.export) : rReport.export != null)
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
        result = 31 * result + (orientation != null ? orientation.hashCode() : 0);
        result = 31 * result + (export != null ? export.hashCode() : 0);
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        result = 31 * result + (useHibernateSession != null ? useHibernateSession.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(ReportType jaxb, RReport repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {

        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setOrientation(RUtil.getRepoEnumValue(jaxb.getOrientation(), ROrientationType.class));
        repo.setExport(RUtil.getRepoEnumValue(jaxb.getExport(), RExportType.class));
        repo.setParent(jaxb.isParent());
        repo.setUseHibernateSession(jaxb.isUseHibernateSession());
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