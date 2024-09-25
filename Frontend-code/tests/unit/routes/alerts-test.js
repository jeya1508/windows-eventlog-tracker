import { module, test } from 'qunit';
import { setupTest } from 'logging/tests/helpers';

module('Unit | Route | alerts', function (hooks) {
  setupTest(hooks);

  test('it exists', function (assert) {
    let route = this.owner.lookup('route:alerts');
    assert.ok(route);
  });
});
