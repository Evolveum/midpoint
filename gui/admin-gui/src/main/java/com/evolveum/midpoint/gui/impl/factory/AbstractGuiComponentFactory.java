package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public abstract class AbstractGuiComponentFactory implements GuiComponentFactory {

	
	protected LookupTableType loadLookupTable(PanelContext panelCtx) {
		
		PrismReferenceValue valueEnumerationRef = panelCtx.getValueEnumerationRef();
		if (valueEnumerationRef == null) {
			return null;
		}
		
		String lookupTableUid = valueEnumerationRef.getOid();
		Task task = panelCtx.getPageBase().createSimpleTask("loadLookupTable");
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
				.createLookupTableRetrieveOptions();
		PrismObject<LookupTableType> lookupTable = WebModelServiceUtils.loadObject(LookupTableType.class,
				lookupTableUid, options, panelCtx.getPageBase(), task, result);
		if (lookupTable == null) {
			return null;
		}
		
		return lookupTable.asObjectable();
		
	}
	
	
	protected List<String> prepareAutoCompleteList(String input, LookupTableType lookupTable) {
		List<String> values = new ArrayList<>();

		if (lookupTable == null) {
			return values;
		}

		List<LookupTableRowType> rows = lookupTable.getRow();

		if (input == null || input.isEmpty()) {
			for (LookupTableRowType row : rows) {
				values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));
			}
		} else {
			for (LookupTableRowType row : rows) {
				if (WebComponentUtil.getOrigStringFromPoly(row.getLabel()) != null
						&& WebComponentUtil.getOrigStringFromPoly(row.getLabel()).toLowerCase().contains(input.toLowerCase())) {
					values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));
				}
			}
		}

		return values;
	}
			
}
