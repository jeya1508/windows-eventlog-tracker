import Controller from '@ember/controller';
import { tracked } from '@glimmer/tracking';
import { action } from '@ember/object';

export default class AlertsController extends Controller {
  @tracked searchTerm = '';
  @tracked logs = [];
  @tracked currentPage = 1;
  @tracked pageSize = 10;
  @tracked totalRecords = 0;
  @tracked totalPages = 0;
  @tracked searchAfter = [];

  constructor() {
    super(...arguments);
  }

  fetchLogs() {
    let endpoint = this.searchTerm
    ? `search?query=${encodeURIComponent(this.searchTerm)}&page=${this.currentPage - 1}&pageSize=${this.pageSize}`
    : `all?page=${this.currentPage - 1}&pageSize=${this.pageSize}`;

    if (this.searchAfter.length > 0 && this.currentPage > 1) {
      const searchAfterString = this.searchAfter.join(',');
      endpoint += `&searchAfter=${encodeURIComponent(searchAfterString)}`;
    }

    const xhr = new XMLHttpRequest();
    xhr.open('GET', `http://localhost:8500/servletlog/v1/alert/${endpoint}`, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;

    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const data = JSON.parse(xhr.responseText);
          this.logs = data.logs; 
          this.totalRecords = data.totalRecords;
          this.totalPages = Math.ceil(this.totalRecords / this.pageSize);
          this.searchAfter = data.searchAfter || [];

        } catch (error) {
          console.error('Error parsing response:', error);
        }
      } else {
        console.error('Error fetching logs:', xhr.statusText);
      }
    };

    xhr.onerror = () => {
      console.error('Network error while fetching logs');
    };

    xhr.send();
  }
  @action
  updateSearchTerm(event) {
    this.searchTerm = event.target.value;
  }
  @action
  async submitSearch() {
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchLogs();
  }

  @action
  updatePageSize(event) {
    this.pageSize = event.target.value;
    this.currentPage = 1;  
    this.searchAfter = [];
    this.fetchLogs();
  }

  @action
  goToNextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.fetchLogs();
    }
  }

  @action
  goToPrevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.fetchLogs();
    }
  }
  @action
  paginationInfo() {
    const start = (this.currentPage - 1) * this.pageSize + 1;
    const end = Math.min(this.currentPage * this.pageSize, this.totalRecords);
    return `${start} - ${end} of ${this.totalRecords}`;
  }
}
