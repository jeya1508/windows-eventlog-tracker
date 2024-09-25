import Route from '@ember/routing/route';
import { inject as service } from '@ember/service';

export default class DashboardRoute extends Route {
  @service session;
  @service router;

  setupController(controller) {
    super.setupController(...arguments);
  }

  beforeModel() {
    if (!this.session.isLoggedIn) {
      this.router.transitionTo('login');
    }
  }

  afterModel() {
    if (this.session.isLoggedIn) {
      window.history.replaceState(null, '', window.location.href);
      window.addEventListener('popstate', this.handlePopState.bind(this));
    }
  }

  willDestroy() {
    window.removeEventListener('popstate', this.handlePopState.bind(this));
  }

  handlePopState(event) {
    if (this.session.isLoggedIn) {
      window.history.replaceState(null, '', window.location.href);
      this.router.transitionTo('dashboard');
    }
  }
}
