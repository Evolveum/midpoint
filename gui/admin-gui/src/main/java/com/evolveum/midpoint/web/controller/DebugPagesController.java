/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.jaxb.JAXBUtil;
import com.evolveum.midpoint.util.Utils;
import com.evolveum.midpoint.util.diff.CalculateXmlDiff;
import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.GenericObjectDto;
import com.evolveum.midpoint.web.model.ObjectDto;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.web.model.ResourceStateDto;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.UserTemplateDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.*;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;

/**
 *
 * @author katuska
 */
@Controller
@Scope("session")
public class DebugPagesController implements Serializable {

	private static final long serialVersionUID = 4072813415567695627L;
	private List<SelectItem> objectsList;
    private String objectType;
    @Autowired(required = true)
    private ObjectTypeCatalog objectTypeCatalog;
    private String id = null;
    private String objectXml;
    private List<ObjectDto> selectedObjectsList;
    private boolean editable = true;
    private boolean visible = false;
    private boolean addable = false;
    private int offset = 0;
    private int maxSize = 20;

    @Autowired(required = true)
    transient RepositoryPortType repositoryService;
    private static transient final org.slf4j.Logger logger = TraceManager.getTrace(DebugPagesController.class);
    private boolean showPopup = false;

    public DebugPagesController() {
    }

    public String editAction() {
        editable = false;
        visible = true;
        addable = false;
        return viewAction();
    }

    private void delete() {
        if ("".equals(id) || id == null) {
            FacesUtils.addErrorMessage("Object ID must be declared");
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Object ID must be declared"));
            return;
        }
        logger.info("Delete object start");

        try {
            repositoryService.deleteObject(id);
        } catch (FaultMessage ex) {

            String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex.getMessage());
            FacesUtils.addErrorMessage("Delete object failed with exception " + message);
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Delete object failed with exception " + message));
            return;
        } catch (Exception ex) {
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Delete object failed with exception " + ex.getMessage()));
              FacesUtils.addErrorMessage("Delete object failed with exception " + ex.getMessage());
            logger.info("Delete object failed");
            logger.error("Exception was {} ", ex);
            return;
        }
        logger.info("Delete object end");
        id = null;
    }

    public String deleteAction() {
        hidePopup();
        delete();
        
        listFirst();
        return null;
    }

    public String deleteById() {
        delete();
        return "";
    }

    public String editObject() {

        if ("".equals(objectXml) || objectXml == null) {
             FacesUtils.addErrorMessage("Object must be declared.");
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Object must be declared."));
        }
        logger.info("object XML {} ", objectXml);

        JAXBElement<ObjectType> el = null;
        try {
            el = (JAXBElement<ObjectType>) JAXBUtil.unmarshal(objectXml);

        } catch (JAXBException ex) {
            FacesUtils.addErrorMessage("Edit object failed with exception " + ex.getMessage());
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Edit object failed with exception " + ex.getMessage()));
            logger.info("Unmarshal of Object type {} failed");
            logger.error("Exception was {} ", ex);
        }

        ObjectType changedObject = el.getValue();
        ObjectContainerType result = getDebugObject(changedObject.getOid());
        ObjectType oldObject = result.getObject();

        try {
            ObjectModificationType objectChange = CalculateXmlDiff.calculateChanges(oldObject, changedObject);
            repositoryService.modifyObject(objectChange);
        } catch (FaultMessage ex) {
            String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex.getMessage());
             FacesUtils.addErrorMessage("Edit object failed with exception " + message);
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Edit object failed with exception " + message));
        } catch (Exception ex) {
             FacesUtils.addErrorMessage("Edit object failed with exception " + ex.getMessage());
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Edit object failed with exception " + ex.getMessage()));
            logger.error("modifyObject failed");
            logger.error("Exception was: ", ex);
            return null;
        }

        return "/debugPagesMain";
    }

    private ObjectContainerType getDebugObject(String oid) {

        ObjectContainerType result = null;


        try {
            result = repositoryService.getObject(oid, new PropertyReferenceListType());
            ObjectType objType = result.getObject();
            if (objType instanceof ResourceType) {
                ResourceType resource = (ResourceType) objType;
                Configuration conf = resource.getConfiguration();
                if (null == conf) {
                    logger.trace("Configuration is null");
                } else {
                    List<Element> any = conf.getAny();
                    if ((null == any) || (any.size() == 0)) {
                        logger.trace("No configuration elements");
                    } else {
                        logger.trace("Configuration contains {} elements", any.size());
                    }
                }
            }
            logger.trace("getDebugObject() returns (wrapped as JAXBObject object): {}", JAXBUtil.silentMarshalWrap(result.getObject(), SchemaConstants.I_OBJECT));
            return result;
        } catch (FaultMessage ex) {
            String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex.getMessage());
            FacesUtils.addErrorMessage("Get object failed with exception " + message);
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Get object failed with exception " + message));
            return null;
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("Get object failed with exception " + ex.getMessage());
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Get object failed with exception " + ex.getMessage()));
            logger.info("Get object failed");
            logger.error("Exception was {} ", ex);
            return null;
        }

    }

    public String viewAction() {

        if ("".equals(id) || id == null) {
            FacesUtils.addErrorMessage("Object ID to be viewed cannot be null");
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Object ID to be viewed cannot be null"));
            return "";
        }

        ObjectFactory of = new ObjectFactory();
        ObjectContainerType result = getDebugObject(id);
        JAXBElement<ObjectType> jaxb = of.createObject(result.getObject());
        try {
            objectXml = JAXBUtil.marshal(jaxb);
            logger.trace("Marshalled JAXB Object: {}", objectXml);
        } catch (JAXBException ex) {
            FacesUtils.addErrorMessage("View object failed: " + ex.getMessage());
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("View object failed: " + ex.getMessage()));
            logger.info("Marshal failed");
            logger.error("Exception was {} ", ex);
            return "";
        }

        return "/debugViewEditObject";

    }

    public String getObject() {

        if ("".equals(id) || id == null) {
            FacesUtils.addErrorMessage("Object ID cannot be null");
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Object ID cannot be null"));
            return "";
        }

        ObjectContainerType result = getDebugObject(id);
        setObjectType(result.getObject().getClass().getSimpleName());
        List<ObjectType> objTypeList = new ArrayList<ObjectType>();
        objTypeList.add(result.getObject());
        selectedObjectsList.clear();
        addToList(objectType, objTypeList);
        return "/debugListObjects";
    }

    public void listFirst(){
        offset = 0;
        listObjects();
    }

    public void listNext(){
        offset += maxSize;
        listObjects();
    }

    public void listPrevious(){
        if (offset < maxSize){
            return;
        }
        offset -= maxSize;
        listObjects();
    }

    public void listLast(){
        offset = -1;
        listObjects();
    }

    public String listObjects() {

        if ("".equals(objectType) || objectType == null) {
            FacesUtils.addErrorMessage("Object type must be chosen.");
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Object type must be chosen."));
            return "";
        }

        logger.info("listObjects start for object type {}", objectType);
        selectedObjectsList = new ArrayList<ObjectDto>();
        String xsdName = Utils.getObjectType(objectType);

        ObjectListType result = null;


        try {
            PagingType paging = new PagingType();

            PropertyReferenceType propertyReferenceType = Utils.fillPropertyReference("name");
            paging.setOrderBy(propertyReferenceType);
            paging.setOffset(BigInteger.valueOf(offset));
            paging.setMaxSize(BigInteger.valueOf(maxSize));
            paging.setOrderDirection(OrderDirectionType.ASCENDING);
            result = repositoryService.listObjects(xsdName, paging);
        } catch (FaultMessage ex) {
            String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex.getMessage());
             FacesUtils.addErrorMessage("List object failed with exception " + message);
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("List object failed with exception " + message));
        } catch (Exception ex) {
            FacesUtils.addErrorMessage("List object failed with exception " + ex.getMessage());
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("List object failed with exception " + ex.getMessage()));
            logger.info("List object failed");
            logger.error("Exception was {} ", ex);
        }

        if (result != null) {
            addToList(objectType, result.getObject());
        }

        logger.info("listObjects end");

        return "/debugListObjects";
    }

    private void addToList(String type, List<ObjectType> objectTypeList) {
        if (("UserType").equals(type)) {
            for (ObjectType obj : objectTypeList) {
                UserDto userDto = new UserDto((UserType) obj);
                selectedObjectsList.add(userDto);

            }
        }
        if (("AccountType").equals(type)) {
            for (ObjectType obj : objectTypeList) {
                AccountShadowDto accountDto = new AccountShadowDto((AccountShadowType) obj);
                selectedObjectsList.add(accountDto);
            }
        }
        if (("ResourceType").equals(type)) {
            for (ObjectType obj : objectTypeList) {
                ResourceDto resourceDto = new ResourceDto((ResourceType) obj);
                selectedObjectsList.add(resourceDto);
            }
        }

        if ("UserTemplateType".equals(type)) {
            for (ObjectType obj : objectTypeList) {
                UserTemplateDto userTemplate = new UserTemplateDto((UserTemplateType) obj);
                selectedObjectsList.add(userTemplate);
            }
        }

        if ("ResourceStateType".equals(type)) {
            for (ObjectType obj : objectTypeList) {
                ResourceStateDto resourceState = new ResourceStateDto((ResourceStateType) obj);
                selectedObjectsList.add(resourceState);
            }
        }

       
        if ("GenericObjectType".equals(type)) {
            for (ObjectType obj : objectTypeList) {
                GenericObjectDto genericObject = new GenericObjectDto((GenericObjectType) obj);
                selectedObjectsList.add(genericObject);
            }
        }
    }

    public String addAction() {
        objectXml = null;
        editable = false;
        visible = false;
        addable = true;
        return "/debugViewEditObject";
    }

    public String addObject() {

        if ("".equals(objectXml) || objectXml == null) {
            FacesUtils.addErrorMessage("Object must be declared.");
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Object must be declared."));
            return "";
        }

        JAXBElement el = null;
        try {
            el = (JAXBElement) JAXBUtil.unmarshal(objectXml);
        } catch (JAXBException ex) {
            FacesUtils.addErrorMessage("Add object failed. The XML Schema is not valid.Exception was: " + ex.getMessage());
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Add object failed. The XML Schema is not valid.Exception was: " + ex.getMessage()));
            logger.info("Unmarshal of Object type {} failed");
            logger.error("Exception was {} ", ex);
            return "";
        }

        ObjectContainerType objectContainer = new ObjectContainerType();
        if (el.getValue() instanceof ObjectType) {
            objectContainer.setObject((ObjectType) el.getValue());
        }

        try {
            java.lang.String result = repositoryService.addObject(objectContainer);
            logger.debug("Result = " + result);
        } catch (FaultMessage ex) {
            String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex.getMessage());
            FacesUtils.addErrorMessage("Add object failed with exception " + message);
//            FacesContext.getCurrentInstance().addMessage("", new FacesMessage("Add object failed with exception " + message));
            return "";
        } catch (Exception ex) {
            logger.info("Add object failed");
            logger.error("Exception was {} ", ex);
            return "";
        }

        return "/debugPagesMain";
    }

    public String fillSelectItemList() {
        id = null;
        editable = true;
        objectsList = new ArrayList<SelectItem>();
        objectsList.add(new SelectItem("UserType"));
        objectsList.add(new SelectItem("AccountType"));
        objectsList.add(new SelectItem("ResourceStateType"));
        objectsList.add(new SelectItem("ResourceType"));
        objectsList.add(new SelectItem("UserTemplateType"));
        objectsList.add(new SelectItem("GenericObjectType"));
        return "/debugPagesMain";
    }

   
    public String backAction() {
        editable = true;
        visible = false;
        addable = false;
        id = null;
        String clientId = FacesContext.getCurrentInstance().getViewRoot().getClientId();
        logger.info(clientId);
        return "/debugPagesMain";
    }

    public boolean isAddable() {
        return addable;
    }

    public void setAddable(boolean addable) {
        this.addable = addable;
    }

    public List<ObjectDto> getSelectedObjectsList() {
        return selectedObjectsList;
    }

    public void setSelectedObjectsList(List<ObjectDto> selectedObjectsList) {
        this.selectedObjectsList = selectedObjectsList;
    }

    public String getId() {
        logger.info("getting param userId {}", id);
        return id;
    }

    public void setId(String id) {
        logger.info("setting param userId with value {} ", id);
        this.id = id;
    }

    public ObjectTypeCatalog getObjectTypeCatalog() {
        return objectTypeCatalog;
    }

    public void setObjectTypeCatalog(ObjectTypeCatalog objectTypeCatalog) {
        this.objectTypeCatalog = objectTypeCatalog;
    }

    public List<SelectItem> getObjectsList() {
        return objectsList;
    }

    public void setObjectsList(List<SelectItem> objectsList) {
        this.objectsList = objectsList;
    }

    public String getObjectType() {
        logger.info("selected object {}", objectType);
        return objectType;
    }

    public void setObjectType(String objectType) {
        logger.info("selected object {}", objectType);

        if (("UserType").equals(objectType)) {
            this.objectType = "UserType";
            return;
        }
        if (("ResourceType").equals(objectType)) {
            this.objectType = "ResourceType";
            return;
        }
        if (("AccountShadowType").equals(objectType)) {
            this.objectType = "AccountType";
            return;
        }
        if ("UserTemplateType".equals(objectType)) {
            this.objectType = "UserTemplateType";
            return;
        }
        if ("ResourceStateType".equals(objectType)) {
            this.objectType = "ResourceStateType";
            return;
        }
        if ("GenericObjectType".equals(objectType)) {
            this.objectType = "GenericObjectType";
            return;
        }
        this.objectType = objectType;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public String getObjectXml() {
        return objectXml;
    }

    public void setObjectXml(String objectXml) {
        this.objectXml = objectXml;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isShowPopup() {
        return showPopup;
    }

    public void hidePopup() {
        showPopup = false;
    }

    public void showPopup() {
        if (StringUtils.isEmpty(id)) {
            FacesUtils.addErrorMessage("Object ID must be declared.");
            return;
        }
        showPopup = true;
    }

    
}
