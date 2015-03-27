function getJsonFromUrl() {
  var query = location.search.substr(1);
  var result = {};
  query.split("&").forEach(function(part) {
    var item = part.split("=");
    result[item[0]] = decodeURIComponent(item[1]);
  });
  return result;
}

function ready(fn) {
  if (document.readyState != 'loading'){
    fn();
  } else {
    document.addEventListener('DOMContentLoaded', fn);
  }
}

function callAjax(url, callback){
	var request = new XMLHttpRequest();
	request.open('GET', url, true);

	request.onload = function() {
		if (this.status >= 200 && this.status < 400) {
			var data = JSON.parse(this.response);
			callback(data);
		} else {
		 // We reached our target server, but it returned an error
		}
	};

	request.onerror = function() {
	  // There was a connection error of some sort
	};

	request.send();
}

function idpOptions(idpInfos){
	var lastIdp = Cookies.get('lastChosenIdp');

	function withSelected(idpInfo){
		return _.extend({selected: idpInfo.id == lastIdp ? "selected" : ""}, idpInfo);
	}

	var template = _.template('<option value="<%= id %>" <%= selected %> ><%= name %></option>');

	var optionFun = _.compose(template, withSelected);

	return _.map(idpInfos, optionFun).join('\n');
}

ready(function(){

	callAjax('/saml/idps', function(idpInfos){
		var optionsHtml = idpOptions(idpInfos);
		var idpSelect = document.getElementById('GET-idpUrl');
		idpSelect.innerHTML = optionsHtml;
	});

	var targetUrl = getJsonFromUrl().targetUrl;
	
	if(targetUrl){
		var urlRelays = document.querySelectorAll('input.targetUrlRelay');
		for(var i=0; i<urlRelays.length; i++){
			var input = urlRelays.item(i);
			input.setAttribute('value', targetUrl);
		}
	}
});



function doPlainLogin() {
	
	$form = $('#plain-login').serializeArray();
	
    var ajax = $.post("/password/login", $form);
    		
	ajax.complete(function(data) {
		
		if (data.status == '200') {
			goToSite();
			
		} else if (data.status == '403') {
			somePlainFail('You have not been able to be authenticated. Maybe have you entered wrong email/password. Please try again!');
			
		} else if (data.status == '500') {
			somePlainFail('Unfortunately there is some server error. Please try again!');
			
		}
		
	});	
}

function goToSite() {
	window.location = getTargetUrl();
}

function getTargetUrl() {
	var url = window.location.href;
	
	if (url.search('targetUrl=') > 0) {
		var target = url.substring(url.indexOf('?') + 1);
		target = target.replace('targetUrl=', '');
		
	} else {
		target = 'https://www.icos-cp.eu';
	}
	
    return target;
}

function somePlainFail(message) {
	$('#plain-fail').html(message);
	$('#plain-fail').addClass('alert alert-danger');
	$('#plain-fail').attr('role', 'alert');
	$('#plain-fail').show();	
}