import Route from '@ember/routing/route';
import { inject as service } from '@ember/service';

export default class SearchRoute extends Route {
  @service session;
  @service router;

  setupController(controller) {
    super.setupController(...arguments);
    controller.fetchResults();
    controller.fetchDevicesList();
  }

  beforeModel() {
    if (!this.session.isLoggedIn) {
      this.router.transitionTo('login');
    }
  }
}
