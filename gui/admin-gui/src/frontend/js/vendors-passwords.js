/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

// we've added global variable export using window.Score = Score; (at the end of the file)
require("./password-score/password-score");
require("./password-score/password-score-options");

require("./bootstrap-strength-meter/bootstrap-strength-meter");
// we've added global variable export using window.strengthMeterOptions = strengthMeterOptions; (at the end of the file)
require("./bootstrap-strength-meter/bootstrap-strength-meter-options");
