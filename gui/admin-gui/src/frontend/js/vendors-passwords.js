/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

// we've added global variable export using window.Score = Score; (at the end of the file)
require("./password-score/password-score");
require("./password-score/password-score-options");

require("./bootstrap-strength-meter/bootstrap-strength-meter");
// we've added global variable export using window.strengthMeterOptions = strengthMeterOptions; (at the end of the file)
require("./bootstrap-strength-meter/bootstrap-strength-meter-options");
