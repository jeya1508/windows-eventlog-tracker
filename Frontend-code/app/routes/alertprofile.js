import Route from '@ember/routing/route';
import { inject as service } from '@ember/service';

export default class AlertprofileRoute extends Route {
  @service session;
  @service router;

  setupController(controller, model) {
    super.setupController(controller, model);
    controller.errorMessage = '';
  }
  queryParams = {
    profileName: {
      refreshModel: true,
    },
    criteria: {
      refreshModel: true,
    },
    notifyEmail: {
      refreshModel: true,
    },
    isEdit: { 
      refreshModel: false,
    },
  };
  model(params) {
    
    return {
      profileName: params.profileName || '',
      criteria: params.criteria || '',
      notifyEmail: params.notifyEmail || '',
      isEdit: params.isEdit || false, 

      errorMessage: '',
    };
  }

  beforeModel() {
    if (!this.session.isLoggedIn) {
      this.router.transitionTo('login');
    }
  }
}
