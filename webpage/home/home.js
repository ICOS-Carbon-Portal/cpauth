$(function(){
	$.getJSON('/whoami', function(uinfo){

		$("#welcomeName").html(uinfo.givenName);

	});
});