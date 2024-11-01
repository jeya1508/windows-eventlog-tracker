import Component from '@glimmer/component';
import { inject as service } from '@ember/service';
import { action } from '@ember/object';
import { inject as controller } from '@ember/controller';
import { tracked } from '@glimmer/tracking';

export default class HeaderComponent extends Component {
  @service session;
  @service router;
  @controller alerts;

  get isLoggedIn() {
    return this.session.isLoggedIn;
  }

  get isOnAlertsRoute() {
    return this.router.currentRouteName === 'alerts';
  }

  get isOnSearchRoute() {
    return this.router.currentRouteName === 'search';
  }

  @action
  logout() {
    this.session.logout();
  }

  @action
  goToAlertProfile() {
    this.router.transitionTo('alertprofile', {
      queryParams: {
        profileName: '',
        criteria: '',
        notifyEmail: '',
        isEdit: false,
      },
    });
  }
  @action
  manageAlertProfiles() {
    this.router.transitionTo('manageAlert');
  }
  @action
  goToAddDevice() {
    this.router.transitionTo('managedevice');
  }
  @action
  exportAsCSV() {
    if (this.isOnAlertsRoute && this.alerts) {
      this.alerts.send('exportAsCSV');
    }
  }

  get isPopupVisible() {
    return this.alerts.isPopupVisible;
  }
  get exportHistoryData() {
    return this.alerts.exportHistoryData;
  }
  @action
  exportHistory() {
    if (this.isOnAlertsRoute && this.alerts) {
      this.alerts.send('exportHistory');
    }
  }

  @action
  closePopup() {
    if (this.isOnAlertsRoute && this.alerts) {
      this.alerts.send('closePopup');
    }
  }

  @action
  clearCSVFile(fileName) {
    if (this.isOnAlertsRoute && this.alerts) {
      this.alerts.send('clearCSVFile', fileName);
    }
  }

  @action
  clearAllFiles() {
    if (this.isOnAlertsRoute && this.alerts) {
      this.alerts.send('clearAllFiles');
    }
  }
  @action
  goToDashboard() {
    this.router.transitionTo('dashboard');
  }
  @action
  goToAlerts() {
    this.router.transitionTo('alerts');
  }
  @action
  goToSearch() {
    this.router.transitionTo('search');
  }
}
