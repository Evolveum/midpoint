/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.util.Collection;

/**
 * Service that exposes security functions for internal use inside midPoint and for other
 * spring-security-enabled purposes.
 *
 * This is using simple {@link MidPointPrincipal} that is NOT GUI-enriched. Therefore it is NOT
 * suitable for use in GUI. See `GuiProfiledPrincipalManager` for that purpose.
 *
 * @author lazyman
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public interface MidPointPrincipalManager extends OwnerResolver {

    String DOT_CLASS = MidPointPrincipalManager.class.getName() + ".";
    String OPERATION_GET_PRINCIPAL = DOT_CLASS + "getPrincipal";
    String OPERATION_UPDATE_USER = DOT_CLASS + "updateUser";

    // This method is used from many test cases, and some of them require GUI Config.
    default MidPointPrincipal getPrincipal(String username, Class<? extends FocusType> clazz)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return getPrincipal(username, clazz, ProfileCompilerOptions.create());
    }

    // TODO add OperationResult here
    MidPointPrincipal getPrincipal(String username, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    // TODO add OperationResult here
    MidPointPrincipal getPrincipalByOid(String oid, Class<? extends FocusType> clazz, ProfileCompilerOptions options)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    MidPointPrincipal getPrincipal(PrismObject<? extends FocusType> focus, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

    MidPointPrincipal getPrincipal(
            PrismObject<? extends FocusType> focus, AuthorizationTransformer authorizationTransformer, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

    // TODO add OperationResult here
    void updateFocus(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas);

}
