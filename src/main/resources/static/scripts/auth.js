document.addEventListener("DOMContentLoaded", function() {
    var registrationForm = document.getElementById("registrationForm");
    registrationForm.addEventListener("submit", function(event) {
        event.preventDefault();
        grecaptcha.ready(function () {
            grecaptcha.execute('6LcTdwsnAAAAAFum6X7fmKEPijGHWVzAsHTZLKpt', { action: 'registration' })
                .then(function (token) {
                    document.getElementById('g-recaptcha-response').value = token;
                    registrationForm.submit();
                });
        });
    });
});