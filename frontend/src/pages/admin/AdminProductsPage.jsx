import { useCallback, useEffect, useState } from 'react';
import {
  createProduct,
  deleteProduct,
  listProducts,
  searchProducts,
  updateProduct
} from '../../api/products.js';
import { useToast } from '../../context/ToastContext.jsx';
import { formatMoney } from '../../utils/format.js';
import Pagination from '../../components/Pagination.jsx';
import Spinner from '../../components/Spinner.jsx';
import ErrorAlert from '../../components/ErrorAlert.jsx';
import EmptyState from '../../components/EmptyState.jsx';

const EMPTY_FORM = {
  name: '',
  description: '',
  price: '',
  quantity: '',
  category: '',
  imageUrl: ''
};

export default function AdminProductsPage() {
  const { push } = useToast();

  const [page, setPage] = useState(0);
  const [query, setQuery] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = query
        ? await searchProducts(query, { page, size: 10 })
        : await listProducts({ page, size: 10 });
      setData(response);
    } catch (exception) {
      setError(exception);
    } finally {
      setLoading(false);
    }
  }, [page, query]);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
    setError(null);
  };

  const openEdit = (product) => {
    setEditingId(product.id);
    setForm({
      name: product.name,
      description: product.description || '',
      price: String(product.price),
      quantity: String(product.quantity),
      category: product.category,
      imageUrl: product.imageUrl || ''
    });
    setFormOpen(true);
    setError(null);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError(null);

    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      quantity: Number(form.quantity),
      category: form.category.trim(),
      imageUrl: form.imageUrl.trim() || null
    };

    try {
      if (editingId) {
        await updateProduct(editingId, payload);
        push('Product updated', 'success');
      } else {
        await createProduct(payload);
        push('Product created', 'success');
      }

      closeForm();
      await load();
    } catch (exception) {
      setError(exception);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (product) => {
    if (!window.confirm(`Delete “${product.name}”?`)) {
      return;
    }

    try {
      await deleteProduct(product.id);
      push('Product deleted', 'success');
      await load();
    } catch (exception) {
      setError(exception);
    }
  };

  const handleSearch = (event) => {
    event.preventDefault();
    setPage(0);
    setQuery(searchInput.trim());
  };

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <h1>Product administration</h1>
          <span className="muted">Create, update and remove catalog items (ADMIN only)</span>
        </div>
        <button type="button" className="btn btn--primary" onClick={openCreate}>
          + New product
        </button>
      </div>

      <ErrorAlert error={error} title="Product operation failed" />

      {formOpen && (
        <section className="panel">
          <div className="panel__heading">
            <h2>{editingId ? `Edit product #${editingId}` : 'Create product'}</h2>
            <button type="button" className="link-button" onClick={closeForm}>
              Close
            </button>
          </div>

          <form onSubmit={handleSubmit} className="form form--grid">
            <label>
              Name
              <input
                required
                maxLength={255}
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
              />
            </label>
            <label>
              Category
              <input
                required
                maxLength={100}
                value={form.category}
                onChange={(event) => setForm({ ...form, category: event.target.value })}
              />
            </label>
            <label>
              Price (INR)
              <input
                type="number"
                required
                min="0.01"
                step="0.01"
                value={form.price}
                onChange={(event) => setForm({ ...form, price: event.target.value })}
              />
            </label>
            <label>
              Stock quantity
              <input
                type="number"
                required
                min="0"
                step="1"
                value={form.quantity}
                onChange={(event) => setForm({ ...form, quantity: event.target.value })}
              />
            </label>
            <label className="form__span">
              Image URL
              <input
                type="url"
                maxLength={500}
                placeholder="https://…"
                value={form.imageUrl}
                onChange={(event) => setForm({ ...form, imageUrl: event.target.value })}
              />
            </label>
            <label className="form__span">
              Description
              <textarea
                rows={3}
                maxLength={2000}
                value={form.description}
                onChange={(event) => setForm({ ...form, description: event.target.value })}
              />
            </label>
            <div className="form__actions form__span">
              <button type="submit" className="btn btn--primary" disabled={saving}>
                {saving ? 'Saving…' : editingId ? 'Save changes' : 'Create product'}
              </button>
              <button type="button" className="btn btn--ghost" onClick={closeForm}>
                Cancel
              </button>
            </div>
          </form>
        </section>
      )}

      <form className="search search--inline" onSubmit={handleSearch} role="search">
        <input
          type="search"
          value={searchInput}
          onChange={(event) => setSearchInput(event.target.value)}
          placeholder="Search catalog by name"
          aria-label="Search catalog"
        />
        <button type="submit" className="btn btn--ghost">
          Search
        </button>
        {query && (
          <button
            type="button"
            className="btn btn--ghost"
            onClick={() => {
              setSearchInput('');
              setQuery('');
              setPage(0);
            }}
          >
            Clear
          </button>
        )}
      </form>

      {loading ? (
        <Spinner label="Loading catalog…" />
      ) : data?.content?.length ? (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Stock</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((product) => (
                  <tr key={product.id}>
                    <td>#{product.id}</td>
                    <td>{product.name}</td>
                    <td>{product.category}</td>
                    <td>{formatMoney(product.price)}</td>
                    <td>{product.quantity}</td>
                    <td className="table__actions">
                      <button type="button" className="btn btn--ghost btn--sm" onClick={() => openEdit(product)}>
                        Edit
                      </button>
                      <button type="button" className="btn btn--danger btn--sm" onClick={() => handleDelete(product)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      ) : (
        !error && <EmptyState title="No products found" message="Create the first catalog item." />
      )}
    </div>
  );
}
