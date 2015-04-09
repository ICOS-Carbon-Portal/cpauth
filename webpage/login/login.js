
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

	$.post("/password/login", $form)
		.done(function() {
			window.location = urlQueryAsObject().targetUrl || '/home/';
		})
		.fail(function(xhr){
			var defaultMessage = xhr.status == '403'
				? 'Authentication error! Maybe you have entered wrong email or password. Please try again!'
				: 'An unexpected server error has occured.';
			somePlainFail(xhr.responseText || defaultMessage);
		});
}


function somePlainFail(message) {
	var $fail = $('#plain-fail');
	$fail.html(message);
	$fail.addClass('alert alert-danger');
	$fail.attr('role', 'alert');
	$fail.show();
}


$(function(){

	$.getJSON('/saml/idps', function(idpInfos){
		var optionsHtml = idpOptions(idpInfos);
		var idpSelect = document.getElementById('GET-idpUrl');
		idpSelect.innerHTML = optionsHtml;
	});

	var targetUrl = urlQueryAsObject().targetUrl;
	
	if(targetUrl){
		$('input.targetUrlRelay').attr('value', targetUrl);
	}

	$("#passwordLoginButton").click(doPlainLogin);
	$("#password").keypress(function(e){
		if(e.which == 13) doPlainLogin();
	});
});


