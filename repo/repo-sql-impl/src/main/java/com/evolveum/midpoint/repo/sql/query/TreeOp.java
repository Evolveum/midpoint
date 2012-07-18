package com.evolveum.midpoint.repo.sql.query;

import javax.xml.namespace.QName;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

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

	@Override
	protected QName[] canHandle() {
		return new QName[] { new QName(SchemaConstantsGenerated.NS_QUERY, "org") };
	}

	@Override
	public Criterion interpret(Element filter, boolean pushNot) throws QueryException {

		updateCriteria();

		Element orgRef = DOMUtil.getChildElement(filter, new QName(SchemaConstantsGenerated.NS_QUERY, "orgRef"));
		String orgRefOid = orgRef.getAttribute("oid");

		Element maxDepth = DOMUtil.getChildElement(filter, new QName(SchemaConstantsGenerated.NS_QUERY, "maxDepth"));
		
		if (maxDepth == null || ("unbounded").equals(maxDepth.getTextContent())) {
			return Restrictions.eq(ANCESTOR_OID, orgRefOid);
		} else {
			Integer depth = Integer.valueOf(maxDepth.getTextContent());
			return Restrictions.and(Restrictions.eq(ANCESTOR_OID, orgRefOid), Restrictions.lt(DEPTH, depth));
		}

	}

	private void updateCriteria() {
		// get root criteria
		Criteria pCriteria = getInterpreter().getCriteria(null);
		// create subcriteria on the ROgrClosure table to search through org
		// struct
		pCriteria.createCriteria(QUERY_PATH, CLOSURE_ALIAS).setFetchMode(ANCESTOR, FetchMode.DEFAULT)
				.createAlias(ANCESTOR, ANCESTOR_ALIAS);

	}
}
