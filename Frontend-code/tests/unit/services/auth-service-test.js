import { module, test } from 'qunit';
import { setupTest } from 'logging/tests/helpers';

module('Unit | Service | authService', function (hooks) {
  setupTest(hooks);

  // TODO: Replace this with your real tests.
  test('it exists', function (assert) {
    let service = this.owner.lookup('service:auth-service');
    assert.ok(service);
  });
});
