package com.evolveum.midpoint.web.page.admin.configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.time.Duration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;


public class PageExportData extends PageAdminConfiguration {

    private static final String DOT_CLASS = PageExportData.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "searchObjects";
    private static final String OPERATION_CREATE_FILE = DOT_CLASS + "createFile";
    private static final Trace LOGGER = TraceManager.getTrace(PageExportData.class);
    private boolean downloading = false;
    private Form mainForm;

    public PageExportData() {
        
        initLayout();
    }    
    private void initLayout() {
    	mainForm = new Form("mainForm");
    	add(mainForm);
    	
    	final IModel<ObjectTypes> choice = new Model<ObjectTypes>(ObjectTypes.SYSTEM_CONFIGURATION);
    	
    	WebMarkupContainer searchPanel = new WebMarkupContainer("searchPanel");
    	searchPanel.setOutputMarkupId(true);
    	mainForm.add(searchPanel);
    	
    	WebMarkupContainer searchContainer = new WebMarkupContainer("searchContainer");
    	searchPanel.add(searchContainer);
    	IModel<String> searchNameModel = initSearch(searchContainer, choice);
    	
    	WebMarkupContainer categoryContainer = new WebMarkupContainer("categoryContainer");
    	searchPanel.add(categoryContainer);
    	initCategory(categoryContainer, choice, searchNameModel);
    	
    	Label exportedData = new Label("exportedData", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString("pageExportData.noProvidedData");
			}
		});
    	exportedData.add(new VisibleEnableBehaviour(){
    		@Override
    		public boolean isVisible() {
    			return !downloading;
    		}
    	});
    	exportedData.setOutputMarkupId(true);
    	mainForm.add(exportedData);
    	
		DownloadLink downloadLink = new DownloadLink("downloadLink", getDefaultFileModel().getObject())
				.setCacheDuration(Duration.NONE);
    	downloadLink.setOutputMarkupId(true);
    	downloadLink.add(new VisibleEnableBehaviour(){
    		@Override
    		public boolean isVisible() {
    			return downloading;
    		}
    	});
    	mainForm.add(downloadLink);
    	
    	Label linkName = new Label("linkName", new Model<String>(""));
    	downloadLink.add(linkName);
    	
    }
    
    private IModel<File> getDefaultFileModel() {
    	return new AbstractReadOnlyModel<File>(){

            @Override
            public File getObject()
            {
            	MidPointApplication application = getMidpointApplication();
    			WebApplicationConfiguration config = application.getWebApplicationConfiguration();
                File folder;
                folder = new File(config.getImportFolder());
				if (!folder.exists() || !folder.isDirectory()) {
					folder.mkdir();
				}
                return folder;
            }
        };
    }
    
    private DownloadLink getDownloadLink() {
    	return (DownloadLink) get("mainForm:downloadLink");
    }
    
    private Label getLinkName() {
    	DownloadLink link = (DownloadLink) get("mainForm:downloadLink");
    	return (Label) link.get("linkName");
    }
    
    private IModel<String> initSearch(WebMarkupContainer container, final IModel<ObjectTypes> choice) {
        final IModel<String> model = new Model<String>();
        TextField<String> searchText = new TextField<String>("searchText", model);
        container.add(searchText);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                new StringResourceModel("pageExportData.button.clear", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                model.setObject(null);
                target.appendJavaScript("init()");
                target.add(PageExportData.this.get("mainForm:searchPanel"));
                listObjectsPerformed(target, model.getObject(), choice.getObject());
            }
        };
        container.add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                new StringResourceModel("pageExportData.button.search", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
            	listObjectsPerformed(target, model.getObject(), choice.getObject());
            	target.add(getListChoice());
            	
            }
        };
        container.add(searchButton);
        return model;
    }
    
    private void initCategory(WebMarkupContainer container, final IModel<ObjectTypes> choice, final IModel<String> searchNameModel) {
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        (PageBase) PageExportData.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        IModel<List<ObjectTypes>> choiceModel = createChoiceModel(renderer);
        ListChoice listChoice = new ListChoice("choice", choice, choiceModel, renderer, choiceModel.getObject().size()) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
        listChoice.setOutputMarkupId(true);
        container.add(listChoice);
    }

    private IModel<List<ObjectTypes>> createChoiceModel(final IChoiceRenderer<ObjectTypes> renderer) {
        return new LoadableModel<List<ObjectTypes>>(false) {

            @Override
            protected List<ObjectTypes> load() {
                List<ObjectTypes> choices = new ArrayList<ObjectTypes>();
                Collections.addAll(choices, ObjectTypes.values());
                Collections.sort(choices, new Comparator<ObjectTypes>() {

                    @Override
                    public int compare(ObjectTypes o1, ObjectTypes o2) {
                        String str1 = (String) renderer.getDisplayValue(o1);
                        String str2 = (String) renderer.getDisplayValue(o2);
                        return String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
                    }
                });

                return choices;
            }
        };
    }
    
    private void listObjectsPerformed(AjaxRequestTarget target, String nameText, ObjectTypes selected) {
    	ObjectQuery query = null;
    	List<?> objects = null;
        if (StringUtils.isNotEmpty(nameText)) {
            try {
                ObjectFilter substring = SubstringFilter.createSubstring(ObjectType.class, getPrismContext(), ObjectType.F_NAME, nameText);
                query = new ObjectQuery();
                query.setFilter(substring);
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't create substring filter", ex);
                error(getString("pageExportData.message.queryException", ex.getMessage()));
                target.add(getFeedbackPanel());
            }
        }
        
        if (selected != null) {
        	Task task = createSimpleTask(OPERATION_SEARCH_OBJECT);
        	OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECT);
        	try {
        		objects = getModelService().searchObjects(selected.getClassDefinition(), query, task, result);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't search objects", ex);
                error(getString("pageExportData.message.searchException", ex.getMessage()));
                target.add(getFeedbackPanel());
			}
        }
        if(objects != null) {
        	downloading = true;
        	DownloadLink link = getDownloadLink();
        	Label linkName = getLinkName();
        	File file = createFile(target, selected.getClassDefinition(), objects).getObject();
        	link.setModelObject(file);
        	linkName.setDefaultModelObject(file.getName());
        	target.add(mainForm);
        	}
    }
    
    private ListChoice getListChoice() {
    	WebMarkupContainer panel = (WebMarkupContainer) get("mainForm:searchPanel:categoryContainer");
        return (ListChoice) panel.get("choice");
    } 
    
    private IModel<File> createFile(AjaxRequestTarget target, Class objectsClass, List<?> objects) {
    	OutputStreamWriter stream = null;
		File file = null;
		
		try {
			// Create file
			MidPointApplication application = getMidpointApplication();
			WebApplicationConfiguration config = application.getWebApplicationConfiguration();
			
			File folder = new File(config.getImportFolder());
			if (!folder.exists() || !folder.isDirectory()) {
				folder.mkdir();
			}
			
			String suffix = objectsClass.getSimpleName();
			
			if(objects.size() == 1) {
				if(objects.get(0) instanceof PrismObject) {
					PrismObject object = (PrismObject)objects.get(0);
					suffix += "_" + WebMiscUtil.getName(object);
				}
			}		
			
			file = new File(folder, "ExportedData_" + suffix + ".xml");
			
			// Check new file, delete if it already exists
			if (file.exists()) {
				file.delete();
			}
			// Save file
			Task task = createSimpleTask(OPERATION_CREATE_FILE);
			file.createNewFile();
			stream = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
			
			if(objects.isEmpty()) {
				downloading = false;
				target.add(mainForm);
			} else {
				for (Object object : objects) {
					if(!(object instanceof PrismObject)) {
						continue;
					}
					String stringObject = getPrismContext().getPrismDomProcessor().serializeObjectToString((PrismObject)object);
					stream.write(stringObject + "\n");
				}
			}
			
		} catch (Exception ex) {
			error(getString("pageExportData.message.createFileException", ex.getMessage()));
			LoggingUtils.logException(LOGGER, "Couldn't create file", ex);
			target.add(getFeedbackPanel());
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}
		return new Model<File>(file);
    }
}
