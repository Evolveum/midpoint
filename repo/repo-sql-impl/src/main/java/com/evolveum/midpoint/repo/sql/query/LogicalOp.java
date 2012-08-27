/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.UnaryLogicalFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.criterion.*;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
public class LogicalOp extends Op {

	private static enum Operation {
		AND, OR, NOT
	}

	private static final Trace LOGGER = TraceManager.getTrace(LogicalOp.class);

	public LogicalOp(QueryInterpreter interpreter) {
		super(interpreter);
	}

	@Override
	public Criterion interpret(ObjectFilter filterPart, boolean pushNot) throws QueryException {
		LOGGER.debug("Interpreting '{}', pushNot '{}'", new Object[] { filterPart.getClass().getSimpleName(), pushNot });
		// validate(filterPart);

		Operation operation = getOperationType(filterPart);

		if (!checkNaryFilterConsistence(filterPart)){
			return getInterpreter().interpret(((NaryLogicalFilter)filterPart).getCondition().get(0), pushNot);
		}
		
		switch (operation) {
		case NOT:
			boolean newPushNot = !pushNot;
			NotFilter notFilter = (NotFilter) filterPart;
			if (notFilter.getFilter() == null) {
				throw new QueryException("Logical filter (not) must have sepcified condition.");
			}
			return getInterpreter().interpret(notFilter.getFilter(), newPushNot);
		case AND:
			Conjunction conjunction = Restrictions.conjunction();
			AndFilter andFilter = (AndFilter) filterPart;
			updateJunction(andFilter.getCondition(), pushNot, conjunction);
			return conjunction;
		case OR:
			Disjunction disjunction = Restrictions.disjunction();
			OrFilter orFilter = (OrFilter) filterPart;
			updateJunction(orFilter.getCondition(), pushNot, disjunction);

			return disjunction;
		}

		throw new QueryException("Unknown state in logical filter.");
	}

	public boolean checkNaryFilterConsistence(ObjectFilter filterPart) throws QueryException {
		if (filterPart instanceof AndFilter || filterPart instanceof OrFilter) {
			NaryLogicalFilter nAryFilter = (NaryLogicalFilter) filterPart;
			if (nAryFilter.getCondition().size() == 1) {
//				throw new QueryException("Logical filter (and, or) must have specisied two conditions at least.");
				LOGGER.debug("Logical filter (and, or) must have specisied two conditions at least. Removing logical filter and processing simple condition.");
				return false;
			}
		}
		return true;
	}

	
	private Junction updateJunction(List<? extends ObjectFilter> conditions, boolean pushNot, Junction junction)
			throws QueryException {
		for (ObjectFilter condition : conditions) {
			// if
			// (SchemaConstantsGenerated.Q_TYPE.equals(DOMUtil.getQNameWithoutPrefix(element)))
			// {
			// LOGGER.debug("Unsupported (unused) element '" +
			// SchemaConstantsGenerated.Q_TYPE
			// + "' in logical filter in query.");
			// continue;
			// }
			junction.add(getInterpreter().interpret(condition, pushNot));
		}

		return junction;
	}

	private Operation getOperationType(ObjectFilter filterPart) throws QueryException {
		if (filterPart instanceof AndFilter) {
			return Operation.AND;
		} else if (filterPart instanceof OrFilter) {
			return Operation.OR;
		} else if (filterPart instanceof NotFilter) {
			return Operation.NOT;
		}

		throw new QueryException("Unknown filter type '" + filterPart.getClass().getSimpleName() + "'.");
	}


	
}
