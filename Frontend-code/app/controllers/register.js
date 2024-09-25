import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { inject as service } from '@ember/service';
import { later } from '@ember/runloop';

export default class RegisterController extends Controller {
  @service router;
  @tracked errorMessage = '';
  constructor() {
    super(...arguments);
  }
  @action
  updateField(fieldName, event) {
    this.model[fieldName] = event.target.value;
  }

  @action
  registerUser(event) {
    event.preventDefault();

    let { name, email, password, confirmPassword } = this.model;

    if (password !== confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return;
    }
    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8500/servletlog/v1/user/register', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onload = () => {
      if (xhr.status === 200) {
        this.errorMessage = 'Registration successful. Redirecting to login';
        later(() => this.router.transitionTo('login'), 2000);
      } else {
        this.errorMessage = 'Registration failed' + xhr.statusText;
      }
    };
    xhr.onerror = () => {
      this.errorMessage = 'Registration request failed';
    };
    xhr.send(JSON.stringify({ name, email, password }));
  }
}
