import Route from '@ember/routing/route';

export default class ManagedeviceRoute extends Route {
    setupController(controller, model) {
        super.setupController(controller, model);
        controller.message = '';
      }
  model(params) {
    return {
      deviceName: params.deviceName || '',
      hostName: params.hostName || '',
      ipAddress: params.ipAddress || '',
      password: params.password || '',

      message: '',
    };
  }
}
