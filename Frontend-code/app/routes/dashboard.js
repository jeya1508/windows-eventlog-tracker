// routes/dashboard.js
import Route from '@ember/routing/route';
import { inject as service } from '@ember/service';

export default class DashboardRoute extends Route {
  @service session;
  @service router;

  beforeModel() {
    if (!this.session.isLoggedIn) {
      this.router.transitionTo('login');
    }
  }

  afterModel() {
    if (this.session.isLoggedIn) {
      window.history.replaceState(null, '', '/dashboard');
      
      window.history.pushState(null, '', '/dashboard');

      window.addEventListener('popstate', this.handlePopState);
    }
  }

  willDestroy() {
    // Clean up the event listener to avoid memory leaks
    window.removeEventListener('popstate', this.handlePopState);
  }

  handlePopState = (event) => {
    if (this.session.isLoggedIn) {
      window.history.pushState(null, '', '/dashboard');
    }
  };
}
