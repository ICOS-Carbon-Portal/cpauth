function urlQueryAsObject() {
	var query = location.search.substr(1);
	var result = {};
	query.split("&").forEach(function(part) {
		var item = part.split("=");
		result[item[0]] = decodeURIComponent(item[1]);
	});
	return result;
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

function doPlainLogin() {

	var $form = $('#plain-login').serializeArray();

	$.post("/password/login", $form).complete(function(data) {

		if (data.status == '200') {
			window.location = urlQueryAsObject().targetUrl || '/home/';

		} else if (data.status == '403') {
			somePlainFail('Authentication error! Maybe you have entered wrong email or password. Please try again!');

		} else {
			somePlainFail('An unexpected server error has occured.');

		}

	});
}

function somePlainFail(message) {
	$('#plain-fail').html(message);
	$('#plain-fail').addClass('alert alert-danger');
	$('#plain-fail').attr('role', 'alert');
	$('#plain-fail').show();
}


$(function(){

	$.getJSON('/saml/idps', function(idpInfos){
		var optionsHtml = idpOptions(idpInfos);
		var idpSelect = document.getElementById('GET-idpUrl');
		idpSelect.innerHTML = optionsHtml;
	});

	var targetUrl = urlQueryAsObject().targetUrl;
	
	if(targetUrl){
		var urlRelays = document.querySelectorAll('input.targetUrlRelay');
		for(var i=0; i<urlRelays.length; i++){
			var input = urlRelays.item(i);
			input.setAttribute('value', targetUrl);
		}
	}
});


