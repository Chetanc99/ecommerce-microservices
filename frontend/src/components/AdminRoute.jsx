import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import Spinner from './Spinner.jsx';

export default function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin, initializing } = useAuth();

  if (initializing) {
    return <Spinner label="Checking your permissions…" />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: '/admin/products' }} />;
  }

  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }

  return children;
}
