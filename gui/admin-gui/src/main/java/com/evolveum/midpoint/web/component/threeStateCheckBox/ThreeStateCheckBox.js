var STATE_UNCHECKED = "UNCHECKED";
var STATE_CHECKED = "CHECKED";
var STATE_UNDEFINED = "UNDEFINED";
var checkbox = {};

var UNCHECKED_NORM = 'UNCHECKED_NORM';
var UNCHECKED_HILI = 'UNCHECKED_HILI';
var INTERMEDIATE_NORM = 'INTERMEDIATE_NORM';
var INTERMEDIATE_HILI = 'INTERMEDIATE_HILI';
var CHECKED_NORM = 'CHECKED_NORM';
var CHECKED_HILI = 'CHECKED_HILI';

var DEFAULT_CONFIG = {
	UNCHECKED_NORM : 'treeStateCheckBox unchecked',
	UNCHECKED_HILI : 'treeStateCheckBox unchecked_highlighted',
	INTERMEDIATE_NORM : 'treeStateCheckBox intermediate',
	INTERMEDIATE_HILI : 'treeStateCheckBox intermediate_highlighted',
	CHECKED_NORM : 'treeStateCheckBox checked',
	CHECKED_HILI : 'treeStateCheckBox checked_highlighted'
};

function initThreeStateCheckBox(threeStateCheckBoxId) {
	var CHECKBOX = new Object();
	checkbox[threeStateCheckBoxId] = CHECKBOX;
	CHECKBOX.ID = threeStateCheckBoxId;
	CHECKBOX.FIRST_STATE = "";
	createThreeStateImageNode(threeStateCheckBoxId);
	updateStateAndImage(threeStateCheckBoxId);
}

function getNextStateFromValue(theValue, fieldId) {
	if(checkbox[fieldId].FIRST_STATE == UNCHECKED_NORM) {
		if (theValue == STATE_CHECKED) { return STATE_UNDEFINED; }
		if (theValue == STATE_UNCHECKED) { return STATE_CHECKED; }
		return STATE_UNCHECKED;
	} else {
		if (theValue == STATE_CHECKED) { return STATE_UNCHECKED; }
		if (theValue == STATE_UNCHECKED) { return STATE_UNDEFINED; }
		return STATE_CHECKED;
	}
	
	
}
function getStateFromValue(theValue, highlightedState) {
	if (theValue == STATE_UNDEFINED) { return (!highlightedState) ? INTERMEDIATE_NORM : INTERMEDIATE_HILI; }
	if (theValue == STATE_CHECKED) { return (!highlightedState) ? CHECKED_NORM : CHECKED_HILI; }
	return (!highlightedState) ? UNCHECKED_NORM : UNCHECKED_HILI;
}

function getFieldId(imageId) {
	var threeStateBoxId = imageId.substring(0, imageId.length - '.Img'.length);
	return threeStateBoxId;
}

function replaceImage(imageId, imageClass) {
	var image = document.getElementById(imageId);
	if (image.className != imageClass) {
		image.className = imageClass;
	}
}
function mouseOverOutOfImage(imageId, mouseOverMode) {

	var fieldId = getFieldId(imageId);
	var threeStateBoxField = document.getElementById(fieldId);
	var currentState = getStateFromValue(threeStateBoxField.value, mouseOverMode);
	if(checkbox[fieldId].FIRST_STATE == "") {
		checkbox[fieldId].FIRST_STATE = currentState;
	}
	return DEFAULT_CONFIG[currentState];
}
function onMouseOverImage(imageId) {
	return function() {
		var imageClass = mouseOverOutOfImage(imageId, true);
		replaceImage(imageId, imageClass);
	};
}
function onMouseOutImage(imageId) {
	return function() {
		var imageClass = mouseOverOutOfImage(imageId, false);
		replaceImage(imageId, imageClass);
	};
}
function onThreestateImageClick(imageId) {
	return function() {
		var fieldId = getFieldId(imageId);
		var threeStateBoxField = document.getElementById(fieldId);
		var nextState = getNextStateFromValue(threeStateBoxField.value, fieldId);
		threeStateBoxField.value = nextState;
		var imageClass = mouseOverOutOfImage(imageId, true);
		replaceImage(imageId, imageClass);
	};
}

function updateStateAndImage(threeStateCheckBoxId) {
	var imageNode = document.getElementById(threeStateCheckBoxId + ".Img");
	var imageClass = mouseOverOutOfImage(imageNode.id, false);
	replaceImage(imageNode.id, imageClass);
}

function createThreeStateImageNode(threeStateCheckBoxId) {
	var boxElement = document.getElementById(threeStateCheckBoxId + "Element");
	var imageNode = document.getElementById(threeStateCheckBoxId + ".Img");

	if (boxElement.addEventListener) {
		boxElement.addEventListener('mouseover', onMouseOverImage(imageNode.id), false);
		boxElement.addEventListener('mouseout', onMouseOutImage(imageNode.id), false);
		boxElement.addEventListener('click', onThreestateImageClick(imageNode.id), false);
	} else if (boxElement.attachEvent) {
		boxElement.attachEvent('onmouseover', onMouseOverImage(imageNode.id));
		boxElement.attachEvent('onmouseout', onMouseOutImage(imageNode.id));
		boxElement.attachEvent('onclick', onThreestateImageClick(imageNode.id));
	}
}
