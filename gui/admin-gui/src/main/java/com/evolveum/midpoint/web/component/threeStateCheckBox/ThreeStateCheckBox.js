var STATE_NONE = "UNCHECKED";
var STATE_ALL = "CHECKED";
var STATE_SOME = "UNDEFINED";

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

function getNextStateFromValue(theValue) {
	if (theValue == STATE_ALL) { return STATE_SOME; }
	if (theValue == STATE_SOME) { return STATE_NONE; }
	return STATE_ALL;
}
function getStateFromValue(theValue, highlightedState) {
	if (theValue == STATE_SOME) { return (!highlightedState) ? INTERMEDIATE_NORM : INTERMEDIATE_HILI; }
	if (theValue == STATE_ALL) { return (!highlightedState) ? CHECKED_NORM : CHECKED_HILI; }
	return (!highlightedState) ? UNCHECKED_NORM : UNCHECKED_HILI;
}

function getFieldId(imageId) {
	var threeStateBoxId = imageId.substring(0, imageId.length - '.Img'.length);
	var threeStateBoxFieldId = document.getElementById(threeStateBoxId + '.Field').value;
	return threeStateBoxFieldId;
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
		var fieldAndContainerIds = getFieldId(imageId);
		var threeStateBoxField = document.getElementById(fieldAndContainerIds);

		var nextState = getNextStateFromValue(threeStateBoxField.value);
		
		threeStateBoxField.value = nextState;
		var imageClass = mouseOverOutOfImage(imageId, true);
		replaceImage(imageId, imageClass);
	};
}

function updateStateAndImage(imageNodeId) {
	var imageClass = mouseOverOutOfImage(imageNodeId, false);
	replaceImage(imageNodeId, imageClass);
}

function createThreeStateImageNode(threeStateCheckBox, imageNodeId) {
	var parent = threeStateCheckBox.parentNode;
	var boxElement = document.createElement("span"); 
	parent.appendChild(boxElement);
	boxElement.className = "threeStateCheckBoxElement";
	boxElement.id = threeStateCheckBox.id + "Element";
	
	boxElement.appendChild(threeStateCheckBox);
	var imageNode = new Image();
	imageNode.id = imageNodeId;
	imageNode.className = DEFAULT_CONFIG[UNCHECKED_NORM];
	boxElement.appendChild(imageNode);

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
function createFieldNameHiddenField(threeStateCheckBox, fieldNameNodeId, threeStateCheckBoxId, labelString) {
	var parent = threeStateCheckBox.parentNode;
	var fieldNode = document.createElement('input');
	fieldNode.id = fieldNameNodeId;
	fieldNode.type = 'hidden';
	fieldNode.value = threeStateCheckBoxId;
	parent.appendChild(fieldNode);

	if(labelString != '') {
		var text = document.createTextNode(labelString);
		var labelElement = document.createElement("span"); 
		labelElement.className = "threeStateLabel";
		labelElement.innerHTML = '';
		parent.appendChild(labelElement);
		labelElement.appendChild(text);
	}
	
}

function initThreeStateCheckBox(threeStateCheckBoxId, labelString) {
	var threeStateCheckBox = document.getElementById(threeStateCheckBoxId);

	var imageNodeId = threeStateCheckBoxId + '.Img';
	createThreeStateImageNode(threeStateCheckBox, imageNodeId);

	var fieldNameNodeId = threeStateCheckBoxId + '.Field';
	createFieldNameHiddenField(threeStateCheckBox, fieldNameNodeId, threeStateCheckBoxId, labelString);

	updateStateAndImage(imageNodeId);
	setStyle(threeStateCheckBox);
}

function setStyle(threeStateCheckBox) {
	var boxElement = document.getElementById(threeStateCheckBox.id + "Element");
	$("#" + boxElement.id).css(threeStateCheckBox.style);
}