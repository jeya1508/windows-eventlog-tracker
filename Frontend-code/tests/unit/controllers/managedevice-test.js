import { module, test } from 'qunit';
import { setupTest } from 'logging/tests/helpers';

module('Unit | Controller | managedevice', function (hooks) {
  setupTest(hooks);

  // TODO: Replace this with your real tests.
  test('it exists', function (assert) {
    let controller = this.owner.lookup('controller:managedevice');
    assert.ok(controller);
  });
});
