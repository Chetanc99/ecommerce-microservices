import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);

    if (form.password !== form.confirm) {
      setError({ message: 'Passwords do not match.' });
      return;
    }

    setSubmitting(true);

    try {
      await register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password
      });
      navigate('/', { replace: true });
    } catch (exception) {
      setError(exception);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-layout">
      <section className="auth-card">
        <h1>Create your account</h1>
        <p className="muted">Registration creates a customer account with the USER role.</p>

        <ErrorAlert error={error} title="Registration failed" />

        <form onSubmit={handleSubmit} className="form">
          <label>
            Full name
            <input
              type="text"
              autoComplete="name"
              required
              maxLength={255}
              value={form.name}
              onChange={update('name')}
              placeholder="Ada Lovelace"
            />
          </label>

          <label>
            Email
            <input
              type="email"
              autoComplete="email"
              required
              maxLength={255}
              value={form.email}
              onChange={update('email')}
              placeholder="you@example.com"
            />
          </label>

          <label>
            Password
            <input
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
              value={form.password}
              onChange={update('password')}
              placeholder="At least 8 characters"
            />
          </label>

          <label>
            Confirm password
            <input
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
              value={form.confirm}
              onChange={update('confirm')}
              placeholder="Repeat your password"
            />
          </label>

          <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-card__footer">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </section>
    </div>
  );
}
