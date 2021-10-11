/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

var accordion = {};
var TINY = {};

function getAccordion(accordionId) {
        return document.getElementById(accordionId);
}
function getPanel(accordionControl, child) {
        return child.getElementsByTagName(accordionControl);
}

TINY.accordion = function() {

        function slider(accordionName) {
                this.accordionName = accordionName;
                this.accordions = [];
        }

        slider.prototype.init = function(accordionId, accordionControl, accordionMultipleSelect, accordionOpenedPanel, accordionSelectedAttr) {
                var accordionComponent = getAccordion(accordionId), i = s = 0;
                var accordionChilds = accordionComponent.childNodes;
                var length = accordionChilds.length;

                this.selectedAttr = accordionSelectedAttr || 0;
                this.multipleSelect = accordionMultipleSelect || 0;
                for (i; i < length; i++) {
                        var child = accordionChilds[i];
                        if (child.nodeType != 3) {
                                this.accordions[s] = {};
                                var header = this.accordions[s].header = getPanel(accordionControl, child)[0];
                                var content = this.accordions[s].content = getPanel('div', child)[0];
                                header.onclick = new Function(this.accordionName + '.expand(0,' + s + ')');

                                if (accordionOpenedPanel == s) {
                                        header.className = this.selectedAttr;
                                        content.style.height = 'auto';
                                        content.opened = 1;
                                } else {
                                        content.style.height = 0;
                                        content.opened = -1;
                                }
                                s++;
                        }
                }
                this.length = s;
        };

        slider.prototype.expand = function(state, component) {
                for ( var i = 0; i < this.length; i++) {
                        var header = this.accordions[i].header;
                        var content = this.accordions[i].content;
                        var currentHeight = content.style.height;
                        currentHeight = currentHeight == 'auto' ? 1 : parseInt(currentHeight);
                        clearInterval(content.timer);

                        if ((currentHeight != 1 && content.opened == -1) && (state == 1 || i == component)) {
                                content.style.height = '';
                                content.totalHeight = content.offsetHeight;
                                content.style.height = currentHeight + 'px';
                                content.opened = 1;
                                header.className = this.selectedAttr;
                                timer(content, 1);

                            $(content).css('overflow', 'visible');
                        } else if (currentHeight > 0 && (state == -1 || this.multipleSelect || i == component)) {
                                content.opened = -1;
                                header.className = '';
                                timer(content, -1);

                            $(content).css('overflow', 'hidden');
                        }
                }
        };

        function timer(content) {
                content.timer = setInterval(function() {slide(content);}, 20);
        }
        function slide(content) {
                var totalHeight = content.offsetHeight;
                var opened = content.opened == 1 ? content.totalHeight - totalHeight : totalHeight;
                content.style.height = totalHeight + (Math.ceil(opened / 4) * content.opened) + 'px';
                content.style.opacity = totalHeight / content.totalHeight;
                content.style.filter = 'alpha(opacity=' + totalHeight * 100 / content.totalHeight + ')';
                if ((content.opened == 1 && totalHeight >= content.totalHeight) || (content.opened != 1 && totalHeight == 1)) {
                        if (content.opened == 1) {
                                content.style.height = 'auto';
                        }
                        clearInterval(content.timer);
                }
        };
        return {
                slider : slider
        };
}();

function createAccordion(id, expanded, multipleSelect, openedPanel) {
        accordion[id] = new TINY.accordion.slider("accordion['" + id + "']");
        accordion[id].init(id, "h3", multipleSelect, openedPanel, "acc-selected");

        if (expanded) {
                accordion[id].expand(1);
        }
}