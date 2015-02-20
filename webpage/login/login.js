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
