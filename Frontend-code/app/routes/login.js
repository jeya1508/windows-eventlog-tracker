import Route from '@ember/routing/route';
import { inject as service } from '@ember/service';

export default class LoginRoute extends Route {
  @service session;
  @service router;

  beforeModel() {
    if (this.session.isLoggedIn) {
      this.router.transitionTo('dashboard');
    }
  }
}
