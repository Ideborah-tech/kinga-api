import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import Dashboard from './pages/Dashboard.jsx';
import MonitorDetail from './pages/MonitorDetail.jsx';
import FailureReport from './pages/FailureReport.jsx';
import MonitorForm from './pages/MonitorForm.jsx';
import Templates from './pages/Templates.jsx';
import Providers from './pages/Providers.jsx';
import DemoControl from './pages/DemoControl.jsx';

export default function App() {
  return (
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
  );
}
