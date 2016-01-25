package com.evolveum.midpoint.web.component.data.column;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceConfigurationDto;

public class ColumnUtils {

	public static List<IColumn> createColumns(List<ColumnTypeDto> columns){
		List<IColumn> tableColumns = new ArrayList<IColumn>();
		for (ColumnTypeDto column : columns) {
			// if (column.isMultivalue()) {
			// PropertyColumn tableColumn = new PropertyColumn(displayModel,
			// propertyExpression)
			// } else {
			PropertyColumn tableColumn = new PropertyColumn(createStringResource(column.getColumnName()),
					column.getSortableColumn(), column.getColumnValue());
			tableColumns.add(tableColumn);
			// }
		}
		return tableColumns;
	}
	
	public static StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return new StringResourceModel(resourceKey).setModel(new Model<String>())
    			.setDefaultValue(resourceKey)
    			.setParameters(objects);

//return StringResourceModelMigration.of(resourceKey, this, new Model<String>(), resourceKey, objects);
}
}
