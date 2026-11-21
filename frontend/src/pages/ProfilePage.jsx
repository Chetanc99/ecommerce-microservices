import { useState } from 'react';
import { changePassword, updateProfile } from '../api/auth.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const { push } = useToast();

  const [profile, setProfile] = useState({ name: user?.name || '', email: user?.email || '' });
  const [profileError, setProfileError] = useState(null);
  const [savingProfile, setSavingProfile] = useState(false);

  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [passwordError, setPasswordError] = useState(null);
  const [savingPassword, setSavingPassword] = useState(false);

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    setProfileError(null);
    setSavingProfile(true);

    try {
      const updated = await updateProfile(profile);
      updateUser(updated);
      push('Profile updated', 'success');
    } catch (exception) {
      setProfileError(exception);
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    setPasswordError(null);

    if (passwords.newPassword !== passwords.confirm) {
      setPasswordError({ message: 'New passwords do not match.' });
      return;
    }

    setSavingPassword(true);

    try {
      await changePassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword
      });
      setPasswords({ currentPassword: '', newPassword: '', confirm: '' });
      push('Password changed', 'success');
    } catch (exception) {
      setPasswordError(exception);
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div className="page narrow">
      <div className="page-heading">
        <h1>Your profile</h1>
        <span className={`status status--${user?.role === 'ADMIN' ? 'info' : 'success'}`}>
          {user?.role}
        </span>
      </div>

      <section className="panel">
        <h2>Account details</h2>
        <ErrorAlert error={profileError} title="Could not update profile" />

        <form onSubmit={handleProfileSubmit} className="form">
          <label>
            Name
            <input
              type="text"
              required
              maxLength={255}
              value={profile.name}
              onChange={(event) => setProfile({ ...profile, name: event.target.value })}
            />
          </label>
          <label>
            Email
            <input
              type="email"
              required
              maxLength={255}
              value={profile.email}
              onChange={(event) => setProfile({ ...profile, email: event.target.value })}
            />
          </label>
          <button type="submit" className="btn btn--primary" disabled={savingProfile}>
            {savingProfile ? 'Saving…' : 'Save changes'}
          </button>
        </form>
      </section>

      <section className="panel">
        <h2>Change password</h2>
        <ErrorAlert error={passwordError} title="Could not change password" />

        <form onSubmit={handlePasswordSubmit} className="form">
          <label>
            Current password
            <input
              type="password"
              autoComplete="current-password"
              required
              value={passwords.currentPassword}
              onChange={(event) =>
                setPasswords({ ...passwords, currentPassword: event.target.value })
              }
            />
          </label>
          <label>
            New password
            <input
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
              value={passwords.newPassword}
              onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })}
            />
          </label>
          <label>
            Confirm new password
            <input
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
              value={passwords.confirm}
              onChange={(event) => setPasswords({ ...passwords, confirm: event.target.value })}
            />
          </label>
          <button type="submit" className="btn btn--primary" disabled={savingPassword}>
            {savingPassword ? 'Updating…' : 'Change password'}
          </button>
        </form>
      </section>
    </div>
  );
}
