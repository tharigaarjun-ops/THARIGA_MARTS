/* ==================================================================
   Shared navbar behaviour: auth state, cart badge, categories menu,
   global search, role dashboards, logout and flash messages.
   ================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initAiAssistant();
  initImageFallback();
});

/** Swap in a neutral placeholder whenever any product image fails to load. */
function initImageFallback() {
  // The "error" event does not bubble, so capture it at the document level.
  document.addEventListener('error', (event) => {
    const img = event.target;
    if (!img || img.tagName !== 'IMG' || img.dataset.fbk) return;
    img.dataset.fbk = '1';
    img.src = '/images/products/fallback.svg';
  }, true);
}

async function initNavbar() {
  highlightActiveNav();
  refreshAuthUI(getStoredUser());
  bindSearch();
  bindLogout();

  const flash = consumeFlash();
  if (flash) {
    setTimeout(() => showToast(flash.message, flash.type || 'info'), 250);
  }

  loadNavCategories();

  if (isLoggedIn()) {
    await refreshCart();
    try {
      const user = await api('/account/profile');
      setSession(getToken(), user);
      refreshAuthUI(user);
      injectRoleLinks(user.role);
    } catch (err) {
      // 401 already cleared the session inside api()
      refreshAuthUI(getStoredUser());
    }
  }
}

document.addEventListener('am:session-expired', () => {
  refreshAuthUI(null);
  injectRoleLinks(null);
});

document.addEventListener('am:cart-changed', () => updateCartBadge());

function highlightActiveNav() {
  const page = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.am-navbar .nav-link[data-nav]').forEach(link => {
    link.classList.toggle('active', link.dataset.nav === page);
  });
}

function refreshAuthUI(user) {
  const guestBox = document.getElementById('authGuest');
  const userBox = document.getElementById('authUser');
  if (!guestBox || !userBox) return;

  const signedIn = !!user && isLoggedIn();
  guestBox.classList.toggle('d-none', signedIn);
  userBox.classList.toggle('d-none', !signedIn);

  if (signedIn) {
    const name = user.fullName || user.email || 'User';
    const role = (user.role || '').toUpperCase();
    const avatar = document.getElementById('userName');
    const emailEl = document.getElementById('userEmail');
    const avatarEl = document.getElementById('userAvatar');
    if (avatar) avatar.textContent = name.split(' ')[0];
    if (emailEl) emailEl.textContent = (user.email || '') + (role ? '  ·  ' + role : '');
    if (avatarEl) avatarEl.textContent = name.trim().charAt(0).toUpperCase();
  }
}

/** Adds "Seller Dashboard" / "Admin Panel" entries to the user menu. */
function injectRoleLinks(role) {
  const dropdown = document.querySelector('#authUser .dropdown-menu');
  if (!dropdown) return;

  dropdown.querySelectorAll('.am-role-link').forEach(entry => entry.remove());

  const links = [];
  if (role === 'ROLE_SELLER') {
    links.push('<li class="am-role-link"><a class="dropdown-item" href="/seller.html">'
      + '<i class="bi bi-shop me-2"></i>Seller Dashboard</a></li>');
  }
  if (role === 'ROLE_ADMIN') {
    links.push('<li class="am-role-link"><a class="dropdown-item" href="/admin.html">'
      + '<i class="bi bi-speedometer2 me-2"></i>Admin Panel</a></li>');
  }

  if (!links.length) return;
  const divider = dropdown.querySelector('.dropdown-divider');
  if (divider) {
    divider.insertAdjacentHTML('beforebegin', links.join(''));
  } else {
    dropdown.insertAdjacentHTML('beforeend', links.join(''));
  }
}

function bindSearch() {
  const form = document.getElementById('navSearchForm');
  const input = document.getElementById('navSearchInput');
  if (!form || !input) return;

  const current = queryParam('search');
  if (current && (location.pathname.endsWith('products.html') || location.pathname === '/')) {
    input.value = current;
  }

  form.addEventListener('submit', event => {
    event.preventDefault();
    const query = input.value.trim();
    location.href = '/products.html' + (query ? '?search=' + encodeURIComponent(query) : '');
  });
}

function bindLogout() {
  const button = document.getElementById('logoutBtn');
  if (!button) return;
  button.addEventListener('click', () => {
    clearSession();
    setFlash('success', 'You have been logged out successfully');
    location.href = '/index.html';
  });
}

async function loadNavCategories() {
  const dropdown = document.getElementById('navCategoryMenu');
  const footerList = document.getElementById('footerCats');
  try {
    const categories = await api('/categories', { auth: false });
    if (!Array.isArray(categories)) return;

    if (dropdown) {
      dropdown.innerHTML = categories.map(category =>
        '<li><a class="dropdown-item" href="/products.html?category=' +
        encodeURIComponent(category.slug) + '">' +
        escapeHtml(category.iconEmoji ? category.iconEmoji + ' ' : '') +
        escapeHtml(category.name) + '</a></li>'
      ).join('');
    }

    if (footerList) {
      footerList.innerHTML = categories.slice(0, 7).map(category =>
        '<a href="/products.html?category=' + encodeURIComponent(category.slug) + '">' +
        escapeHtml(category.name) + '</a>'
      ).join('');
    }
  } catch (err) {
    // categories are decorative here - ignore failures
  }
}

/* ==================================================================
   AI Shopping Assistant (floating chat widget)
   Sends the shopper's question to POST /api/ai/chat. The OpenAI API
   key is read by the backend from the OPENAI_API_KEY environment
   variable and is never exposed to the browser.
   ================================================================== */

function initAiAssistant() {
  if (document.getElementById('amAiFab')) return;

  const fab = document.createElement('button');
  fab.id = 'amAiFab';
  fab.className = 'am-ai-fab';
  fab.type = 'button';
  fab.title = 'Ask the AI shopping assistant';
  fab.setAttribute('aria-label', 'AI shopping assistant');
  fab.innerHTML = '<i class="bi bi-stars"></i>';

  const panel = document.createElement('div');
  panel.className = 'am-ai-panel';
  panel.id = 'amAiPanel';
  panel.hidden = true;
  panel.innerHTML =
    '<div class="am-ai-head">' +
      '<i class="bi bi-stars"></i>' +
      '<div><b>THARIGA AI Assistant</b>' +
      '<div class="am-ai-sub">Stationery help &amp; picks</div></div>' +
      '<button class="am-ai-close" type="button" aria-label="Close">&times;</button>' +
    '</div>' +
    '<div class="am-ai-messages" id="amAiMessages"></div>' +
    '<form class="am-ai-form" id="amAiForm">' +
      '<input id="amAiInput" type="text" placeholder="Ask about stationery…" autocomplete="off" aria-label="Message the AI assistant">' +
      '<button class="am-ai-send" type="submit" aria-label="Send"><i class="bi bi-send"></i></button>' +
    '</form>';

  document.body.appendChild(fab);
  document.body.appendChild(panel);

  const messages = panel.querySelector('#amAiMessages');
  const form = panel.querySelector('#amAiForm');
  const input = panel.querySelector('#amAiInput');

  fab.addEventListener('click', () => {
    panel.hidden = !panel.hidden;
    if (!panel.hidden) {
      input.focus();
      if (!messages.dataset.greeted) {
        messages.dataset.greeted = '1';
        addAiMessage('bot', "Hi! I'm your THARIGA MART shopping assistant. Ask me about pens, notebooks, school bags, art supplies or anything else in the shop, and I'll recommend products for you.");
      }
    }
  });
  panel.querySelector('.am-ai-close').addEventListener('click', () => { panel.hidden = true; });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    addAiMessage('user', text);
    await sendAiMessage(text);
  });
}

function addAiMessage(who, html) {
  const messages = document.getElementById('amAiMessages');
  if (!messages) return null;
  const div = document.createElement('div');
  div.className = 'am-ai-msg ' + who;
  div.innerHTML = html;
  messages.appendChild(div);
  messages.scrollTop = messages.scrollHeight;
  return div;
}

function showAiTyping() {
  const messages = document.getElementById('amAiMessages');
  if (!messages) return null;
  const div = document.createElement('div');
  div.className = 'am-ai-msg bot';
  div.innerHTML = '<span class="am-ai-typing"><span></span><span></span><span></span></span>';
  messages.appendChild(div);
  messages.scrollTop = messages.scrollHeight;
  return div;
}

async function sendAiMessage(text) {
  const typing = showAiTyping();
  try {
    const response = await api('/ai/chat', { method: 'POST', body: { message: text } });
    if (typing) typing.remove();
    const data = response && response.data ? response.data : response;
    const reply = (data && data.reply)
      ? data.reply
      : "I'm not sure about that. Try asking about pens, notebooks, bags or art supplies.";
    let html = escapeHtml(reply).replace(/\n/g, '<br>');
    const products = (data && Array.isArray(data.products)) ? data.products : [];
    if (products.length) {
      html += '<div class="am-ai-recs">' + products.map(p =>
        '<a class="am-ai-rec" href="/product-details.html?id=' + encodeURIComponent(p.id) + '">' +
          '<img src="' + escapeHtml(p.image || '/images/stationery/stationery.svg') + '" alt="' + escapeHtml(p.name) + '" loading="lazy">' +
          '<span><span class="t">' + escapeHtml(p.name) + '</span><br><span class="p">' + formatINR(p.price) + '</span></span>' +
        '</a>').join('') + '</div>';
    }
    if (data && data.aiEnabled === false) {
      html += '<div class="small text-muted mt-1">Set the OPENAI_API_KEY environment variable for full AI-powered answers.</div>';
    }
    addAiMessage('bot', html);
  } catch (err) {
    if (typing) typing.remove();
    if (err && err.status === 401) {
      addAiMessage('bot', 'Please <a href="/login.html">log in</a> to chat with the AI assistant.');
    } else {
      addAiMessage('bot', escapeHtml((err && err.message) || 'Sorry, I could not reach the assistant right now.'));
    }
  }
}
