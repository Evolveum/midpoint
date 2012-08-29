package com.evolveum.midpoint.provisioning.ucf.query;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class LogicalOperation extends Operation{

	private static final Trace LOGGER = TraceManager.getTrace(LogicalOperation.class);
	
 	public LogicalOperation(FilterInterpreter interpreter) {
		super(interpreter);
	}

	@Override
	public Filter interpret(ObjectFilter objectFilter) throws SchemaException{
		
		if (objectFilter instanceof NotFilter){
			NotFilter not = (NotFilter) objectFilter;
			if (not.getFilter() == null){
				LOGGER.debug("Not filter does not contain any condition. Skipping processing not filter.");
				return null;
			}
			
			Filter f = getInterpreter().interpret(objectFilter);
			return FilterBuilder.not(f);
		} else {
		
			NaryLogicalFilter nAry = (NaryLogicalFilter) objectFilter;
			List<? extends ObjectFilter> conditions =  nAry.getCondition();
			if (conditions == null || conditions.isEmpty()){
				LOGGER.debug("No conditions sepcified for logical filter. Skipping processing logical filter.");
				return null;
			}
			if (conditions.size() < 2){
				LOGGER.debug("Logical filter contains only one condition. Skipping processing logical filter and process simple operation of type {}.", conditions.get(0).getClass().getSimpleName());
				return getInterpreter().interpret(conditions.get(0));
			}
			
			List<Filter> filters = new ArrayList<Filter>();
			for (ObjectFilter objFilter : nAry.getCondition()){
				Filter f = getInterpreter().interpret(objFilter);
				filters.add(f);
			}
			
			Filter nAryFilter = null;
			if (filters.size() > 2) {
				if (nAry instanceof AndFilter) {
					nAryFilter = interpretAnd(filters.get(0), filters.subList(1, filters.size()));
				} else if (nAry instanceof OrFilter) {
					nAryFilter = interpretOr(filters.get(0), filters.subList(1, filters.size()));
				}
			}
			return nAryFilter;
		}
		
	}
	
	private Filter interpretAnd(Filter andF, List<Filter> filters){
		
		if (filters.size() == 0){
			return andF;
		}
		
		andF = FilterBuilder.and(andF, filters.get(0));
		andF = interpretAnd(andF, filters.subList(1, filters.size()));	
		return andF;
	}
	
	
private Filter interpretOr(Filter orF, List<Filter> filters){
		
		if (filters.size() == 0){
			return orF;
		}
		
		orF = FilterBuilder.and(orF, filters.get(0));
		orF = interpretAnd(orF, filters.subList(1, filters.size()));	
		return orF;
	}

}
