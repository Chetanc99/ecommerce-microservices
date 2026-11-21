import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getProduct } from '../api/products.js';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { formatMoney } from '../utils/format.js';
import QuantityStepper from '../components/QuantityStepper.jsx';
import Spinner from '../components/Spinner.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function ProductDetailPage() {
  const { productId } = useParams();
  const { addItem } = useCart();
  const { push } = useToast();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    getProduct(productId)
      .then((response) => {
        if (active) {
          setProduct(response);
        }
      })
      .catch((exception) => {
        if (active) {
          setError(exception);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [productId]);

  if (loading) {
    return <Spinner label="Loading product…" />;
  }

  if (error || !product) {
    return (
      <div className="page narrow">
        <ErrorAlert error={error} title="Product unavailable" />
        <Link to="/" className="btn btn--ghost">
          ← Back to catalog
        </Link>
      </div>
    );
  }

  const outOfStock = (product.quantity ?? 0) <= 0;

  const handleAdd = () => {
    addItem(product, quantity);
    push(`${quantity} × ${product.name} added to cart`, 'success');
  };

  return (
    <div className="page">
      <Link to="/" className="back-link">
        ← Back to catalog
      </Link>

      <div className="product-detail">
        <div className="product-detail__media">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} />
          ) : (
            <span aria-hidden="true">{product.name?.charAt(0)?.toUpperCase() || 'N'}</span>
          )}
        </div>

        <div className="product-detail__info">
          <span className="eyebrow">{product.category}</span>
          <h1>{product.name}</h1>
          <p className="product-detail__price">{formatMoney(product.price)}</p>
          <p className={outOfStock ? 'stock stock--out' : 'stock'}>
            {outOfStock ? 'Out of stock' : `${product.quantity} unit(s) available`}
          </p>
          <p className="product-detail__description">
            {product.description || 'No description has been provided for this product yet.'}
          </p>

          <div className="product-detail__actions">
            <QuantityStepper
              value={quantity}
              max={Math.max(1, product.quantity ?? 1)}
              onChange={setQuantity}
              disabled={outOfStock}
            />
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleAdd}
              disabled={outOfStock}
            >
              {outOfStock ? 'Unavailable' : 'Add to cart'}
            </button>
          </div>

          <dl className="meta-list">
            <div>
              <dt>Product ID</dt>
              <dd>#{product.id}</dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{product.category}</dd>
            </div>
          </dl>
        </div>
      </div>
    </div>
  );
}
