/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * @DEPRECATED
 *
 * this probably can be deleted [lazyman]
 * there is only stuff for preview page, and it will be disabled.
 *
 * 1/ checkboxes are handled through wicket
 * 2/ user preview table stuff should not be here (I don't even know if it's still used)
 */
function initTable(){
	var cssSelectedRow = {
      'background' : '#d8f4d8',
      'border-color' : '#FFFFFF'
    };
	
	var cssAddedValue = {
		      'background' : '#d8f4d8',
		      'border-color' : '#FFFFFF'
		    };
	
	var cssSecondaryValue = {
		      'background' : '#E0F0FF',
		      'border-color' : '#FFFFFF'
		    };
	
	var cssDeletedValue = {
		      'background' : '#FFD7CA',
		      'border-color' : '#FFFFFF'
		    };

/*         now handled properly through wicket
//TODO colors and other css properties must be in LESS file (for later configuration)

	$(".sortedTable table tbody tr").click(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find(".tableCheckbox").find("input[type='checkbox']").attr("checked", false);
			$(this).find("td").css("background","#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		} else {
			$(this).find(".tableCheckbox").find("input[type='checkbox']").attr("checked", true);
			$(this).find("td").css("background","#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
			
		}
		checkAllChecked($(this).parents(".sortedTable"));
	});
	
	$(".sortedTable td input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
			$(this).attr("checked", false);
			$(this).closest("tr").find("td").css("background","#d8f4d8");
			$(this).closest("tr").find("td").css("border-color","#FFFFFF");
			
		} else {
			$(this).attr("checked", true);
			$(this).parents("tr").find("td").css("background","#FFFFFF");
			$(this).parents("tr").find("td").css("border-color","#F2F2F2");
		}
		checkAllChecked($(this).parents(".sortedTable"));
	});
//	
//	$(document).find(".sortedTable").each(function(index){
//		var row =  $(this).find("table tbody tr");
//		if(row.find(".tableCheckbox").length > 0) {
//			row.find(".tableCheckbox").find("input[type='checkbox']:checked").parents("tr:first").find("td").css(cssSelectedRow);
//			checkAllChecked($(this));
//		}
//	});
*/
	

	$(document).find(".sortedTable .secondaryValue").each(function(){
		$(this).parents("tr:first").find("td").css(cssSecondaryValue);
	});
	
	$(document).find(".sortedTable .deletedValue").each(function(){
		$(this).parents("tr:first").find("td").css(cssDeletedValue);
	});
	
	$(document).find(".sortedTable .addedValue").each(function(){
		$(this).parents("tr:first").find("td").css(cssAddedValue);
	});
	

    /*
	$("thead input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
//			$(this).parents(".sortedTable").find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", true);
			$(this).parents(".sortedTable").find("tbody").find("td").css("background","#d8f4d8");
			$(this).parents(".sortedTable").find("tbody").find("td").css("border-color","#FFFFFF");
		} else {		
			$(this).parents(".sortedTable").find("tbody").find("tr").each(function() {
				var deleted = false;
				$(this).find("img").each(function() {
					if($(this).attr("class") == "deletedValue") {
						deleted = true;
					}
				});
				
				$(this).find("td").each(function() {
					if($(this).attr("class") == "deletedValue") {
						deleted = true;
					}
				});
				
				if(deleted) {
					$(this).find("td").css(cssDeletedValue);
				} else {
					$(this).find("td").css("background","#FFFFFF");
					$(this).find("td").css("border-color","#F2F2F2");
				}
			});
//			$(this).parents(".sortedTable").find("tbody").find("tr").find(".tableCheckbox").find("input[type='checkbox']").attr("checked", false);
		}
	});
	*/

/*
	function checkAllChecked(parent) {
		if(parent.find("tbody tr").find(".tableCheckbox").length > 0) {
			var isAllChecked = false;
			
			parent.find("tbody tr").find(".tableCheckbox").find("input[type='checkbox']").each(function(index){
				if($(this).is(":checked")){
					isAllChecked = true;
				} else {
					isAllChecked = false;
					return false;
				}
			});
			if(isAllChecked) {
				parent.find("thead").find("input[type='checkbox']").attr("checked", true);
			} else {
				parent.find("thead").find("input[type='checkbox']").attr("checked", false);
			}
		}
	}
*/
	
	             /*
	$(".sortedTable table tbody tr").mouseenter(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#c6e9c6");
		} else if($(this).find(".deletedValue").length > 0) {
			$(this).find("td").css("background", "#FFC2AE");
		} else if($(this).find(".secondaryValue").length > 0) {
			$(this).find("td").css("background", "#D0E0FF");
		} else if($(this).find(".addedValue").length > 0) {
			$(this).find("td").css("background", "#c6e9c6");
		} else {
			$(this).find("td").css("background", "#f2f2f2");
			$(this).find("td").css("border-color","#FFFFFF");
		}
	}).mouseleave(function(){
		if($(this).find(".tableCheckbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
		} else if($(this).find(".deletedValue").length > 0) {
			$(this).find("td").css("background", "#FFD7CA");
		} else if($(this).find(".secondaryValue").length > 0) {
			$(this).find("td").css("background", "#E0F0FF");
		} else if($(this).find(".addedValue").length > 0) {
			$(this).find("td").css("background", "#d8f4d8");
		} else {
			$(this).find("td").css("background", "#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		}
	})
	*/
//        .find(".tableCheckbox").find("input[type='checkbox']").click(function(){
//		checkAllChecked($(this).parents(".sortedTable"));
//	});

}