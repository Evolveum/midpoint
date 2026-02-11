/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.mel.value.AbstractContainerValueCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ContainerValueCelValue;
import com.evolveum.midpoint.model.common.expression.script.mel.value.ObjectCelValue;

import com.evolveum.midpoint.model.common.expression.script.mel.value.QNameCelValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.NullValue;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.*;

/**
 * Extensions for CEL compiler and runtime implementing behavior of Prism
 * objects, properties and other items and value.
 *
 * TODO: merge with CelObjectExtensions
 *
 * @author Radovan Semancik
 */
public class CelPrismItemsExtensions extends AbstractMidPointCelExtensions {

    public static final Trace LOGGER = TraceManager.getTrace(CelPrismItemsExtensions.class);

    public CelPrismItemsExtensions() {
        super();
        initialize();
    }

    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

            // This is supposed to handle prismobject[qname] as well, as cel-java seem to handle both
            // ContainerValueCelValue.CEL_TYPE and ObjectCelValue.CEL_TYPE as DYN.
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "index_map",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-container-index_map-qname",
                                    "Resolves a structure using a QName",
                                    SimpleType.ANY,
                                    ContainerValueCelValue.CEL_TYPE,
                                    QNameCelValue.CEL_TYPE)),
                    CelFunctionBinding.from("prism-container-index_map-qname", ContainerValueCelValue.class, QNameCelValue.class,
                            CelPrismItemsExtensions::prismIndexMap)),

            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "find",
                            CelOverloadDecl.newMemberOverload(
                                    "prism-object-find-string",
                                    "Returns an item to which the specified item path refers.",
                                    SimpleType.ANY,
                                    ObjectCelValue.CEL_TYPE,
                                    SimpleType.STRING)),
                    CelFunctionBinding.from("prism-object-find-string", ObjectCelValue.class, String.class,
                            CelPrismItemsExtensions::prismFind))
        );
    }

    private static Object prismIndexMap(AbstractContainerValueCelValue<?> celValue, QNameCelValue celQName) {
        if (CelTypeMapper.isCellNull(celValue) || CelTypeMapper.isCellNull(celQName)) {
            return NullValue.NULL_VALUE;
        }
        return CelTypeMapper.toCelValue(celValue.getContainerValue().find(ItemName.fromQName(celQName.getQName())));
    }

    ;

    private static final class Library implements CelExtensionLibrary<CelPrismItemsExtensions> {
        private final CelPrismItemsExtensions version0;

        private Library() {
            version0 = new CelPrismItemsExtensions();
        }

        @Override
        public String name() {
            return "prism";
        }

        @Override
        public ImmutableSet<CelPrismItemsExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    private static final Library LIBRARY = new Library();

    public static CelExtensionLibrary<CelPrismItemsExtensions> library() {
        return LIBRARY;
    }

    @Override
    public int version() {
        return 0;
    }

    public static Object prismFind(ObjectCelValue<?> objectCelValue, String stringPath) {
//        LOGGER.info("EEEEEEEX prismIndexString({},{})", objectCelValue, stringPath);
        Object o = objectCelValue.getObject().find(PrismContext.get().itemPathParser().asItemPath(stringPath));
//        LOGGER.info("EEEEEEEY prismIndexString({},{}): {}", objectCelValue, stringPath, o);
        if (o == null) {
            return NullValue.NULL_VALUE;
        }
        return o;
    }

}
