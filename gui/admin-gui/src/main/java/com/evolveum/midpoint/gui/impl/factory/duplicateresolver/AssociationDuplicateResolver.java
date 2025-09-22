package com.evolveum.midpoint.gui.impl.factory.duplicateresolver;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
@Component
public class AssociationDuplicateResolver extends ContainerDuplicateResolver<ShadowAssociationTypeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationDuplicateResolver.class);

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), ShadowAssociationTypeDefinitionType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ShadowAssociationTypeDefinitionType duplicateObject(ShadowAssociationTypeDefinitionType originalBean, PageBase pageBase) {
        PrismContainerValue<ShadowAssociationTypeDefinitionType> originalObject = originalBean.asPrismContainerValue();
        PrismContainerValue<ShadowAssociationTypeDefinitionType> duplicate =
                DuplicationProcessHelper.duplicateContainerValueDefault(originalObject);
        @NotNull ShadowAssociationTypeDefinitionType duplicatedBean = duplicate.asContainerable();

        QName name = originalBean.getName();

        String displayName;
        if (StringUtils.isEmpty(originalBean.getDisplayName())) {
            displayName = name.getLocalPart();
        } else {
            displayName = originalBean.getDisplayName();
        }

        String copyOf = LocalizationUtil.translate("DuplicationProcessHelper.copyOf", new Object[] { displayName });

        duplicatedBean
                .name(new QName(
                        name.getNamespaceURI(),
                        LocalizationUtil.translate("DuplicationProcessHelper.copyOf", new Object[] { name.getLocalPart() }),
                        name.getPrefix()))
                .displayName(copyOf)
                .lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED)
                .description(copyOf +
                        (originalBean.getDescription() == null ? "" : (System.lineSeparator() + originalBean.getDescription())));

        fixAssociationRef(originalBean, duplicatedBean);

        return duplicatedBean;
    }

    /**
     * Duplicate without adding "copyOf" to name, displayName, or description.
     */
    public ShadowAssociationTypeDefinitionType duplicateObjectWithoutCopyOf(ShadowAssociationTypeDefinitionType originalBean) {
        PrismContainerValue<ShadowAssociationTypeDefinitionType> originalObject = originalBean.asPrismContainerValue();
        PrismContainerValue<ShadowAssociationTypeDefinitionType> duplicate =
                DuplicationProcessHelper.duplicateContainerValueDefault(originalObject);
        @NotNull ShadowAssociationTypeDefinitionType duplicatedBean = duplicate.asContainerable();

        duplicatedBean
                .name(originalBean.getName())
                .displayName(originalBean.getDisplayName())
                .lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED)
                .description(originalBean.getDescription());

        fixAssociationRef(originalBean, duplicatedBean);

        return duplicatedBean;
    }

    /**
     * Extracted logic to handle association ref uniqueness.
     */
    private void fixAssociationRef(ShadowAssociationTypeDefinitionType originalBean,
            @NotNull ShadowAssociationTypeDefinitionType duplicatedBean) {
        if (duplicatedBean.getSubject() != null
                && duplicatedBean.getSubject().getAssociation() != null
                && duplicatedBean.getSubject().getAssociation().getRef() != null
                && !duplicatedBean.getSubject().getAssociation().getRef().getItemPath().isEmpty()) {

            try {
                QName refQName = ((NameItemPathSegment) duplicatedBean.getSubject().getAssociation().getRef().getItemPath().first()).getName();
                String origLocalPart = refQName.getLocalPart();
                int index = 1;
                while (AssociationChildWrapperUtil.existAssociationConfiguration(
                        refQName.getLocalPart(),
                        (PrismContainer<ShadowAssociationTypeDefinitionType>) originalBean.asPrismContainerValue().getParent())) {

                    refQName = new QName(refQName.getNamespaceURI(), origLocalPart + index, refQName.getPrefix());
                    index++;
                }
                duplicatedBean.getSubject().getAssociation().ref(new ItemPathType(ItemPath.create(refQName)));
            } catch (SchemaException e) {
                LOGGER.error("Couldn't resolve association ref attribute.", e);
            }
        }
    }
}
