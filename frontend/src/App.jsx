import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './OwnerContext.jsx';
import Layout from './components/Layout.jsx';
import AuthPage from './pages/AuthPage.jsx';
import Dashboard from './pages/Dashboard.jsx';
import MonitorDetail from './pages/MonitorDetail.jsx';
import FailureReport from './pages/FailureReport.jsx';
import MonitorForm from './pages/MonitorForm.jsx';
import Templates from './pages/Templates.jsx';
import Providers from './pages/Providers.jsx';
import DemoControl from './pages/DemoControl.jsx';

/** Sends signed-out visitors to the sign-in page, then back where they were going. */
function RequireAuth({ children }) {
  const { user, checking } = useAuth();
  const location = useLocation();
  if (checking) return <div className="boot">Loading KingaAPI…</div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/signup" element={<AuthPage mode="signup" />} />
      <Route
        path="*"
        element={(
          <RequireAuth>
            <Layout>
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/monitors/new" element={<MonitorForm />} />
                <Route path="/monitors/:id" element={<MonitorDetail />} />
                <Route path="/monitors/:id/edit" element={<MonitorForm />} />
                <Route path="/monitors/:id/report" element={<FailureReport />} />
                <Route path="/templates" element={<Templates />} />
                <Route path="/providers" element={<Providers />} />
                <Route path="/demo" element={<DemoControl />} />
                <Route path="*" element={<div className="card empty">Page not found.</div>} />
              </Routes>
            </Layout>
          </RequireAuth>
        )}
      />
    </Routes>
  );
}
