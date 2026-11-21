import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getOrder } from '../api/orders.js';
import { getPaymentForOrder } from '../api/payments.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { formatMoney } from '../utils/format.js';
import { payForOrder } from '../utils/checkout.js';
import StatusBadge from '../components/StatusBadge.jsx';
import Spinner from '../components/Spinner.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

const TRANSIENT = new Set(['CREATED', 'PENDING']);
const MAX_POLLS = 20;

export default function PaymentStatusPage() {
  const { orderId } = useParams();
  const { user } = useAuth();
  const { removeItems } = useCart();
  const { push } = useToast();

  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [paying, setPaying] = useState(false);
  const attempts = useRef(0);

  const load = useCallback(
    async (silent = false) => {
      if (!silent) {
        setLoading(true);
      }
      setError(null);

      try {
        const response = await getPaymentForOrder(orderId);
        setPayment(response);
      } catch (exception) {
        setError(exception);
      } finally {
        setLoading(false);
      }
    },
    [orderId]
  );

  useEffect(() => {
    attempts.current = 0;
    load();
  }, [load]);

  useEffect(() => {
    if (!payment || !TRANSIENT.has(payment.status) || attempts.current >= MAX_POLLS) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      attempts.current += 1;
      load(true);
    }, 3000);

    return () => window.clearTimeout(timer);
  }, [payment, load]);

  const handlePayNow = async () => {
    setError(null);
    setPaying(true);

    try {
      await payForOrder({
        orderId,
        amount: payment?.amount,
        user,
        onVerified: async () => {
          push('Payment successful.', 'success');
          load();
          const order = await getOrder(orderId).catch(() => null);
          if (order) {
            removeItems(order.items.map((item) => item.productId));
          }
        },
        onDismiss: () => {
          push('Payment was dismissed.', 'warning');
          load(true);
        },
        onPaymentFailed: (reason) => {
          push(reason || 'Payment failed. You can retry.', 'warning');
          load(true);
        },
        onFailure: (exception) => setError(exception)
      });
    } catch (exception) {
      setError(exception);
    } finally {
      setPaying(false);
    }
  };

  const verified = payment?.status === 'SUCCESS';

  return (
    <div className="page narrow">
      <div className="page-heading">
        <h1>Payment status</h1>
        <Link to={`/orders/${orderId}`} className="btn btn--ghost btn--sm">
          View order
        </Link>
      </div>

      {loading ? (
        <Spinner label="Loading payment status…" />
      ) : error ? (
        <>
          <ErrorAlert error={error} title="Payment status unavailable" />
          <p className="muted">
            If you just placed the order, it is safe to check your <Link to="/orders">order history</Link>.
          </p>
        </>
      ) : payment ? (
        <section className={`payment-card ${verified ? 'payment-card--success' : ''}`}>
          <div className="payment-card__hero">
            <span className="payment-card__icon" aria-hidden="true">
              {verified ? '✓' : payment.status === 'FAILED' ? '!' : '⏳'}
            </span>
            <div>
              <StatusBadge status={payment.status} />
              <h2>{formatMoney(payment.amount)}</h2>
              <p className="muted">
                {verified
                  ? 'Your payment was verified and the order is confirmed.'
                  : TRANSIENT.has(payment.status)
                    ? 'Waiting for Razorpay to confirm this payment. This page refreshes automatically.'
                    : 'This payment was not completed.'}
              </p>
            </div>
          </div>

          <dl className="meta-list">
            <div>
              <dt>Order</dt>
              <dd>#{payment.orderId}</dd>
            </div>
            <div>
              <dt>Payment record</dt>
              <dd>#{payment.paymentId}</dd>
            </div>
            {payment.razorpayPaymentId && (
              <div>
                <dt>Razorpay payment</dt>
                <dd className="mono">{payment.razorpayPaymentId}</dd>
              </div>
            )}
          </dl>

          {TRANSIENT.has(payment.status) && (
            <div className="payment-card__actions">
              <button type="button" className="btn btn--primary" onClick={handlePayNow} disabled={paying}>
                {paying ? 'Opening payment…' : 'Pay now'}
              </button>
              <button type="button" className="btn btn--ghost" onClick={() => load(true)}>
                Refresh status
              </button>
            </div>
          )}

          <div className="payment-card__actions">
            <Link to="/orders" className="btn btn--ghost">
              Go to order history
            </Link>
          </div>
        </section>
      ) : (
        <ErrorAlert
          error={{ message: 'No payment exists for this order yet.' }}
          title="No payment found"
        />
      )}
    </div>
  );
}
