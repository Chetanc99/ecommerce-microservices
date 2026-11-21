export function formatMoney(amount, currency = 'INR') {
  const value = Number(amount ?? 0);

  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2
    }).format(value);
  } catch {
    return `${currency} ${value.toFixed(2)}`;
  }
}

export function formatDate(value) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? '—'
    : date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

const STATUS_LABELS = {
  PENDING_PAYMENT: 'Awaiting payment',
  CONFIRMED: 'Confirmed',
  PAYMENT_FAILED: 'Payment failed',
  CANCELLED: 'Cancelled',
  CREATED: 'Payment created',
  PENDING: 'Payment pending',
  SUCCESS: 'Paid',
  FAILED: 'Failed'
};

export function friendlyStatus(status) {
  return STATUS_LABELS[status] || status || 'Unknown';
}

export function statusTone(status) {
  switch (status) {
    case 'CONFIRMED':
    case 'SUCCESS':
      return 'success';
    case 'PAYMENT_FAILED':
    case 'FAILED':
      return 'danger';
    case 'CANCELLED':
      return 'muted';
    case 'PENDING_PAYMENT':
    case 'CREATED':
    case 'PENDING':
      return 'warning';
    default:
      return 'info';
  }
}
