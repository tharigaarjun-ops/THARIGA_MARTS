/* ==================================================================
   Checkout page: shipping form + saved addresses + coupon + payment
   choice + order placement with server-side stock validation.
   ================================================================== */

const CHECKOUT_DELIVERY_CHARGE = 49;
const CHECKOUT_FREE_DELIVERY_ABOVE = 499;

let checkoutCart = [];
let appliedCoupon = null;

document.addEventListener('DOMContentLoaded', async () => {
  if (!requireAuth()) return;

  const cart = await refreshCart();
  checkoutCart = cartItems();

  if (checkoutCart.length === 0) {
    renderEmptyCheckout();
    return;
  }

  renderCheckout(checkoutCart);
  await prefillFromProfile();
  await prefillDefaultAddress();
});

function renderEmptyCheckout() {
  document.getElementById('checkoutRoot').innerHTML =
    emptyStateHtml('bi-cart-x', 'Nothing to checkout',
      'Your cart is empty. Add some stationery first.',
      '<a class="btn btn-primary-am" href="/products.html">Browse stationery</a>');
}

async function prefillFromProfile() {
  try {
    const profile = await api('/account/profile');
    const nameField = document.getElementById('shipName');
    const phoneField = document.getElementById('shipPhone');
    if (nameField && !nameField.value) nameField.value = profile.fullName || '';
    if (phoneField && !phoneField.value && profile.phone) phoneField.value = profile.phone;
  } catch (err) {
    // profile prefill is optional
  }
}

/** Prefills the form from the buyer's default saved address. */
async function prefillDefaultAddress() {
  try {
    const addresses = await api('/account/addresses');
    const defaultAddress = addresses.find(address => address.isDefault) || addresses[0];
    if (!defaultAddress) return;

    const set = (id, value) => {
      const field = document.getElementById(id);
      if (field && !field.value && value) field.value = value;
    };
    set('shipName', defaultAddress.fullName);
    set('shipPhone', defaultAddress.phone);
    set('shipLine1', defaultAddress.streetAddress);
    set('shipLine2', defaultAddress.landmark);
    set('shipCity', defaultAddress.city);
    set('shipState', defaultAddress.state);
    set('shipPincode', defaultAddress.postalCode);
  } catch (err) {
    // saved addresses are optional
  }
}

function renderCheckout(cart) {
  const root = document.getElementById('checkoutRoot');
  const subtotal = cartSubtotal();
  const delivery = subtotal >= CHECKOUT_FREE_DELIVERY_ABOVE ? 0 : CHECKOUT_DELIVERY_CHARGE;

  const itemsHtml = cart.map(item =>
    '<div class="d-flex justify-content-between gap-2 py-2 border-bottom">' +
      '<div class="d-flex gap-2 align-items-center">' +
        '<div class="item-thumb"><img src="' + escapeHtml(item.image || '/images/stationery/stationery.svg') + '" alt="' + escapeHtml(item.name) + '"></div>' +
        '<div>' +
          '<div class="fw-semibold small">' + escapeHtml(item.name) + '</div>' +
          '<div class="small text-muted">Qty: ' + escapeHtml(item.quantity) + '</div>' +
        '</div>' +
      '</div>' +
      '<div class="fw-semibold small text-nowrap">' + formatINR(item.price * item.quantity) + '</div>' +
    '</div>'
  ).join('');

  root.innerHTML =
    '<div class="row g-4">' +
      '<div class="col-lg-7">' +
        '<form id="checkoutForm" novalidate>' +
          '<div class="alert alert-danger alert-msg d-none" id="checkoutAlert"></div>' +

          '<div class="panel mb-3">' +
            '<h5><i class="bi bi-geo-alt me-2 text-primary"></i>Delivery Address</h5>' +
            '<div class="row g-3">' +
              '<div class="col-md-6">' +
                '<label class="form-label" for="shipName">Receiver name *</label>' +
                '<input class="form-control" id="shipName" name="name" type="text" placeholder="Full name" autocomplete="name">' +
                '<div class="field-error" data-error-for="name"></div>' +
              '</div>' +
              '<div class="col-md-6">' +
                '<label class="form-label" for="shipPhone">Mobile number *</label>' +
                '<input class="form-control" id="shipPhone" name="phone" type="tel" maxlength="10" placeholder="10-digit mobile" autocomplete="tel">' +
                '<div class="field-error" data-error-for="phone"></div>' +
              '</div>' +
              '<div class="col-12">' +
                '<label class="form-label" for="shipLine1">Address line (house no, street) *</label>' +
                '<input class="form-control" id="shipLine1" name="line1" type="text" placeholder="e.g. 42, Galaxy Apartments, MG Road" autocomplete="street-address">' +
                '<div class="field-error" data-error-for="line1"></div>' +
              '</div>' +
              '<div class="col-12">' +
                '<label class="form-label" for="shipLine2">Landmark <span class="text-muted">(optional)</span></label>' +
                '<input class="form-control" id="shipLine2" name="line2" type="text" placeholder="Nearby landmark">' +
                '<div class="field-error" data-error-for="line2"></div>' +
              '</div>' +
              '<div class="col-md-4">' +
                '<label class="form-label" for="shipCity">City *</label>' +
                '<input class="form-control" id="shipCity" name="city" type="text" placeholder="City" autocomplete="address-level2">' +
                '<div class="field-error" data-error-for="city"></div>' +
              '</div>' +
              '<div class="col-md-4">' +
                '<label class="form-label" for="shipState">State *</label>' +
                '<input class="form-control" id="shipState" name="state" type="text" placeholder="State" autocomplete="address-level1">' +
                '<div class="field-error" data-error-for="state"></div>' +
              '</div>' +
              '<div class="col-md-4">' +
                '<label class="form-label" for="shipPincode">Pincode *</label>' +
                '<input class="form-control" id="shipPincode" name="pincode" type="text" maxlength="6" placeholder="6-digit pincode" autocomplete="postal-code">' +
                '<div class="field-error" data-error-for="pincode"></div>' +
              '</div>' +
            '</div>' +
          '</div>' +

          '<div class="panel">' +
            '<h5><i class="bi bi-credit-card me-2 text-primary"></i>Payment Method</h5>' +
            '<div class="d-grid gap-2">' +
              '<label class="pay-option active" data-pay-option>' +
                '<input class="form-check-input mt-1" type="radio" name="payment" value="COD" checked>' +
                '<span><b>Cash on Delivery</b><span>Pay when your stationery arrives at your door</span></span>' +
              '</label>' +
              '<label class="pay-option" data-pay-option>' +
                '<input class="form-check-input mt-1" type="radio" name="payment" value="UPI">' +
                '<span><b>UPI (demo)</b><span>GPay / PhonePe / Paytm - simulated payment, no real charge</span></span>' +
              '</label>' +
            '</div>' +
            '<div class="field-error" data-error-for="payment"></div>' +
          '</div>' +
        '</form>' +
      '</div>' +

      '<div class="col-lg-5">' +
        '<div class="panel">' +
          '<h5><i class="bi bi-bag me-2 text-primary"></i>Order Details</h5>' +
          itemsHtml +
          '<div class="summary-row mt-3"><span>Subtotal</span><span id="sumSubtotal">' + formatINR(subtotal) + '</span></div>' +
          '<div class="summary-row" id="discountRow" style="display:none"><span>Coupon discount <span class="text-muted" id="couponLabel"></span></span><span class="text-success" id="sumDiscount"></span></div>' +
          '<div class="summary-row"><span>Delivery</span><span id="sumDelivery">' +
            (delivery === 0 ? '<span class="free">FREE</span>' : formatINR(delivery)) +
          '</span></div>' +
          '<div class="summary-row total"><span>Payable</span><span id="sumTotal">' + formatINR(subtotal + delivery) + '</span></div>' +

          '<div class="input-group mt-3">' +
            '<input class="form-control" id="couponCode" type="text" placeholder="Coupon code (e.g. KAWAII10)" ' +
              'aria-label="Coupon code" style="text-transform:uppercase">' +
            '<button class="btn btn-outline-primary" id="applyCouponBtn" type="button">Apply</button>' +
          '</div>' +
          '<div class="form-text" id="couponHint">Try KAWAII10 (10% off) or THARIGA20 (20% off orders above ₹499)</div>' +
          '<div class="field-error" data-error-for="coupon"></div>' +

          '<div class="d-grid mt-3">' +
            '<button class="btn btn-primary-am btn-lg" id="placeOrderBtn" type="button">' +
              '<i class="bi bi-bag-check-fill me-1"></i>Place Order</button>' +
          '</div>' +
          '<div class="text-center mt-2">' +
            '<a class="link-quiet" href="/cart.html"><i class="bi bi-arrow-left me-1"></i>Edit cart</a>' +
          '</div>' +
        '</div>' +
      '</div>' +
    '</div>';

  // payment option highlight
  root.querySelectorAll('[data-pay-option]').forEach(option => {
    option.addEventListener('click', () => {
      root.querySelectorAll('[data-pay-option]').forEach(o => o.classList.remove('active'));
      option.classList.add('active');
      option.querySelector('input').checked = true;
    });
  });

  document.getElementById('applyCouponBtn').addEventListener('click', applyCouponCode);
  document.getElementById('couponCode').addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      event.preventDefault();
      applyCouponCode();
    }
  });

  document.getElementById('placeOrderBtn').addEventListener('click', placeOrder);
}

function checkoutTotals() {
  const subtotal = cartSubtotal();
  const discount = appliedCoupon && appliedCoupon.valid
    ? Number(appliedCoupon.discountAmount || 0) : 0;
  const delivery = (subtotal - discount) >= CHECKOUT_FREE_DELIVERY_ABOVE ? 0 : CHECKOUT_DELIVERY_CHARGE;
  return { subtotal, discount, delivery, total: subtotal - discount + delivery };
}

function updateSummary() {
  const totals = checkoutTotals();
  const subTotalEl = document.getElementById('sumSubtotal');
  const deliveryEl = document.getElementById('sumDelivery');
  const totalEl = document.getElementById('sumTotal');
  const discountRow = document.getElementById('discountRow');
  const discountEl = document.getElementById('sumDiscount');
  const couponLabel = document.getElementById('couponLabel');

  if (subTotalEl) subTotalEl.textContent = formatINR(totals.subtotal);
  if (deliveryEl) deliveryEl.innerHTML = totals.delivery === 0
    ? '<span class="free">FREE</span>'
    : formatINR(totals.delivery);
  if (totalEl) totalEl.textContent = formatINR(totals.total);
  if (discountRow) discountRow.style.display = totals.discount > 0 ? '' : 'none';
  if (discountEl && totals.discount > 0) discountEl.textContent = '-' + formatINR(totals.discount);
  if (couponLabel && appliedCoupon && appliedCoupon.valid) {
    couponLabel.textContent = '(' + appliedCoupon.code + ')';
  }
}

async function applyCouponCode() {
  const input = document.getElementById('couponCode');
  const code = (input.value || '').trim();
  if (!code) {
    appliedCoupon = null;
    updateSummary();
    return;
  }

  const button = document.getElementById('applyCouponBtn');
  setLoading(button, true, 'Checking…');

  try {
    const result = await api('/coupons/validate?code='
      + encodeURIComponent(code) + '&subtotal=' + cartSubtotal().toFixed(2),
      { auth: false });

    if (result.valid) {
      appliedCoupon = result;
      showToast(result.message, 'success');
    } else {
      appliedCoupon = null;
      showToast(result.message || 'Coupon code is not valid', 'warning');
    }
    updateSummary();
  } catch (err) {
    appliedCoupon = null;
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

function validateCheckoutForm(form) {
  clearFieldErrors(form);

  const value = name => (form.querySelector('[name="' + name + '"]') || { value: '' }).value.trim();
  const data = {
    name: value('name'),
    phone: value('phone'),
    line1: value('line1'),
    line2: value('line2'),
    city: value('city'),
    state: value('state'),
    pincode: value('pincode'),
    payment: (form.querySelector('[name="payment"]:checked') || {}).value || ''
  };

  let valid = true;
  const fail = (field, message) => { setFieldError(form, field, message); valid = false; };

  if (data.name.length < 2) fail('name', 'Receiver name is required');
  if (!/^[6-9][0-9]{9}$/.test(data.phone)) fail('phone', 'Enter a valid 10-digit mobile number');
  if (data.line1.length < 4) fail('line1', 'Enter your full address');
  if (!data.city) fail('city', 'City is required');
  if (!data.state) fail('state', 'State is required');
  if (!/^[1-9][0-9]{5}$/.test(data.pincode)) fail('pincode', 'Enter a valid 6-digit pincode');
  if (!data.payment) fail('payment', 'Choose a payment method');

  if (!valid) {
    const firstError = form.querySelector('.field-error:not(:empty)');
    if (firstError) firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  return valid ? data : null;
}

async function placeOrder() {
  const form = document.getElementById('checkoutForm');
  const alertBox = document.getElementById('checkoutAlert');
  alertBox.classList.add('d-none');

  const shipping = validateCheckoutForm(form);
  if (!shipping) {
    showToast('Please correct the highlighted fields', 'error');
    return;
  }

  const cart = cartItems();
  if (cart.length === 0) {
    renderEmptyCheckout();
    return;
  }

  const button = document.getElementById('placeOrderBtn');
  setLoading(button, true, 'Placing order…');

  try {
    const response = await api('/orders', {
      method: 'POST',
      body: {
        paymentMethod: shipping.payment,
        couponCode: appliedCoupon && appliedCoupon.valid ? appliedCoupon.code : null,
        shipping: {
          name: shipping.name,
          phone: shipping.phone,
          line1: shipping.line1,
          line2: shipping.line2,
          city: shipping.city,
          state: shipping.state,
          pincode: shipping.pincode
        },
        items: cart.map(item => ({
          productId: item.productId,
          quantity: item.quantity
        }))
      }
    });

    const order = response.data;
    await clearCart();
    setFlash('success', 'Order ' + order.orderNumber + ' placed successfully!');
    location.href = '/order-details.html?id=' + encodeURIComponent(order.id);
  } catch (err) {
    setLoading(button, false);

    if (err.status === 409) {
      alertBox.textContent = err.message;
      alertBox.classList.remove('d-none');
      alertBox.scrollIntoView({ behavior: 'smooth', block: 'center' });
      showToast('Stock updated - adjust quantities in your cart', 'warning');
      await refreshCart();
      renderCartStockNotice();
    } else if (err.status === 401) {
      handleApiError(err);
    } else {
      applyServerErrors(form, err.errors);
      alertBox.textContent = err.message || 'Could not place the order. Please try again.';
      alertBox.classList.remove('d-none');
      handleApiError(err);
    }
  }
}

/** After a stock conflict, re-render the summary with fresh stock numbers. */
async function renderCartStockNotice() {
  checkoutCart = cartItems();
  updateSummary();
}
