function updateCharacterCount() {
    let textarea = document.getElementById("text");
    let characterCount = document.getElementById("characterCount");
    characterCount.textContent = 300 - textarea.value.length;
}

function updateCharacterMessageCount() {
    let textarea = document.getElementById("messageContent");
    let characterCount = document.getElementById("characterMessageCount");
    characterCount.textContent = 200 - textarea.value.length;
}

const profileButton = document.querySelector('.profile');
const profileBlock = document.getElementById('profileBlock');

function showProfileBlock() {
    profileBlock.style.display = 'block';
    setTimeout(function () {
        profileBlock.classList.add('show');
    }, 10);
}

function hideProfileBlock() {
    profileBlock.classList.remove('show');
    setTimeout(function () {
        profileBlock.style.display = 'none';
    }, 300);
}

profileButton.addEventListener('click', function (event) {
    if (profileBlock.style.display === 'block') {
        hideProfileBlock();
    } else {
        showProfileBlock();
    }
});

document.addEventListener('click', function (event) {
    const target = event.target;
    if (target !== profileButton && !profileBlock.contains(target)) {
        hideProfileBlock();
    }
});

const topicButton = document.getElementById('topicButton');
const messageButton = document.getElementById('messageButton');
const mainForm = document.querySelector('.main-form');
const messageForm = document.querySelector('.message-form');

// Добавляем обработчики событий на нажатие кнопок
topicButton.addEventListener('click', () => {
    mainForm.style.display = 'block';
    messageForm.style.display = 'none';
});

messageButton.addEventListener('click', () => {
    mainForm.style.display = 'none';
    messageForm.style.display = 'block';
});