import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { totalItems } = useCart();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setOpen(false);
    navigate('/');
  };

  const close = () => setOpen(false);

  return (
    <header className="site-header">
      <div className="container nav">
        <Link to="/" className="brand" onClick={close}>
          <span className="brand__mark">N</span>
          NovaCart
        </Link>

        <button
          type="button"
          className="nav__toggle"
          aria-expanded={open}
          aria-label="Toggle navigation"
          onClick={() => setOpen((value) => !value)}
        >
          ☰
        </button>

        <nav className={`nav__links ${open ? 'is-open' : ''}`}>
          <NavLink to="/" end onClick={close}>
            Shop
          </NavLink>

          {isAuthenticated && (
            <NavLink to="/orders" onClick={close}>
              Orders
            </NavLink>
          )}

          {isAdmin && (
            <NavLink to="/admin/products" onClick={close}>
              Admin
            </NavLink>
          )}

          <NavLink to="/cart" onClick={close} className="nav__cart">
            Cart
            {totalItems > 0 && <span className="badge">{totalItems}</span>}
          </NavLink>

          {isAuthenticated ? (
            <>
              <NavLink to="/profile" onClick={close}>
                {user?.name || 'Profile'}
              </NavLink>
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleLogout}>
                Sign out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" onClick={close}>
                Sign in
              </NavLink>
              <Link to="/register" className="btn btn--primary btn--sm" onClick={close}>
                Create account
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
