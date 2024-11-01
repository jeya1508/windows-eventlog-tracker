import Service from '@ember/service';
import { tracked } from '@glimmer/tracking';
import { service } from '@ember/service';

export default class SessionService extends Service {
  @tracked isLoggedIn = false;
  @service router;
  constructor() {
    super(...arguments);
    const storedIsLoggedIn = localStorage.getItem('isLoggedIn');
    if (storedIsLoggedIn === 'true') {
      this.isLoggedIn = true;
    }
  }

  setSessionId(sessionId) {
    this.sessionId = sessionId;
  }

  getSessionId() {
    return this.sessionId;
  }

  login() {
    this.isLoggedIn = true;
    localStorage.setItem('isLoggedIn', 'true');
  }

  logout() {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8500/servletlog/v1/user/logout', true);
    xhr.withCredentials = true;
    xhr.onload = () => {
      if (xhr.status === 200) {
        this.isLoggedIn = false;
        this.sessionId = null;
        localStorage.removeItem('isLoggedIn');
        this.router.transitionTo('index');
      } else {
        console.error('Logout failed:', xhr.statusText);
      }
    };
    xhr.onerror = () => {
      console.error('Logout request failed:', xhr.statusText);
    };
    xhr.send();
  }
}
