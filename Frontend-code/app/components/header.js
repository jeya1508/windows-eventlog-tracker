import Component from '@glimmer/component';
import { inject as service } from '@ember/service';
import { action } from '@ember/object';

export default class HeaderComponent extends Component {
  @service session;
  @service router;

  get isLoggedIn() {
    return this.session.isLoggedIn;
  }

  get isOnAlertsRoute() {
    return this.router.currentRouteName === 'alerts';
  }

  get isOnSearchRoute() {
    return this.router.currentRouteName === 'search';
  }

  @action
  logout() {
    this.session.logout();
    this.router.transitionTo('index');
  }

  @action
  goToAlertProfile() {
    this.router.transitionTo('alertprofile', {
      queryParams: {
        profileName: '',
        criteria: '',
        notifyEmail: '',
        isEdit: false,
      },
    });
  }
  @action
  manageAlertProfiles() {
    this.router.transitionTo('manageAlert');
  }
  @action
  goToAddDevice() {
    this.router.transitionTo('managedevice');
  }
}
