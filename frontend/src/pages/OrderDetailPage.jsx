import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { cancelOrder, getOrder } from '../api/orders.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { formatDate, formatMoney } from '../utils/format.js';
import { payForOrder } from '../utils/checkout.js';
import Spinner from '../components/Spinner.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const CANCELLABLE = new Set(['PENDING_PAYMENT', 'PAYMENT_FAILED']);

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const { user } = useAuth();
  const { removeItems } = useCart();
  const { push } = useToast();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await getOrder(orderId);
      setOrder(response);
    } catch (exception) {
      setError(exception);
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleCancel = async () => {
    if (!window.confirm(`Cancel order #${order.id}?`)) {
      return;
    }

    setBusy(true);

    try {
      const updated = await cancelOrder(order.id);
      setOrder(updated);
      push('Order cancelled', 'success');
    } catch (exception) {
      setError(exception);
    } finally {
      setBusy(false);
    }
  };

  const handlePay = async () => {
    setError(null);
    setBusy(true);

    try {
      await payForOrder({
        orderId: order.id,
        amount: order.totalAmount,
        user,
        onVerified: () => {
          removeItems((order.items || []).map((item) => item.productId));
          push('Payment successful.', 'success');
          load();
        },
        onDismiss: () => push('Payment dismissed.', 'warning'),
        onPaymentFailed: (reason) => push(reason || 'Payment failed. You can retry.', 'warning'),
        onFailure: (exception) => setError(exception)
      });
    } catch (exception) {
      setError(exception);
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <Spinner label="Loading order…" />;
  }

  if (error || !order) {
    return (
      <div className="page narrow">
        <ErrorAlert error={error} title="Order unavailable" />
        <Link to="/orders" className="btn btn--ghost">
          ← Back to orders
        </Link>
      </div>
    );
  }

  return (
    <div className="page">
      <Link to="/orders" className="back-link">
        ← Back to orders
      </Link>

      <div className="page-heading">
        <div>
          <h1>Order #{order.id}</h1>
          <span className="muted">Placed {formatDate(order.createdAt)}</span>
        </div>
        <StatusBadge status={order.status} />
      </div>

      <ErrorAlert error={error} title="Order action failed" />

      <div className="order-detail">
        <section className="panel">
          <h2>Items</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Unit price</th>
                <th>Qty</th>
                <th>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {order.items.map((item) => (
                <tr key={item.productId}>
                  <td>
                    <Link to={`/products/${item.productId}`}>{item.productName}</Link>
                  </td>
                  <td>{formatMoney(item.price)}</td>
                  <td>{item.quantity}</td>
                  <td>{formatMoney(item.subtotal)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={3}>Total</td>
                <td>{formatMoney(order.totalAmount)}</td>
              </tr>
            </tfoot>
          </table>
        </section>

        <aside className="summary">
          <h2>Actions</h2>
          <div className="summary__row">
            <span>Owner</span>
            <span>You (user #{order.userId})</span>
          </div>
          <div className="summary__row summary__row--total">
            <span>Total</span>
            <strong>{formatMoney(order.totalAmount)}</strong>
          </div>

          {order.status === 'PENDING_PAYMENT' && (
            <>
              <button type="button" className="btn btn--primary btn--block" disabled={busy} onClick={handlePay}>
                {busy ? 'Opening payment…' : 'Pay now'}
              </button>
              <Link to={`/payment/status/${order.id}`} className="btn btn--ghost btn--block">
                Payment status
              </Link>
            </>
          )}

          {order.status === 'CONFIRMED' && (
            <div className="alert alert--success">
              Payment verified. This order is confirmed and cannot be cancelled online.
            </div>
          )}

          {CANCELLABLE.has(order.status) && (
            <button type="button" className="btn btn--danger btn--block" disabled={busy} onClick={handleCancel}>
              Cancel order
            </button>
          )}

          {order.status === 'CANCELLED' && (
            <p className="muted">This order was cancelled. Reserved stock has been released.</p>
          )}
        </aside>
      </div>
    </div>
  );
}
