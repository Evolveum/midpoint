package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.dialog.DeleteAllDialog;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.resources.dto.TestConnectionResultDto;

public class TestConnectionDialog extends ModalWindow{

    private static final Trace LOGGER = TraceManager.getTrace(TestConnectionDialog.class);

    private static final String DOT_CLASS = TestConnectionDialog.class.getName() + ".";
 
    private static final String TABLE_RESULT_ID = "connectionResults";

    private boolean initialized;
    
	public TestConnectionDialog(String id){
		super(id);
	}
	
	public TestConnectionDialog(String id, IModel<List<TestConnectionResultDto>> model) {
		super(id, model);
	    setTitle("Test Connection Result");
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(TestConnectionDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        showUnloadConfirmation(false);
        setResizable(false);
        setInitialWidth(800);
        setInitialHeight(400);
        setWidthUnit("px");
        
        setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                return true;
            }
        });

        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            @Override
            public void onClose(AjaxRequestTarget target) {
                TestConnectionDialog.this.close(target);
            }
        });

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);

        
//        initLayout(content, model);
	}
	
	@Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized){
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()), new ListModel<TestConnectionResultDto>());
        initialized = true;
    }
	
	
	private void initLayout(WebMarkupContainer content, IModel<List<TestConnectionResultDto>> model){
		
		
		DataTable table= createTable(model);
		
		content.addOrReplace(table);
		
	}
	
	public void updateModel(IModel<List<TestConnectionResultDto>> model){
		
		initLayout((WebMarkupContainer) getContent(), model);
		
	}
	
	private DataTable createTable(IModel<List<TestConnectionResultDto>> model){
		ListDataProvider<TestConnectionResultDto> listprovider = new ListDataProvider<TestConnectionResultDto>(getPage(), model);		
		List<ColumnTypeDto> columns = Arrays.asList(
				new ColumnTypeDto<String>("Operation Name", "operationName", null),
				new ColumnTypeDto("Status", "status", null),
				new ColumnTypeDto<String>("Error Message", "errorMessage", null));
		
		DataTable table = new DataTable(TABLE_RESULT_ID, ColumnUtils.createColumns(columns), listprovider, 10);
//		TablePanel table = new TablePanel(TABLE_RESULT_ID, listprovider, );
		table.setOutputMarkupId(true);
		return table;
	}


    private PageBase getPagebase(){
        return (PageBase) getPage();
    }

    public void yesPerformed(AjaxRequestTarget target) {

    }

    public void noPerformed(AjaxRequestTarget target) {
        close(target);
    }
	
	

}
