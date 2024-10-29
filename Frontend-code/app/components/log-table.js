import Component from '@glimmer/component';
import { action } from '@ember/object';
export default class LogTableComponent extends Component {
  formatTimestamp(epochTime) {
    let date = new Date(epochTime * 1000);
    return date.toLocaleString();
  }
  @action
  sortColumn(column, order) {
    // Call the action from the controller to handle sorting
    if (typeof this.args.sortLogs === 'function') {
      this.args.sortLogs(column, order);
    }
  }
}
