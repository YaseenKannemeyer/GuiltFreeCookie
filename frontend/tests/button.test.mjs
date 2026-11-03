import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'vite';
import { createSSRApp, h } from 'vue';
import { renderToString } from 'vue/server-renderer';
import { createMemoryHistory, createRouter } from 'vue-router';

test('navigation buttons preserve router-generated hrefs and disabled semantics', async () => {
  const server = await createServer({ server: { middlewareMode: true, hmr: false, ws: false }, appType: 'custom' });
  try {
    const { default: Button } = await server.ssrLoadModule('/src/components/ui/BaseButton.vue');
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/cart', component: { render: () => null } }] });
    await router.push('/cart');
    const render = async props => {
      const app = createSSRApp({ render: () => h(Button, props, () => 'Go to cart') });
      app.use(router);
      return renderToString(app);
    };
    assert.match(await render({ to: '/cart' }), /href="\/cart"/);
    const disabledLink = await render({ to: '/cart', disabled: true });
    assert.match(disabledLink, /aria-disabled="true"/);
    assert.match(disabledLink, /tabindex="-1"/);
    assert.match(await render({ loading: true }), /disabled/);
  } finally { await server.close(); }
});
