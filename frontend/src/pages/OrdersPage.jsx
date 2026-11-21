import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { cancelOrder, listOrders } from '../api/orders.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { formatDate, formatMoney } from '../utils/format.js';
import { payForOrder } from '../utils/checkout.js';
import Pagination from '../components/Pagination.jsx';
import Spinner from '../components/Spinner.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import EmptyState from '../components/EmptyState.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

const CANCELLABLE = new Set(['PENDING_PAYMENT', 'PAYMENT_FAILED']);

export default function OrdersPage() {
  const { user } = useAuth();
  const { removeItems } = useCart();
  const { push } = useToast();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busyOrderId, setBusyOrderId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await listOrders({ page, size: 8 });
      setData(response);
    } catch (exception) {
      setError(exception);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const handleCancel = async (order) => {
    if (!window.confirm(`Cancel order #${order.id}? This cannot be undone.`)) {
      return;
    }

    setBusyOrderId(order.id);

    try {
      await cancelOrder(order.id);
      push(`Order #${order.id} cancelled`, 'success');
      await load();
    } catch (exception) {
      setError(exception);
    } finally {
      setBusyOrderId(null);
    }
  };

  const handlePay = async (order) => {
    setError(null);
    setBusyOrderId(order.id);

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
      setBusyOrderId(null);
    }
  };

  return (
    <div className="page">
      <div className="page-heading">
        <h1>Your orders</h1>
        <span className="muted">Track payments and cancel unpaid orders</span>
      </div>

      <ErrorAlert error={error} title="Order action failed" />

      {loading ? (
        <Spinner label="Loading orders…" />
      ) : data?.content?.length ? (
        <>
          <div className="order-list">
            {data.content.map((order) => (
              <article key={order.id} className="order-card">
                <header className="order-card__header">
                  <div>
                    <h2>Order #{order.id}</h2>
                    <span className="muted">{formatDate(order.createdAt)}</span>
                  </div>
                  <StatusBadge status={order.status} />
                </header>

                <ul className="order-card__items">
                  {order.items.map((item) => (
                    <li key={item.productId}>
                      <span>
                        {item.productName} × {item.quantity}
                      </span>
                      <span>{formatMoney(item.subtotal)}</span>
                    </li>
                  ))}
                </ul>

                <footer className="order-card__footer">
                  <strong>{formatMoney(order.totalAmount)}</strong>
                  <div className="order-card__actions">
                    <Link to={`/orders/${order.id}`} className="btn btn--ghost btn--sm">
                      Details
                    </Link>
                    {order.status === 'PENDING_PAYMENT' && (
                      <button
                        type="button"
                        className="btn btn--primary btn--sm"
                        disabled={busyOrderId === order.id}
                        onClick={() => handlePay(order)}
                      >
                        Pay now
                      </button>
                    )}
                    {CANCELLABLE.has(order.status) && (
                      <button
                        type="button"
                        className="btn btn--danger btn--sm"
                        disabled={busyOrderId === order.id}
                        onClick={() => handleCancel(order)}
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </footer>
              </article>
            ))}
          </div>

          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      ) : (
        !error && (
          <EmptyState
            title="No orders yet"
            message="When you place an order it will appear here."
            actionLabel="Go shopping"
            actionTo="/"
          />
        )
      )}
    </div>
  );
}
