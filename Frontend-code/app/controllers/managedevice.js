import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { inject as service } from '@ember/service';
import { later } from '@ember/runloop';

export default class ManagedeviceController extends Controller {
  @service router;
  @tracked message = '';

  constructor() {
    super(...arguments);
  }
  @action
  updateField(fieldName, event) {
    this.model[fieldName] = event.target.value;
  }

  @action
  addDevice(event) {
    event.preventDefault();

    const deviceNameInput = document.getElementById('deviceName');
    const ipAddressInput = document.getElementById('ipAddress');
    const userNameInput = document.getElementById('userName');
    const passwordInput = document.getElementById('password');

    deviceNameInput.classList.remove('error-outline');
    ipAddressInput.classList.remove('error-outline');
    userNameInput.classList.remove('error-outline');
    passwordInput.classList.remove('error-outline');

    let isFormValid = true;
    let firstEmptyField = null;
    if (!this.model.deviceName) {
      isFormValid = false;
      deviceNameInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = deviceNameInput;
      }
    }
    if (!this.model.ipAddress) {
      isFormValid = false;
      ipAddressInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = ipAddressInput;
      }
    }
    if (!this.model.userName) {
      isFormValid = false;
      userNameInput.classList.add('error-outline');
      if (!firstEmptyField) {
        firstEmptyField = userNameInput;
      }
    }
    if (!this.model.password) {
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

    let { deviceName, ipAddress, userName, password } = this.model;
    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8500/servletlog/v1/device/add', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = () => {
      const response = JSON.parse(xhr.responseText);
      if (xhr.status === 200 || xhr.status === 201) {
        this.message = 'Device added successfully';
        later(() => this.router.transitionTo('search'), 2000);
      } else if (xhr.status === 400 || xhr.status === 500) {
        this.message = response.error || 'Failed to add the device';
      } else {
        this.message = 'Unexpected error occured while adding the device';
      }
      alert(this.message);
    };
    xhr.onerror = () => {
      this.message = 'Device addition failed';
    };
    xhr.send(JSON.stringify({ deviceName, ipAddress, userName, password }));
  }
  @action
  redirectToSearch() {
    this.router.transitionTo('search');
  }
}
