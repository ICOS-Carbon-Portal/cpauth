
$(function(){

	$('#new-link').click(function() {
		hideMessage();
		resetNewUserCredentials();
	});

	$('#edit-link').click(function(){
		hideMessage();
		populateAccountsList();
	});

	$('#new-account-button').click(createNewAccount);

	$("#mail").keypress(enterKeyHandler(function(){
		$("#password").focus();
	}));
	$("#password").keypress(enterKeyHandler(createNewAccount));

});


function createNewAccount() {
	if (!validateNewUserCredentials()) return;

	$.post('/password/createaccount', $('#new-account').serializeArray())
		.pipe(function(){
			return $.ajax({
				method: 'PUT',
				url: '/db/users/' + $('#mail').val(),
				contentType: 'application/json',
				data: '{profile: {}}'
			});
		})
		.done(function() {
			resetNewUserCredentials();
			reportSuccess('A new account has been created!');
		})
		.fail(reportError);
}

function validateNewUserCredentials() {

	if (! /\S+@\S+/.test($('#mail').val())) {
		$('#mail').parent().addClass('has-error');
		reportError('Valid email address must be provided!');
		return false;
	}

	if ($('#password').val().length < 6) {
		$('#password').parent().addClass('has-error');
		reportError('Password was too short!');
		return false;
	}

	$('#mail').parent().removeClass('has-error');
	$('#password').parent().removeClass('has-error');
	hideMessage();
	return true;
}

function resetNewUserCredentials() {
	$('#mail').val('');
	$('#password').val('');
}


function populateAccountsList() {
	$('#list-accounts').empty();

	$.getJSON("/password/accountslist")
		.done(function(data) {

			$('#list-accounts').append('<table class="table table-striped"></table>');
			$('#list-accounts table').append('<tr><th>User</th><th>Delete</th><th>Is admin?</th></tr>');

			$.each(data, function(i, entry){
			
				var id = entry.id.email;
				var isAdmin = entry.isAdmin;

				var editAdmin = isAdmin
					? '<input type="checkbox" onClick="unmakeAdmin(\'' + id + '\')" checked />'
					: '<input type="checkbox" onClick="makeAdmin(\'' + id + '\')" />';

				var deleteButton = '<button class="btn btn-default" onClick="deleteAccount(\'' + id + '\')">Delete</button>';
				$('#list-accounts table').append('<tr><td>' + id + '</td><td>' + deleteButton + '</td><td>' + editAdmin + '</td></tr>');

			});

		})
		.fail(reportError);
}


function deleteAccount(mail) {
	$.post("/password/deleteaccount", 'mail=' + mail)
		.done(function() {
			reportSuccess(mail + ' has been removed');
		})
		.fail(reportError)
		.always(populateAccountsList);
}

function makeAdmin(mail) {
	$.post("/password/makeadmin", 'mail=' + mail)
		.done(function() {
			reportSuccess(mail + ' is now an administrator');
		})
		.fail(reportError)
		.always(populateAccountsList);
}

function unmakeAdmin(mail) {
	$.post("/password/unmakeadmin", 'mail=' + mail)
		.done(function() {
			reportSuccess(mail + ' is not an administrator any longer');
		})
		.fail(reportError)
		.always(populateAccountsList);
}

