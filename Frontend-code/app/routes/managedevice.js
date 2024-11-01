import Route from '@ember/routing/route';

export default class ManagedeviceRoute extends Route {
  setupController(controller, model) {
    super.setupController(controller, model);
    controller.message = '';
  }
  model(params) {
    return {
      deviceName: params.deviceName || '',
      userName: params.userName || '',
      ipAddress: params.ipAddress || '',
      password: params.password || '',

      message: '',
    };
  }
}
