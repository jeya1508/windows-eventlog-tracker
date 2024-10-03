import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { inject as service } from '@ember/service';
import { later } from '@ember/runloop';

export default class AlertprofileController extends Controller {
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
  addAlertProfile(event) {
    event.preventDefault();
    let { profileName, criteria, notifyEmail } = this.model;

    const xhr = new XMLHttpRequest();
    const method = this.model.isEdit ? 'PUT' : 'POST'; 
    const url = this.model.isEdit 
      ? `http://localhost:8500/servletlog/v1/alert/profile/${this.model.profileName}` 
      : 'http://localhost:8500/servletlog/v1/alert/profile';

    xhr.open(method, url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = () => {
      const response = JSON.parse(xhr.responseText);
      if (xhr.status === 200 || xhr.status === 201) {
        this.errorMessage = 'Alert profile saved successfully';
        later(() => this.router.transitionTo('alerts'), 2000);
      } else if (xhr.status === 400 || xhr.status === 500) {
        this.errorMessage = response.error || 'Failed to save alert profile';
      } else {
        this.errorMessage = 'An unexpected error occurred';
      }
    };
    xhr.onerror = () => {
      this.errorMessage = 'Alert profile addition/update failed';
    };
    xhr.send(JSON.stringify({ profileName, criteria, notifyEmail }));
  }

  @action
  redirectToAlert() {
    this.router.transitionTo('alerts');
  }
}
