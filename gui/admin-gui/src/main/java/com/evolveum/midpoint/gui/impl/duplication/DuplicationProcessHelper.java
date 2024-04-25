package com.evolveum.midpoint.gui.impl.duplication;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.common.cleanup.CleanupPath;
import com.evolveum.midpoint.common.cleanup.CleanupPathAction;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.common.cleanup.ObjectCleaner;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/***
 * Contains method for creating and modifying new duplicated object.
 */
public class DuplicationProcessHelper {

    /**
     * Adding new action for duplication of object to items menu.
     */
    public static <O extends ObjectType> void addDuplicationActionForObject(List<InlineMenuItem> menuItems, PageBase pageBase) {
        menuItems.add(new InlineMenuItem(pageBase.createStringResource("DuplicationProcessHelper.menu.duplicate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<O>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        O bean = resolveObject(getRowModel());
                        PrismObject<O> object = (PrismObject<O>) bean.asPrismObject();
                        PrismObject<O> duplicatedObject;
                        ContainerableDuplicateResolver<O> resolver =
                                pageBase.getRegistry().findContainerableDuplicateResolver(object.getDefinition());
                        if (resolver == null) {
                            duplicatedObject = duplicateObjectDefault(object);
                        } else {
                            O duplicatedBean = resolver.duplicateObject(bean);
                            if (duplicatedBean == null) {
                                pageBase.error(LocalizationUtil.translate("DuplicationProcessHelper.errorMessage.duplicate"));
                                return;
                            }
                            duplicatedObject = (PrismObject<O>) duplicatedBean.asPrismObject();
                        }

                        DetailsPageUtil.dispatchToObjectDetailsPage(duplicatedObject, true, pageBase);
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return false;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });
    }

    /**
     * Adding new action for duplication of container value to items menu.
     */
    public static <C extends Containerable> void addDuplicationActionForContainer(
            List<InlineMenuItem> menuItems,
            SerializableBiConsumer<PrismContainerValue<C>, AjaxRequestTarget> createDuplicatedItem,
            PageBase pageBase) {
        menuItems.add(new InlineMenuItem(pageBase.createStringResource("DuplicationProcessHelper.menu.duplicate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<C>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        C bean = resolveContainer(getRowModel());
                        PrismContainerValue<C> container = bean.asPrismContainerValue();
                        PrismContainerValue<C> duplicatedContainer;
                        ContainerableDuplicateResolver<C> resolver =
                                pageBase.getRegistry().findContainerableDuplicateResolver(
                                        container.getDefinition(), resolveParentContainer(getRowModel()));
                        if (resolver == null) {
                            duplicatedContainer = duplicateContainerValueDefault(container);
                        } else {
                            C duplicatedBean = resolver.duplicateObject(bean);
                            if (duplicatedBean == null) {
                                pageBase.error(LocalizationUtil.translate("DuplicationProcessHelper.errorMessage.duplicate"));
                                return;
                            }
                            duplicatedContainer = duplicatedBean.asPrismContainerValue();
                        }

                        createDuplicatedItem.accept(duplicatedContainer, target);
                    }
                };
            }

            @Override
            public boolean showConfirmationDialog() {
                return false;
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        });
    }

    /**
     * Duplicate object that can be reused.
     */
    public static <O extends ObjectType> PrismObject<O> duplicateObjectDefault(PrismObject<O> object) {
        PrismObject<O> duplicate = object.cloneComplex(CloneStrategy.REUSE);
        ObjectCleaner cleanupProcessor = new ObjectCleaner();
        cleanupProcessor.setRemoveContainerIds(true);
        cleanupProcessor.setPaths(List.of(new CleanupPath(CredentialsType.COMPLEX_TYPE, ItemPath.EMPTY_PATH, CleanupPathAction.REMOVE)));
        cleanupProcessor.process(duplicate);
        duplicate.setOid(null);
        return duplicate;
    }

    /**
     * Duplicate container value that can be reused.
     */
    public static <C extends Containerable> PrismContainerValue<C> duplicateContainerValueDefault(PrismContainerValue<C> container) {
        PrismContainerValue<C> duplicate = PrismValueCollectionsUtil.cloneCollectionComplex(
                        CloneStrategy.REUSE,
                        Collections.singletonList(container))
                .iterator().next();
        duplicate.setParent(container.getParent());
        ObjectCleaner cleanupProcessor = new ObjectCleaner();
        cleanupProcessor.setRemoveContainerIds(true);
        cleanupProcessor.process(duplicate);
        return duplicate;
    }

    private static <O extends ObjectType> O resolveObject(IModel<SelectableBean<O>> rowModel) {
        if (rowModel == null) {
            return null;
        }

        SelectableBean<O> bean = rowModel.getObject();
        if (bean == null) {
            return null;
        }

        return bean.getValue();
    }

    private static <C extends Containerable> C resolveContainer(IModel<PrismContainerValueWrapper<C>> rowModel) {
        if (rowModel == null) {
            return null;
        }

        PrismContainerValueWrapper<C> wrapper = rowModel.getObject();
        if (wrapper == null) {
            return null;
        }

        return wrapper.getRealValue();
    }

    private static <C extends Containerable, P extends Containerable> PrismContainerValue<P> resolveParentContainer(IModel<PrismContainerValueWrapper<C>> rowModel) {
        if (rowModel == null) {
            return null;
        }

        PrismContainerValueWrapper<C> wrapper = rowModel.getObject();
        if (wrapper == null) {
            return null;
        }

        PrismContainerWrapper<C> parentContainer = wrapper.getParent();
        if (parentContainer == null) {
            return null;
        }

        PrismContainerValueWrapper<P> parentContainerValue = (PrismContainerValueWrapper<P>) parentContainer.getParent();
        return parentContainerValue.getNewValue();
    }

}
