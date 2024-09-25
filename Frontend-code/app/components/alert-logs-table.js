import Component from '@glimmer/component';

export default class AlertLogsTableComponent extends Component {
  formatTimestamp(epochTime) {
    let date = new Date(epochTime * 1000); 
    return date.toLocaleString(); 
  }
}
