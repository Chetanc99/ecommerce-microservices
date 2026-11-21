import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ProductCard from '../components/ProductCard.jsx';
import Pagination from '../components/Pagination.jsx';
import Spinner from '../components/Spinner.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import EmptyState from '../components/EmptyState.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import {
  listProducts,
  listProductsByCategory,
  searchProducts
} from '../api/products.js';

const PAGE_SIZE = 12;

const CATEGORIES = [
  'Electronics',
  'Fashion',
  'Home',
  'Books',
  'Sports',
  'Beauty',
  'Grocery',
  'Other'
];

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
  { value: 'name,asc', label: 'Name: A to Z' }
];

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { addItem } = useCart();
  const { push } = useToast();

  const query = searchParams.get('q') || '';
  const category = searchParams.get('category') || '';
  const sort = searchParams.get('sort') || 'createdAt,desc';
  const parsedPage = Number(searchParams.get('page') || 0);
  const page = Number.isFinite(parsedPage) && parsedPage >= 0 ? Math.floor(parsedPage) : 0;

  const [searchInput, setSearchInput] = useState(query);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setSearchInput(query);
  }, [query]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    const options = { page, size: PAGE_SIZE, sort };

    const load = query
      ? searchProducts(query, options)
      : category
        ? listProductsByCategory(category, options)
        : listProducts(options);

    load
      .then((response) => {
        if (active) {
          setData(response);
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
  }, [query, category, sort, page]);

  const updateParams = (changes) => {
    const next = new URLSearchParams(searchParams);

    Object.entries(changes).forEach(([key, value]) => {
      if (value === '' || value === null || value === undefined) {
        next.delete(key);
      } else {
        next.set(key, value);
      }
    });

    if (!('page' in changes)) {
      next.delete('page');
    }

    setSearchParams(next);
  };

  const handleSearch = (event) => {
    event.preventDefault();
    updateParams({ q: searchInput.trim(), category: '', page: 0 });
  };

  const handleAdd = (product) => {
    addItem(product, 1);
    push(`${product.name} added to cart`, 'success');
  };

  const heading = useMemo(() => {
    if (query) {
      return `Results for “${query}”`;
    }
    if (category) {
      return category;
    }
    return 'All products';
  }, [query, category]);

  return (
    <div className="page">
      <section className="hero">
        <div>
          <span className="eyebrow">Microservices demo store</span>
          <h1>Everyday essentials, engineered for speed.</h1>
          <p className="muted">
            Browse the catalog, add items to your cart and complete a real Razorpay test-mode checkout.
          </p>
        </div>
        <form className="search" onSubmit={handleSearch} role="search">
          <input
            type="search"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search products by name"
            aria-label="Search products"
          />
          <button type="submit" className="btn btn--primary">
            Search
          </button>
        </form>
      </section>

      <section className="catalog">
        <div className="catalog__toolbar">
          <div className="chips" role="group" aria-label="Filter by category">
            <button
              type="button"
              className={!category && !query ? 'chip is-active' : 'chip'}
              onClick={() => updateParams({ category: '', q: '', page: 0 })}
            >
              All
            </button>
            {CATEGORIES.map((item) => (
              <button
                key={item}
                type="button"
                className={category === item ? 'chip is-active' : 'chip'}
                onClick={() => updateParams({ category: item, q: '', page: 0 })}
              >
                {item}
              </button>
            ))}
          </div>

          <label className="sort">
            Sort
            <select value={sort} onChange={(event) => updateParams({ sort: event.target.value, page: 0 })}>
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="catalog__heading">
          <h2>{heading}</h2>
          {data && <span className="muted">{data.totalElements} product(s)</span>}
        </div>

        <ErrorAlert error={error} title="Could not load products" />

        {loading ? (
          <Spinner label="Loading products…" />
        ) : data?.content?.length ? (
          <>
            <div className="product-grid">
              {data.content.map((product) => (
                <ProductCard key={product.id} product={product} onAdd={handleAdd} />
              ))}
            </div>
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              onChange={(nextPage) => updateParams({ page: nextPage })}
            />
          </>
        ) : (
          !error && (
            <EmptyState
              title="No products found"
              message="Try a different search term or category."
              actionLabel="Browse everything"
              actionTo="/"
            />
          )
        )}
      </section>
    </div>
  );
}
