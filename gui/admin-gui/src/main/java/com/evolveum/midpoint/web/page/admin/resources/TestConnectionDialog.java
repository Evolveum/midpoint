package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.resources.dto.TestConnectionResultDto;

public class TestConnectionDialog extends ModalWindow{

	
	private static final String TABLE_RESULT_ID = "connectionResults";
	
	public TestConnectionDialog(String id){
		super(id);
	}
	
	public TestConnectionDialog(String id, IModel<List<TestConnectionResultDto>> model) {
		super(id, model);
	    setTitle("Test Connection Result");
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(ConfirmationDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        showUnloadConfirmation(false);
        setResizable(false);
        setInitialWidth(350);
        setInitialHeight(150);
        setWidthUnit("px");
        
        initLayout(model);
	}
	
	
	private void initLayout(IModel<List<TestConnectionResultDto>> model){
		
		
		ListDataProvider<TestConnectionResultDto> listprovider = new ListDataProvider<TestConnectionResultDto>(getPage(), model);		
		List<ColumnTypeDto> columns = Arrays.asList(
				new ColumnTypeDto<String>("Operation Name", "operationName", null),
				new ColumnTypeDto("Status", "status", null),
				new ColumnTypeDto<String>("Error Message", "errorMessage", null));
		
		TablePanel table = new TablePanel(TABLE_RESULT_ID, listprovider, ColumnUtils.createColumns(columns));
		
		add(table);
		
	}

}
