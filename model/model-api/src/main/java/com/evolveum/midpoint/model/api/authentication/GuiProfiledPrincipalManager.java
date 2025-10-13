/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipalManager;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public interface GuiProfiledPrincipalManager extends MidPointPrincipalManager {

    @Override
    GuiProfiledPrincipal getPrincipal(String username, Class<? extends FocusType> clazz, ProfileCompilerOptions options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    GuiProfiledPrincipal getPrincipal(ObjectQuery query, Class<? extends FocusType> clazz, ProfileCompilerOptions options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    @Override
    GuiProfiledPrincipal getPrincipalByOid(String oid, Class<? extends FocusType> clazz, ProfileCompilerOptions options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    @Override
    GuiProfiledPrincipal getPrincipal(PrismObject<? extends FocusType> focus, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

    @Override
    GuiProfiledPrincipal getPrincipal(
            PrismObject<? extends FocusType> focus, AuthorizationTransformer authorizationTransformer, ProfileCompilerOptions options, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException;

    @Override
    void updateFocus(MidPointPrincipal principal, Collection<? extends ItemDelta<?, ?>> itemDeltas);

    List<UserSessionManagementType> getLocalLoggedInPrincipals();

    void terminateLocalSessions(TerminateSessionEvent terminateSessionEvent);

    @NotNull
    CompiledGuiProfile refreshCompiledProfile(GuiProfiledPrincipal guiProfiledPrincipal);

    @NotNull
    CompiledGuiProfile refreshCompiledProfile(GuiProfiledPrincipal guiProfiledPrincipal, ProfileCompilerOptions options);
}
