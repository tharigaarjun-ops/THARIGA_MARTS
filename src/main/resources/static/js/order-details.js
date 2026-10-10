/* ==================================================================
   Order details page: status timeline, items, address, totals,
   payment record and buyer cancellation.
   ================================================================== */

const STATUS_STEPS = ['ORDER_PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED'];
const STEP_ICONS = {
  ORDER_PLACED: 'bi-bag-check',
  CONFIRMED: 'bi-patch-check',
  SHIPPED: 'bi-truck',
  DELIVERED: 'bi-house-check'
};
const CANCELLABLE = ['ORDER_PLACED', 'CONFIRMED'];

document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth()) return;
  loadOrderDetails();
});

async function loadOrderDetails() {
  const root = document.getElementById('orderRoot');
  const id = queryParam('id');

  if (!id) {
    root.innerHTML = emptyStateHtml('bi-question-circle', 'No order selected',
      'Pick an order from your history to see its details.',
      '<a class="btn btn-primary-am" href="/orders.html">View my orders</a>');
    return;
  }

  try {
    const response = await api('/orders/' + encodeURIComponent(id));
    renderOrder(response.data);
    document.title = 'Order ' + response.data.orderNumber + ' | THARIGA MART';
  } catch (err) {
    if (err.status === 404 || err.status === 403) {
      root.innerHTML = emptyStateHtml('bi-shield-x', 'Order not available',
        err.status === 403
          ? 'You can only view your own orders.'
          : 'This order could not be found.',
        '<a class="btn btn-primary-am" href="/orders.html">Back to my orders</a>');
    } else {
      root.innerHTML = emptyStateHtml('bi-exclamation-triangle', 'Could not load this order',
        err.message || 'Please try again shortly.',
        '<button class="btn btn-primary-am" onclick="location.reload()">Retry</button>');
    }
    handleApiError(err);
  }
}

function statusLabel(status) {
  if (!status) return 'Unknown';
  return status === 'ORDER_PLACED' ? 'Order Placed'
      : status.charAt(0) + status.slice(1).toLowerCase();
}

function paymentLabel(method) {
  if (method === 'COD') return 'Cash on Delivery';
  if (method === 'UPI') return 'UPI';
  if (method === 'UPI_QR') return 'UPI (QR)';
  return method || '';
}

function paymentStatusLabel(status) {
  if (!status) return '';
  return status === 'PENDING' ? 'Pending'
      : status.charAt(0) + status.slice(1).toLowerCase();
}

function renderOrder(order) {
  const root = document.getElementById('orderRoot');
  const items = order.items || [];
  const subtotal = Number(order.totalAmount) || 0;
  const discount = Number(order.discountAmount) || 0;
  const shipping = Number(order.shippingAmount) || 0;
  const total = Number(order.finalAmount) || 0;

  const currentStepIndex = STATUS_STEPS.indexOf(order.status);
  const cancelled = order.status === 'CANCELLED';

  const timelineHtml = cancelled
    ? '<div class="alert alert-danger mb-0"><i class="bi bi-x-octagon me-2"></i>' +
      'This order was cancelled.' +
      (order.cancellationReason
        ? ' Reason: ' + escapeHtml(order.cancellationReason)
        : '') +
      '</div>'
    : '<div class="timeline">' + STATUS_STEPS.map((step, index) =>
        '<div class="tl-step' + (index <= currentStepIndex ? ' done' : '') + '">' +
          '<div class="tl-dot"><i class="bi ' + STEP_ICONS[step] + '"></i></div>' +
          escapeHtml(statusLabel(step)) +
        '</div>'
      ).join('') + '</div>';

  const itemsHtml = items.map(item => {
    return '<div class="item-row">' +
      '<div class="item-thumb">' +
        (item.productId
          ? '<a href="/product-details.html?id=' + encodeURIComponent(item.productId) + '">' +
            '<img src="' + escapeHtml(item.productImageUrl || '/images/stationery/stationery.svg') +
            '" alt="' + escapeHtml(item.productTitle) + '"></a>'
          : '<img src="' + escapeHtml(item.productImageUrl || '/images/stationery/stationery.svg') +
            '" alt="' + escapeHtml(item.productTitle) + '">') +
      '</div>' +
      '<div class="flex-grow-1">' +
        '<div class="fw-semibold">' + escapeHtml(item.productTitle) + '</div>' +
        '<div class="small text-muted">' + formatINR(item.unitPrice) + ' × ' + item.quantity +
          (item.sellerName ? ' · Seller: ' + escapeHtml(item.sellerName) : '') + '</div>' +
      '</div>' +
      '<div class="fw-bold">' + formatINR(item.subtotal) + '</div>' +
    '</div>';
  }).join('');

  const discountRow = discount > 0
    ? '<div class="summary-row text-success"><span>Coupon discount' +
      (order.couponCode ? ' (' + escapeHtml(order.couponCode) + ')' : '') +
      '</span><span>-' + formatINR(discount) + '</span></div>'
    : '';

  const paymentInfo = order.payment
    ? '<div class="d-flex justify-content-between small mb-1">' +
        '<span class="text-muted">Transaction ID</span>' +
        '<span class="text-truncate ms-3" style="max-width: 60%">' +
          escapeHtml(order.payment.transactionId || '-') + '</span>' +
      '</div>' +
      '<div class="d-flex justify-content-between small mb-1">' +
        '<span class="text-muted">Payment status</span>' +
        '<span class="status-pill status-' + escapeHtml(order.payment.status || '') + '">' +
          escapeHtml(paymentStatusLabel(order.payment.status)) + '</span>' +
      '</div>'
    : '';

  const cancelButton = (!cancelled && CANCELLABLE.includes(order.status))
    ? '<button class="btn btn-outline-danger btn-sm" id="cancelOrderBtn" type="button">' +
      '<i class="bi bi-x-circle me-1"></i>Cancel order</button>'
    : '';

  root.innerHTML =
    '<div class="order-card mb-4">' +
      '<div class="order-head">' +
        '<div>' +
          '<div class="order-number fs-5"><i class="bi bi-bag-check me-2 text-primary"></i>' +
            escapeHtml(order.orderNumber) + '</div>' +
          '<div class="order-meta">Placed on ' + formatDate(order.placedAt) +
            ' · Payment: ' + paymentLabel(order.paymentMethod) + '</div>' +
        '</div>' +
        '<span class="status-pill status-' + escapeHtml(order.status) + '">' +
          escapeHtml(statusLabel(order.status)) + '</span>' +
      '</div>' +
      timelineHtml +
    '</div>' +

    '<div class="row g-4">' +
      '<div class="col-lg-7">' +
        '<div class="panel">' +
          '<h5><i class="bi bi-box-seam me-2 text-primary"></i>Items (' + items.length + ')</h5>' +
          itemsHtml +
          '<div class="summary-row mt-3"><span>Subtotal</span><span>' + formatINR(subtotal) + '</span></div>' +
          discountRow +
          '<div class="summary-row"><span>Delivery</span>' +
            (shipping === 0 ? '<span class="free">FREE</span>' : '<span>' + formatINR(shipping) + '</span>') +
          '</div>' +
          '<div class="summary-row total"><span>Total payable</span><span>' + formatINR(total) + '</span></div>' +
        '</div>' +
      '</div>' +

      '<div class="col-lg-5">' +
        '<div class="panel mb-3">' +
          '<h5><i class="bi bi-geo-alt me-2 text-primary"></i>Delivery Address</h5>' +
          '<p class="mb-1 fw-semibold">' + escapeHtml(order.shipName) + '</p>' +
          '<p class="mb-1 text-muted small">' +
            escapeHtml(order.shipLine1) +
            (order.shipLine2 ? ', ' + escapeHtml(order.shipLine2) : '') + '<br>' +
            escapeHtml(order.shipCity) + ', ' + escapeHtml(order.shipState) +
            ' - <b>' + escapeHtml(order.shipPincode) + '</b></p>' +
          '<p class="mb-0 text-muted small"><i class="bi bi-telephone me-1"></i>' +
            escapeHtml(order.shipPhone) + '</p>' +
        '</div>' +

        '<div class="panel mb-3">' +
          '<h5><i class="bi bi-credit-card me-2 text-primary"></i>Payment</h5>' +
          '<div class="d-flex justify-content-between small mb-1">' +
            '<span class="text-muted">Method</span>' +
            '<span>' + paymentLabel(order.paymentMethod) + '</span>' +
          '</div>' +
          '<div class="d-flex justify-content-between small mb-1">' +
            '<span class="text-muted">Amount</span>' +
            '<span class="fw-semibold">' + formatINR(total) + '</span>' +
          '</div>' +
          paymentInfo +
        '</div>' +

        '<div class="panel">' +
          '<h5><i class="bi bi-question-circle me-2 text-primary"></i>Need help?</h5>' +
          (cancelButton
            ? '<p class="small text-muted mb-2">Need to cancel? You can cancel while the order is ' +
              'still being prepared.</p>' +
              '<div class="d-grid gap-2 mb-3">' + cancelButton + '</div>'
            : '') +
          '<div class="d-grid gap-2">' +
            '<a class="btn btn-outline-primary btn-sm" href="/orders.html">' +
              '<i class="bi bi-arrow-left me-1"></i>All my orders</a>' +
            '<a class="btn btn-primary-am btn-sm" href="/products.html">' +
              '<i class="bi bi-bag me-1"></i>Continue shopping</a>' +
          '</div>' +
        '</div>' +
      '</div>' +
    '</div>';

  const cancelBtn = document.getElementById('cancelOrderBtn');
  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => cancelOrder(order.id));
  }
}

async function cancelOrder(orderId) {
  const reason = prompt('Reason for cancellation (optional):', '');
  if (reason === null) return; // dismissed

  const button = document.getElementById('cancelOrderBtn');
  if (button) setLoading(button, true, 'Cancelling…');

  try {
    await api('/orders/' + encodeURIComponent(orderId) + '/cancel', {
      method: 'POST',
      body: reason.trim() ? { reason: reason.trim() } : {}
    });
    showToast('Order cancelled successfully', 'success');
    loadOrderDetails();
  } catch (err) {
    if (button) setLoading(button, false);
    handleApiError(err);
  }
}
