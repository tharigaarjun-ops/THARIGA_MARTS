/* ==================================================================
   Product details page: full info, quantity picker, add to cart,
   buy now and related products.
   ================================================================== */

let currentProduct = null;
let selectedQuantity = 1;

document.addEventListener('DOMContentLoaded', () => {
  bindCardActions(document.getElementById('relatedGrid'));
  loadProductDetails();
});

async function loadProductDetails() {
  const container = document.getElementById('productContainer');
  const id = queryParam('id');

  if (!id) {
    container.innerHTML = emptyStateHtml('bi-question-circle', 'No product selected',
      'Pick a stationery from the shop to see its details.',
      '<a class="btn btn-primary-am" href="/products.html">Browse stationery</a>');
    return;
  }

  try {
    const response = await api('/products/' + encodeURIComponent(id), { auth: false });
    currentProduct = response.data;
    selectedQuantity = 1;
    renderProduct(currentProduct);
    document.title = currentProduct.name + ' | THARIGA MART';
    loadRelated(currentProduct);
  } catch (err) {
    if (err.status === 404) {
      container.innerHTML = emptyStateHtml('bi-emoji-frown', 'Product not found',
        'This stationery may have been removed from the catalogue.',
        '<a class="btn btn-primary-am" href="/products.html">Browse other stationery</a>');
    } else {
      container.innerHTML = emptyStateHtml('bi-exclamation-triangle', 'Could not load this product',
        err.message || 'Please try again shortly.',
        '<button class="btn btn-primary-am" onclick="location.reload()">Retry</button>');
    }
  }
}

function renderProduct(product) {
  const container = document.getElementById('productContainer');
  const maxQty = Math.max(1, Math.min(10, product.stock || 1));

  const mrpHtml = product.mrp && Number(product.mrp) > Number(product.price)
    ? '<span class="mrp fs-4 ms-2">' + formatINR(product.mrp) + '</span>'
    : '';
  const discountHtml = product.discountPercent > 0
    ? '<span class="badge bg-success ms-2 align-middle">' + product.discountPercent + '% OFF</span>'
    : '';

  const stockHtml = !product.inStock
    ? '<span class="badge text-bg-danger">Out of stock</span>'
    : product.stock <= 5
      ? '<span class="badge text-bg-warning">Only ' + product.stock + ' left - order soon!</span>'
      : '<span class="badge text-bg-success">In stock · ' + product.stock + ' units</span>';

  const wishlistHtml = isLoggedIn()
    ? '<button class="btn btn-outline-secondary px-3" id="wishlistBtn" type="button">' +
      '<i class="bi bi-heart" id="wishlistIcon"></i><span id="wishlistLabel">Wishlist</span></button>'
    : '';

  const actionsHtml = product.inStock
    ? '<div class="d-flex flex-wrap gap-3 align-items-center mt-4">' +
        '<div class="qty-box">' +
          '<button type="button" id="qtyMinus" aria-label="Decrease quantity">&minus;</button>' +
          '<input id="qtyInput" type="text" inputmode="numeric" value="1" readonly aria-label="Quantity">' +
          '<button type="button" id="qtyPlus" aria-label="Increase quantity">+</button>' +
        '</div>' +
        '<button class="btn btn-outline-primary px-4" id="addToCartBtn" type="button">' +
          '<i class="bi bi-cart-plus me-1"></i>Add to Cart</button>' +
        '<button class="btn btn-primary-am px-4" id="buyNowBtn" type="button">' +
          '<i class="bi bi-lightning-charge-fill me-1"></i>Buy Now</button>' +
        wishlistHtml +
      '</div>'
    : '<div class="alert alert-danger mt-4 mb-0"><i class="bi bi-x-octagon me-2"></i>' +
      'This product is currently out of stock.</div>' +
      (isLoggedIn()
        ? '<div class="mt-3">' + wishlistHtml + '</div>'
        : '');

  container.innerHTML =
    '<div class="breadcrumb-am mb-3">' +
      '<a href="/index.html">Home</a> <span class="mx-1">/</span> ' +
      '<a href="/products.html">Shop</a> <span class="mx-1">/</span> ' +
      '<a href="/products.html?category=' + escapeHtml(product.categorySlug) + '">' +
        escapeHtml(product.categoryName) + '</a> <span class="mx-1">/</span> ' +
      '<span>' + escapeHtml(product.name) + '</span>' +
    '</div>' +

    '<div class="row g-4">' +
      '<div class="col-md-6">' +
        '<div class="detail-gallery">' +
          '<img src="' + escapeHtml(product.image) + '" alt="' + escapeHtml(product.name) + '">' +
        '</div>' +
        '<div class="row g-2 mt-1">' +
          '<div class="col-4"><div class="trust-card py-3"><div class="t-icon"><i class="bi bi-truck"></i></div>' +
            '<h6 class="small">Free delivery</h6><p class="small mb-0">On orders above ₹499</p></div></div>' +
          '<div class="col-4"><div class="trust-card py-3"><div class="t-icon"><i class="bi bi-arrow-counterclockwise"></i></div>' +
            '<h6 class="small">7-day returns</h6><p class="small mb-0">Easy return policy</p></div></div>' +
          '<div class="col-4"><div class="trust-card py-3"><div class="t-icon"><i class="bi bi-shield-check"></i></div>' +
            '<h6 class="small">Brand warranty</h6><p class="small mb-0">100% genuine items</p></div></div>' +
        '</div>' +
      '</div>' +

      '<div class="col-md-6">' +
        '<div class="detail-panel">' +
          '<div class="product-brand">' + escapeHtml(product.brand) + ' · ' + escapeHtml(product.categoryName) + '</div>' +
          '<h3 class="mt-1">' + escapeHtml(product.name) + '</h3>' +
          '<div class="mb-2">' + stockHtml + '</div>' +
          '<div class="price-row my-3">' +
            '<span class="detail-price">' + formatINR(product.price) + '</span>' + mrpHtml + discountHtml +
          '</div>' +
          '<p class="text-muted mb-3">' + escapeHtml(product.description) + '</p>' +

          '<table class="table table-borderless spec-table small mb-0">' +
            '<tbody>' +
              '<tr><th>Brand</th><td>' + escapeHtml(product.brand) + '</td></tr>' +
              '<tr><th>Category</th><td>' + escapeHtml(product.categoryName) + '</td></tr>' +
              '<tr><th>Availability</th><td>' + (product.inStock ? product.stock + ' units in stock' : 'Out of stock') + '</td></tr>' +
              '<tr><th>Item code</th><td>AM-' + String(product.id).padStart(5, '0') + '</td></tr>' +
              '<tr><th>Delivery</th><td>2-4 business days</td></tr>' +
            '</tbody>' +
          '</table>' +

          actionsHtml +
        '</div>' +
      '</div>' +

      '<div class="col-12 mt-4">' +
        '<div class="panel">' +
          '<h5><i class="bi bi-chat-left-text me-2 text-primary"></i>Customer reviews</h5>' +
          '<div id="reviewList" class="mb-3"></div>' +
          (isLoggedIn()
            ? '<form id="reviewForm" novalidate>' +
                '<div class="alert alert-danger alert-msg d-none" id="reviewAlert"></div>' +
                '<div class="row g-3">' +
                  '<div class="col-md-3">' +
                    '<label class="form-label" for="reviewRating">Rating *</label>' +
                    '<select class="form-select" id="reviewRating" name="rating">' +
                      '<option value="5">★★★★★ - Excellent</option>' +
                      '<option value="4">★★★★☆ - Very good</option>' +
                      '<option value="3">★★★☆☆ - Good</option>' +
                      '<option value="2">★★☆☆☆ - Fair</option>' +
                      '<option value="1">★☆☆☆☆ - Poor</option>' +
                    '</select>' +
                  '</div>' +
                  '<div class="col-md-9">' +
                    '<label class="form-label" for="reviewTitle">Title</label>' +
                    '<input class="form-control" id="reviewTitle" name="title" type="text" ' +
                      'maxlength="80" placeholder="Summarize your experience">' +
                  '</div>' +
                  '<div class="col-12">' +
                    '<label class="form-label" for="reviewComment">Your review *</label>' +
                    '<textarea class="form-control" id="reviewComment" name="comment" rows="3" ' +
                      'maxlength="1000" placeholder="How was the product quality, packaging, delivery?"></textarea>' +
                    '<div class="field-error" data-error-for="comment"></div>' +
                  '</div>' +
                '</div>' +
                '<button class="btn btn-primary-am mt-3" type="submit" id="postReviewBtn">' +
                  '<i class="bi bi-send me-1"></i>Post review</button>' +
              '</form>'
            : '<p class="text-muted small mb-0"><a href="/login.html">Log in</a> to write a review.</p>') +
        '</div>' +
      '</div>' +
    '</div>';

  if (product.inStock) {
    bindProductActions(maxQty);
  }
  bindWishlistButton(product.id);
  loadReviews(product.id);
}

function bindWishlistButton(productId) {
  const button = document.getElementById('wishlistBtn');
  if (!button) return;

  button.addEventListener('click', async () => {
    try {
      const response = await api('/wishlist/' + encodeURIComponent(productId), { method: 'POST' });
      const inWishlist = response.data && response.data.inWishlist;
      const icon = document.getElementById('wishlistIcon');
      const label = document.getElementById('wishlistLabel');
      if (icon) {
        icon.classList.toggle('bi-heart', !inWishlist);
        icon.classList.toggle('bi-heart-fill', inWishlist);
        icon.classList.toggle('text-danger', inWishlist);
      }
      if (label) label.textContent = inWishlist ? 'In wishlist' : 'Wishlist';
      showToast(inWishlist ? 'Added to wishlist' : 'Removed from wishlist', 'success');
    } catch (err) {
      handleApiError(err);
    }
  });
}

async function loadReviews(productId) {
  const list = document.getElementById('reviewList');
  if (!list) return;

  try {
    const reviews = await api('/products/' + encodeURIComponent(productId) + '/reviews', { auth: false });
    if (!Array.isArray(reviews) || reviews.length === 0) {
      list.innerHTML = '<p class="text-muted small mb-0">No reviews yet - be the first to share your experience.</p>';
      return;
    }
    list.innerHTML = reviews.map(review =>
      '<div class="border-bottom py-3">' +
        '<div class="d-flex justify-content-between align-items-center">' +
          '<div class="fw-semibold small">' + escapeHtml(review.userName || 'Customer') +
            (review.verifiedPurchase ? ' <span class="badge bg-success-subtle text-success ms-1">Verified purchase</span>' : '') +
          '</div>' +
          '<div class="text-warning small">' + '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating) + '</div>' +
        '</div>' +
        '<div class="small fw-semibold">' + escapeHtml(review.title) + '</div>' +
        '<div class="small text-muted">' + escapeHtml(review.comment) + '</div>' +
        '<div class="small text-muted">' + formatDate(review.createdAt) + '</div>' +
      '</div>'
    ).join('');
  } catch (err) {
    list.innerHTML = '<p class="text-muted small mb-0">Reviews could not be loaded.</p>';
  }
}

async function postReview(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const alertBox = document.getElementById('reviewAlert');
  clearFieldErrors(form);
  alertBox.classList.add('d-none');

  const comment = form.querySelector('[name="comment"]').value.trim();
  if (comment.length < 5) {
    setFieldError(form, 'comment', 'Please write at least 5 characters');
    return;
  }

  const button = document.getElementById('postReviewBtn');
  setLoading(button, true, 'Posting…');

  try {
    await api('/products/' + encodeURIComponent(currentProduct.id) + '/reviews', {
      method: 'POST',
      body: {
        rating: Number(form.querySelector('[name="rating"]').value),
        title: form.querySelector('[name="title"]').value.trim(),
        comment: comment
      }
    });
    showToast('Review posted - thank you!', 'success');
    form.reset();
    loadReviews(currentProduct.id);
  } catch (err) {
    alertBox.textContent = err.message || 'Could not post the review';
    alertBox.classList.remove('d-none');
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const reviewForm = document.getElementById('reviewForm');
  if (reviewForm) reviewForm.addEventListener('submit', postReview);
});

function bindProductActions(maxQty) {
  const input = document.getElementById('qtyInput');

  document.getElementById('qtyMinus').addEventListener('click', () => {
    selectedQuantity = Math.max(1, selectedQuantity - 1);
    input.value = selectedQuantity;
  });

  document.getElementById('qtyPlus').addEventListener('click', () => {
    selectedQuantity = Math.min(maxQty, selectedQuantity + 1);
    input.value = selectedQuantity;
    if (selectedQuantity === maxQty && maxQty < 10) {
      showToast('Only ' + maxQty + ' unit(s) available', 'warning');
    }
  });

  document.getElementById('addToCartBtn').addEventListener('click', async () => {
    const added = await addToCart(currentProduct.id, selectedQuantity);
    if (added) showToast(selectedQuantity + ' × ' + currentProduct.name + ' added to cart', 'success');
  });

  document.getElementById('buyNowBtn').addEventListener('click', async () => {
    const added = await addToCart(currentProduct.id, selectedQuantity);
    if (added) location.href = '/checkout.html';
  });
}

async function loadRelated(product) {
  const section = document.getElementById('relatedSection');
  const grid = document.getElementById('relatedGrid');

  try {
    const related = await api('/products/' + encodeURIComponent(product.id) + '/related', { auth: false });
    if (!Array.isArray(related) || related.length === 0) return;
    grid.innerHTML = related.map(productCardHtml).join('');
    section.classList.remove('d-none');
  } catch (err) {
    // related products are optional
  }
}
