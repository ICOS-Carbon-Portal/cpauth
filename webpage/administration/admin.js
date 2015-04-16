/*
 * Administration
 */


/*
 * Code regarding create new account
 */ 
$('#new-butt').click(function() {
	resetNew();
	resetValidateNewError();
	$('#new-message-success').hide();
	$('#new-message-error').hide();
});

function createNewAccount() {

	resetValidateNewError();
	
	if (validateNew()) {
		$.post("/password/createaccount", $('#new-account').serializeArray())

		.done(function() {
			resetNew();
			newMessageSuccess('A new account has been created!');
		})
			
		.fail(function(data) {
			newMessageError(data.responseText);
		});
	
	} else {
		newMessageError('You must complete all fields!');
	}
}

function newMessageError(message) {
	$('#new-message-success').hide();
	
	$('#new-message-error').html(message);
	$('#new-message-error').addClass('alert alert-danger');
	$('#new-message-error').attr('role', 'alert');
	$('#new-message-error').show();
}

function newMessageSuccess(message) {
	$('#new-message-error').hide();
	
	$('#new-message-success').html(message);
	$('#new-message-success').addClass('alert alert-success');
	$('#new-message-success').attr('role', 'alert');
	$('#new-message-success').show();
	$('#new-message-success').fadeOut(1000 * 10);
}

function validateNew() {
	var ok = true;
	
	if ($('#givenName').val().length < 1) {
		$('#givenName').parent().addClass('has-error');
		ok = false;
	}
	
	if ($('#surname').val().length < 1) {
		$('#surname').parent().addClass('has-error');
		ok = false;
	}
	
	if ($('#mail').val().length < 1) {
		$('#mail').parent().addClass('has-error');
		ok = false;
		
	} else if (! /\S+@\S+/.test($('#mail').val())) {
		$('#mail').parent().addClass('has-error');
		ok = false;
	}
	
	
	if ($('#password').val().length < 1) {
		$('#password').parent().addClass('has-error');
		ok = false;
	}
	
	return ok;
}

function resetNew() {
	$('#givenName').val('');
	$('#surname').val('');
	$('#mail').val('');
	$('#password').val('');
	
}

function resetValidateNewError() {
	$('#givenName').parent().removeClass('has-error');
	$('#surname').parent().removeClass('has-error');
	$('#mail').parent().removeClass('has-error');
	$('#password').parent().removeClass('has-error');
}



/*
 * Code regarding edit account
 */
$('#list-accounts-butt').click(function() {
	populateListAccounts();
});


function populateListAccounts() {
	$('#list-accounts').empty();
	
	$.getJSON("/password/accountslist")
	
	.done(function(data) {
		
		$('#list-accounts').append('<table class="table table-striped"></table>');
		$('#list-accounts table').append('<tr><th>User</th><th>Delete</th><th>Is admin?</th></tr>');
		
		$.each(data, function(i, entry){
			
			var info = entry.info;
			var isAdmin = entry.isAdmin;
			
			var editAdmin = '';
			if (isAdmin) {
				editAdmin = '<input type="checkbox" onClick="unmakeAdmin(\'' + info.mail + '\')" checked />';
			} else {
				editAdmin = '<input type="checkbox" onClick="makeAdmin(\'' + info.mail + '\')" />';
			}
			
			$('#list-accounts table').append('<tr><td>' + info.givenName + '&nbsp;' + info.surname + '&nbsp;(' + info.mail + ')</td><td><button class="btn btn-default" onClick="deleteAccount(\'' + info.mail + '\')">Delete</button></td><td>' + editAdmin + '</td></tr>');
				
		});
		
	})
	
	.fail(function(data) {
		editMessageError(data.responseText);
	});
}


function deleteAccount(mail) {
	$.post("/password/deleteaccount", 'mail=' + mail)

	.done(function() {
		$('#edit-message-error').hide();
	})
		
	.fail(function(data) {
		editMessageError(data.responseText);
	});
	
	populateListAccounts();
}

function makeAdmin(mail) {
	$.post("/password/makeadmin", 'mail=' + mail)

	.done(function() {
		$('#edit-message-error').hide();
	})
		
	.fail(function(data) {
		editMessageError(data.responseText);
	});
	
	populateListAccounts();
}

function unmakeAdmin(mail) {
	$.post("/password/unmakeadmin", 'mail=' + mail)

	.done(function() {
		$('#edit-message-error').hide();
	})
		
	.fail(function(data) {
		editMessageError(data.responseText);
	});
	
	populateListAccounts();
}

function editMessageError(message) {
    $('#edit-message-error').html(message);
    $('#edit-message-error').addClass('alert alert-danger');
    $('#edit-message-error').attr('role', 'alert');
    $('#edit-message-error').show();
}