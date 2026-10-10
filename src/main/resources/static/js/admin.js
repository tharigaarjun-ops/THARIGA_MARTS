/* ==================================================================
   Admin Panel: store stats, order fulfilment, user management
   and full catalogue management.
   ================================================================== */

let adminCategories = [];
let adminSellers = [];
let adminProducts = [];

document.addEventListener('DOMContentLoaded', () => {
  initAdminPanel();
});

async function initAdminPanel() {
  document.getElementById('addProductBtn').addEventListener('click', () => openProductForm());
  document.getElementById('cancelProductBtn').addEventListener('click', hideProductForm);
  document.getElementById('productForm').addEventListener('submit', saveProduct);
  document.getElementById('refreshProductsBtn').addEventListener('click', loadAdminProducts);

  // Role gate: admins only.
  try {
    const profile = await api('/account/profile');
    if (profile.role !== 'ROLE_ADMIN') {
      document.getElementById('adminNotice').classList.remove('d-none');
      document.getElementById('statsRow').classList.add('d-none');
      return;
    }
  } catch (err) {
    document.getElementById('adminNotice').classList.remove('d-none');
    return;
  }

  await Promise.all([
    loadCategories(),
    loadAdminStats(),
    loadAdminOrders(),
    loadAdminUsers(),
    loadAdminProducts()
  ]);
}

/* ------------------------- lookups ------------------------- */

async function loadCategories() {
  try {
    adminCategories = await api('/categories', { auth: false });
    const select = document.getElementById('pCategory');
    select.innerHTML = '<option value="">Select category…</option>' +
      adminCategories.map(category =>
        '<option value="' + escapeHtml(category.id) + '">' +
        escapeHtml(category.name) + '</option>').join('');
  } catch (err) {
    adminCategories = [];
  }

  try {
    const users = await api('/admin/users');
    adminSellers = users.filter(user => user.role === 'ROLE_SELLER');
    const sellerSelect = document.getElementById('pSeller');
    sellerSelect.innerHTML = '<option value="">Select seller…</option>' +
      adminSellers.map(seller =>
        '<option value="' + escapeHtml(seller.id) + '">' +
        escapeHtml(seller.name) + ' (' + escapeHtml(seller.email) + ')</option>').join('');
  } catch (err) {
    adminSellers = [];
  }
}

/* ------------------------- stats ------------------------- */

async function loadAdminStats() {
  try {
    const stats = await api('/admin/stats');
    document.getElementById('statUsers').textContent = stats.totalUsers;
    document.getElementById('statProducts').textContent = stats.totalProducts;
    document.getElementById('statOrders').textContent = stats.totalOrders;
    document.getElementById('statRevenue').textContent = formatINR(stats.totalRevenue || 0);
    document.getElementById('statPending').textContent = stats.pendingOrders;
    document.getElementById('statSellers').textContent = stats.totalSellers;
    document.getElementById('statPayments').textContent = stats.completedPayments;
  } catch (err) {
    handleApiError(err);
  }
}

/* ------------------------- orders ------------------------- */

function statusLabel(status) {
  if (!status) return 'Unknown';
  return status === 'ORDER_PLACED' ? 'Order Placed'
      : status.charAt(0) + status.slice(1).toLowerCase();
}

async function loadAdminOrders() {
  const root = document.getElementById('adminOrders');

  try {
    const orders = await api('/admin/orders');

    if (!Array.isArray(orders) || orders.length === 0) {
      root.innerHTML = '<p class="text-muted small mb-0">No orders yet.</p>';
      return;
    }

    root.innerHTML = '<div class="table-responsive"><table class="table table-sm align-middle mb-0">' +
      '<thead><tr>' +
        '<th>Order</th><th>Customer</th><th>Items</th>' +
        '<th>Amount</th><th>Status</th><th class="text-end">Fulfil</th>' +
      '</tr></thead><tbody>' +
      orders.map(order => {
        const editable = order.status === 'ORDER_PLACED' || order.status === 'CONFIRMED'
          || order.status === 'SHIPPED';
        return '<tr>' +
          '<td><div class="fw-semibold small">' + escapeHtml(order.orderNumber) + '</div>' +
            '<div class="text-muted small">' + formatDate(order.placedAt) + '</div></td>' +
          '<td class="small">' + escapeHtml(order.customerName || '-') + '</td>' +
          '<td class="small">' + order.itemCount + '</td>' +
          '<td class="small fw-semibold">' + formatINR(order.finalAmount) + '</td>' +
          '<td><span class="status-pill status-' + escapeHtml(order.status) + '">' +
            escapeHtml(statusLabel(order.status)) + '</span></td>' +
          '<td class="text-end text-nowrap">' +
            (editable
              ? '<button class="btn btn-sm btn-outline-primary me-1" type="button" ' +
                  'data-order-action="confirm" data-id="' + escapeHtml(order.id) + '" ' +
                  'title="Confirm">Confirm</button>' +
                '<button class="btn btn-sm btn-outline-secondary me-1" type="button" ' +
                  'data-order-action="ship" data-id="' + escapeHtml(order.id) + '" ' +
                  'title="Ship">Ship</button>' +
                '<button class="btn btn-sm btn-outline-success me-1" type="button" ' +
                  'data-order-action="deliver" data-id="' + escapeHtml(order.id) + '" ' +
                  'title="Mark delivered">Deliver</button>' +
                '<button class="btn btn-sm btn-outline-danger" type="button" ' +
                  'data-order-action="cancel" data-id="' + escapeHtml(order.id) + '">Cancel</button>'
              : '<span class="text-muted small">—</span>') +
          '</td>' +
        '</tr>';
      }).join('') +
      '</tbody></table></div>';

    const nextStatus = { confirm: 'CONFIRMED', ship: 'SHIPPED', deliver: 'DELIVERED', cancel: 'CANCELLED' };
    root.querySelectorAll('[data-order-action]').forEach(button => {
      button.addEventListener('click', () => {
        const id = Number(button.dataset.id);
        const status = nextStatus[button.dataset.orderAction];
        updateOrderStatus(id, status);
      });
    });
  } catch (err) {
    root.innerHTML = '<p class="text-muted small mb-0">Could not load orders.</p>';
    handleApiError(err);
  }
}

async function updateOrderStatus(orderId, status) {
  try {
    await api('/admin/orders/' + encodeURIComponent(orderId) + '/status', {
      method: 'PUT',
      body: { status }
    });
    showToast('Order ' + status.toLowerCase().replace('_', ' '), 'success');
    await Promise.all([loadAdminOrders(), loadAdminStats()]);
  } catch (err) {
    handleApiError(err);
  }
}

/* ------------------------- users ------------------------- */

async function loadAdminUsers() {
  const root = document.getElementById('adminUsers');

  try {
    const users = await api('/admin/users');

    if (!Array.isArray(users) || users.length === 0) {
      root.innerHTML = '<p class="text-muted small mb-0">No users yet.</p>';
      return;
    }

    root.innerHTML = '<div class="table-responsive"><table class="table table-sm align-middle mb-0">' +
      '<thead><tr>' +
        '<th>User</th><th>Role</th><th>Orders</th><th>Status</th><th class="text-end">Actions</th>' +
      '</tr></thead><tbody>' +
      users.map(user =>
        '<tr>' +
          '<td><div class="fw-semibold small">' + escapeHtml(user.name) + '</div>' +
            '<div class="text-muted small">' + escapeHtml(user.email) + '</div></td>' +
          '<td><select class="form-select form-select-sm" style="width:auto;display:inline-block" ' +
            'data-user-role data-id="' + escapeHtml(user.id) + '">' +
            ['ROLE_BUYER', 'ROLE_SELLER', 'ROLE_ADMIN'].map(role =>
              '<option value="' + role + '"' + (user.role === role ? ' selected' : '') + '>' +
              role.replace('ROLE_', '') + '</option>').join('') +
          '</select></td>' +
          '<td class="small">' + (user.orderCount || 0) + '</td>' +
          '<td>' + (user.enabled
            ? '<span class="badge bg-success-subtle text-success">Active</span>'
            : '<span class="badge bg-secondary">Disabled</span>') + '</td>' +
          '<td class="text-end">' +
            '<button class="btn btn-sm ' + (user.enabled ? 'btn-outline-warning' : 'btn-outline-success') +
              '" type="button" data-user-toggle data-id="' + escapeHtml(user.id) + '" ' +
              'data-enabled="' + (user.enabled ? 'true' : 'false') + '">' +
              (user.enabled ? 'Disable' : 'Enable') + '</button>' +
          '</td>' +
        '</tr>'
      ).join('') +
      '</tbody></table></div>';

    root.querySelectorAll('[data-user-role]').forEach(select => {
      select.addEventListener('change', () => {
        updateUserRole(Number(select.dataset.id), select.value);
      });
    });

    root.querySelectorAll('[data-user-toggle]').forEach(button => {
      button.addEventListener('click', () => {
        const id = Number(button.dataset.id);
        const enabled = button.dataset.enabled !== 'true';
        updateUserEnabled(id, enabled);
      });
    });
  } catch (err) {
    root.innerHTML = '<p class="text-muted small mb-0">Could not load users.</p>';
    handleApiError(err);
  }
}

async function updateUserRole(userId, role) {
  try {
    await api('/admin/users/' + encodeURIComponent(userId) + '/role', {
      method: 'PUT',
      body: { role }
    });
    showToast('Role updated', 'success');
    await loadAdminUsers();
    await loadAdminStats();
  } catch (err) {
    handleApiError(err);
    await loadAdminUsers();
  }
}

async function updateUserEnabled(userId, enabled) {
  try {
    await api('/admin/users/' + encodeURIComponent(userId) + '/enabled', {
      method: 'PUT',
      body: { enabled }
    });
    showToast(enabled ? 'Account enabled' : 'Account disabled', 'success');
    await loadAdminUsers();
  } catch (err) {
    handleApiError(err);
    await loadAdminUsers();
  }
}

/* ------------------------- products ------------------------- */

async function loadAdminProducts() {
  const root = document.getElementById('adminProducts');

  try {
    adminProducts = await api('/admin/products');

    if (!Array.isArray(adminProducts) || adminProducts.length === 0) {
      root.innerHTML = '<p class="text-muted small mb-0">No products yet.</p>';
      return;
    }

    root.innerHTML = '<div class="table-responsive"><table class="table table-sm align-middle mb-0">' +
      '<thead><tr>' +
        '<th>Product</th><th>Seller</th><th>Price</th><th>Stock</th><th>Status</th>' +
        '<th class="text-end">Actions</th>' +
      '</tr></thead><tbody>' +
      adminProducts.map(product =>
        '<tr>' +
          '<td><div class="fw-semibold small">' + escapeHtml(product.name) + '</div>' +
            '<div class="text-muted small">' + escapeHtml(product.categoryName || '') + '</div></td>' +
          '<td class="small">' + escapeHtml(product.sellerName || '-') + '</td>' +
          '<td class="small">' + formatINR(product.price) + '</td>' +
          '<td class="small">' +
            (Number(product.stock) <= 5
              ? '<span class="badge bg-warning text-dark">' + product.stock + '</span>'
              : '<span class="badge bg-success-subtle text-success">' + product.stock + '</span>') +
          '</td>' +
          '<td>' + (product.active
            ? '<span class="badge bg-primary-subtle text-primary">Active</span>'
            : '<span class="badge bg-secondary">Hidden</span>') + '</td>' +
          '<td class="text-end text-nowrap">' +
            '<button class="btn btn-sm btn-outline-primary me-1" type="button" ' +
              'data-product-action="edit" data-id="' + escapeHtml(product.id) + '">Edit</button>' +
            '<button class="btn btn-sm btn-outline-secondary me-1" type="button" ' +
              'data-product-action="stock" data-id="' + escapeHtml(product.id) + '">Stock</button>' +
            '<button class="btn btn-sm btn-outline-danger" type="button" ' +
              'data-product-action="delete" data-id="' + escapeHtml(product.id) + '">' +
              '<i class="bi bi-trash"></i></button>' +
          '</td>' +
        '</tr>'
      ).join('') +
      '</tbody></table></div>';

    root.querySelectorAll('[data-product-action]').forEach(button => {
      button.addEventListener('click', () => {
        const id = Number(button.dataset.id);
        const product = adminProducts.find(entry => entry.id === id);
        if (!product) return;

        const action = button.dataset.productAction;
        if (action === 'edit') openProductForm(product);
        if (action === 'stock') correctStock(product);
        if (action === 'delete') deleteProduct(product);
      });
    });
  } catch (err) {
    root.innerHTML = '<p class="text-muted small mb-0">Could not load products.</p>';
    handleApiError(err);
  }
}

function correctStock(product) {
  const value = prompt('New stock quantity for "' + product.name + '" (current: ' + product.stock + '):',
    String(product.stock));
  if (value === null) return;

  const quantity = Number(value);
  if (!Number.isInteger(quantity) || quantity < 0) {
    showToast('Enter a valid whole number (0 or more)', 'error');
    return;
  }

  api('/admin/products/' + encodeURIComponent(product.id) + '/stock', {
    method: 'PUT',
    body: { stock: quantity }
  }).then(() => {
    showToast('Stock updated to ' + quantity, 'success');
    loadAdminProducts();
  }).catch(err => handleApiError(err));
}

async function deleteProduct(product) {
  if (!confirm('Delete "' + product.name + '"? This removes it from the catalogue.')) return;

  try {
    await api('/admin/products/' + encodeURIComponent(product.id), { method: 'DELETE' });
    showToast('Product deleted', 'info');
    await loadAdminProducts();
    await loadAdminStats();
  } catch (err) {
    handleApiError(err);
  }
}

/* ------------------------- product form ------------------------- */

function openProductForm(product) {
  const panel = document.getElementById('productFormPanel');
  const form = document.getElementById('productForm');
  clearFieldErrors(form);

  document.getElementById('productFormTitle').innerHTML = product
    ? '<i class="bi bi-box-seam me-2 text-primary"></i>Edit product'
    : '<i class="bi bi-box-seam me-2 text-primary"></i>Add product';
  document.getElementById('productId').value = product ? product.id : '';
  document.getElementById('pTitle').value = product ? (product.name || '') : '';
  document.getElementById('pSku').value = product ? (product.sku || '') : '';
  document.getElementById('pBrand').value = product ? (product.brand || '') : '';
  document.getElementById('pCategory').value = product ? (product.categoryId || '') : '';
  document.getElementById('pSeller').value = product ? (product.sellerId || '') : '';
  document.getElementById('pPrice').value = product ? (product.price || '') : '';
  document.getElementById('pMrp').value = product && product.mrp ? product.mrp : '';
  document.getElementById('pStock').value = product ? (product.stock || 0) : '';
  document.getElementById('pDiscount').value = product && product.discountPercent
    ? product.discountPercent : '';
  document.getElementById('pImage').value = product ? (product.image || '') : '';
  document.getElementById('pDescription').value = product ? (product.description || '') : '';
  document.getElementById('pActive').checked = product ? !!product.active : true;

  panel.classList.remove('d-none');
  panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function hideProductForm() {
  document.getElementById('productFormPanel').classList.add('d-none');
}

async function saveProduct(event) {
  event.preventDefault();
  const form = event.currentTarget;
  clearFieldErrors(form);

  const id = document.getElementById('productId').value;
  const payload = {
    name: document.getElementById('pTitle').value.trim(),
    sku: document.getElementById('pSku').value.trim(),
    brand: document.getElementById('pBrand').value.trim(),
    categoryId: Number(document.getElementById('pCategory').value),
    sellerId: Number(document.getElementById('pSeller').value),
    price: Number(document.getElementById('pPrice').value),
    mrp: document.getElementById('pMrp').value ? Number(document.getElementById('pMrp').value) : null,
    stock: Number(document.getElementById('pStock').value),
    discountPercent: document.getElementById('pDiscount').value
      ? Number(document.getElementById('pDiscount').value) : 0,
    image: document.getElementById('pImage').value.trim(),
    description: document.getElementById('pDescription').value.trim(),
    active: document.getElementById('pActive').checked
  };

  let valid = true;
  const fail = (field, message) => { setFieldError(form, field, message); valid = false; };
  if (payload.name.length < 3) fail('name', 'Enter a product name (3+ characters)');
  if (!payload.categoryId) fail('categoryId', 'Select a category');
  if (!payload.sellerId) fail('sellerId', 'Select the seller who owns this product');
  if (!(payload.price > 0)) fail('price', 'Enter a price above 0');
  if (!Number.isInteger(payload.stock) || payload.stock < 0) fail('stock', 'Enter a valid stock quantity');
  if (payload.description.length < 10) fail('description', 'Describe the product (10+ characters)');
  if (!valid) return;

  const button = document.getElementById('saveProductBtn');
  setLoading(button, true, 'Saving…');

  try {
    if (id) {
      await api('/admin/products/' + encodeURIComponent(id), { method: 'PUT', body: payload });
      showToast('Product updated', 'success');
    } else {
      await api('/admin/products', { method: 'POST', body: payload });
      showToast('Product created', 'success');
    }
    hideProductForm();
    await Promise.all([loadAdminProducts(), loadAdminStats()]);
  } catch (err) {
    applyServerErrors(form, err.errors);
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}
