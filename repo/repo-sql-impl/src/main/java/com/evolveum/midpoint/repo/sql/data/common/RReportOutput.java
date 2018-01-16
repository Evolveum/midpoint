
package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;

import java.util.Collection;

@Entity
@ForeignKey(name = "fk_report_output")
@Persister(impl = MidPointJoinedPersister.class)
public class RReportOutput extends RObject<ReportOutputType> {

    private RPolyString nameCopy;
    private REmbeddedReference reportRef;

    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    @Embedded
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public REmbeddedReference getReportRef() {
        return reportRef;
    }

    public void setReportRef(REmbeddedReference reportRef) {
        this.reportRef = reportRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RReportOutput object = (RReportOutput) o;

        if (nameCopy != null ? !nameCopy.equals(object.nameCopy) : object.nameCopy != null)
            return false;
        if (reportRef != null ? !reportRef.equals(object.reportRef) : object.reportRef != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nameCopy != null ? nameCopy.hashCode() : 0);
        return result;
    }

    public static void copyFromJAXB(ReportOutputType jaxb, RReportOutput repo, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setReportRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getReportRef(), repositoryContext.prismContext));
    }

    @Override
    public ReportOutputType toJAXB(PrismContext prismContext,
                                   Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {

        ReportOutputType object = new ReportOutputType();
        RUtil.revive(object, prismContext);
        RReportOutput.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}