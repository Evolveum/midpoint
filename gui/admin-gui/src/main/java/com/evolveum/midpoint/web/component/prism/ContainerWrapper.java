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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
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
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 * @author katkav
 */
public class ContainerWrapper<C extends Containerable> extends PrismWrapper implements ItemWrapper<PrismContainer<C>, PrismContainerDefinition<C>, ContainerValueWrapper<C>>, Serializable, DebugDumpable {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapper.class);

	private String displayName;
    private PrismContainer<C> container;
    private ContainerStatus status;

    private ItemPath path;
    private List<ContainerValueWrapper<C>> values;

    private boolean readonly;
    private boolean showInheritedObjectAttributes;


	ContainerWrapper(PrismContainer<C> container, ContainerStatus status, ItemPath path) {
		Validate.notNull(container, "container must not be null.");
		Validate.notNull(status, "Container status must not be null.");

		this.container = container;
		this.status = status;
		this.path = path;
	}

	ContainerWrapper(PrismContainer<C> container, ContainerStatus status, ItemPath path, boolean readOnly, boolean showInheritedObjectAttributes) {
        Validate.notNull(container, "container must not be null.");
        Validate.notNull(container.getDefinition(), "container definition must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.container = container;
        this.status = status;
        this.path = path;
        this.readonly = readOnly;
        this.showInheritedObjectAttributes = showInheritedObjectAttributes;
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (container != null) {
            container.revive(prismContext);
        }
        if (getItemDefinition() != null) {
        	getItemDefinition().revive(prismContext);
        }
        if (values != null) {
            for (ContainerValueWrapper itemWrapper : values) {
                itemWrapper.revive(prismContext);
            }
        }
    }

    @Override
    public PrismContainerDefinition<C> getItemDefinition() {
            return container.getDefinition();
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public ItemPath getPath() {
        return path;
    }

    @Override
    public PrismContainer<C> getItem() {
        return container;
    }

   
    @Override
    public List<ContainerValueWrapper<C>> getValues() {
    	 if (values == null) {
         	values = new ArrayList<>();
         }
         return values;
    	
    }

    public void setProperties(List<ContainerValueWrapper<C>> properties) {
        this.values = properties;
    }

    public PropertyOrReferenceWrapper findPropertyWrapper(QName name) {
        Validate.notNull(name, "QName must not be null.");
        for (ContainerValueWrapper wrapper : getValues()) {
        	PropertyOrReferenceWrapper propertyWrapper = wrapper.findPropertyWrapper(name);
        	if (propertyWrapper != null) {
        		return propertyWrapper;
        	}
        }
        return null;
    }
    
    public ContainerWrapper<C> findContainerWrapper(ItemPath path) {
        Validate.notNull(path, "QName must not be null.");
        for (ContainerValueWrapper<C> wrapper : getValues()) {
            ContainerWrapper<C> containerWrapper = wrapper.findContainerWrapper(path);
            if (containerWrapper != null) {
            	return containerWrapper;
            }
        }
        return null;
    }
    
    public ContainerValueWrapper<C> findContainerValueWrapper(ItemPath path) {
        Validate.notNull(path, "QName must not be null.");
        for (ContainerValueWrapper<C> wrapper : getValues()) {
        	
        	if (path.equivalent(wrapper.getPath())) {
        		return wrapper;
        	}
        	
        	ContainerValueWrapper<C> containerWrapper = wrapper.findContainerValueWrapper(path);
            if (containerWrapper != null) {
            	return containerWrapper;
            }
        }
        return null;
    }

    public void computeStripes() {
    	int visibleProperties = 0;
    	for (ContainerValueWrapper<C> item: values) {
    		item.computeStripes();
//    		if (item.isVisible()) {
//    			visibleProperties++;
//    		}
//    		if (visibleProperties % 2 == 0) {
//    			item.setStripe(true);
//    		} else {
//    			item.setStripe(false);
//    		}
    	}
    }

	public boolean isShowInheritedObjectAttributes() {
		return showInheritedObjectAttributes;
	}

    @Override
    public String getDisplayName() {
        if (StringUtils.isNotEmpty(displayName)) {
            return displayName;
        }
        return getDisplayNameFromItem(container);
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    @Override
    public QName getName() {
        return getItem().getElementName();
    }

    public boolean isMain() {
        return path == null || path.isEmpty();
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
        for (ContainerValueWrapper item : getValues()) {
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
        builder.append(getDisplayNameFromItem(container));
        builder.append(" (");
        builder.append(status);
        builder.append(") ");
        builder.append(getValues() == null ? null : getValues().size());
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

//    @Override
//    public List<ValueWrapper> getValues() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//


    @Override
    public boolean isEmpty() {
        return getItem().isEmpty();
    }

//   PrismContainer<C> getContainer() {
//        // TODO Auto-generated method stub
//        return container
//    }

    
    //TODO add new PrismContainerValue to association container
    public void addValue(boolean showEmpty) {
    	getValues().add(createItem(showEmpty));
    }

    public ContainerValueWrapper<C> createItem(boolean showEmpty) {
    	PrismContainerValue<C> pcv = container.createNewValue();
        ContainerValueWrapper<C> wrapper = new ContainerValueWrapper<C>(this, pcv, ValueStatus.ADDED, pcv.getPath());
        wrapper.setShowEmpty(showEmpty, true);
        return wrapper;
    }

    public void sort(final PageBase pageBase) {
    	for (ContainerValueWrapper<C> valueWrapper : getValues()) {
    		valueWrapper.sort(pageBase);
    	}
    }

//	public void sort(final PageBase pageBase) {
//		if (objectWrapper.isSorted()){
//            Collections.sort(values, new Comparator<ItemWrapper>(){
//                @Override
//                public int compare(ItemWrapper pw1, ItemWrapper pw2) {
//                    ItemDefinition id1 = pw1.getItemDefinition();
//                    ItemDefinition id2 = pw2.getItemDefinition();
//                    String str1 =(id1 != null ? (id1.getDisplayName() != null ?
//                            (pageBase.createStringResource(id1.getDisplayName()) != null &&
//                                    StringUtils.isNotEmpty(pageBase.createStringResource(id1.getDisplayName()).getString()) ?
//                                    pageBase.createStringResource(id1.getDisplayName()).getString() : id1.getDisplayName()):
//                            (id1.getName() != null && id1.getName().getLocalPart() != null ? id1.getName().getLocalPart() : "")) : "");
//                    String str2 =(id2 != null ? (id2.getDisplayName() != null ?
//                            (pageBase.createStringResource(id2.getDisplayName()) != null &&
//                                    StringUtils.isNotEmpty(pageBase.createStringResource(id2.getDisplayName()).getString()) ?
//                                    pageBase.createStringResource(id2.getDisplayName()).getString() : id2.getDisplayName()):
//                            (id2.getName() != null && id2.getName().getLocalPart() != null ? id2.getName().getLocalPart() : "")) : "");
//                    return str1.compareToIgnoreCase(str2);
//                }
//            });
//        }
//        else {
//            final int[] maxOrderArray = new int[3];
//            Collections.sort(values, new Comparator<ItemWrapper>(){
//                @Override
//                public int compare(ItemWrapper pw1, ItemWrapper pw2) {
//                    ItemDefinition id1 = pw1.getItemDefinition();
//                    ItemDefinition id2 = pw2.getItemDefinition();
//
//                    //we need to find out the value of the biggest displayOrder to put
//                    //properties with null display order to the end of the list
//                    int displayOrder1 = (id1 != null && id1.getDisplayOrder() != null) ? id1.getDisplayOrder() : 0;
//                    int displayOrder2 = (id2 != null && id2.getDisplayOrder() != null) ? id2.getDisplayOrder() : 0;
//                    if (maxOrderArray[0] == 0){
//                        maxOrderArray[0] = displayOrder1 > displayOrder2 ? displayOrder1 + 1 : displayOrder2 + 1;
//                    }
//                    maxOrderArray[1] = displayOrder1;
//                    maxOrderArray[2] = displayOrder2;
//
//                    int maxDisplayOrder = NumberUtils.max(maxOrderArray);
//                    maxOrderArray[0] = maxDisplayOrder + 1;
//
//                    return Integer.compare(id1 != null  && id1.getDisplayOrder() != null ? id1.getDisplayOrder() : maxDisplayOrder,
//                            id2 != null && id2.getDisplayOrder() != null ? id2.getDisplayOrder() : maxDisplayOrder);
//                }
//            });
//        }
//	}

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ContainerWrapper: ").append(PrettyPrinter.prettyPrint(getName())).append("\n");
        DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent + 1);
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
        DebugUtil.debugDumpWithLabel(sb, "containerDefinition", getItemDefinition() == null ? null : getItemDefinition().toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "container", container == null ? null : container.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpLabel(sb, "properties", indent + 1);
        sb.append("\n");
        DebugUtil.debugDump(sb, values, indent + 2, false);
        return sb.toString();
    }

	@Override
	public boolean isStripe() {
		// Does not make much sense, but it is given by the interface
		return false;
	}

	@Override
	public void setStripe(boolean isStripe) {
		// Does not make much sense, but it is given by the interface
	}

	public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {
//		if (getItemDefinition().getName().equals(ShadowType.F_ASSOCIATION)) {
//			//create ContainerDelta for association container
//			//HACK HACK HACK create correct procession for association container data
//			//according to its structure
//			ContainerDelta<ShadowAssociationType> associationDelta =
//					ContainerDelta.createDelta(ShadowType.F_ASSOCIATION,
//							(PrismContainerDefinition<ShadowAssociationType>) getItemDefinition());
//			for (ItemWrapper itemWrapper : getValues()) {
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

			for (ContainerValueWrapper<C> itemWrapper : getValues()) {
				if (!itemWrapper.hasChanged()) {
					continue;
				}
				
				itemWrapper.collectModifications(delta);
//				ItemPath containerPath = getPath() != null ? getPath() : ItemPath.EMPTY_PATH;
//				
//				if (itemWrapper instanceof PropertyWrapper) {
//					ItemDelta pDelta = computePropertyDeltas((PropertyWrapper) itemWrapper, containerPath);
//					if (!pDelta.isEmpty()) {
//						//HACK to remove a password replace delta is to be created
//						if (getName().equals(CredentialsType.F_PASSWORD)) {
//							if (pDelta.getValuesToDelete() != null){
//								pDelta.resetValuesToDelete();
//								pDelta.setValuesToReplace(new ArrayList());
//							}
//						}
//						delta.addModification(pDelta);
//					}
//				} else if (itemWrapper instanceof ReferenceWrapper) {
//					ReferenceDelta pDelta = computeReferenceDeltas((ReferenceWrapper) itemWrapper, containerPath);
//					if (!pDelta.isEmpty()) {
//						delta.addModification(pDelta);
//					}
//				} else {
//					LOGGER.trace("Delta from wrapper: ignoring {}", itemWrapper);
//				}
			}
//		}
	}

//	private ItemDelta computePropertyDeltas(PropertyWrapper propertyWrapper, ItemPath containerPath) {
//		ItemDefinition itemDef = propertyWrapper.getItemDefinition();
//		ItemDelta pDelta = itemDef.createEmptyDelta(containerPath.subPath(itemDef.getName()));
//		addItemDelta(propertyWrapper, pDelta, itemDef, containerPath);
//		return pDelta;
//	}
//
//	private ReferenceDelta computeReferenceDeltas(ReferenceWrapper referenceWrapper, ItemPath containerPath) {
//		PrismReferenceDefinition propertyDef = referenceWrapper.getItemDefinition();
//		ReferenceDelta pDelta = new ReferenceDelta(containerPath, propertyDef.getName(), propertyDef,
//				propertyDef.getPrismContext());
//		addItemDelta(referenceWrapper, pDelta, propertyDef, containerPath.subPath(propertyDef.getName()));
//		return pDelta;
//	}
//
//	private void addItemDelta(ItemWrapper<? extends Item, ? extends ItemDefinition> itemWrapper, ItemDelta pDelta, ItemDefinition propertyDef,
//			ItemPath containerPath) {
//		for (ValueWrapper valueWrapper : itemWrapper.getValues()) {
//			valueWrapper.normalize(propertyDef.getPrismContext());
//			ValueStatus valueStatus = valueWrapper.getStatus();
//			if (!valueWrapper.hasValueChanged()
//					&& (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
//				continue;
//			}
//
//			// TODO: need to check if the resource has defined
//			// capabilities
//			// todo this is bad hack because now we have not tri-state
//			// checkbox
////			if (SchemaConstants.PATH_ACTIVATION.equivalent(containerPath) && getObject() != null) {
////
////				PrismObject<?> object = getObject().getObject();
////				if (object.asObjectable() instanceof ShadowType
////						&& (((ShadowType) object.asObjectable()).getActivation() == null || ((ShadowType) object
////						.asObjectable()).getActivation().getAdministrativeStatus() == null)) {
////
////					if (!getObject().hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
////							ActivationCapabilityType.class)) {
////						continue;
////					}
////				}
////			}
//
//			PrismValue newValCloned = ObjectWrapper.clone(valueWrapper.getValue());
//			PrismValue oldValCloned = ObjectWrapper.clone(valueWrapper.getOldValue());
//			switch (valueWrapper.getStatus()) {
//				case ADDED:
//					if (newValCloned != null) {
//						if (SchemaConstants.PATH_PASSWORD.equivalent(containerPath)) {
//							// password change will always look like add,
//							// therefore we push replace
//							if (LOGGER.isTraceEnabled()) {
//								LOGGER.trace("Delta from wrapper: {} (password) ADD -> replace {}", pDelta.getPath(), newValCloned);
//							}
//							pDelta.setValuesToReplace(Arrays.asList(newValCloned));
//						} else if (propertyDef.isSingleValue()) {
//							// values for single-valued properties
//							// should be pushed via replace
//							// in order to prevent problems e.g. with
//							// summarizing deltas for
//							// unreachable resources
//							if (LOGGER.isTraceEnabled()) {
//								LOGGER.trace("Delta from wrapper: {} (single,new) ADD -> replace {}", pDelta.getPath(), newValCloned);
//							}
//							pDelta.setValueToReplace(newValCloned);
//						} else {
//							if (LOGGER.isTraceEnabled()) {
//								LOGGER.trace("Delta from wrapper: {} (multi,new) ADD -> add {}", pDelta.getPath(), newValCloned);
//							}
//							pDelta.addValueToAdd(newValCloned);
//						}
//					}
//					break;
//				case DELETED:
//					if (newValCloned != null) {
//						if (LOGGER.isTraceEnabled()) {
//							LOGGER.trace("Delta from wrapper: {} (new) DELETE -> delete {}", pDelta.getPath(), newValCloned);
//						}
//						pDelta.addValueToDelete(newValCloned);
//					}
//					if (oldValCloned != null) {
//						if (LOGGER.isTraceEnabled()) {
//							LOGGER.trace("Delta from wrapper: {} (old) DELETE -> delete {}", pDelta.getPath(), oldValCloned);
//						}
//						pDelta.addValueToDelete(oldValCloned);
//					}
//					break;
//				case NOT_CHANGED:
//					// this is modify...
//					if (propertyDef.isSingleValue()) {
//						// newValCloned.isEmpty()
//						if (newValCloned != null && !newValCloned.isEmpty()) {
//							if (LOGGER.isTraceEnabled()) {
//								LOGGER.trace("Delta from wrapper: {} (single,new) NOT_CHANGED -> replace {}", pDelta.getPath(), newValCloned);
//							}
//							pDelta.setValuesToReplace(Arrays.asList(newValCloned));
//						} else {
//							if (oldValCloned != null) {
//								if (LOGGER.isTraceEnabled()) {
//									LOGGER.trace("Delta from wrapper: {} (single,old) NOT_CHANGED -> delete {}", pDelta.getPath(), oldValCloned);
//								}
//								pDelta.addValueToDelete(oldValCloned);
//							}
//						}
//					} else {
//						if (newValCloned != null && !newValCloned.isEmpty()) {
//							if (LOGGER.isTraceEnabled()) {
//								LOGGER.trace("Delta from wrapper: {} (multi,new) NOT_CHANGED -> add {}", pDelta.getPath(), newValCloned);
//							}
//							pDelta.addValueToAdd(newValCloned);
//						}
//						if (oldValCloned != null) {
//							if (LOGGER.isTraceEnabled()) {
//								LOGGER.trace("Delta from wrapper: {} (multi,old) NOT_CHANGED -> delete {}", pDelta.getPath(), oldValCloned);
//							}
//							pDelta.addValueToDelete(oldValCloned);
//						}
//					}
//					break;
//			}
//		}
//	}

	@Override
	public boolean checkRequired(PageBase pageBase) {
    	boolean rv = true;
		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (!itemWrapper.checkRequired(pageBase)) {
				rv = false;
			}
		}
		return rv;
	}
	
	@Override
	public void setShowEmpty(boolean showEmpty, boolean recursive) {
		super.setShowEmpty(showEmpty, recursive);
		if (recursive) {
			getValues().forEach(value -> value.setShowEmpty(showEmpty, recursive));
		}
		
	}
	
	@Override
	public void setShowMetadata(boolean showMetadata) {
		super.setShowMetadata(showMetadata);
		getValues().forEach(value -> value.setShowMetadata(showMetadata));
	}

	@Override
	public boolean isEnforceRequiredFields() {
		return true;//objectWrapper != null && objectWrapper.isEnforceRequiredFields();
	}

	@Override
	public ContainerWrapper getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isVisible() {
		PrismContainerDefinition<C> def = getItemDefinition();
		
		if (def.isIgnored() || (def.isOperational()) && (!def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
			return false;
		}
		
		switch (status) {
			case MODIFYING :
				return  isNotEmptyAndCanReadAndModify(def) || showEmptyCanReadAndModify(def);
			case ADDING :
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
		return def.canAdd() && isShowEmpty();
	}
	
	private boolean emphasizedAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && def.isEmphasized();
	}
	
}
