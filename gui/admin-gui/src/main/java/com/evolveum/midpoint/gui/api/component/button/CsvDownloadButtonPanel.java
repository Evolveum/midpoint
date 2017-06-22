package com.evolveum.midpoint.gui.api.component.button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.ExportToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AbstractAjaxDownloadBehavior;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;

public abstract class CsvDownloadButtonPanel extends BasePanel {

	private static final String ID_EXPORT_DATA = "exportData";
	
   public CsvDownloadButtonPanel(String id) {
		this(id, false);
	}
   
   //TODO: temporal HACK.. there is no time to do it properly, just reusing the current implementation
   public CsvDownloadButtonPanel(String id, boolean isAudit) {
	   super(id);
	   initLayout(isAudit);
   }

private static final long serialVersionUID = 1L;

	private void initLayout(boolean isAudit) {
    	CSVDataExporter csvDataExporter = new CSVDataExporter() {
    		private static final long serialVersionUID = 1L;
    		
    		@Override
    		public <T> void exportData(IDataProvider<T> dataProvider, List<IExportableColumn<T, ?>> columns,
    				OutputStream outputStream) throws IOException {
    			if (isAudit) {
    			        ((AuditEventRecordProvider) dataProvider).setExportSize(true);
                        super.exportData(dataProvider, columns, outputStream);
                        ((AuditEventRecordProvider) dataProvider).setExportSize(false);
                
    			} else {
    				super.exportData(dataProvider, columns, outputStream);
    			}
    			
    		}
    	};
        final AbstractAjaxDownloadBehavior ajaxDownloadBehavior = new AbstractAjaxDownloadBehavior() {
        	private static final long serialVersionUID = 1L;
    		
    		@Override
    		public IResourceStream getResourceStream() {
    			return new ExportToolbar.DataExportResourceStreamWriter(csvDataExporter, getDataTable());
    		}
    		
    		public String getFileName() {
    			return CsvDownloadButtonPanel.this.getFilename();
    		}
    	}; 
    	
        add(ajaxDownloadBehavior);
        
        AjaxIconButton exportDataLink = new AjaxIconButton(ID_EXPORT_DATA, new Model<>("fa fa-download"),
                createStringResource("CsvDownloadButtonPanel.export")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ajaxDownloadBehavior.initiate(target);
            }
        };
        add(exportDataLink);
    }
	
	protected abstract DataTable<?,?> getDataTable();
	
	protected abstract String getFilename();

}
