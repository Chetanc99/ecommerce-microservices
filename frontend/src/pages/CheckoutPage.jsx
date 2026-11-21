import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { createOrder } from '../api/orders.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { formatMoney } from '../utils/format.js';
import { payForOrder } from '../utils/checkout.js';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function CheckoutPage() {
  const { items, subtotal, clearCart } = useCart();
  const { user } = useAuth();
  const { push } = useToast();
  const navigate = useNavigate();

  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState(null);
  const [createdOrder, setCreatedOrder] = useState(null);

  if (items.length === 0 && !createdOrder) {
    return <Navigate to="/cart" replace />;
  }

  const handlePlaceOrder = async () => {
    setError(null);
    setPlacing(true);

    let order = createdOrder;

    try {
      if (!order) {
        order = await createOrder(items);
        setCreatedOrder(order);
      }

      await payForOrder({
        orderId: order.id,
        amount: order.totalAmount,
        user,
        onVerified: () => {
          clearCart();
          push('Payment successful. Your order is confirmed.', 'success');
          navigate(`/orders/${order.id}`, { replace: true });
        },
        onDismiss: () => {
          push('Payment was dismissed. Your order is awaiting payment.', 'warning');
          navigate(`/orders/${order.id}`);
        },
        onPaymentFailed: (reason) => {
          setError({ message: reason || 'Payment failed. You can retry safely.' });
        },
        onFailure: (exception) => setError(exception)
      });
    } catch (exception) {
      setError(exception);
    } finally {
      setPlacing(false);
    }
  };

  return (
    <div className="page">
      <div className="page-heading">
        <h1>Checkout</h1>
        <span className="muted">Razorpay test mode</span>
      </div>

      <div className="checkout-layout">
        <section className="panel">
          <h2>Review your items</h2>

          <ul className="checkout-items">
            {(createdOrder?.items || items).map((item) => {
              const productId = item.productId;
              const name = item.productName || item.name;
              const price = item.price;
              const quantity = item.quantity;

              return (
                <li key={productId}>
                  <div>
                    <strong>{name}</strong>
                    <span className="muted"> × {quantity}</span>
                  </div>
                  <span>{formatMoney(price * quantity)}</span>
                </li>
              );
            })}
          </ul>

          <ErrorAlert error={error} title="Checkout could not be completed" />

          {createdOrder && !error && (
            <div className="alert alert--info">
              <strong>Order #{createdOrder.id} created</strong>
              <p>Complete the Razorpay checkout to confirm it.</p>
            </div>
          )}

          {error?.status === 503 && createdOrder && (
            <div className="alert alert--warning">
              <strong>Payment is not configured on the server</strong>
              <p>
                Order #{createdOrder.id} is saved as awaiting payment. Ask the administrator to set the
                Razorpay test credentials, then retry from your order page.
              </p>
              <Link to={`/orders/${createdOrder.id}`} className="btn btn--ghost btn--sm">
                View order
              </Link>
            </div>
          )}

          <div className="checkout-note">
            This is a demo storefront. No shipping address is collected and no real money is charged.
          </div>
        </section>

        <aside className="summary">
          <h2>Payment summary</h2>
          <div className="summary__row">
            <span>Items</span>
            <span>{items.reduce((total, item) => total + item.quantity, 0)}</span>
          </div>
          <div className="summary__row">
            <span>Subtotal</span>
            <span>{formatMoney(createdOrder?.totalAmount ?? subtotal)}</span>
          </div>
          <div className="summary__row">
            <span>Shipping</span>
            <span>Free</span>
          </div>
          <div className="summary__row summary__row--total">
            <span>Total</span>
            <strong>{formatMoney(createdOrder?.totalAmount ?? subtotal)}</strong>
          </div>

          <button
            type="button"
            className="btn btn--primary btn--block"
            onClick={handlePlaceOrder}
            disabled={placing}
          >
            {placing ? 'Opening payment…' : createdOrder ? 'Retry payment' : 'Place order and pay'}
          </button>

          <Link to="/cart" className="btn btn--ghost btn--block">
            Back to cart
          </Link>
        </aside>
      </div>
    </div>
  );
}
