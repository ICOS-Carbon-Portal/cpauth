@(authHostOpt: Option[String], dataHostOpt: Option[String])

window.addEventListener("load", function(){

	let menuButton = document.getElementById("menu-button");

	if (menuButton !== null) {
		menuButton.addEventListener('click', function() {
			document.getElementById('cp-main-menu').classList.toggle('open');
		});
	}

	var menuGroups = document.getElementsByClassName("open_menu");

	for(var idx = 0; idx < menuGroups.length; idx++){
		var elem = menuGroups[idx];

		elem.addEventListener("click", function(event){
			this.parentElement.parentElement.classList.toggle('open');
		});
	}

	@for(authHost <- authHostOpt){

		function ajaxGet(url, action){
			var xhr = new XMLHttpRequest();
			xhr.open("GET", url);
			xhr.send(null);

			xhr.onreadystatechange = function () {
				if (xhr.readyState === 4 && xhr.status === 200) {
					action(xhr);
				}
			};
		}

		ajaxGet('/whoami', function(xhr){
			var response = JSON.parse(xhr.response);

			if (response.email) {
				let email = response.email;
				document.getElementById("accountLnk").addEventListener('click', function(){
					window.location = 'https://@(authHost)/';
				});
				document.getElementById("accountLnk").style.display = 'block';

				@for(dataHost <- dataHostOpt) {
					document.getElementById("cartLink").addEventListener('click', function () {
						window.location = 'https://@(dataHost)/portal#{"route":"cart"}';
					});
					document.getElementById("cartLink").style.display = 'block';
				}

				let addButton = document.getElementById("meta-add-to-cart-button");
				let removeButton = document.getElementById("meta-remove-from-cart-button");

				if (addButton) {
					let objId = addButton.dataset.id;
					fetch(`https://@(authHost)/db/users/${email}?keys=${encodeURIComponent('{cart:1}')}`, { credentials: 'include' })
						.then(response => response.json())
						.then(data => {
							if (data.cart._items.some(i => i._id === objId)) {
								removeButton.classList.remove('d-none');
							} else {
								addButton.classList.remove('d-none');
							}

							removeButton.addEventListener("click", () => {
								addButton.classList.remove('d-none');
								removeButton.classList.add('d-none');
								let items = data.cart._items.filter(i => i._id != objId)
								data.cart._items = items;
								updateProfile(email, data);
							});

							addButton.addEventListener("click", () => {
								addButton.classList.add('d-none');
								removeButton.classList.remove('d-none');
								data.cart._items.push({"_id": objId})
								updateProfile(email, data);
							});

							if (window.location.hash == "#add-to-cart") {
								history.replaceState(null, "", window.location.href.split('#')[0]);
								addButton.classList.add('d-none');
								removeButton.classList.remove('d-none');
								data.cart._items.push({"_id": objId})
								updateProfile(email, data);
							}

						});
				}

			} else {
				document.getElementById("logInLnk").addEventListener('click', () => loginAndRedirect(window.location.href));
				document.getElementById("logInLnk").style.display = 'inline';

				let addButton = document.getElementById("meta-add-to-cart-button");
				if (addButton) {
					addButton.addEventListener("click", () => loginAndRedirect(window.location.href + "#add-to-cart"));
					addButton.classList.remove('d-none');
				}
			}
		});

		const updateProfile = (email, data) => {
			fetch(`https://@(authHost)/db/users/${email}`, {
				credentials: 'include',
				method: 'PATCH',
				mode: 'cors',
				headers: new Headers({
					'Content-Type': 'application/json'
				}),
				body: JSON.stringify(data)
			});
		};

		const loginAndRedirect = (url) => {
			window.location = 'https://@(authHost)/login/?targetUrl=' + encodeURIComponent(url);
		}

	}
});
