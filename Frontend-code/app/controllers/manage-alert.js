import Controller from '@ember/controller';
import { tracked } from '@glimmer/tracking';
import { action } from '@ember/object';
import {inject as service} from '@ember/service';

export default class ManageAlertsController extends Controller {
  @tracked alertProfiles = [];
  @service router;
  constructor() {
    super(...arguments);
    this.fetchAlertProfiles();
  }

  fetchAlertProfiles() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', 'http://localhost:8500/servletlog/v1/alert/profile', true);
    xhr.withCredentials = true;
    xhr.onload = () => {
      if (xhr.status === 200) {
        const response = JSON.parse(xhr.responseText);
        this.alertProfiles = response.message;
      } else {
        console.error('Error fetching alert profiles:', xhr.statusText);
      }
    };
    xhr.onerror = () => {
      console.error('Request failed');
    };
    xhr.send();
  }

  @action
  editProfile(profile) {
    this.router.transitionTo('alertprofile', {
      queryParams: { 
        profileName: profile.profileName,
        criteria: profile.criteria,
        notifyEmail: profile.notifyEmail ,
        isEdit: true 
      }
    });
    
  }

  @action
  deleteProfile(profile) {
    if (confirm(`Are you sure you want to delete the profile "${profile.profileName}"?`)) {
      const xhr = new XMLHttpRequest();
      xhr.open('DELETE', `http://localhost:8500/servletlog/v1/alert/profile/${profile.profileName}`, true);
      xhr.withCredentials=true;
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          this.fetchAlertProfiles();
        } else {
          console.error('Error deleting alert profile:', xhr.statusText);
        }
      };
      xhr.onerror = () => {
        console.error('Request failed');
      };
      xhr.send();
    }
  }
  @action
  redirectToAlert(){
    this.router.transitionTo('alerts');
  }
}

