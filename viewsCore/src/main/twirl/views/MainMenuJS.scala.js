@import se.lu.nateko.cp.viewscore.viewsConfig
@import eu.icoscp.envri.Envri

@()(implicit envri: Envri)

window.addEventListener("load", function(){

	const menuButton = document.getElementById("menu-button");

	if (menuButton !== null) {
		const ddToggles = document.querySelectorAll("#cp_theme_d10_menu .top-node .dd-toggle");
		const menuContainer = document.querySelector("#cp_theme_d10_menu");

		ddToggles.forEach((ddToggle) => {
			ddToggle.addEventListener('click', function (event) {
				let target = ddToggle.closest(".top-node");
				target.classList.toggle("open");
				event.preventDefault();
			});

			ddToggle.addEventListener('keyup', function (event) {
				if (event.key === "Enter") {
					let target = ddToggle.closest(".top-node");
					target.classList.toggle("open");
					event.preventDefault();
				}
			});
		});

		menuContainer.addEventListener('mouseout', function (event) {
			if (event.target === document.activeElement) {
				event.target.blur();
			}
		});
	}

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

	ajaxGet('/whoami', function(xhr) {
		var response = JSON.parse(xhr.response);

		if (response.email) {
			const email = response.email;

			fetch(`https://@(viewsConfig.authHost)/db/users/${email}?keys=${encodeURIComponent('{cart:1}')}`, { credentials: 'include' })
				.then(response => response.json())
				.then(data => {

					const cartLinks = document.querySelectorAll('.cart-link');
					cartLinks.forEach(link => {
						link.querySelector('.items-number').innerText = data.cart._items.length;
						link.querySelector("a").href = 'https://@(viewsConfig.dataHost)/portal#{"route":"cart"}';;
						link.classList.remove("d-none");
					});

					const accountLinks = document.querySelectorAll('.account-link');
					accountLinks.forEach(link => {
						link.querySelector("a").href = 'https://@(viewsConfig.authHost)/';
						link.classList.remove("d-none");
					});

					const addButton = document.getElementById("meta-add-to-cart-button");
					const removeButton = document.getElementById("meta-remove-from-cart-button");

					if (addButton) {
						const objId = addButton.dataset.id;
						if (data.cart._items.some(i => i._id === objId)) {
							removeButton.classList.remove('d-none');
						} else {
							addButton.classList.remove('d-none');
						}

						removeButton.addEventListener("click", () => {
							addButton.classList.remove('d-none');
							removeButton.classList.add('d-none');
							const items = data.cart._items.filter(i => i._id != objId)
							data.cart._items = items;
							updateProfile(email, data);
							cartLinks.forEach(link => {
								link.querySelector('.items-number').innerText = data.cart._items.length;
							});
						});

						addButton.addEventListener("click", () => {
							addButton.classList.add('d-none');
							removeButton.classList.remove('d-none');
							data.cart._items.push({"_id": objId})
							updateProfile(email, data);
							cartLinks.forEach(link => {
								link.querySelector('.items-number').innerText = data.cart._items.length;
							});
						});

						if (window.location.hash == "#add-to-cart") {
							history.replaceState(null, "", window.location.href.split('#')[0]);
							addButton.classList.add('d-none');
							removeButton.classList.remove('d-none');
							data.cart._items.push({"_id": objId})
							updateProfile(email, data);
							cartLinks.forEach(link => {
								link.querySelector('.items-number').innerText = data.cart._items.length;
							});
						}
					}
				});

		} else {
			const loginLinks = document.querySelectorAll('.login-link');
			loginLinks.forEach(link => {
				link.querySelector("a").href = getRedirectUrl(window.location.href);
				link.classList.remove("d-none");
			});

			const addButton = document.getElementById("meta-add-to-cart-button");
			if (addButton) {
				addButton.addEventListener("click", () => {
					window.location = getRedirectUrl(window.location.href + "#add-to-cart")
				});
				addButton.classList.remove('d-none');
			}
		}
	});

	const updateProfile = (email, data) => {
		fetch(`https://@(viewsConfig.authHost)/db/users/${email}`, {
			credentials: 'include',
			method: 'PATCH',
			mode: 'cors',
			headers: new Headers({
				'Content-Type': 'application/json'
			}),
			body: JSON.stringify(data)
		});
	};

	const getRedirectUrl = (url) => 'https://@(viewsConfig.authHost)/login/?targetUrl=' + encodeURIComponent(url);

});
