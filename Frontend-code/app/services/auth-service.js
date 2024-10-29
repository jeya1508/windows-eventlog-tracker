import Service from '@ember/service';
import { inject as service } from '@ember/service';
import { tracked } from '@glimmer/tracking';

export default class AuthService extends Service {
  @service session;
  @service router;

  @tracked isAuthenticated = false;

  async handleGoogleCallback(code) {
    try {
      let response = await fetch(
        `http://localhost:8500/servletlog/v1/user/google-callback?code=${code}`,
        {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
        },
      );

      if (response.ok) {
        let data = await response.json();
        this.isAuthenticated = true;
        this.session.login();
        this.session.setSessionId(data.sessionId); // Set session ID
        this.router.transitionTo('dashboard'); // Redirect to dashboard
      } else {
        console.error('Authentication failed', await response.text());
      }
    } catch (error) {
      console.error('Error during authentication:', error);
    }
  }
}
