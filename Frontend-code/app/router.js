import EmberRouter from '@ember/routing/router';
import config from 'logging/config/environment';

export default class Router extends EmberRouter {
  location = config.locationType;
  rootURL = config.rootURL;
}

Router.map(function () {
  this.route('login');
  this.route('register');
  this.route('dashboard');
  this.route('search');
  this.route('alerts');
  this.route('alertprofile');
  this.route('manageAlert');
  this.route('managedevice');
});
