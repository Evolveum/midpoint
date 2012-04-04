var objectFormHelpContainer = null;
var interval = 0;

window.onresize = function() {
	//setMenuPositionWhileScroll();
}; 

$(document).ready(function(){
	init();
});

function init() {
	//loadScript("js/less.js");
	setMenuPositionWhileScroll();
	
	$(".left-menu ul li").css("opacity", .8);
	$(".left-menu ul li a").css("opacity", .5);
	$(".left-menu ul").css("margin-top", - $(".left-menu ul").height() / 2);
	setTimeout("showLeftMenu()",500);
	$(".left-menu .selected-left").css("opacity", 1);
	$(".left-menu .selected-left").append("<img class='leftNavArrow' src='img/leftNavArrow.png' alt='' />");
	$(".left-menu .selected-left").parent().css("opacity", 1);
	$(".left-menu .selected-left").parent().css("background", "#333333");
	
	if ($.browser.msie && $.browser.version < 9.0){
		$(".acc .acc-section").css("height", "1px");
		$(".acc-content .sortedTable table").css("width", $(".acc-content").width());
	}
	
	$(".optionPanel").css("height",$(".optionRightBar").height());
	$(".optionLeftBar").css("height",$(".optionRightBar").height());
	
	$(".submitTable tbody input[type='checkbox']:checked").parent().parent().find("td").css("background","#d8f4d8");
	$(".submitTable tbody input[type='checkbox']:checked").parent().parent().find("td").css("border-color","#FFFFFF");
	
	$("thead input[type='checkbox']").click(function(){
		if($(this).is(":checked")){
			$(this).parent().parent().parent().parent().find("tbody").find("tr").find(".checkbox").find("input[type='checkbox']").attr("checked", true);
			$(this).parent().parent().parent().parent().find("tbody").find("td").css("background","#d8f4d8");
			$(this).parent().parent().parent().parent().find("tbody").find("td").css("border-color","#FFFFFF");
		} else {
			$(this).parent().parent().parent().parent().find("tbody").find("td").css("background","#FFFFFF");
			$(this).parent().parent().parent().parent().find("tbody").find("td").css("border-color","#F2F2F2");
			$(this).parent().parent().parent().parent().find("tbody").find("tr").find(".checkbox").find("input[type='checkbox']").attr("checked", false);
		}
	});
	
	$(".sortedTable table thead").find(".sortable").find("a").find("div").append("<span class='sortableArrowIcon'></span>");
	
	$(".left-menu ul").mouseenter(function(){
		$(".left-menu ul").stop();
		$(".left-menu ul").animate({left: 0}, {duration: 500, easing: "easeOutQuart"});
	}).mouseleave(function(){
		$(".left-menu ul").stop();
		$(".left-menu ul").animate({left: -252}, {duration: 500, easing: "easeOutQuart"});
	});
	
	$(".left-menu ul li").mouseenter(function(){
		$(this).stop();
		$(this).find("a").stop();
		if($(this).attr("class") != "leftMenuTopClear" && $(this).attr("class") != "leftMenuBottomClear"){
			$(this).animate({opacity : 1}, 200);
			$(this).find("a").animate({opacity : 1}, 250);
		}
	}).mouseleave(function(){
		$(this).stop();
		$(this).find("a").stop();
		if($(this).find("a").attr("class") != "selected-left"){
			$(this).animate({opacity : .8}, 200);
			$(this).find("a").animate({opacity : .5}, 250);
		}
	});
	
	$(".objectFormHeaderControllButtonMinMax").click(function(){
		var id = $(this).attr("id");
		var tableForm = id.substring((id.length) - 11, id.length); 
		
		if($(this).attr("src").indexOf("Minimize") != -1){
			var img = $(this).attr("src").replace("Minimize","Maximize");
			$(this).attr("src", img);
			$(this).attr("title", "Maximize");
			$("#tbody_" + tableForm).hide();
			$("#tfoot_" + tableForm).hide();
			
		} else {
			var img = $(this).attr("src").replace("Maximize","Minimize");
			$(this).attr("src", img);
			$(this).attr("title", "Minimize");
			$("#tbody_" + tableForm).show();
			$("#tfoot_" + tableForm).show();
		}
	});
	
	$(".objectFormHeaderControllButtonShowEmptyFields").click(function(){
		if($(this).attr("src").indexOf("Show") != -1){
			var img = $(this).attr("src").replace("Show","Hide");
			$(this).attr("src", img);
			$(this).attr("title", "Hide empty fields");
			
		} else {
			var img = $(this).attr("src").replace("Hide","Show");
			$(this).attr("src", img);
			$(this).attr("title", "Show empty fields");
		}
	});
	
	$(".objectFormAttribute").mouseenter(function(){
		objectFormHelpContainer = $(this).find(".objectFormHelpContainer");

		interval = setTimeout("showFormHelpContainer()",1000);
	}).mouseleave(function(){
		hideFormHelpContainer();
	});
	
	$(".sortedTable table tbody tr").mouseenter(function(){
		if($(this).find(".checkbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#c6e9c6");
		} else {
			$(this).find("td").css("background", "#f2f2f2");
		}
	}).mouseleave(function(){
		if($(this).find(".checkbox").find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
		} else {
			$(this).find("td").css("background", "#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		}
	});
	
	$(".submitTable tbody tr").mouseenter(function(){
		if($(this).find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#c6e9c6");
		} else {
			$(this).find("td").css("background", "#f2f2f2");
		}
	}).mouseleave(function(){
		if($(this).find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
		} else {
			$(this).find("td").css("background", "#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
			
		}
	});
	
	var el = $('.searchPanel');
    el.focus(function(e) {
        if (e.target.value == e.target.defaultValue)
            e.target.value = '';
    });
    el.blur(function(e) {
        if (e.target.value == '')
            e.target.value = e.target.defaultValue;
    });
    
    $(".accordion").togglepanels();
    
    $("#messages-topError").click(function(){
    	if($("#messages_error_content").css("display") === "none"){
    		$("#messages_error_content").show();
    		$(this).find(".messages-topError-arrow").addClass("arrow-up");
    		$(this).addClass("selected");
    	} else {
    		$("#messages_error_content").hide();
    		$(this).find(".messages-topError-arrow").removeClass("arrow-up");
    		$(this).removeClass("selected");
    	}
    });
    
    $(".messages-details-bold").click(function(){
    	var idBlock = $(this).attr("id");
    	if($(this).parent().find(".messages-details-content").css("display") === "none"){
    		$(this).parent().addClass("selected-section");
    		$("#"+idBlock+"_content").show();
    		$(this).find(".messages-details-bold-arrow").addClass("arrow-down");
    	} else {
    		$(this).parent().removeClass("selected-section");
    		$("#"+idBlock+"_content").hide();
    		$(this).find(".messages-details-bold-arrow").removeClass("arrow-down");
    	}
    });
    
    $(".errorStack").click(function(){
    	var idBlock = $(this).attr("id");
    	var text = "";
    	if($("#"+idBlock+"_content").css("display") === "none"){
    		text = $(this).text().replace("SHOW","HIDE");
    		$("#"+idBlock+"_content").show();
    	} else {
    		$("#"+idBlock+"_content").hide();
    		text = $(this).text().replace("HIDE","SHOW");
    	}
    	$(this).text(text);
    });
    
    $(".optionLeftBar").mouseenter(function(){
    	$(this).css("cursor","pointer");
    	$(this).css("backgroundColor","#BBBBBB");
    }).click(function(){
    	if($(".optionRightBar").css("display") == "none"){
    		$(".optionRightBar").show();
    		$(".optionLeftBarArrow").css("backgroundPosition","0px 0px");
    		$(".optionResult").css("padding-left", "220px");
    		unselectText();
    	} else {
    		$(".optionRightBar").hide();
    		$(".optionResult").css("padding-left", "0");
    		$(".optionLeftBarArrow").css("backgroundPosition","0px 16px");
    	}
    }).mouseleave(function(){
    	$(this).css("backgroundColor","#c9c9c9");
    });
    
}

function showLeftMenu() {
	if($(".left-menu ul").find(".leftMenuRow").html() != null){
		$(".left-menu ul").animate({left: -252}, {duration: 500, easing: "easeOutQuart"});
	}
}

function showFormHelpContainer(){
	objectFormHelpContainer.show();
	clearTimeout(interval);
}

function hideFormHelpContainer(){
	clearTimeout(interval);
	objectFormHelpContainer.hide();
}

function setMenuPositionWhileScroll() {
	if (($.browser.msie && $.browser.version >= 9.0) || (!$.browser.msie)) {
		$(window).scroll(function() {
			var scroll = $(window).scrollTop();
			if (scroll >= 60) {
				$(".top-menu").css("position", "fixed");
				$(".top-menu").css("top", "0px");
			} else {
				$(".top-menu").css("position", "absolute");
				$(".top-menu").css("top", "60px");
			}
		}); 
	}
	
	if ($.browser.msie && $.browser.version < 9.0){
		window.onresize = function() {
			$(".acc-content .sortedTable table").css("width", $(".acc-content").width());
		};
	}
}

function setFooterPos(){
	var winHeight = $(window).height();
	var contentHeight = $(".content").height() + 200;
	if(winHeight > contentHeight){
		$("#footer").css("position", "absolute");
		$("#footer").css("bottom", 0);
	} else {
		$("#footer").css("position", "relative");
		$("#footer").css("bottom", 0);
	}
}

$.fn.togglepanels = function(){
  return this.each(function(){
    $(this).addClass("ui-accordion ui-accordion-icons ui-widget ui-helper-reset")
  .find("h3")
    .addClass("ui-accordion-header ui-helper-reset ui-state-default ui-corner-top ui-corner-bottom")
    .hover(function() { $(this).toggleClass("ui-state-hover"); })
    .prepend('<span class="ui-icon ui-icon-triangle-1-e"></span>')
    .click(function() {
      $(this)
        .toggleClass("ui-accordion-header-active ui-state-active ui-state-default ui-corner-bottom")
        .find("> .ui-icon").toggleClass("ui-icon-triangle-1-e ui-icon-triangle-1-s").end()
        .next().slideToggle();
      return false;
    })
    .next()
      .addClass("ui-accordion-content ui-helper-reset ui-widget-content ui-corner-bottom")
      .hide();
  });
};

function loadScript(url){
    var script = document.createElement("script");
    script.type = "text/javascript";

    if (script.readyState){  //IE
        script.onreadystatechange = function(){
            if (script.readyState == "loaded" ||
                    script.readyState == "complete"){
                script.onreadystatechange = null;
                //callback();
            }
        };
    } else {  //Others
        script.onload = function(){
            //callback();
        };
    }
    script.src = url;
    document.getElementsByTagName("head")[0].appendChild(script);
}

function unselectText(){
	var sel ;
	if(document.selection && document.selection.empty){
		document.selection.empty() ;
	} else if(window.getSelection) {
		sel=window.getSelection();
		if(sel && sel.removeAllRanges)
		sel.removeAllRanges() ;
	}
}
