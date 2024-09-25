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

    let loginData = {
      email: this.email,
      password: this.password,
    };

    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8500/servletlog/v1/user/login', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          let jsonResponse = JSON.parse(xhr.responseText);
          this.session.setSessionId(jsonResponse.sessionId);
          this.session.login();

          this.password = '';

          this.router.transitionTo('dashboard');
        } catch (error) {
          console.error('Error parsing response:', error);
          alert('Login failed! Please try again.');
        }
      } else {
        alert('Login failed! Please try again.');
      }
    };
    xhr.onerror = () => {
      console.error('Login request failed', xhr.statusText);
    };
    xhr.send(JSON.stringify(loginData));
  }
}
