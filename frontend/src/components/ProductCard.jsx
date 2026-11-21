import { Link } from 'react-router-dom';
import { formatMoney } from '../utils/format.js';

export default function ProductCard({ product, onAdd }) {
  const outOfStock = (product.quantity ?? 0) <= 0;

  return (
    <article className="product-card">
      <Link to={`/products/${product.id}`} className="product-card__media">
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} loading="lazy" />
        ) : (
          <span className="product-card__placeholder" aria-hidden="true">
            {product.name?.charAt(0)?.toUpperCase() || 'N'}
          </span>
        )}
      </Link>

      <div className="product-card__body">
        <span className="eyebrow">{product.category}</span>
        <h3>
          <Link to={`/products/${product.id}`}>{product.name}</Link>
        </h3>
        <p className="product-card__price">{formatMoney(product.price)}</p>
        <p className={`stock ${outOfStock ? 'stock--out' : ''}`}>
          {outOfStock ? 'Out of stock' : `${product.quantity} in stock`}
        </p>

        <button
          type="button"
          className="btn btn--primary btn--block"
          disabled={outOfStock}
          onClick={() => onAdd(product)}
        >
          {outOfStock ? 'Unavailable' : 'Add to cart'}
        </button>
      </div>
    </article>
  );
}
