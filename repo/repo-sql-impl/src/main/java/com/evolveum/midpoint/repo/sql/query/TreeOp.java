package com.evolveum.midpoint.repo.sql.query;

import javax.xml.namespace.QName;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;

public class TreeOp extends Op {

	private static final String QUERY_PATH = "descendants";
	private static final String CLOSURE_ALIAS = "closure";
	private static final String ANCESTOR = CLOSURE_ALIAS + ".ancestor";
	private static final String ANCESTOR_ALIAS = "anc";
	private static final String ANCESTOR_OID = ANCESTOR_ALIAS + ".oid";
	private static final String DEPTH = CLOSURE_ALIAS + ".depth";

	public TreeOp(QueryInterpreter interpreter) {
		super(interpreter);
		// TODO Auto-generated constructor stub
	}

	// TODO: implement
	public Criterion interpret(ObjectFilter filter, boolean pushNot) throws QueryException {

		

		if (!(filter instanceof OrgFilter)){
			throw new QueryException("Wrong filter type to interpret. Expected that the filter will be instance of OrgFilter but it was: "+ filter.getClass().getSimpleName());
		}
		OrgFilter org = (OrgFilter) filter;
		
		if (org.isRoot()){
//			Criteria pCriteria = getInterpreter().getCriteria(null);
			DetachedCriteria dc = DetachedCriteria.forClass(ROrgClosure.class);
			String[] strings = new String[1];
			strings[0] = "descendant.oid";
			Type[] type = new Type[1];
			type[0] = StringType.INSTANCE;
			dc.setProjection(Projections.sqlGroupProjection("descendant_oid", "descendant_oid having count(descendant_oid)=1", strings, type));
//			pCriteria.add(Subqueries.in("this.oid", dc));
			return Subqueries.propertyIn("oid", dc);
//			Query rootOrgQuery = session.createQuery("select org from ROrg as org where org.oid in (select descendant.oid from ROrgClosure group by descendant.oid having count(descendant.oid)=1)");
		}
		
		updateCriteria();
		
		if (org.getOrgRef() == null) {
			throw new QueryException("No organization reference defined in the search query.");
		}

		if (org.getOrgRef().getOid() == null) {
			throw new QueryException("No oid specified in organization reference " + org.getOrgRef().dump());
		}

		String orgRefOid = org.getOrgRef().getOid();

		Integer maxDepth = org.getMaxDepth();
		if (maxDepth != null && maxDepth < 0) {
			maxDepth = null;
		}

		if (maxDepth == null) {
			
			return Restrictions.eq(ANCESTOR_OID, orgRefOid);
		} else {
			Conjunction conjunction = Restrictions.conjunction();
			conjunction.add(Restrictions.eq(ANCESTOR_OID, orgRefOid));
			conjunction.add(Restrictions.le(DEPTH, maxDepth));
			conjunction.add(Restrictions.gt(DEPTH, 0));
			return conjunction;
//			return Restrictions.and(Restrictions.eq(ANCESTOR_OID, orgRefOid), Restrictions.le(DEPTH, maxDepth));
		}

	}

	private void updateCriteria() {
		// get root criteria
		Criteria pCriteria = getInterpreter().getCriteria(null);
		// create subcriteria on the ROgrClosure table to search through org
		// struct
		pCriteria.createCriteria(QUERY_PATH, CLOSURE_ALIAS).setFetchMode(ANCESTOR, FetchMode.DEFAULT)
				.createAlias(ANCESTOR, ANCESTOR_ALIAS).setProjection(Projections.groupProperty(CLOSURE_ALIAS+".descendant"));

	}
}
