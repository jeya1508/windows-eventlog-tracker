import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { inject as service } from '@ember/service';

export default class LoginController extends Controller {
  @service router;
  @service session;

  @tracked email = '';
  @tracked password = '';

  constructor() {
    super(...arguments);
  }

  @action
  updateEmail(event) {
    this.email = event.target.value;
  }

  @action
  updatePassword(event) {
    this.password = event.target.value;
  }

  @action
  submitLogin(event) {
    event.preventDefault();

    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    emailInput.classList.remove('error-outline');
    passwordInput.classList.remove('error-outline');

    let isFormValid = true;
    let firstEmptyField = null;
    if (!this.email) {
      isFormValid = false;
      emailInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = emailInput;
      }
    }

    if (!this.password) {
      isFormValid = false;
      passwordInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = passwordInput;
      }
    }
    if (firstEmptyField) {
      alert('Fill all the fields');
      firstEmptyField.focus();
      return;
    }

    let loginData = {
      email: this.email,
      password: this.password,
    };

    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8500/servletlog/v1/user/login', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = () => {
      try {
        if (xhr.status === 200) {
          let jsonResponse = JSON.parse(xhr.responseText);
          this.session.setSessionId(jsonResponse.sessionId);
          this.session.login();

          this.password = '';

          this.router.transitionTo('dashboard');
        } else {
          let resp = JSON.parse(xhr.responseText);
          alert(resp.error);
        }
      } catch (error) {
        console.error('Error parsing response:', error);
        alert('Login failed! Please try again.');
      }
    };
    xhr.onerror = () => {
      console.error('Login request failed', xhr.statusText);
    };
    xhr.send(JSON.stringify(loginData));
  }

  @action
  signInWithGoogle() {
    const oauthUrl = 'http://localhost:8500/servletlog/v1/user/login';

    const popupWidth = 500;
    const popupHeight = 500;
    const left = (window.screen.width / 2) - (popupWidth / 2);
    const top = (window.screen.height / 2) - (popupHeight / 2);

    const popup = window.open(
      oauthUrl,
      'Google Sign-In',
      `width=${popupWidth},height=${popupHeight},top=${top},left=${left}`
    );

    const popupCheckInterval = setInterval(() => {
      if (!popup || popup.closed) {
        clearInterval(popupCheckInterval);
        return;
      }

      try {
        const popupUrl = popup.location.href;
        const url = new URL(popupUrl);
        const sessionId = url.searchParams.get('sessionId');

        if (sessionId) {
          clearInterval(popupCheckInterval);
          popup.close();

          this.session.setSessionId(sessionId);
          this.session.login();
          this.router.transitionTo('dashboard');
        }
      } catch (error) {
      }
    }, 500);
  }
}
