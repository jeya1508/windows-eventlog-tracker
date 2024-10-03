import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import { computed } from '@ember/object';
import { inject as service } from '@ember/service';
export default class SearchController extends Controller {
  @tracked searchTerm = '';
  @tracked results = [];
  @tracked currentPage = 1;
  @tracked pageSize = 10;
  @tracked totalRecords = 0;
  @tracked totalPages = 0;
  @tracked searchAfter = [];
  @tracked searchCategory = '';
  @tracked sortOrder = '';

  @service session;
  @service router;

  constructor() {
    super(...arguments);
    this.fetchResults();
  }

  fetchResults() {
    let endpoint = this.searchTerm
      ? `search?query=${encodeURIComponent(this.searchTerm)}&page=${this.currentPage - 1}&pageSize=${this.pageSize}`
      : `search/all?page=${this.currentPage - 1}&pageSize=${this.pageSize}`;

    if (this.searchAfter && this.searchAfter.length > 0) {
      const searchAfterString = this.searchAfter.join(',');
      endpoint += `&searchAfter=${encodeURIComponent(searchAfterString)}`;
    }

    if (this.searchCategory) {
      endpoint += `&sortBy=${this.searchCategory}`;
    }

    if (this.sortOrder) {
      endpoint += `&sortOrder=${this.sortOrder}`;
    }

    const xhr = new XMLHttpRequest();
    xhr.open(
      'GET',
      `http://localhost:8500/servletlog/v1/logs/${endpoint}`,
      true,
    );
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          let data = JSON.parse(xhr.responseText);
          this.results =
            data.logs.map((item) =>
              Object.entries(item)
                .filter(([key, value]) => key !== 'sortValues' && key !== '_id')
                .map(([key, value]) => `${key}: ${value}`)
                .join(', '),
            ) || [];
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
      console.error('Request failed', xhr.statusText);
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
    this.fetchResults();
  }

  @action
  changePageSize(event) {
    this.pageSize = event.target.value;
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchResults();
  }

  @action
  goToNextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.fetchResults();
    }
  }

  @action
  goToPrevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.fetchResults();
    }
  }

  @action
  updateSearchCategory(event) {
    this.searchCategory = event.target.value;
  }

  @action
  updateSortOrder(event) {
    this.sortOrder = event.target.value;
  }

  @action
  applySorting() {
    this.currentPage = 1;
    this.searchAfter = [];
    this.fetchResults();
  }

  @computed('currentPage', 'pageSize', 'totalRecords')
  get paginationInfo() {
    const start = (this.currentPage - 1) * this.pageSize + 1;
    const end = Math.min(this.currentPage * this.pageSize, this.totalRecords);
    return `${start} - ${end} of ${this.totalRecords}`;
  }
}
