import Route from '@ember/routing/route';
import { inject as service } from '@ember/service';
export default class AlertsRoute extends Route {
  @service session;
  @service router;
  setupController(controller) {
    super.setupController(...arguments);
    controller.fetchLogs();
    controller.fetchAlertProfiles();
  }
  beforeModel() {
    if (!this.session.isLoggedIn) {
      this.router.transitionTo('login');
    }
  }
}
