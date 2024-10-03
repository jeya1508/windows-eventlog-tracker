import { module, test } from 'qunit';
import { setupTest } from 'logging/tests/helpers';

module('Unit | Route | manageAlert', function (hooks) {
  setupTest(hooks);

  test('it exists', function (assert) {
    let route = this.owner.lookup('route:manage-alert');
    assert.ok(route);
  });
});
