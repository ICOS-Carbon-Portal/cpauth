var lastIdpCookieName = "lastChosenIdp";
var maxResults = 100;
var $idpInput, $idpBtn;

function urlQueryAsObject() {
	var query = location.search.substr(1);
	var result = {};
	query.split("&").forEach(function(part) {
		var item = part.split("=");
		result[item[0]] = decodeURIComponent(item[1]);
	});
	return result;
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

function enterKeyHandler(innerFun){
	return function(e){
		if(e.which == 13) innerFun(e);
	}
}


function somePlainFail(message) {
	var $fail = $('#plain-fail');
	$fail.html(message);
	$fail.addClass('alert alert-danger');
	$fail.attr('role', 'alert');
	$fail.show();
}


function setLastIpd(idpInfos){
	var lastIdp = Cookies.get(lastIdpCookieName);
	if (lastIdp && lastIdp != "undefined"){
		$idpBtn.prop("disabled", false);

		var idp = $.grep(idpInfos, function(idp){ return idp.id === lastIdp })[0];

		$idpInput.val(idp.name);
		setButtonEv(idp.id);
	} else {
		$idpBtn.prop("disabled", true);
	}
}


function setButtonEv(idpUrl){
	$idpBtn.off();
	$idpBtn.prop("disabled", false);

	$idpBtn.click(function(){
		var targetUrl = encodeURIComponent($("#targetUrl").val() == undefined ? "" : $("#targetUrl").val());

		Cookies.set(lastIdpCookieName, idpUrl, { expires: (60*60*24*365), domain: "/login/" });
		window.location = "/saml/login?targetUrl=" + targetUrl + "&idpUrl=" + idpUrl;
		return false;
	});
}


function suggestIdps(idpInfos){
	$idpInput.autocomplete({
		minLength: 2,
		autoFocus: true,

		source: function (request, response) {
			var counter = 0;

			response($.map(idpInfos, function (value, key) {
				if(value.name.toLowerCase().indexOf(request.term.toLowerCase()) != -1) {
					counter++;
					// Return max results
					if(counter <= maxResults) {
						return {
							value: value.id,
							label: value.name
						}
					}
				}
			}));
		},

		focus: function(event, ui) {
			return false;
		},

		select: function(event, ui) {
			$idpInput.val(ui.item.label);
			setButtonEv(ui.item.value);
			return false;
		},

		change: function(event, ui){
			if (ui.item == null){
				$idpBtn.off();
				$idpBtn.prop("disabled", true);
			}
			return false;
		}
	});
}

$(function(){
	$idpInput = $("#idpUrlInput");
	$idpBtn = $("#signonBtn");
	$idpBtn.prop("disabled", true);

	$.getJSON('/saml/idps', function(idpInfos){
		suggestIdps(idpInfos);
		setLastIpd(idpInfos);
	});

	var targetUrl = urlQueryAsObject().targetUrl;

	if(targetUrl){
		$('input.targetUrlRelay').attr('value', targetUrl);
	}

	$("#passwordLoginButton").click(doPlainLogin);
	$("#password").keypress(enterKeyHandler(doPlainLogin));
	$("#mail").keypress(enterKeyHandler(function(){
		$("#password").focus();
	}));

});
