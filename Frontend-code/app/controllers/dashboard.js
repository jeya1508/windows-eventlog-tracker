import Controller from '@ember/controller';
import { action } from '@ember/object';
import { inject as service } from '@ember/service';

export default class DashboardController extends Controller {
  @service router;
  constructor() {
    super(...arguments);
  }
  @action
  redirectToSearch() {
    this.router.transitionTo('search');
  }

  @action
  redirectToAlerts() {
    this.router.transitionTo('alerts');
  }
}
