/* ==================================================================
   My Account: profile view/edit, password change, logout.
   ================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  if (!requireAuth()) return;

  // password visibility toggles (same helper used by auth pages)
  document.querySelectorAll('.toggle-pass').forEach(button => {
    button.addEventListener('click', () => {
      const target = document.getElementById(button.dataset.target);
      if (!target) return;
      const showing = target.type === 'text';
      target.type = showing ? 'password' : 'text';
      button.classList.toggle('bi-eye', showing);
      button.classList.toggle('bi-eye-slash', !showing);
    });
  });

  document.getElementById('accountLogoutBtn').addEventListener('click', () => {
    clearSession();
    setFlash('success', 'You have been logged out successfully');
    location.href = '/index.html';
  });

  document.getElementById('profileForm').addEventListener('submit', saveProfile);
  document.getElementById('passwordForm').addEventListener('submit', changePassword);

  document.getElementById('addAddressBtn').addEventListener('click', () => openAddressForm());
  document.getElementById('cancelAddressBtn').addEventListener('click', hideAddressForm);
  document.getElementById('addressForm').addEventListener('submit', saveAddress);

  loadProfile();
  loadAddresses();
});

async function loadProfile() {
  try {
    const profile = await api('/account/profile');
    setSession(getToken(), profile);
    if (typeof refreshAuthUI === 'function') refreshAuthUI(profile);

    document.getElementById('profileName').textContent = profile.fullName;
    document.getElementById('profileEmail').textContent = profile.email;
    document.getElementById('profileRole').textContent = profile.role;
    document.getElementById('profileAvatar').textContent =
      (profile.fullName || '?').trim().charAt(0).toUpperCase();
    document.getElementById('profileSince').textContent = profile.createdAt
      ? new Date(profile.createdAt).toLocaleDateString('en-IN', {
          day: '2-digit', month: 'short', year: 'numeric'
        })
      : '—';

    document.getElementById('accFullName').value = profile.fullName || '';
    document.getElementById('accPhone').value = profile.phone || '';
    document.getElementById('accEmail').value = profile.email || '';

    try {
      const orders = await api('/orders');
      document.getElementById('profileOrders').textContent =
        Array.isArray(orders) ? String(orders.length) : '0';
    } catch (e) {
      document.getElementById('profileOrders').textContent = '—';
    }
  } catch (err) {
    handleApiError(err);
  }
}

async function saveProfile(event) {
  event.preventDefault();
  const form = event.currentTarget;
  clearFieldErrors(form);

  const fullName = form.querySelector('[name="fullName"]').value.trim();
  const phone = form.querySelector('[name="phone"]').value.trim();

  let valid = true;
  if (fullName.length < 2) {
    setFieldError(form, 'fullName', 'Enter your full name');
    valid = false;
  }
  if (phone && !/^[6-9][0-9]{9}$/.test(phone)) {
    setFieldError(form, 'phone', 'Enter a valid 10-digit mobile number');
    valid = false;
  }
  if (!valid) return;

  const button = document.getElementById('saveProfileBtn');
  setLoading(button, true, 'Saving…');

  try {
    const updated = await api('/account/profile', {
      method: 'PUT',
      body: { fullName, phone }
    });
    setSession(getToken(), updated);
    if (typeof refreshAuthUI === 'function') refreshAuthUI(updated);
    document.getElementById('profileName').textContent = updated.fullName;
    showToast('Profile updated successfully', 'success');
  } catch (err) {
    applyServerErrors(form, err.errors);
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

async function changePassword(event) {
  event.preventDefault();
  const form = event.currentTarget;
  clearFieldErrors(form);

  const currentPassword = form.querySelector('[name="currentPassword"]').value;
  const newPassword = form.querySelector('[name="newPassword"]').value;
  const confirmPassword = form.querySelector('[name="confirmPassword"]').value;

  let valid = true;
  if (!currentPassword) {
    setFieldError(form, 'currentPassword', 'Enter your current password');
    valid = false;
  }
  if (newPassword.length < 8 || !/[A-Za-z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
    setFieldError(form, 'newPassword', 'Use 8+ characters with a letter and a number');
    valid = false;
  }
  if (confirmPassword !== newPassword) {
    setFieldError(form, 'confirmPassword', 'Passwords do not match');
    valid = false;
  }
  if (!valid) return;

  const button = document.getElementById('changePasswordBtn');
  setLoading(button, true, 'Updating…');

  try {
    const response = await api('/account/password', {
      method: 'PUT',
      body: { currentPassword, newPassword }
    });
    form.reset();
    showToast(response.message || 'Password changed successfully', 'success');
  } catch (err) {
    applyServerErrors(form, err.errors);
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

/* ------------------------- address book ------------------------- */

async function loadAddresses() {
  const list = document.getElementById('addressList');
  if (!list) return;

  try {
    const addresses = await api('/account/addresses');
    renderAddresses(addresses);
  } catch (err) {
    list.innerHTML = '<p class="text-muted small mb-0">Could not load saved addresses.</p>';
  }
}

function renderAddresses(addresses) {
  const list = document.getElementById('addressList');

  if (!Array.isArray(addresses) || addresses.length === 0) {
    list.innerHTML = '<p class="text-muted small mb-0">No saved addresses yet. ' +
      'Add one to check out faster.</p>';
    return;
  }

  list.innerHTML = addresses.map(address =>
    '<div class="border rounded p-3 mb-2">' +
      '<div class="d-flex justify-content-between align-items-start">' +
        '<div>' +
          '<div class="fw-semibold small">' +
            escapeHtml(address.fullName) +
            (address.isDefault ? ' <span class="badge bg-primary-subtle text-primary ms-1">Default</span>' : '') +
            ' <span class="text-muted small">(' + escapeHtml(address.addressType || 'HOME').toLowerCase() + ')</span>' +
          '</div>' +
          '<div class="small text-muted">' +
            escapeHtml(address.streetAddress) +
            (address.landmark ? ', ' + escapeHtml(address.landmark) : '') + '<br>' +
            escapeHtml(address.city) + ', ' + escapeHtml(address.state) +
            ' - <b>' + escapeHtml(address.postalCode) + '</b><br>' +
            '<i class="bi bi-telephone me-1"></i>' + escapeHtml(address.phone) +
          '</div>' +
        '</div>' +
        '<div class="d-flex flex-column gap-1">' +
          (address.isDefault ? '' :
            '<button class="btn btn-sm btn-outline-primary" type="button" data-address-action="default" ' +
              'data-id="' + escapeHtml(address.id) + '">Set default</button>') +
          '<button class="btn btn-sm btn-outline-secondary" type="button" data-address-action="edit" ' +
            'data-id="' + escapeHtml(address.id) + '">Edit</button>' +
          '<button class="btn btn-sm btn-outline-danger" type="button" data-address-action="delete" ' +
            'data-id="' + escapeHtml(address.id) + '">Delete</button>' +
        '</div>' +
      '</div>' +
    '</div>'
  ).join('');

  list.querySelectorAll('[data-address-action]').forEach(button => {
    button.addEventListener('click', async () => {
      const id = Number(button.dataset.id);
      const action = button.dataset.addressAction;
      if (action === 'edit') openAddressForm(addresses.find(entry => entry.id === id));
      if (action === 'default') await setDefaultAddress(id);
      if (action === 'delete') await deleteAddress(id);
    });
  });
}

function openAddressForm(address) {
  const panel = document.getElementById('addressFormPanel');
  document.getElementById('addressFormTitle').innerHTML = address
    ? '<i class="bi bi-geo-alt me-2 text-primary"></i>Edit address'
    : '<i class="bi bi-geo-alt me-2 text-primary"></i>Add address';
  document.getElementById('addressId').value = address ? address.id : '';
  document.getElementById('addrFullName').value = address ? address.fullName : '';
  document.getElementById('addrPhone').value = address ? address.phone : '';
  document.getElementById('addrLine1').value = address ? address.streetAddress : '';
  document.getElementById('addrLandmark').value = address ? (address.landmark || '') : '';
  document.getElementById('addrCity').value = address ? address.city : '';
  document.getElementById('addrState').value = address ? address.state : '';
  document.getElementById('addrPincode').value = address ? address.postalCode : '';
  document.getElementById('addrType').value = address ? (address.addressType || 'HOME') : 'HOME';
  document.getElementById('addrCountry').value = address ? (address.country || 'India') : 'India';
  document.getElementById('addrDefault').checked = address ? !!address.isDefault : false;
  panel.classList.remove('d-none');
  panel.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function hideAddressForm() {
  document.getElementById('addressFormPanel').classList.add('d-none');
}

async function saveAddress(event) {
  event.preventDefault();
  const form = event.currentTarget;
  clearFieldErrors(form);

  const id = document.getElementById('addressId').value;
  const payload = {
    fullName: form.querySelector('[name="fullName"]').value.trim(),
    phone: form.querySelector('[name="phone"]').value.trim(),
    streetAddress: form.querySelector('[name="streetAddress"]').value.trim(),
    landmark: form.querySelector('[name="landmark"]').value.trim(),
    city: form.querySelector('[name="city"]').value.trim(),
    state: form.querySelector('[name="state"]').value.trim(),
    postalCode: form.querySelector('[name="postalCode"]').value.trim(),
    addressType: form.querySelector('[name="addressType"]').value,
    country: form.querySelector('[name="country"]').value.trim() || 'India',
    isDefault: form.querySelector('[name="isDefault"]').checked
  };

  let valid = true;
  const fail = (field, message) => { setFieldError(form, field, message); valid = false; };
  if (payload.fullName.length < 2) fail('fullName', 'Enter the receiver name');
  if (!/^[6-9][0-9]{9}$/.test(payload.phone)) fail('phone', 'Enter a valid 10-digit mobile number');
  if (payload.streetAddress.length < 4) fail('streetAddress', 'Enter your full address');
  if (!payload.city) fail('city', 'City is required');
  if (!payload.state) fail('state', 'State is required');
  if (!/^[1-9][0-9]{5}$/.test(payload.postalCode)) fail('postalCode', 'Enter a valid 6-digit pincode');
  if (!valid) return;

  const button = document.getElementById('saveAddressBtn');
  setLoading(button, true, 'Saving…');

  try {
    if (id) {
      await api('/account/addresses/' + encodeURIComponent(id), { method: 'PUT', body: payload });
      showToast('Address updated', 'success');
    } else {
      await api('/account/addresses', { method: 'POST', body: payload });
      showToast('Address saved', 'success');
    }
    hideAddressForm();
    form.reset();
    document.getElementById('addrCountry').value = 'India';
    await loadAddresses();
  } catch (err) {
    applyServerErrors(form, err.errors);
    handleApiError(err);
  } finally {
    setLoading(button, false);
  }
}

async function setDefaultAddress(id) {
  try {
    const addresses = await api('/account/addresses');
    const target = addresses.find(entry => entry.id === id);
    if (!target) return;
    target.isDefault = true;
    await api('/account/addresses/' + encodeURIComponent(id), { method: 'PUT', body: target });
    showToast('Default address updated', 'success');
    await loadAddresses();
  } catch (err) {
    handleApiError(err);
  }
}

async function deleteAddress(id) {
  if (!confirm('Delete this saved address?')) return;
  try {
    await api('/account/addresses/' + encodeURIComponent(id), { method: 'DELETE' });
    showToast('Address deleted', 'info');
    await loadAddresses();
  } catch (err) {
    handleApiError(err);
  }
}
