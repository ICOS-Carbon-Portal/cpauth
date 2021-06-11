@(authHostOpt: Option[String])

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
                let logoutLink = document.getElementById("logOutLnk")
                if (logoutLink) {
                    logoutLink.addEventListener('click', function(){
                        ajaxGet('/logout', function(){
                            window.location.reload();
                        });
                    });
                    logoutLink.style.display = 'inline';
                }
                document.getElementById("accountLnk").addEventListener('click', function(){
                    window.location = 'https://@(authHost)/';
                });
                document.getElementById("accountLnk").style.display = 'block';
            } else {
                document.getElementById("logInLnk").addEventListener('click', function(){
                    window.location = 'https://@(authHost)/login/?targetUrl=' + encodeURIComponent(window.location.href);
                });
                document.getElementById("logInLnk").style.display = 'inline';
            }
        });

    }
});
