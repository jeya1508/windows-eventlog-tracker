import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { inject as service } from '@ember/service';
import { later } from '@ember/runloop';

export default class ManagedeviceController extends Controller {
    @service router;
    @tracked message = '';
    
    constructor() {
        super(...arguments);
    }
    @action
    updateField(fieldName, event) {
      this.model[fieldName] = event.target.value;
    }

    @action
    addDevice(event){
        event.preventDefault();
        let {deviceName,ipAddress,hostName,password} = this.model;
        const xhr = new XMLHttpRequest();
        xhr.open('POST','http://localhost:8500/servletlog/v1/device/add',true);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.withCredentials = true;
        xhr.onload = () =>{
            const response = JSON.parse(xhr.responseText);
            if(xhr.status === 200 || xhr.status === 201)
            {
                this.message = 'Device added successfully';
                later(() => this.router.transitionTo('search'), 2000);
            }
            else if(xhr.status === 400 || xhr.status === 500)
            {
                this.message = response.error || 'Failed to add the device';
            }
            else{
                this.message = 'Unexpected error occured while adding the device';
            }
        };
        xhr.onerror = () =>
        {
            this.message = 'Device addition failed';
        }
        xhr.send(JSON.stringify({deviceName,ipAddress,hostName,password}));
    }
    @action
    redirectToSearch()
    {
        this.router.transitionTo('search');
    }
}
