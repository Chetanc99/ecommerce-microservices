const CHECKOUT_URL = 'https://checkout.razorpay.com/v1/checkout.js';

export function loadRazorpay() {
  if (window.Razorpay) {
    return Promise.resolve(true);
  }

  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${CHECKOUT_URL}"]`);

    if (existing) {
      existing.addEventListener('load', () => resolve(true));
      existing.addEventListener('error', () => reject(new Error('Could not load Razorpay Checkout')));
      return;
    }

    const script = document.createElement('script');
    script.src = CHECKOUT_URL;
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => reject(new Error('Could not load Razorpay Checkout'));
    document.body.appendChild(script);
  });
}

export async function openRazorpay({
  keyId,
  razorpayOrderId,
  amount,
  currency = 'INR',
  name,
  email,
  onSuccess,
  onDismiss,
  onPaymentFailed
}) {
  await loadRazorpay();

  return new Promise((resolve, reject) => {
    let settled = false;

    const finish = () => {
      if (!settled) {
        settled = true;
        resolve();
      }
    };

    try {
      const checkout = new window.Razorpay({
        key: keyId,
        order_id: razorpayOrderId,
        amount: Math.round(Number(amount) * 100),
        currency,
        name: 'NovaCart',
        description: 'Order payment',
        prefill: { name, email },
        theme: { color: '#4f46e5' },
        handler: async (response) => {
          try {
            await onSuccess?.(response);
          } finally {
            finish();
          }
        },
        modal: {
          ondismiss: () => {
            if (settled) {
              return;
            }
            onDismiss?.();
            finish();
          }
        }
      });

      checkout.on('payment.failed', (response) => {
        if (settled) {
          return;
        }
        onPaymentFailed?.(response?.error?.description || 'Payment failed');
        finish();
      });

      checkout.open();
    } catch (error) {
      reject(error);
    }
  });
}
