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

ready(function(){
	var targetUrl = getJsonFromUrl().targetUrl;
	
	if(targetUrl){
		var urlRelays = document.querySelectorAll('input.targetUrlRelay');
		for(var i=0; i<urlRelays.length; i++){
			var input = urlRelays.item(i);
			input.setAttribute('value', targetUrl);
		}
	}
});
