import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { formatMoney } from '../utils/format.js';
import QuantityStepper from '../components/QuantityStepper.jsx';
import EmptyState from '../components/EmptyState.jsx';

export default function CartPage() {
  const { items, subtotal, setQuantity, removeItem, totalItems } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  if (items.length === 0) {
    return (
      <div className="page narrow">
        <EmptyState
          title="Your cart is empty"
          message="Add a few products and they will show up here."
          actionLabel="Start shopping"
          actionTo="/"
        />
      </div>
    );
  }

  const handleCheckout = () => {
    navigate(isAuthenticated ? '/checkout' : '/login', {
      state: isAuthenticated ? undefined : { from: '/checkout' }
    });
  };

  return (
    <div className="page">
      <div className="page-heading">
        <h1>Your cart</h1>
        <span className="muted">{totalItems} item(s)</span>
      </div>

      <div className="cart-layout">
        <section className="cart-list" aria-label="Cart items">
          {items.map((item) => (
            <article key={item.productId} className="cart-item">
              <div className="cart-item__media">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} />
                ) : (
                  <span aria-hidden="true">{item.name?.charAt(0)?.toUpperCase() || 'N'}</span>
                )}
              </div>

              <div className="cart-item__info">
                <Link to={`/products/${item.productId}`} className="cart-item__name">
                  {item.name}
                </Link>
                <span className="muted">{formatMoney(item.price)} each</span>
              </div>

              <QuantityStepper
                value={item.quantity}
                max={item.available || 99}
                onChange={(value) => setQuantity(item.productId, value)}
              />

              <strong className="cart-item__total">
                {formatMoney(item.price * item.quantity)}
              </strong>

              <button
                type="button"
                className="link-button"
                onClick={() => removeItem(item.productId)}
              >
                Remove
              </button>
            </article>
          ))}
        </section>

        <aside className="summary">
          <h2>Order summary</h2>
          <div className="summary__row">
            <span>Subtotal</span>
            <span>{formatMoney(subtotal)}</span>
          </div>
          <div className="summary__row">
            <span>Shipping</span>
            <span>Free</span>
          </div>
          <div className="summary__row summary__row--total">
            <span>Total</span>
            <strong>{formatMoney(subtotal)}</strong>
          </div>

          <button type="button" className="btn btn--primary btn--block" onClick={handleCheckout}>
            {isAuthenticated ? 'Proceed to checkout' : 'Sign in to check out'}
          </button>

          <Link to="/" className="btn btn--ghost btn--block">
            Continue shopping
          </Link>
        </aside>
      </div>
    </div>
  );
}
