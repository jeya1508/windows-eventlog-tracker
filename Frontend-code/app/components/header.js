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

  @action
  logout() {
    this.session.logout();
    this.router.transitionTo('index');
  }

  @action
  goToAlertProfile() {
    this.router.transitionTo('alertprofile');
  }
}
