import { module, test } from 'qunit';
import { setupRenderingTest } from 'logging/tests/helpers';
import { render } from '@ember/test-helpers';
import { hbs } from 'ember-cli-htmlbars';

module('Integration | Component | log-table', function (hooks) {
  setupRenderingTest(hooks);

  test('it renders', async function (assert) {
    // Set any properties with this.set('myProperty', 'value');
    // Handle any actions with this.set('myAction', function(val) { ... });

    await render(hbs`<LogTable />`);

    assert.dom().hasText('');

    // Template block usage:
    await render(hbs`
      <LogTable>
        template block text
      </LogTable>
    `);

    assert.dom().hasText('template block text');
  });
});
