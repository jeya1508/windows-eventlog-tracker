import Route from '@ember/routing/route';
import {inject as service} from '@ember/service';
export default class AlertprofileRoute extends Route {
  @service session;
  @service router;
    setupController(controller, model) {
      super.setupController(controller, model);
      controller.errorMessage = '';
    }
  model() {
    return {
      profileName: '',
      criteria: '',
      notifyEmail: '',
      errorMessage: '',
    };
  }
    beforeModel() {
        if (!this.session.isLoggedIn) {
          this.router.transitionTo('login');
        }
    }
}
