import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { computed } from '@ember/object';
import { inject as service } from '@ember/service';
export default class SearchController extends Controller {
  @tracked searchTerm = '';
  @tracked searchField = '';
  @tracked results = [];
  @tracked currentPage = 1;
  @tracked pageSize = 10;
  @tracked totalRecords = 0;
  @tracked totalPages = 0;
  @tracked searchAfter = [];
  @tracked searchCategory = '';
  @tracked sortCategory = '';
  @tracked sortOrder = '';
  @tracked condition = '';
  @tracked selectedDevice = '';
  @tracked isDeviceModalOpen = false;
  @tracked devices = [];

  @tracked tempSelectedDevice = this.selectedDevice;
  @tracked isLoading = false;
  @service session;
  @service router;

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

    const searchTerm = `${this.searchCategory}${condition}${this.searchField}`;

    return searchTerm;
  }
  fetchResults(encodedSearchTerm) {
    let endpoint = encodedSearchTerm
      ? `search?query=${encodedSearchTerm}&page=${this.currentPage - 1}&pageSize=${this.pageSize}`
      : `search/all?page=${this.currentPage - 1}&pageSize=${this.pageSize}`;

    if (this.searchAfter && this.searchAfter.length > 0) {
      const searchAfterString = this.searchAfter.join(',');
      endpoint += `&searchAfter=${encodeURIComponent(searchAfterString)}`;
    }
    if (this.selectedDevice) {
      endpoint += `&deviceName=${this.selectedDevice}`;
    }
    if (this.sortCategory) {
      endpoint += `&sortBy=${this.sortCategory}`;
    }

    if (this.sortOrder) {
      endpoint += `&sortOrder=${this.sortOrder}`;
    }

    this.isLoading = true;

    const xhr = new XMLHttpRequest();
    xhr.open(
      'GET',
      `http://localhost:8500/servletlog/v1/logs/${endpoint}`,
      true,
    );
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;

    xhr.onload = () => {
      this.isLoading = false;
      if (xhr.status === 200) {
        try {
          let data = JSON.parse(xhr.responseText);
          this.results = data.logs;
          this.totalRecords = data.totalRecords;
          this.totalPages = Math.ceil(this.totalRecords / this.pageSize);
          this.searchAfter = data.searchAfter || [];
        } catch (error) {
          console.error('Error:', error);
          alert('An error occurred while fetching results.');
        }
      } else {
        console.error('Error fetching results');
      }
    };

    xhr.onerror = () => {
      this.isLoading = false;
      console.error('Request failed', xhr.statusText);
    };

    xhr.send();
  }

  @action
  sortLogs(column, order) {
    this.sortCategory = column;
    this.sortOrder = order;
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchResults(this.constructSearchTerm());
  }
  @action
  updateSearchTerm(event) {
    this.searchTerm = event.target.value;
  }
  @action
  updateSearchField(event) {
    this.searchField = event.target.value;
  }
  @action
  updateCondition(event) {
    this.condition = event.target.value;
  }
  @action
  applyFiltering() {
    this.searchTerm = this.constructSearchTerm();
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchResults(this.searchTerm);
    this.isDeviceModalOpen = false;
  }
  @action
  async submitSearch() {
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchResults(
      this.searchTerm ? encodeURIComponent(this.searchTerm) : null,
    );
  }

  @action
  changePageSize(event) {
    this.pageSize = event.target.value;
    this.currentPage = 1;
    this.searchAfter = [];
    const searchTerm = this.constructSearchTerm();
    this.fetchResults(searchTerm);
  }

  @action
  goToNextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      const searchTerm = this.constructSearchTerm();
      this.fetchResults(searchTerm);
    }
  }

  @action
  goToPrevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      const searchTerm = this.constructSearchTerm();
      this.fetchResults(searchTerm);
    }
  }

  @action
  updateSearchCategory(event) {
    this.searchCategory = event.target.value;
  }
  @action
  updateSortCategory(event) {
    this.sortCategory = event.target.value;
  }

  @action
  updateSortOrder(event) {
    this.sortOrder = event.target.value;
  }

  @action
  updateSelectedDevice(event) {
    this.selectedDevice = event.target.value;
    this.getLogsFromDevice();
  }
  @action
  getLogsFromDevice() {
    this.isLoading = true;
    const xhr = new XMLHttpRequest();
    xhr.open(
      'GET',
      `http://localhost:8500/servletlog/v1/device/find?deviceName=${this.selectedDevice}`,
    );
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = () => {
      this.isLoading = false;
      if (xhr.status === 200) {
        try {
          let data = JSON.parse(xhr.responseText);
          this.results = data.logs;
          this.totalRecords = data.totalRecords;
          this.totalPages = Math.ceil(this.totalRecords / this.pageSize);
          this.searchAfter = data.searchAfter || [];
        } catch (error) {
          console.error('Error:', error);
          alert('An error occurred while fetching results.');
        }
      } else {
        console.error('Error fetching results');
      }
    };

    xhr.onerror = () => {
      this.isLoading = false;
      console.error('Request failed', xhr.statusText);
    };

    xhr.send();
  }
  @action
  applySorting() {
    this.currentPage = 1;
    this.searchAfter = [];
    const searchTerm = this.constructSearchTerm();
    this.fetchResults(searchTerm);
  }

  @computed('currentPage', 'pageSize', 'totalRecords')
  get paginationInfo() {
    const start = (this.currentPage - 1) * this.pageSize + 1;
    const end = Math.min(this.currentPage * this.pageSize, this.totalRecords);
    return `${start} - ${end} of ${this.totalRecords}`;
  }
  @action
  openDeviceSelection() {
    // this.tempSelectedDevice = this.selectedDevice;
    this.isDeviceModalOpen = true;
  }

  @action
  closeDeviceSelection() {
    this.isDeviceModalOpen = false;
  }
  @action
  updateSelectedDeviceTemp(event) {
    this.tempSelectedDevice = event.target.value;
  }

  @action
  submitSelectedDevice() {
    this.selectedDevice = this.tempSelectedDevice;
    this.isDeviceModalOpen = false;
  }
  @action
  fetchDevicesList() {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', 'http://localhost:8500/servletlog/v1/device/all', true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;

    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const data = JSON.parse(xhr.responseText);
          this.devices = data || [];
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
}
