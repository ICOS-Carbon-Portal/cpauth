@import se.lu.nateko.cp.viewscore.hostsConfig
@import eu.icoscp.envri.Envri

@()(implicit envri: Envri)

window.addEventListener("load", function(){

	const menuButton = document.getElementById("menu-button");

	if (menuButton !== null) {
		const ddToggles = document.querySelectorAll("#cp_theme_d10_menu .top-node .drop-down-toggle");
		const menuContainer = document.querySelector("#cp_theme_d10_menu");

		ddToggles.forEach((ddToggle) => {
			ddToggle.addEventListener('click', function (event) {
				let target = ddToggle.closest(".top-node");
				if (window.innerWidth < 992) {
					target.classList.toggle("open");
				}
				event.preventDefault();
			});

			ddToggle.addEventListener('keyup', function (event) {
				if (event.key === "Enter") {
					let target = ddToggle.closest(".top-node");
					if (window.innerWidth < 992) {
						target.classList.toggle("open");
					}
					event.preventDefault();
				}
			});
		});

		menuContainer.addEventListener('mouseout', function (event) {
			if (event.target === document.activeElement) {
				event.target.blur();
			}
		});

		const menuBackdrop = document.querySelector(".menu-backdrop");
		let committedTopNode = null;
		let backdropHeight = 0;
		let dwellTimer = null;
		let closeTimer = null;
		let switchTimer = null;
		const DWELL_TIME = 200;
		const CLOSE_GRACE = 100;
		const SWITCH_DELAY = 160;


		function revealDropdown(topNode) {
			if (committedTopNode !== topNode) { return; }
			const dropdown = topNode.querySelector(':scope > .dropdown-menu');
			if (!dropdown) { return; }
			const prevHeight = backdropHeight;
			const newHeight = dropdown.offsetHeight;
			backdropHeight = newHeight;
			menuBackdrop.style.height = newHeight + 'px';

			if (prevHeight > 0) { // dropdown already visible
				const startClipPct = newHeight > prevHeight
					? (100 * (newHeight - prevHeight) / newHeight).toFixed(2) + '%'
					: '0%';
				dropdown.style.transition = 'opacity 0.2s ease-in-out, transform 0.2s ease-in-out';
				dropdown.style.clipPath = `inset(0 0 ${startClipPct} 0)`;
				dropdown.getBoundingClientRect(); // commit the start state before restoring the CSS transition
				dropdown.style.transition = ''; // restore CSS transition, which re-adds clip-path as a transitioning property
			}

			dropdown.style.visibility = 'visible';
			dropdown.style.opacity = '1';
			dropdown.style.transform = 'translateY(0)';
			dropdown.style.clipPath = 'inset(0 0 0 0)';
		}

		function showDropdown(topNode) {
			clearTimeout(closeTimer);
			clearTimeout(switchTimer);
			const prev = committedTopNode;
			committedTopNode = topNode;
			if (prev && prev !== topNode) {
				hideDropdown(prev, true);
				switchTimer = setTimeout(function () { revealDropdown(topNode); }, SWITCH_DELAY);
			} else {
				revealDropdown(topNode);
			}
		}

		function hideDropdown(topNode, menuAlreadyOpen) {
			const dropdown = topNode.querySelector(':scope > .dropdown-menu');
			if (!dropdown) { return; }
			dropdown.style.opacity = '';
			dropdown.style.transform = '';
			if (!menuAlreadyOpen) {
				dropdown.style.clipPath = '';
			}
			setTimeout(function () {
				dropdown.style.visibility = '';
				dropdown.style.clipPath = '';
			}, 200);
		}

		function closeMenu() {
			if (committedTopNode) {
				hideDropdown(committedTopNode, false);
				committedTopNode = null;
			}
			backdropHeight = 0;
			menuBackdrop.style.height = '0px';
		}

		window.addEventListener('resize', function () {
			if (committedTopNode) {
				revealDropdown(committedTopNode);
			}
		});

		document.querySelectorAll("#cp_theme_d10_menu .top-node").forEach((el) => {
			el.addEventListener('mouseenter', function () {
				if (window.innerWidth < 992) {
					return;
				}
				clearTimeout(closeTimer);
				dwellTimer = setTimeout(function () { showDropdown(el); }, DWELL_TIME);
			});

			el.addEventListener('mouseleave', function () {
				if (window.innerWidth < 992) {
					return;
				}
				clearTimeout(dwellTimer);
				closeTimer = setTimeout(closeMenu, CLOSE_GRACE);
			});

			el.addEventListener('focusin', function () {
				if (window.innerWidth < 992) {
					return;
				}
				clearTimeout(dwellTimer);
				clearTimeout(closeTimer);
				showDropdown(el);
			});

			el.addEventListener('focusout', function (event) {
				if (window.innerWidth < 992) {
					return;
				}
				const nextTopNode = event.relatedTarget && event.relatedTarget.closest('#cp_theme_d10_menu .top-node');
				if (!el.contains(event.relatedTarget) && committedTopNode === el && !nextTopNode) {
					closeMenu();
				}
			});
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

			fetch(`https://@(hostsConfig.authHost)/db/users/${email}?keys=${encodeURIComponent('{cart:1}')}`, { credentials: 'include' })
				.then(response => response.json())
				.then(data => {

					const cartLinks = document.querySelectorAll('.cart-link');
					cartLinks.forEach(link => {
						link.querySelector('.items-number').innerText = data.cart._items.length;
						link.querySelector("a").href = 'https://@(hostsConfig.dataHost)/portal#{"route":"cart"}';;
						link.classList.remove("d-none");
					});

					const accountLinks = document.querySelectorAll('.account-link');
					accountLinks.forEach(link => {
						link.querySelector("a").href = 'https://@(hostsConfig.authHost)/';
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
		fetch(`https://@(hostsConfig.authHost)/db/users/${email}`, {
			credentials: 'include',
			method: 'PATCH',
			mode: 'cors',
			headers: new Headers({
				'Content-Type': 'application/json'
			}),
			body: JSON.stringify(data)
		});
	};

	const getRedirectUrl = (url) => 'https://@(hostsConfig.authHost)/login/?targetUrl=' + encodeURIComponent(url);
});
