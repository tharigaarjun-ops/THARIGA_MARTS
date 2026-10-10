/* ==================================================================
   Seller Dashboard: store profile, product listing / editing,
   stock correction and sales overview.
   ================================================================== */

const SELLER_FEE_FREE = 499;

let sellerCategories = [];
let sellerProducts = [];

document.addEventListener('DOMContentLoaded', () => {
  initSellerDashboard();
});

async function initSellerDashboard() {
  document.getElementById('sellerProfileForm').addEventListener('submit', saveSellerProfile);
  document.getElementById('listProductBtn').addEventListener('click', () => openProductForm());
  document.getElementById('cancelProductBtn').addEventListener('click', hideProductForm);
  document.getElementById('productForm').addEventListener('submit', saveProduct);
  document.getElementById('refreshProductsBtn').addEventListener('click', loadSellerProducts);

  // Role gate: this page is for sellers only.
  try {
    const profile = await api('/account/profile');
    if (profile.role !== 'ROLE_SELLER') {
      document.getElementById('sellerNotice').classList.remove('d-none');
      document.getElementById('statsRow').classList.add('d-none');
      return;
    }
  } catch (err) {
    document.getElementById('sellerNotice').classList.remove('d-none');
    return;
  }

  await Promise.all([
    loadCategories(),
    loadSellerProfile(),
    loadSellerStats(),
    loadSellerProducts(),
    loadSellerOrders()
  ]);
}

/* ------------------------- categories ------------------------- */

async function loadCategories() {
  try {
    sellerCategories = await api('/categories', { auth: false });
    const select = document.getElementById('pCategory');
    select.innerHTML = '<option value="">Select category…</option>' +
      sellerCategories.map(category =>
        '<option value="' + escapeHtml(category.id) + '">' +
        escapeHtml(category.name) + '</option>').join('');
  } catch (err) {
    sellerCategories = [];
  }
}

/* ------------------------- store profile ------------------------- */

async function loadSellerProfile() {
  try {
    const profile = await api('/seller/profile');

    const statusBox = document.getElementById('profileStatus');
    if (profile && profile.status) {
      const label = profile.status === 'PENDING_APPROVAL' ? 'Pending approval'
          : profile.status.charAt(0) + profile.status.slice(1).toLowerCase();
      const cls = profile.status === 'APPROVED' ? 'bg-success'
          : profile.status === 'REJECTED' ? 'bg-danger' : 'bg-warning';
      statusBox.innerHTML = '<span class="badge ' + cls + '">' + label + '</span>';
    }

    document.getElementById('storeName').value = profile ? (profile.storeName || '') : '';
    document.getElementById('storeDescription').value = profile ? (profile.storeDescription || '') : '';
    document.getElementById('businessAddress').value = profile ? (profile.businessAddress || '') : '';
    document.getElementById('gstin').value = profile ? (profile.gstin || '') : '';
  } catch (err) {
    // no profile yet - blank form is fine
  }
}

async function saveSellerProfile(event) {
  event.preventDefault();
  const form = event.currentTarget;
  clearFieldErrors(form);

  const storeName = document.getElementById('storeName').value.trim();
  if (storeName.length < 2) {
    setFieldError(form, 'storeName', 'Enter your store name');
    return;
  }

  const button = document.getElementById('saveProfileBtn');
  setLoading(button, true, 'Saving…');

  try {
    await api('/seller/profile', {
      method: 'POST',
      body: {
        storeName,
        storeDescription: document.getElementById('storeDescription').value.trim(),
        businessAddress: document.getElementById('businessAddress').value.trim(),
        gstin: document.getElementById('gstin').value.trim()
      }
    });
    showToast('Store profile saved', 'success');
    await loadSellerProfile();
  } catch (err) {
    applyServerErrors(form, err.errors);
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

/* ------------------------- stats ------------------------- */

async function loadSellerStats() {
  try {
    const stats = await api('/seller/stats');
    document.getElementById('statProducts').textContent = stats.productCount;
    document.getElementById('statOrders').textContent = stats.orderCount;
    document.getElementById('statUnits').textContent = stats.unitsSold;
    document.getElementById('statRevenue').textContent = formatINR(stats.revenue || 0);
  } catch (err) {
    document.getElementById('statProducts').textContent = '—';
    document.getElementById('statOrders').textContent = '—';
    document.getElementById('statUnits').textContent = '—';
    document.getElementById('statRevenue').textContent = '—';
  }
}

/* ------------------------- my products ------------------------- */

async function loadSellerProducts() {
  const root = document.getElementById('sellerProducts');

  try {
    sellerProducts = await api('/seller/products');

    if (!Array.isArray(sellerProducts) || sellerProducts.length === 0) {
      root.innerHTML = '<p class="text-muted small mb-0">You have not listed any products yet. ' +
        'Click "List a product" to add your first stationery item.</p>';
      return;
    }

    root.innerHTML = '<div class="table-responsive"><table class="table table-sm align-middle mb-0">' +
      '<thead><tr>' +
        '<th>Product</th><th>Price</th><th>Stock</th><th>Status</th><th class="text-end">Actions</th>' +
      '</tr></thead><tbody>' +
      sellerProducts.map(product =>
        '<tr>' +
          '<td><div class="fw-semibold small">' + escapeHtml(product.name) + '</div>' +
            '<div class="text-muted small">' + escapeHtml(product.categoryName || '') + '</div></td>' +
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
            '<button class="btn btn-sm ' + (product.active ? 'btn-outline-warning' : 'btn-outline-success') +
              '" type="button" data-product-action="toggle" data-id="' + escapeHtml(product.id) + '" ' +
              'title="' + (product.active ? 'Hide from shop' : 'Show in shop') + '">' +
              '<i class="bi ' + (product.active ? 'bi-eye-slash' : 'bi-eye') + '"></i></button>' +
          '</td>' +
        '</tr>'
      ).join('') +
      '</tbody></table></div>';

    root.querySelectorAll('[data-product-action]').forEach(button => {
      button.addEventListener('click', () => {
        const id = Number(button.dataset.id);
        const product = sellerProducts.find(entry => entry.id === id);
        if (!product) return;

        const action = button.dataset.productAction;
        if (action === 'edit') openProductForm(product);
        if (action === 'stock') correctStock(product);
        if (action === 'toggle') toggleProductActive(product);
      });
    });
  } catch (err) {
    root.innerHTML = '<p class="text-muted small mb-0">Could not load your products.</p>';
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

  api('/seller/products/' + encodeURIComponent(product.id) + '/stock', {
    method: 'PUT',
    body: { stock: quantity }
  }).then(() => {
    showToast('Stock updated to ' + quantity, 'success');
    loadSellerProducts();
    loadSellerStats();
  }).catch(err => handleApiError(err));
}

async function toggleProductActive(product) {
  try {
    await api('/seller/products/' + encodeURIComponent(product.id), {
      method: 'PUT',
      body: {
        name: product.name,
        sku: product.sku,
        description: product.description,
        brand: product.brand,
        price: product.price,
        mrp: product.mrp,
        stock: product.stock,
        discountPercent: product.discountPercent,
        image: product.image,
        categoryId: product.categoryId,
        active: !product.active
      }
    });
    showToast(product.active ? 'Product hidden from shop' : 'Product is live again', 'success');
    loadSellerProducts();
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
    : '<i class="bi bi-box-seam me-2 text-primary"></i>List a product';
  document.getElementById('productId').value = product ? product.id : '';
  document.getElementById('pTitle').value = product ? (product.name || '') : '';
  document.getElementById('pSku').value = product ? (product.sku || '') : '';
  document.getElementById('pBrand').value = product ? (product.brand || '') : '';
  document.getElementById('pCategory').value = product ? (product.categoryId || '') : '';
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
  if (!(payload.price > 0)) fail('price', 'Enter a price above 0');
  if (!Number.isInteger(payload.stock) || payload.stock < 0) fail('stock', 'Enter a valid stock quantity');
  if (payload.description.length < 10) fail('description', 'Describe the product (10+ characters)');
  if (!valid) return;

  const button = document.getElementById('saveProductBtn');
  setLoading(button, true, 'Saving…');

  try {
    if (id) {
      await api('/seller/products/' + encodeURIComponent(id), { method: 'PUT', body: payload });
      showToast('Product updated', 'success');
    } else {
      await api('/seller/products', { method: 'POST', body: payload });
      showToast('Product listed for sale', 'success');
    }
    hideProductForm();
    await Promise.all([loadSellerProducts(), loadSellerStats()]);
  } catch (err) {
    applyServerErrors(form, err.errors);
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

/* ------------------------- orders containing my items ------------------------- */

async function loadSellerOrders() {
  const root = document.getElementById('sellerOrders');

  try {
    const orders = await api('/seller/orders');

    if (!Array.isArray(orders) || orders.length === 0) {
      root.innerHTML = '<p class="text-muted small mb-0">No orders for your products yet.</p>';
      return;
    }

    root.innerHTML = orders.map(order =>
      '<div class="border rounded p-3 mb-2">' +
        '<div class="d-flex justify-content-between align-items-start">' +
          '<div>' +
            '<div class="fw-semibold small">' + escapeHtml(order.orderNumber) + '</div>' +
            '<div class="text-muted small">' +
              formatDate(order.placedAt) + ' · ' + escapeHtml(order.customerName || '') + '</div>' +
          '</div>' +
          '<span class="status-pill status-' + escapeHtml(order.status) + '">' +
            escapeHtml(order.status) + '</span>' +
        '</div>' +
        '<div class="small text-muted mt-2">' +
          (order.items || []).map(item =>
            escapeHtml(item.productTitle) + ' × ' + item.quantity).join(', ') +
        '</div>' +
        '<div class="small mt-1">Your sale: <b>' + formatINR(order.totalAmount) + '</b></div>' +
      '</div>'
    ).join('');
  } catch (err) {
    root.innerHTML = '<p class="text-muted small mb-0">Could not load orders.</p>';
    handleApiError(err);
  }
}
