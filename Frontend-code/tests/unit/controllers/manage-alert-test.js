import { module, test } from 'qunit';
import { setupTest } from 'logging/tests/helpers';

module('Unit | Controller | manage-alert', function (hooks) {
  setupTest(hooks);

  // TODO: Replace this with your real tests.
  test('it exists', function (assert) {
    let controller = this.owner.lookup('controller:manage-alert');
    assert.ok(controller);
  });
});
