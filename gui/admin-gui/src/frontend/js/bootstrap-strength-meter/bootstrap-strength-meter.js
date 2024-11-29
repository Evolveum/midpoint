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
                base: 250,
                hierarchy: {
                    '0': ['progress-bar-danger', 'Very weak'],
                    '25': ['progress-bar-danger', 'Weak'],
                    '50': ['progress-bar-warning', 'Good'],
                    '75': ['progress-bar-success', 'Strong'],
                    '100': ['progress-bar-success', 'Very strong']
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
                settings.passwordScore.options = Score.prototype.options.concat(strengthMeterOptions);

            var progressBar;
            var passcheckTimeout;
            var core = {

                /**
                 * Initialize the plugin.
                 */
                init: function() {
                    progressBar = settings.container;
                    progressBar.attr('aria-valuemin', 0)
                            .attr('aria-valuemax', 100);

                    input.on('input', core.keyup);
                    input.on('keyup', core.keyup)
                            .keyup();
                },
                queue: function(event){
                    var password = $(event.target).val();
                    var value = 0;

                    if (password.length > 0) {
                        if (password.length < 50) {
                            var score = new Score(password);
                            value = score.calculateEntropyScore(settings.passwordScore.options, settings.passwordScore.append);
                        } else {
                            console.debug("Hitting password size limit for calculating entropy")
                            value = 1000;
                        }
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
                            .attr('aria-valuenow', width)
                            .css('width', width + '%');

                    for (var value in settings.hierarchy) {
                        if (width >= value) {
                            var text = settings.hierarchy[value][1];
                            var color = settings.hierarchy[value][0];

                            progressBar.text(text)
                                .removeClass()
                                .addClass('progress-bar')
                                .addClass(color)
                                .css('min-width', (text.length*7) + 'px');
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
