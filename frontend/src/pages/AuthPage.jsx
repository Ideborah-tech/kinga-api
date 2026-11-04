import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../OwnerContext.jsx';
import PaymentScene from '../components/PaymentScene.jsx';

/** Sign in and create account: the 3D scene fills the screen, the form floats on a card. No scrolling. */
export default function AuthPage({ mode }) {
  const isSignup = mode === 'signup';
  const { user, login, register, demoLogin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const target = location.state?.from || '/';
  if (user) return <Navigate to={target} replace />;

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const run = async (action) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      navigate(target, { replace: true });
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };

  const submit = (e) => {
    e.preventDefault();
    run(() => (isSignup ? register(form.name, form.email, form.password) : login(form.email, form.password)));
  };

  return (
    <div className="auth">
      <div className="auth-stage">
        <PaymentScene className="auth-scene" />
        <div className="auth-brand">
          <img src="/shield.svg" alt="" width="44" height="44" />
          <span className="wordmark">Kinga<b>API</b></span>
        </div>
        <div className="auth-tagline">
          <h1>It’s them, <span>not you.</span></h1>
          <p>Payment API monitoring for African developers. Know in seconds whether a failed payment is your code or your provider.</p>
        </div>
      </div>

      <div className="auth-side">
        <svg className="auth-curve" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <path className="fill" d="M100 0 L62 0 C12 22 8 42 44 54 C84 67 74 88 34 100 L100 100 Z" />
          <path className="line" d="M50 0 C0 22 -4 42 32 54 C72 67 62 88 22 100" vectorEffect="non-scaling-stroke" />
        </svg>

        <main className="auth-card">
          <nav className="auth-switch" aria-label="Account">
            <Link to="/login" className={isSignup ? '' : 'active'} aria-current={isSignup ? undefined : 'page'}>Sign in</Link>
            <Link to="/signup" className={isSignup ? 'active' : ''} aria-current={isSignup ? 'page' : undefined}>Create account</Link>
          </nav>

          <h2 className="auth-title">{isSignup ? 'Create your account' : 'Welcome back'}</h2>

          {error && <div className="alert alert-error">{error}</div>}

          <form className="auth-form" onSubmit={submit}>
            {isSignup && (
              <label>Full name
                <input value={form.name} onChange={set('name')} required autoComplete="name" placeholder="Amina Wanjiru" />
              </label>
            )}
            <label>Email
              <input type="email" value={form.email} onChange={set('email')} required autoComplete="email" placeholder="you@company.com" />
            </label>
            <label>Password
              <input
                type="password" value={form.password} onChange={set('password')} required
                minLength={isSignup ? 8 : undefined}
                autoComplete={isSignup ? 'new-password' : 'current-password'}
                placeholder={isSignup ? 'At least 8 characters' : 'Your password'}
              />
            </label>
            <button className="btn btn-primary btn-block" disabled={busy}>
              {busy ? 'Please wait…' : isSignup ? 'Create account' : 'Sign in'}
            </button>
          </form>

          <div className="auth-divider"><span>or</span></div>

          <button className="btn btn-ghost btn-block" disabled={busy} onClick={() => run(demoLogin)}>
            Try the demo
          </button>
        </main>
      </div>
    </div>
  );
}
