import Route from '@ember/routing/route';
import { inject as service } from '@ember/service'; // Inject services
import { action } from '@ember/object';

export default class LoginRoute extends Route {
  @service session;
  @service router;

  afterModel() {
    const url = new URL(window.location.href);
    const sessionId = url.searchParams.get('sessionId');
    if (sessionId) {
      this.session.setSessionId(sessionId);
      this.session.login();
      this.router.transitionTo('dashboard'); // Redirect to dashboard
    }
  }
}
