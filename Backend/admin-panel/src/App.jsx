import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout_page from './pages/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import BusesPage from './pages/BusesPage';
import SchedulesPage from './pages/SchedulesPage';
import NoticesPage from './pages/NoticesPage';
import StudentsPage from './pages/StudentsPage';
import TeachersPage from './pages/TeachersPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminProfilePage from './pages/AdminProfilePage';

function ProtectedRoute({ children }) {
  const { admin, loading } = useAuth();
  if (loading) return null;
  return admin ? children : <Navigate to="/login" />;
}

function PublicRoute({ children }) {
  const { admin, loading } = useAuth();
  if (loading) return null;
  return admin ? <Navigate to="/" /> : children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
          <Route path="/" element={<ProtectedRoute><Layout_page /></ProtectedRoute>}>
            <Route index element={<DashboardPage />} />
            <Route path="buses" element={<BusesPage />} />
            <Route path="schedules" element={<SchedulesPage />} />
            <Route path="notices" element={<NoticesPage />} />
            <Route path="students" element={<StudentsPage />} />
            <Route path="teachers" element={<TeachersPage />} />
            <Route path="profile" element={<AdminProfilePage />} />
            <Route path="admins" element={<AdminUsersPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
