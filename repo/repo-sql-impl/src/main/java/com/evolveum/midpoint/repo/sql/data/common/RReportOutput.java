
package com.evolveum.midpoint.repo.sql.data.common;

import java.util.Collection;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportOutputType;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_report_output",
        indexes = {@Index(name = "iReportOutputName", columnNames = "name_orig")})
@ForeignKey(name = "fk_reportoutput")
public class RReportOutput extends RObject<ReportOutputType> {

	//private static final Trace LOGGER = TraceManager.getTrace(RReportOutput.class);
	private RPolyString name;
	private String reportFilePath;
	private REmbeddedReference reportRef;	

	public RPolyString getName() {
		return name;
	}

	public void setName(RPolyString name) {
		this.name = name;
	}
 
	public String getReportFilePath() {
		return reportFilePath;
	}

	public void setReportFilePath(String reportFilePath) {
		this.reportFilePath = reportFilePath;
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

		if (name != null ? !name.equals(object.name) : object.name != null) 
			return false;
	    if (reportFilePath != null ? !reportFilePath.equals(object.reportFilePath) : object.reportFilePath != null) 
	    	return false;
	    if (reportRef != null ? !reportRef.equals(object.reportRef) : object.reportRef != null) 
	    	return false;
	    
	    return true;
	}
	
	@Override
	public int hashCode() {
		int result = super.hashCode();
	      	result = 31 * result + (name != null ? name.hashCode() : 0);
	        result = 31 * result + (reportFilePath != null ? reportFilePath.hashCode() : 0);
	    return result;
	}
	
    public static void copyFromJAXB(ReportOutputType jaxb, RReportOutput repo, PrismContext prismContext) throws
    DtoTranslationException {
    	RObject.copyFromJAXB(jaxb, repo, prismContext);

    	repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
    	repo.setReportFilePath(jaxb.getFilePath());
        repo.setReportRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getReportRef(), prismContext));
    }

    public static void copyToJAXB(RReportOutput repo, ReportOutputType jaxb, PrismContext prismContext,
                          Collection<SelectorOptions<GetOperationOptions>> options) throws
    DtoTranslationException {
    	RObject.copyToJAXB(repo, jaxb, prismContext, options);

    	jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
    	jaxb.setFilePath(repo.getReportFilePath());
    	if (repo.getReportRef() != null) {
             jaxb.setReportRef(repo.getReportRef().toJAXB(prismContext));
         }
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