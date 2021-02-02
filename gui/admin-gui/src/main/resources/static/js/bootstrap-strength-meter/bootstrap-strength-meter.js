/**
 * bootstrap-strength-meter.js
 * https://github.com/davidstutz/bootstrap-strength-meter
 *
 * Copyright 2013 - 2019 David Stutz
 */
!function($) {

    "use strict";// jshint ;_;

    var StrengthMeter = {

        progressBar: function(input, options) {

            var defaults = {
                container: input.parent(),
                base: 80,
                hierarchy: {
                    '0': 'progress-bar-danger',
                    '50': 'progress-bar-warning',
                    '100': 'progress-bar-success'
                },
                passwordScore: {
                    options: [],
                    append: true
                }
            };

            var settings = $.extend(true, {}, defaults, options);

            if (typeof options === 'object' && 'hierarchy' in options) {
                settings.hierarchy = options.hierarchy;
            }

            var progressBar;
            var passcheckTimeout;
            var core = {

                /**
                 * Initialize the plugin.
                 */
                init: function() {
                    progressBar = settings.container;
                    progressBar.attr('aria-valuemin', 0)
                            .attr('aria-valuemay', 100);

                    input.on('keyup', core.keyup)
                            .keyup();
                },
                queue: function(event){
                    var password = $(event.target).val();
                    var value = 0;

                    if (password.length > 0) {
                        var score = new Score(password);
                        value = score.calculateEntropyScore(settings.passwordScore.options, settings.passwordScore.append);
                    }

                    core.update(value);
                  },

                /**
                 * Update progress bar.
                 *
                 * @param {string} value
                 */
                update: function(value) {
                    var width = Math.floor((value/settings.base)*100);

                    if (width > 100) {
                        width = 100;
                    }

                    progressBar
                            .attr('area-valuenow', width)
                            .css('width', width + '%');

                    for (var value in settings.hierarchy) {
                        if (width >= value) {
                            progressBar
                                    .removeClass()
                                    .addClass('progress-bar')
                                    .addClass(settings.hierarchy[value]);
                        }
                    }
                },

                /**
                 * Event binding on password input.
                 *
                 * @param {Object} event
                 */
                keyup: function(event) {
                    if(passcheckTimeout)clearTimeout(passcheckTimeout);
                    passcheckTimeout = setTimeout( function(){
                        core.queue(event);
                    },500);
                }
            };

            core.init();
        },
        text: function(input, options) {

            var defaults = {
                container: input.parent(),
                hierarchy: {
                    '0': ['text-danger', 'ridiculous'],
                    '25': ['text-danger', 'very weak'],
                    '50': ['text-warning', 'weak'],
                    '75': ['text-warning', 'good'],
                    '100': ['text-success', 'strong'],
                    '125': ['text-success', 'very strong']
                },
                passwordScore: {
                    options: [],
                    append: true
                }
            };

            var settings = $.extend(true, {}, defaults, options);

            if (typeof options === 'object' && 'hierarchy' in options) {
                settings.hierarchy = options.hierarchy;
            }

            var core = {

                /**
                 * Initialize the plugin.
                 */
                init: function() {
                    input.on('keyup', core.keyup)
                        .keyup();
                },

                /**
                 * Update text element.
                 *
                 * @param {string} value
                 */
                update: function(value) {
                    for (var border in settings.hierarchy) {
                         if (value >= border) {
                             var text = settings.hierarchy[border][1];
                             var color = settings.hierarchy[border][0];

                             settings.container.text(text)
                                .removeClass()
                                .addClass(color);
                         }
                    }
                },

                /**
                 * Event binding on input element.
                 *
                 * @param {Object} event
                 */
                keyup: function(event) {
                    var password = $(event.target).val();
                    var value = 0;

                    if (password.length > 0) {
                        var score = new Score(password);
                        value = score.calculateEntropyScore(settings.passwordScore.options, settings.passwordScore.append);
                    }

                    core.update(value);
                }
            };

            core.init();
        },

        tooltip: function(input, options) {

            var defaults = {
                hierarchy: {
                    '0': 'ridiculous',
                    '25': 'very weak',
                    '50': 'weak',
                    '75': 'good',
                    '100': 'strong',
                    '125': 'very strong'
                },
                tooltip: {
                    placement: 'right'
                },
                passwordScore: {
                    options: [],
                    append: true
                }
            };

            var settings = $.extend(true, {}, defaults, options);

            if (typeof options === 'object' && 'hierarchy' in options) {
                settings.hierarchy = options.hierarchy;
            }

            var core = {

                /**
                 * Initialize the plugin.
                 */
                init: function() {
                    input.tooltip(settings.tooltip);

                    input.on('keyup', core.keyup)
                        .keyup();
                },

                /**
                 * Update tooltip.
                 *
                 * @param {string} value
                 */
                update: function(value) {
                    for (var border in settings.hierarchy) {
                         if (value >= border) {
                             var text = settings.hierarchy[border];

                             input.attr('data-original-title', text)
                                    .tooltip('show');
                         }
                    }
                },

                /**
                 * Event binding on input element.
                 *
                 * @param {Object} event
                 */
                keyup: function(event) {
                    var password = $(event.target).val();
                    var value = 0;

                    if (password.length > 0) {
                        var score = new Score(password);
                        value = score.calculateEntropyScore(settings.passwordScore.options, settings.passwordScore.append);
                    }

                    core.update(value);
                }
            };

            core.init();
        }
    };

    $.fn.strengthMeter = function(type, options) {
        type = (type === undefined) ? 'tooltip' : type;

        if (!type in StrengthMeter) {
            return;
        }

        var instance = this.data('strengthMeter');
        var elem = this;

        return elem.each(function() {
            var strengthMeter;

            if (instance) {
                return;
            }

            strengthMeter = StrengthMeter[type](elem, options);
            elem.data('strengthMeter', strengthMeter);
        });
    };

}(window.jQuery);
