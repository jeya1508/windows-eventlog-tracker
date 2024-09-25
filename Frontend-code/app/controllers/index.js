import Controller from '@ember/controller';
import { action } from '@ember/object';
import { inject as service } from '@ember/service';

export default class IndexController extends Controller {
  @service router;
  constructor() {
    super(...arguments);
  }
  @action
  redirectToLogin() {
    this.router.transitionTo('login');
  }

  @action
  redirectToRegister() {
    this.router.transitionTo('register');
  }
}
