import Controller from '@ember/controller';
import { tracked } from '@glimmer/tracking';
import { action } from '@ember/object';

export default class AlertsController extends Controller {
  @tracked searchTerm = '';
  @tracked logs = [];
  @tracked currentPage = 1;
  @tracked pageSize = 10;
  @tracked totalRecords = 0;
  @tracked alertCategory = '';
  @tracked condition = '';
  @tracked profileName = '';
  @tracked totalPages = 0;
  @tracked searchAfter = [];
  @tracked alertProfiles = []; 

  constructor() {
    super(...arguments);
  }

  constructSearchTerm() {
    let condition = '';
    if (this.condition === 'equals') {
      condition = '=';  
    } else if (this.condition === 'notequal') {
      condition = '!='; 
    }

    const searchTerm = `${this.alertCategory}${condition}${this.profileName}`;
    return encodeURIComponent(searchTerm).replace(/!/g, "%21");
  }

  fetchLogs(encodedSearchTerm) {
    let endpoint = encodedSearchTerm
      ? `search?query=${encodedSearchTerm}&page=${this.currentPage - 1}&pageSize=${this.pageSize}`
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
      const data = JSON.parse(xhr.responseText);
      if (xhr.status === 200) {
        try {
          this.logs = data.logs;
          this.totalRecords = data.totalRecords;
          this.totalPages = Math.ceil(this.totalRecords / this.pageSize);
          this.searchAfter = data.searchAfter || [];
        } catch (error) {
          console.error('Error parsing response:', error);
        }
      } else {
        console.error('Error fetching logs');
      }
    };
  
    xhr.onerror = () => {
      console.error('Network error while fetching logs');
    };
  
    xhr.send();
  }
  
  fetchAlertProfiles() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', 'http://localhost:8500/servletlog/v1/alert/allProfiles', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
  
    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const data = JSON.parse(xhr.responseText);
          this.alertProfiles = data.profileNames || []; 
        } catch (error) {
          console.error('Error parsing alert profiles:', error);
        }
      } else {
        console.error('Error fetching alert profiles');
      }
    };
  
    xhr.onerror = () => {
      console.error('Network error while fetching alert profiles');
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
    this.fetchLogs(this.searchTerm ? encodeURIComponent(this.searchTerm) : null);
  }

  @action
  updatePageSize(event) {
    this.pageSize = event.target.value;
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchLogs(this.searchTerm ? encodeURIComponent(this.searchTerm) : null);
  }

  @action
  goToNextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.fetchLogs(this.searchTerm ? encodeURIComponent(this.searchTerm) : null);
    }
  }
  
  @action
  goToPrevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.fetchLogs(this.searchTerm ? encodeURIComponent(this.searchTerm) : null);
    }
  }
  
  @action
  applyFiltering() {
    this.searchTerm = this.constructSearchTerm(); 
    this.currentPage = 1;
    this.fetchLogs(this.searchTerm); 
  }

  @action
  paginationInfo() {
    const start = (this.currentPage - 1) * this.pageSize + 1;
    const end = Math.min(this.currentPage * this.pageSize, this.totalRecords);
    return `${start} - ${end} of ${this.totalRecords}`;
  }

  @action
  changeAlertCategory(event) {
    this.alertCategory = event.target.value;
  }

  @action
  changeCondition(event) {
    this.condition = event.target.value;
  }

  @action
  changeProfileName(event) {
    this.profileName = event.target.value;
  }

  @action
  exportAsCSV() {
    console.log('Exporting as CSV...');

    const xhr = new XMLHttpRequest();
    const searchTerm = this.constructSearchTerm(); 

    xhr.open('GET', `http://localhost:8500/servletlog/v1/alert/export/csv?query=${searchTerm}`, true);
    xhr.withCredentials = true;
    xhr.responseType = 'blob'; 

    xhr.onload = () => {
      if (xhr.status === 200) {
        const blob = xhr.response;

        const link = document.createElement('a');
        const url = window.URL.createObjectURL(blob);
        link.href = url;
        link.download = 'exported_alerts.csv'; 

        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      } else {
        console.error('Failed to export CSV:', xhr.statusText);
      }
    };
    xhr.onerror = () => {
      console.error('Network error occurred while exporting CSV.');
    };

    xhr.send();
  }
}
