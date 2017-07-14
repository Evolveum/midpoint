package com.evolveum.midpoint.gui.api.component.button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
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
import sun.rmi.runtime.Log;

public abstract class CsvDownloadButtonPanel extends BasePanel {

	private static final Trace LOGGER = TraceManager.getTrace(CsvDownloadButtonPanel.class);
	private static final String DOT_CLASS = CsvDownloadButtonPanel.class.getName() + ".";
	private static final String OPERATION_GET_EXPORT_SIZE_LIMIT = DOT_CLASS + "getDefaultExportSizeLimit";

	private static final String ID_EXPORT_DATA = "exportData";
	
   public CsvDownloadButtonPanel(String id) {
	   super(id);
	   initLayout();
   }

private static final long serialVersionUID = 1L;

	private void initLayout() {
    	CSVDataExporter csvDataExporter = new CSVDataExporter() {
    		private static final long serialVersionUID = 1L;
    		
    		@Override
    		public <T> void exportData(IDataProvider<T> dataProvider, List<IExportableColumn<T, ?>> columns,
    				OutputStream outputStream) throws IOException {
				((BaseSortableDataProvider) dataProvider).setExportSize(true);
				super.exportData(dataProvider, columns, outputStream);
				((BaseSortableDataProvider) dataProvider).setExportSize(false);
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
            	long exportSizeLimit = -1;
            	try {
					AdminGuiConfigurationType adminGuiConfig = getPageBase().getModelInteractionService().getAdminGuiConfiguration(null,
							new OperationResult(OPERATION_GET_EXPORT_SIZE_LIMIT));
					if (adminGuiConfig != null && adminGuiConfig.getDefaultExportSettings() != null &&
							adminGuiConfig.getDefaultExportSettings().getSizeLimit() != null){
						exportSizeLimit = adminGuiConfig.getDefaultExportSettings().getSizeLimit();
					}
				} catch (Exception ex){
					LOGGER.error("Unable to get csv export size limit,", ex);
				}
				if (exportSizeLimit >= 0){
					ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
							createStringResource("CsvDownloadButtonPanel.confirmationMessage", exportSizeLimit)){
						private static final long serialVersionUID = 1L;

						@Override
						public void yesPerformed(AjaxRequestTarget target) {
							getPageBase().hideMainPopup(target);
							ajaxDownloadBehavior.initiate(target);
						}
					};
					getPageBase().showMainPopup(confirmationPanel, target);
				} else {
					ajaxDownloadBehavior.initiate(target);
				}
            }
        };
        add(exportDataLink);
    }
	
	protected abstract DataTable<?,?> getDataTable();
	
	protected abstract String getFilename();

}
