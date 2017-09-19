/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
public class ContainerValueWrapper<C extends Containerable> extends PrismWrapper implements Serializable, DebugDumpable {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerValueWrapper.class);

//	private String displayName;
    private ContainerWrapper<C> containerWrapper;
    private PrismContainerValue<C> containerValue;
    private ValueStatus status;

    private ItemPath path;
    private List<ItemWrapper> properties;

    private boolean readonly;
    private boolean showInheritedObjectAttributes;

//    private PrismContainerDefinition<C> containerDefinition;

  
    ContainerValueWrapper(ContainerWrapper<C> containerWrapper, PrismContainerValue<C> containerValue, ValueStatus status, ItemPath path) {
    	Validate.notNull(status, "Container status must not be null.");

		this.containerWrapper = containerWrapper;
		this.containerValue = containerValue;
		this.status = status;
		this.path = path;
		this.readonly = containerWrapper.isReadonly(); // [pm] this is quite questionable
		this.showInheritedObjectAttributes = containerWrapper.isShowInheritedObjectAttributes();

	}

	ContainerValueWrapper(PrismContainerValue<C> container, ValueStatus status, ItemPath path, boolean readOnly, boolean showInheritedObjectAttributes) {
//		super(null, container);
        Validate.notNull(container, "container must not be null.");
        Validate.notNull(container.getParent().getDefinition(), "container definition must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.containerValue = container;
//		this.containerDefinition = container.getParent().getDefinition();
        this.status = status;
        this.path = path;
        this.readonly = readOnly;
        this.showInheritedObjectAttributes = showInheritedObjectAttributes;
    }
	
	public PrismContainerDefinition<C> getDefinition() {
		return containerValue.getParent().getDefinition();
	}

    public void revive(PrismContext prismContext) throws SchemaException {
        if (containerValue != null) {
            containerValue.revive(prismContext);
        }
        if (getDefinition() != null) {
        	getDefinition().revive(prismContext);
        }
        if (properties != null) {
            for (ItemWrapper itemWrapper : properties) {
                itemWrapper.revive(prismContext);
            }
        }
    }

//    @Override
//    public PrismContainerDefinition<C> getItemDefinition() {
//		if (containerDefinition != null) {
//			return containerDefinition;
//		}
//        if (main) {
//            return containerWrapper.getItemDefinition();
//        } else {
//            return containerWrapper.getItemDefinition().findContainerDefinition(path);
//        }
//    }

    @Nullable
	ContainerWrapper<C> getContainer() {
        return containerWrapper;
    }

    public ValueStatus getStatus() {
        return status;
    }

    public ItemPath getPath() {
        return path;
    }

    public PrismContainerValue<C> getContainerValue() {
        return containerValue;
    }

    public List<ItemWrapper> getItems() {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        return properties;
    }

    public void setProperties(List<ItemWrapper> properties) {
        this.properties = properties;
    }
 
    public void computeStripes() {
    	if (properties == null) {
    		return;
    	}
    	int visibleProperties = 0;
    	
    	for (ItemWrapper item: properties) {
//    		if (item.isVisible(isShowEmpty())) {
    			visibleProperties++;
//    		}
    		if (visibleProperties % 2 == 0) {
    			item.setStripe(true);
    		} else {
    			item.setStripe(false);
    		}
    	}
    }

	public boolean isShowInheritedObjectAttributes() {
		return showInheritedObjectAttributes;
	}

    public boolean isMain() {
        return path==null || path.isEmpty();
    }

    static String getDisplayNameFromItem(Item item) {
        Validate.notNull(item, "Item must not be null.");

        String displayName = item.getDisplayName();
        if (StringUtils.isEmpty(displayName)) {
            QName name = item.getElementName();
            if (name != null) {
                displayName = name.getLocalPart();
            } else {
                displayName = item.getDefinition().getTypeName().getLocalPart();
            }
        }

        return displayName;
    }

    public boolean hasChanged() {
        for (ItemWrapper item : getItems()) {
            if (item.hasChanged()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ContainerWrapper(");
//        builder.append(getDisplayNameFromItem(containerValue));
        builder.append(" (");
        builder.append(status);
        builder.append(") ");
        builder.append(getItems() == null ? null : getItems().size());
        builder.append(" items)");
        return builder.toString();
    }

    /**
     * This methods check if we want to show property in form (e.g.
     * failedLogins, fetchResult, lastFailedLoginTimestamp must be invisible)
     *
     * @return
     * @deprecated will be implemented through annotations in schema
     */
    @Deprecated
    private boolean skipProperty(PrismPropertyDefinition def) {
        final List<QName> names = new ArrayList<QName>();
        names.add(PasswordType.F_FAILED_LOGINS);
        names.add(PasswordType.F_LAST_FAILED_LOGIN);
        names.add(PasswordType.F_LAST_SUCCESSFUL_LOGIN);
        names.add(PasswordType.F_PREVIOUS_SUCCESSFUL_LOGIN);
        names.add(ObjectType.F_FETCH_RESULT);
        // activation
        names.add(ActivationType.F_EFFECTIVE_STATUS);
        names.add(ActivationType.F_VALIDITY_STATUS);
        // user
        names.add(UserType.F_RESULT);
        // org and roles
        names.add(OrgType.F_APPROVAL_PROCESS);
        names.add(OrgType.F_APPROVER_EXPRESSION);
        names.add(OrgType.F_AUTOMATICALLY_APPROVED);
        names.add(OrgType.F_CONDITION);


        for (QName name : names) {
            if (name.equals(def.getName())) {
                return true;
            }
        }

        return false;
    }

    public boolean isReadonly() {
		// readonly flag in container is an override. Do not get the value from definition
		// otherwise it will be propagated to items and overrides the item definition.
    	return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }


	public void sort(final PageBase pageBase) {
		if (containerWrapper.isSorted()){
            Collections.sort(properties, new Comparator<ItemWrapper>(){
                @Override
                public int compare(ItemWrapper pw1, ItemWrapper pw2) {
                    ItemDefinition id1 = pw1.getItemDefinition();
                    ItemDefinition id2 = pw2.getItemDefinition();
                    String str1 =(id1 != null ? (id1.getDisplayName() != null ?
                            (pageBase.createStringResource(id1.getDisplayName()) != null &&
                                    StringUtils.isNotEmpty(pageBase.createStringResource(id1.getDisplayName()).getString()) ?
                                    pageBase.createStringResource(id1.getDisplayName()).getString() : id1.getDisplayName()):
                            (id1.getName() != null && id1.getName().getLocalPart() != null ? id1.getName().getLocalPart() : "")) : "");
                    String str2 =(id2 != null ? (id2.getDisplayName() != null ?
                            (pageBase.createStringResource(id2.getDisplayName()) != null &&
                                    StringUtils.isNotEmpty(pageBase.createStringResource(id2.getDisplayName()).getString()) ?
                                    pageBase.createStringResource(id2.getDisplayName()).getString() : id2.getDisplayName()):
                            (id2.getName() != null && id2.getName().getLocalPart() != null ? id2.getName().getLocalPart() : "")) : "");
                    return str1.compareToIgnoreCase(str2);
                }
            });
        }
        else {
            final int[] maxOrderArray = new int[3];
            Collections.sort(properties, new Comparator<ItemWrapper>(){
                @Override
                public int compare(ItemWrapper pw1, ItemWrapper pw2) {
                    ItemDefinition id1 = pw1.getItemDefinition();
                    ItemDefinition id2 = pw2.getItemDefinition();

                    //we need to find out the value of the biggest displayOrder to put
                    //properties with null display order to the end of the list
                    int displayOrder1 = (id1 != null && id1.getDisplayOrder() != null) ? id1.getDisplayOrder() : 0;
                    int displayOrder2 = (id2 != null && id2.getDisplayOrder() != null) ? id2.getDisplayOrder() : 0;
                    if (maxOrderArray[0] == 0){
                        maxOrderArray[0] = displayOrder1 > displayOrder2 ? displayOrder1 + 1 : displayOrder2 + 1;
                    }
                    maxOrderArray[1] = displayOrder1;
                    maxOrderArray[2] = displayOrder2;

                    int maxDisplayOrder = NumberUtils.max(maxOrderArray);
                    maxOrderArray[0] = maxDisplayOrder + 1;

                    return Integer.compare(id1 != null  && id1.getDisplayOrder() != null ? id1.getDisplayOrder() : maxDisplayOrder,
                            id2 != null && id2.getDisplayOrder() != null ? id2.getDisplayOrder() : maxDisplayOrder);
                }
            });
        }
	}

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ContainervalueWrapper: ");//.append(PrettyPrinter.prettyPrint(getName())).append("\n");
        DebugUtil.debugDumpWithLabel(sb, "displayName", getDisplayName(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "status", status == null ? null : status.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "main", isMain(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "readonly", readonly, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "showInheritedObjectAttributes", showInheritedObjectAttributes, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "path", path == null ? null : path.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "containerDefinition", getDefinition() == null ? null : getDefinition().toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "container", containerValue == null ? null : containerValue.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "properties", indent + 1);
        sb.append("\n");
        DebugUtil.debugDump(sb, properties, indent + 2, false);
        return sb.toString();
    }

	public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {
//		if (getItemDefinition().getName().equals(ShadowType.F_ASSOCIATION)) {
//			//create ContainerDelta for association container
//			//HACK HACK HACK create correct procession for association container data
//			//according to its structure
//			ContainerDelta<ShadowAssociationType> associationDelta =
//					ContainerDelta.createDelta(ShadowType.F_ASSOCIATION,
//							(PrismContainerDefinition<ShadowAssociationType>) getItemDefinition());
//			for (ItemWrapper itemWrapper : getItems()) {
//				AssociationWrapper associationItemWrapper = (AssociationWrapper) itemWrapper;
//				List<ValueWrapper> assocValueWrappers = associationItemWrapper.getValues();
//				for (ValueWrapper assocValueWrapper : assocValueWrappers) {
//					PrismContainerValue<ShadowAssociationType> assocValue = (PrismContainerValue<ShadowAssociationType>) assocValueWrapper.getValue();
//					if (!assocValue.isEmpty()) {
//						if (assocValueWrapper.getStatus() == ValueStatus.DELETED) {
//							associationDelta.addValueToDelete(assocValue.clone());
//						} else if (assocValueWrapper.getStatus().equals(ValueStatus.ADDED)) {
//							associationDelta.addValueToAdd(assocValue.clone());
//						}
//					}
//				}
//			}
//			if (!associationDelta.isEmpty()) {
//				delta.addModification(associationDelta);
//			}
//		} else {
			if (!hasChanged()) {
				return;
			}

			for (ItemWrapper itemWrapper : getItems()) {
				if (!itemWrapper.hasChanged()) {
					continue;
				}
				ItemPath containerPath = getPath() != null ? getPath() : ItemPath.EMPTY_PATH;
				if (itemWrapper instanceof PropertyWrapper) {
					ItemDelta pDelta = computePropertyDeltas((PropertyWrapper) itemWrapper, containerPath);
					if (!pDelta.isEmpty()) {
						//HACK to remove a password replace delta is to be created
						if (getContainer().getName().equals(CredentialsType.F_PASSWORD)) {
							if (pDelta.getValuesToDelete() != null){
								pDelta.resetValuesToDelete();
								pDelta.setValuesToReplace(new ArrayList());
							}
						}
						delta.addModification(pDelta);
					}
				} else if (itemWrapper instanceof ReferenceWrapper) {
					ReferenceDelta pDelta = computeReferenceDeltas((ReferenceWrapper) itemWrapper, containerPath);
					if (!pDelta.isEmpty()) {
						delta.addModification(pDelta);
					}
				} else {
					LOGGER.trace("Delta from wrapper: ignoring {}", itemWrapper);
				}
			}
//		}
	}

	private ItemDelta computePropertyDeltas(PropertyWrapper propertyWrapper, ItemPath containerPath) {
		ItemDefinition itemDef = propertyWrapper.getItemDefinition();
		ItemDelta pDelta = itemDef.createEmptyDelta(propertyWrapper.getItem().getPath());
		addItemDelta(propertyWrapper, pDelta, itemDef, containerPath);
		return pDelta;
	}

	private ReferenceDelta computeReferenceDeltas(ReferenceWrapper referenceWrapper, ItemPath containerPath) {
		PrismReferenceDefinition propertyDef = referenceWrapper.getItemDefinition();
		ReferenceDelta pDelta = new ReferenceDelta(containerPath, propertyDef.getName(), propertyDef,
				propertyDef.getPrismContext());
		addItemDelta(referenceWrapper, pDelta, propertyDef, containerPath.subPath(propertyDef.getName()));
		return pDelta;
	}

	private void addItemDelta(PropertyOrReferenceWrapper itemWrapper, ItemDelta pDelta, ItemDefinition propertyDef,
			ItemPath containerPath) {
		for (Object vWrapper : itemWrapper.getValues()) {
			if (!(vWrapper instanceof ValueWrapper)) {
				continue;
			} 
			
			ValueWrapper valueWrapper = (ValueWrapper) vWrapper;
			
			valueWrapper.normalize(propertyDef.getPrismContext());
			ValueStatus valueStatus = valueWrapper.getStatus();
			if (!valueWrapper.hasValueChanged()
					&& (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
				continue;
			}

			// TODO: need to check if the resource has defined
			// capabilities
			// todo this is bad hack because now we have not tri-state
			// checkbox
//			if (SchemaConstants.PATH_ACTIVATION.equivalent(containerPath) && getContainer().getObject() != null) {
//
//				PrismObject<?> object = getContainer().getObject().getObject();
//				if (object.asObjectable() instanceof ShadowType
//						&& (((ShadowType) object.asObjectable()).getActivation() == null || ((ShadowType) object
//						.asObjectable()).getActivation().getAdministrativeStatus() == null)) {
//
//					if (!getContainer().getObject().hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
//							ActivationCapabilityType.class)) {
//						continue;
//					}
//				}
//			}

			PrismValue newValCloned = ObjectWrapper.clone(valueWrapper.getValue());
			PrismValue oldValCloned = ObjectWrapper.clone(valueWrapper.getOldValue());
			switch (valueWrapper.getStatus()) {
				case ADDED:
					if (newValCloned != null) {
						if (SchemaConstants.PATH_PASSWORD.equivalent(containerPath)) {
							// password change will always look like add,
							// therefore we push replace
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (password) ADD -> replace {}", pDelta.getPath(), newValCloned);
							}
							pDelta.setValuesToReplace(Arrays.asList(newValCloned));
						} else if (propertyDef.isSingleValue()) {
							// values for single-valued properties
							// should be pushed via replace
							// in order to prevent problems e.g. with
							// summarizing deltas for
							// unreachable resources
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (single,new) ADD -> replace {}", pDelta.getPath(), newValCloned);
							}
							pDelta.setValueToReplace(newValCloned);
						} else {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (multi,new) ADD -> add {}", pDelta.getPath(), newValCloned);
							}
							pDelta.addValueToAdd(newValCloned);
						}
					}
					break;
				case DELETED:
					if (newValCloned != null) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Delta from wrapper: {} (new) DELETE -> delete {}", pDelta.getPath(), newValCloned);
						}
						pDelta.addValueToDelete(newValCloned);
					}
					if (oldValCloned != null) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Delta from wrapper: {} (old) DELETE -> delete {}", pDelta.getPath(), oldValCloned);
						}
						pDelta.addValueToDelete(oldValCloned);
					}
					break;
				case NOT_CHANGED:
					// this is modify...
					if (propertyDef.isSingleValue()) {
						// newValCloned.isEmpty()
						if (newValCloned != null && !newValCloned.isEmpty()) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (single,new) NOT_CHANGED -> replace {}", pDelta.getPath(), newValCloned);
							}
							pDelta.setValuesToReplace(Arrays.asList(newValCloned));
						} else {
							if (oldValCloned != null) {
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("Delta from wrapper: {} (single,old) NOT_CHANGED -> delete {}", pDelta.getPath(), oldValCloned);
								}
								pDelta.addValueToDelete(oldValCloned);
							}
						}
					} else {
						if (newValCloned != null && !newValCloned.isEmpty()) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (multi,new) NOT_CHANGED -> add {}", pDelta.getPath(), newValCloned);
							}
							pDelta.addValueToAdd(newValCloned);
						}
						if (oldValCloned != null) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (multi,old) NOT_CHANGED -> delete {}", pDelta.getPath(), oldValCloned);
							}
							pDelta.addValueToDelete(oldValCloned);
						}
					}
					break;
			}
		}
	}
	
	//TODO: unify with other isVisibleMethods
	public boolean isVisible() {
		PrismContainerDefinition<C> def = getDefinition();
		
		if (def.isIgnored() || (def.isOperational() && !def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
			return false;
		}
		
		if (def.getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
			return isShowMetadata();
		}
		
		//TODO: emphasized
		switch (status) {
			case NOT_CHANGED :
				return  isNotEmptyAndCanReadAndModify(def) || showEmptyCanReadAndModify(def);
			case ADDED :
				return emphasizedAndCanAdd(def) || showEmptyAndCanAdd(def);
		}
		
		return false;
	}
	
	private boolean isNotEmptyAndCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead() && def.canModify();
	}
	
	private boolean showEmptyCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead() && def.canModify() && isShowEmpty();
	}

	private boolean showEmptyAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd();
	}
	
	private boolean emphasizedAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && def.isEmphasized();
	}
	
	@Override
	public void setShowEmpty(boolean showEmpty) {
		super.setShowEmpty(showEmpty);
		getItems().forEach(item -> {
			item.setShowEmpty(showEmpty);
		});
	}
	
	@Override
	public void setShowMetadata(boolean showMetadata) {
		super.setShowMetadata(showMetadata);
		getItems().forEach(value -> {
			if (value instanceof ContainerWrapper) {
				((ContainerWrapper<C>) value).setShowMetadata(showMetadata);
			}
		});
	}

	
//	@Override
	public boolean checkRequired(PageBase pageBase) {
    	boolean rv = true;
		for (ItemWrapper itemWrapper : getItems()) {
			if (!itemWrapper.checkRequired(pageBase)) {
				rv = false;
			}
		}
		return rv;
	}
	
	 public PropertyOrReferenceWrapper findPropertyWrapper(QName name) {
	        Validate.notNull(name, "QName must not be null.");
	        for (ItemWrapper wrapper : getItems()) {
	        	if (wrapper instanceof ContainerWrapper) {
	        		continue;
	        	}
	            if (QNameUtil.match(name, wrapper.getItem().getElementName())) {
	                return (PropertyOrReferenceWrapper) wrapper;
	            }
	        }
	        return null;
	    }
	
	public ContainerWrapper<C> findContainerWrapper(ItemPath path) {
        Validate.notNull(path, "QName must not be null.");
        for (ItemWrapper wrapper : getItems()) {
        	if (!(wrapper instanceof ContainerWrapper)) {
        		continue;
        	}
        	ContainerWrapper<C> containerWrapper = (ContainerWrapper<C>) wrapper;
        	if (containerWrapper.getPath().equivalent(path)) {
        		return containerWrapper;
        	}
        	
//            return ((ContainerWrapper<C>)wrapper).findContainerWrapper(path);
         
        }
        return null;
    }
	
	public ContainerValueWrapper<C> findContainerValueWrapper(ItemPath path) {
        Validate.notNull(path, "QName must not be null.");
        for (ItemWrapper wrapper : getItems()) {
        	if (!(wrapper instanceof ContainerValueWrapper)) {
        		continue;
        	}
            return ((ContainerValueWrapper<C>)wrapper).findContainerValueWrapper(path);
         
        }
        return null;
    }
	
	public String getDisplayName() {
		if (getContainer().isMain()){
			return "prismContainer.mainPanelDisplayName";
		}
		
		if (getDefinition() == null) {
			return WebComponentUtil.getDisplayName(containerValue);
		}
		
		if (getDefinition().isSingleValue()) {
			
			return ContainerWrapper.getDisplayNameFromItem(getContainerValue().getContainer());
		} 
		return WebComponentUtil.getDisplayName(containerValue);
	
//		return containerValue.toHumanReadableString();
	}
//
//	@Override
//	public boolean isEnforceRequiredFields() {
//		return containerWrapper != null && containerWrapper.isEnforceRequiredFields();
//	}


}
