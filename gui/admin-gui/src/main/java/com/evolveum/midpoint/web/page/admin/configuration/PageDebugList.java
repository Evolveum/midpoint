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

package com.evolveum.midpoint.web.page.admin.configuration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.file.Files;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ButtonColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.option.OptionContent;
import com.evolveum.midpoint.web.component.option.OptionItem;
import com.evolveum.midpoint.web.component.option.OptionPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;

/**
 * @author lazyman
 * @author mserbak
 */

public class PageDebugList extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugList.class);
    private static final String DOT_CLASS = PageDebugList.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "loadObjects";
	private static final String OPERATION_CREATE_DOWNLOAD_FILE = DOT_CLASS + "createDownloadFile";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ZIP_CHECK = "zipCheck";

    private boolean deleteSelected; //todo what is this used for?
    private IModel<ObjectTypes> choice = null;
    private ObjectType object = null; //todo what is this used for?

    public PageDebugList() {
        initLayout();
    }

    private void initLayout() {
    	//confirm delete
    	add(new ConfirmationDialog("confirmDeletePopup", createStringResource("pageDebugList.dialog.title.confirmDelete"),
                createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);
                //todo wtf
                if(deleteSelected){
                	deleteSelected = false;
                	deleteSelectedConfirmedPerformed(target);
                } else {
                	deleteObjectConfirmedPerformed(target);
                }
                
            }
        });
    	
        //listed type
        //todo wtf is this? why is prism object definition in session, where is this saved?
    	IModel<ObjectTypes> sessionChoice = null;
    	PrismObjectDefinition selectedCategory = (PrismObjectDefinition)getSession().getAttribute("category");
    	if(selectedCategory != null) {
    		sessionChoice = new Model<ObjectTypes>(ObjectTypes.getObjectTypeFromTypeQName(selectedCategory.getTypeName()));
    	} else {
    		sessionChoice = new Model<ObjectTypes>(ObjectTypes.SYSTEM_CONFIGURATION);
    	}
    	final IModel<ObjectTypes> choice = sessionChoice;

        List<IColumn<? extends ObjectType>> columns = new ArrayList<IColumn<? extends ObjectType>>();

        IColumn column = new CheckBoxHeaderColumn<ObjectType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<? extends ObjectType>>(createStringResource("pageDebugList.name"), "name", "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                objectEditPerformed(target, object.getOid());
            }
        };
        columns.add(column);

        column = new ButtonColumn<SelectableBean<? extends ObjectType>>(createStringResource("pageDebugList.operation"),
                createStringResource("pageDebugList.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<? extends ObjectType>> rowModel) {
                ObjectType object = rowModel.getObject().getValue();
                deleteObjectPerformed(target, choice, object);
            }
        };
        columns.add(column);

        final Form main = new Form(ID_MAIN_FORM);
        add(main);
        
        OptionPanel option = new OptionPanel("option", createStringResource("pageDebugList.optionsTitle"), getPage(), false);
        option.setOutputMarkupId(true);
        main.add(option);

        OptionItem item = new OptionItem("search", createStringResource("pageDebugList.search"));
        option.getBodyContainer().add(item);
        IModel<String> searchNameModel = initSearch(item, choice);

        item = new OptionItem("category", createStringResource("pageDebugList.selectType"));
        option.getBodyContainer().add(item);
        initCategory(item, choice, searchNameModel);

        OptionContent content = new OptionContent("optionContent");
        main.add(content);
        
        Class provider = selectedCategory == null ? SystemConfigurationType.class : selectedCategory.getCompileTimeClass();
        TablePanel table = new TablePanel("table", new RepositoryObjectDataProvider(PageDebugList.this,
        		provider), columns);
        table.setOutputMarkupId(true);
        content.getBodyContainer().add(table);

        AjaxLinkButton delete = new AjaxLinkButton("deleteSelected", ButtonType.NEGATIVE,
                createStringResource("pageDebugList.button.deleteSelected")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteSelectedPerformed(target, choice);
            }
        };
        main.add(delete);
        
        final AjaxDownloadBehaviorFromFile ajaxDownloadBehavior = new AjaxDownloadBehaviorFromFile(true) {

            @Override
            protected File initFile() {
            	return initDownloadFile(choice);
            }
        };
        main.add(ajaxDownloadBehavior);
        
        
		AjaxLinkButton export = new AjaxLinkButton("exportAll",
				createStringResource("pageDebugList.button.exportAll")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
                ajaxDownloadBehavior.initiate(target);
			}
		};
        main.add(export);

        //todo this method has 150 lines, which is way to much. break it into smaller chunks
        AjaxCheckBox zipCheck = new AjaxCheckBox(ID_ZIP_CHECK, new Model<Boolean>(false)){

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
			}
        };
        main.add(zipCheck);
    }

    //todo remove argument, choice model all over the place (at least two of them, again two variables...)
    private File initDownloadFile(IModel<ObjectTypes> choice) {
        OperationResult result = new OperationResult(OPERATION_CREATE_DOWNLOAD_FILE);
        MidPointApplication application = getMidpointApplication();
        WebApplicationConfiguration config = application.getWebApplicationConfiguration();
        File folder = new File(config.getExportFolder());
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdir();
        }

        String suffix = choice.getObject().getClassDefinition().getSimpleName() + "_"
                + System.currentTimeMillis();
        File file = new File(folder, "ExportedData_" + suffix + ".xml");

        try {
            result.recordSuccess();
            if(hasToZip()) {
                file = createZipForDownload(file, folder, suffix, result);
            } else {
                file.createNewFile();
                createXmlForDownload(file, result);
            }
            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't init download link", ex);
            result.recordFatalError("Couldn't init download link", ex);
        }

        if(!result.isSuccess()) {
            showResultInSession(result);
            getSession().error(getString("pageDebugList.message.createFileException"));
            Files.remove(file);

            setResponsePage(PageDebugList.class);
        }

        return file;
    }

    private boolean hasToZip() {
        AjaxCheckBox zipCheck = (AjaxCheckBox) get(ID_MAIN_FORM + ":" + ID_ZIP_CHECK);
        return zipCheck.getModelObject();
    }

    private IModel<String> initSearch(OptionItem item, final IModel<ObjectTypes> choice) {
        final IModel<String> model = new Model<String>();
        TextField<String> search = new TextField<String>("searchText", model);
        item.add(search);

        AjaxSubmitLinkButton clearButton = new AjaxSubmitLinkButton("clearButton",
                new StringResourceModel("pageDebugList.button.clear", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                model.setObject(null);
                target.appendJavaScript("init()");
                target.add(PageDebugList.this.get(ID_MAIN_FORM + ":option"));
                listObjectsPerformed(target, model.getObject(), choice.getObject());
            }
        };
        item.add(clearButton);

        AjaxSubmitLinkButton searchButton = new AjaxSubmitLinkButton("searchButton",
                new StringResourceModel("pageDebugList.button.search", this, null)) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                PageBase page = (PageBase) getPage();
                target.add(page.getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                listObjectsPerformed(target, model.getObject(), choice.getObject());
            }
        };
        item.add(searchButton);

        return model;
    }

    private void initCategory(OptionItem item, final IModel<ObjectTypes> choice, final IModel<String> searchNameModel) {
        IChoiceRenderer<ObjectTypes> renderer = new IChoiceRenderer<ObjectTypes>() {

            @Override
            public Object getDisplayValue(ObjectTypes object) {
                return new StringResourceModel(object.getLocalizationKey(), PageDebugList.this, null).getString();
            }

            @Override
            public String getIdValue(ObjectTypes object, int index) {
                return object.getClassDefinition().getSimpleName();
            }
        };

        IModel<List<ObjectTypes>> choiceModel = createChoiceModel(renderer);
        final ListChoice listChoice = new ListChoice("choice", choice, choiceModel, renderer, choiceModel.getObject().size()) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
        listChoice.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(listChoice);
                listObjectsPerformed(target, searchNameModel.getObject(), choice.getObject());
            }
        });
        item.getBodyContainer().add(listChoice);
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

    private TablePanel getListTable() {
        OptionContent content = (OptionContent) get(ID_MAIN_FORM + ":optionContent");
        return (TablePanel) content.getBodyContainer().get("table");
    }

    private void listObjectsPerformed(AjaxRequestTarget target, String nameText, ObjectTypes selected) {
        RepositoryObjectDataProvider provider = getTableDataProvider();
        if (StringUtils.isNotEmpty(nameText)) {
            try {
				ObjectFilter substring = SubstringFilter.createSubstring(ObjectType.class, getPrismContext(),
						ObjectType.F_NAME, nameText);
				ObjectQuery query = new ObjectQuery();
                query.setFilter(substring);
                provider.setQuery(query);
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't create substring filter", ex);
                error(getString("pageDebugList.message.queryException", ex.getMessage()));
                target.add(getFeedbackPanel());
            }
        } else {
            provider.setQuery(null);
        }

        if (selected != null) {
            provider.setType(selected.getClassDefinition());
        }
        
        TablePanel table = getListTable();
        target.add(table);
    }

    private void objectEditPerformed(AjaxRequestTarget target, String oid) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, oid);
        setResponsePage(PageDebugView.class, parameters);
    }

    private RepositoryObjectDataProvider<ObjectType> getTableDataProvider() {
        TablePanel tablePanel = getListTable();
        DataTable table = tablePanel.getDataTable();
        return (RepositoryObjectDataProvider<ObjectType>) table.getDataProvider();
    }
    
    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
				if (deleteSelected) {
					List<SelectableBean<ObjectType>> selectedList = WebMiscUtil
							.getSelectedData(getListTable());
					
					if (selectedList.size() > 1) {
						return createStringResource("pageDebugList.message.deleteSelectedConfirm",
								selectedList.size()).getString();
					}

					SelectableBean<ObjectType> selectedItem = selectedList.get(0);
					return createStringResource("pageDebugList.message.deleteObjectConfirm",
							selectedItem.getValue().getName().getOrig()).getString();
				}
				
				return createStringResource("pageDebugList.message.deleteObjectConfirm" ,object.getName().getOrig())
						.getString();
            }
        };
    }
    
    private void deleteSelectedConfirmedPerformed(AjaxRequestTarget target){
        ObjectTypes type = choice.getObject();

        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);
        List<SelectableBean<ObjectType>> beans = WebMiscUtil.getSelectedData(getListTable());
        for (SelectableBean<ObjectType> bean : beans) {
            ObjectType object = bean.getValue();
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            try {
                ObjectDelta delta = ObjectDelta.createDeleteDelta(type.getClassDefinition(), object.getOid(), getPrismContext());

                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta),
                        ModelExecuteOptions.createRaw(),
                        createSimpleTask(OPERATION_DELETE_OBJECT), subResult);
                subResult.recordSuccess();
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't delete objects.", ex);
                LoggingUtils.logException(LOGGER, "Couldn't delete objects", ex);
            }
        }
        result.recomputeStatus();

        RepositoryObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }
    
    private void deleteObjectConfirmedPerformed(AjaxRequestTarget target){
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
        try {
            ObjectTypes type = choice.getObject();
            ObjectDelta delta = ObjectDelta.createDeleteDelta(type.getClassDefinition(), object.getOid(), getPrismContext());

            getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta),
            		ModelExecuteOptions.createRaw(),
                    createSimpleTask(OPERATION_DELETE_OBJECT), result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't delete object '" + object.getName() + "'.", ex);
        }

        RepositoryObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getListTable());
        target.add(getFeedbackPanel());
    }
    
    private void deleteSelectedPerformed(AjaxRequestTarget target, IModel<ObjectTypes> choice) {
    	List<SelectableBean<ObjectType>> selected = WebMiscUtil.getSelectedData(getListTable());
        if (selected.isEmpty()) {
            warn(getString("pageDebugList.message.nothingSelected"));
            target.add(getFeedbackPanel());
            return;
        }
        
    	ModalWindow dialog = (ModalWindow) get("confirmDeletePopup");
    	deleteSelected = true;
    	this.choice = choice;
        dialog.show(target);
    }

    private void deleteObjectPerformed(AjaxRequestTarget target, IModel<ObjectTypes> choice, ObjectType object) {
    	ModalWindow dialog = (ModalWindow) get("confirmDeletePopup");
    	this.choice = choice;
    	this.object = object;
        dialog.show(target);
    }
    
    private void createXmlForDownload(File file, OperationResult result) {
    	OutputStreamWriter stream = null;
		try {
			LOGGER.trace("creating xml file {}", file.getName());
			stream = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
            List<PrismObject> objects = getExportedObjects();
            String stringObject;
            stream.write(createHeaderForXml());
            for (PrismObject object : objects) {
                //todo this will create file that doesn't contain all objects, operation result wont show it if it happened in the middle of file
                try {
                    stringObject = getPrismContext().getPrismDomProcessor().serializeObjectToString(object);
                    stream.write("\t" + stringObject + "\n");
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Failed to parse objects to string for xml. Reason:", ex);
                    result.recordFatalError("Failed to parse objects to string for xml. Reason:", ex);
                }
            }
            stream.write("</objects>");
			LOGGER.debug("created xml file {}", file.getName());
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't create xml file", ex);
			result.recordFatalError("Couldn't create xml file", ex);
		}
		
		if (stream != null) {
			IOUtils.closeQuietly(stream);
		}
    }

    private List<PrismObject> getExportedObjects() {
        ObjectQuery query = getTableDataProvider().getQuery();

        ObjectQuery clonedQuery = null;
        if (query != null) {
            clonedQuery = new ObjectQuery();
            clonedQuery.setFilter(query.getFilter());
        }
        Class type = getTableDataProvider().getType();
        if (type == null) {
            type = ObjectType.class;
        }

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECT);
        List<PrismObject> objects = null;
        try {
            objects = getModelService().searchObjects(type, clonedQuery,
                    SelectorOptions.createCollection(new ItemPath(), GetOperationOptions.createRaw()),
                    createSimpleTask(OPERATION_SEARCH_OBJECT), result);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load objects", ex);
        } finally {
            result.recomputeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        return objects != null ? objects : new ArrayList<PrismObject>();
    }
    
    private File createZipForDownload(File file, File folder, String suffix, OperationResult result) {
		File zipFile = new File(folder, "ExportedData_" + suffix + ".zip");
		OutputStreamWriter stream = null;
		ZipOutputStream out = null;
		try {
			LOGGER.trace("adding file {} to zip archive", file.getName());
			out = new ZipOutputStream(new FileOutputStream(zipFile));
			final ZipEntry entry = new ZipEntry(file.getName());
			List<PrismObject> objects = getExportedObjects();

            String stringObject;
            //FIXME: could cause problem with unzip in java when size is not set, however it is not our case
            //entry.setSize(stringObject.length());
            out.putNextEntry(entry);
            out.write(createHeaderForXml().getBytes());
            for (PrismObject object : objects) {
                //todo this will create file that doesn't contain all objects, operation result wont show it if it happened in the middle of file
                try {
                    stringObject = getPrismContext().getPrismDomProcessor().serializeObjectToString(object);
                    out.write(("\t" + stringObject + "\n").getBytes());
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Failed to parse object " + WebMiscUtil.getName(object)
                            + " to string for zip", ex);
                    result.recordFatalError("Failed to parse object " + WebMiscUtil.getName(object)
                            + " to string for zip", ex);
                }
            }
            out.write("</objects>".getBytes());
            stream = new OutputStreamWriter(out, "utf-8");        //todo wtf?
			LOGGER.debug("added file {} to zip archive", file.getName());
		} catch (IOException ex) {
			LoggingUtils.logException(LOGGER, "Failed to write to stream.", ex);
			result.recordFatalError("Failed to write to stream.", ex);
		} finally {
            //todo wtf? check on stream != null, than using out
			if (null != stream) {
				try {
					out.finish();
					out.closeEntry();
					out.close();
					stream.close();
				} catch (final IOException ex) {
					LoggingUtils.logException(LOGGER, "Failed to pack file '" + file + "' to zip archive '" + out + "'", ex);
					result.recordFatalError("Failed to pack file '" + file + "' to zip archive '" + out + "'", ex);
				}
			}
		}
		return zipFile;
	}
    
    private String createHeaderForXml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<objects xmlns='").append(SchemaConstantsGenerated.NS_COMMON).append("'\n");
        builder.append("\txmlns:c='").append(SchemaConstantsGenerated.NS_COMMON).append("'\n");
        builder.append("\txmlns:org='").append(SchemaConstants.NS_ORG).append("'>\n");

        return builder.toString();
	}
}
