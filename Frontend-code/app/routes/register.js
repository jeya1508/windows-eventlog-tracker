// app/routes/register.js
import Route from '@ember/routing/route';

export default class RegisterRoute extends Route {
  model() {
    return {
      name: '',
      email: '',
      password: '',
      confirmPassword: '',
      errorMessage: '',
    };
  }
}
