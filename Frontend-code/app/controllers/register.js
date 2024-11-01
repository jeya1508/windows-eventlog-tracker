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

    const nameInput = document.getElementById('name');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    nameInput.classList.remove('error-outline');
    emailInput.classList.remove('error-outline');
    passwordInput.classList.remove('error-outline');
    confirmPasswordInput.classList.remove('error-outline');

    let isFormValid = true;
    let firstEmptyField = null;
    if (!this.model.name) {
      isFormValid = false;
      nameInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = nameInput;
      }
    }
    if (!this.model.email) {
      isFormValid = false;
      emailInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = emailInput;
      }
    }

    if (!this.model.password) {
      isFormValid = false;
      passwordInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = passwordInput;
      }
    }
    if (!this.model.confirmPassword) {
      isFormValid = false;
      confirmPasswordInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = confirmPasswordInput;
      }
    }
    if (firstEmptyField) {
      alert('Fill all the fields');
      firstEmptyField.focus();
      return;
    }

    let { name, email, password, confirmPassword } = this.model;

    if (password !== confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      alert(this.errorMessage);
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
        let resp = JSON.parse(xhr.responseText);
        alert(resp.error);
      }
    };
    xhr.onerror = () => {
      this.errorMessage = 'Registration request failed';
    };
    xhr.send(JSON.stringify({ name, email, password }));
  }
}
