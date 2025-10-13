--
-- Copyright (C) 2010-2023 Evolveum and contributors
--
-- Licensed under the EUPL-1.2 or later.
--

CREATE INDEX iAExtensionPolyNorm
    ON m_assignment_ext_poly (norm) INITRANS 30;

CREATE INDEX iExtensionPolyNorm
    ON m_object_ext_poly (norm) INITRANS 30;
