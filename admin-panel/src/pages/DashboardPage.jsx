import { useState, useEffect } from 'react';
import { dashboardAPI } from '../api';
import { Link } from 'react-router-dom';
import { Bus, Users, GraduationCap, BookOpen, Bell, CheckCircle, Clock, ArrowRight, Activity } from 'lucide-react';

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardAPI.getStats()
      .then(res => setStats(res.data))
      .catch(err => console.error('Dashboard API error:', err.response?.status, err.response?.data || err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin rounded-full h-8 w-8 border-2 border-teal-200 border-t-emerald-600" />
    </div>
  );

  const statCards = [
    { label: 'Total Buses', value: stats?.totalBuses, icon: Bus, color: 'bg-emerald-600', lightBg: 'bg-emerald-50', sub: `${stats?.activeBuses} active`, link: '/buses' },
    { label: 'Students', value: stats?.totalStudents, icon: GraduationCap, color: 'bg-cyan-600', lightBg: 'bg-cyan-50', sub: `${stats?.pendingStudents} pending`, link: '/students' },
    { label: 'Teachers', value: stats?.totalTeachers, icon: Users, color: 'bg-sky-600', lightBg: 'bg-sky-50', sub: `${stats?.pendingTeachers} pending`, link: '/teachers' },
    { label: 'Schedules', value: stats?.totalSchedules, icon: BookOpen, color: 'bg-amber-500', lightBg: 'bg-amber-50', sub: 'Active routes', link: '/schedules' },
    { label: 'Notices', value: stats?.totalNotices, icon: Bell, color: 'bg-rose-500', lightBg: 'bg-rose-50', sub: 'Posted', link: '/notices' },
    { label: 'Verified', value: stats?.verifiedStudents, icon: CheckCircle, color: 'bg-teal-600', lightBg: 'bg-teal-50', sub: `of ${stats?.totalStudents}`, link: '/students' },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-500 mt-1">Overview of your bus tracking system</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {statCards.map(({ label, value, icon: Icon, color, lightBg, sub, link }) => (
          <Link key={label} to={link} className="stat-card group">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-gray-500 font-medium">{label}</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">{value ?? 0}</p>
                <p className="text-xs text-gray-400 mt-2">{sub}</p>
              </div>
              <div className={`${lightBg} p-2.5 rounded-xl`}>
                <div className={`${color} p-2 rounded-lg`}>
                  <Icon className="w-5 h-5 text-white" />
                </div>
              </div>
            </div>
            <div className="flex items-center gap-1 mt-4 text-xs text-gray-400 group-hover:text-emerald-600 transition-colors">
              <span>View details</span>
              <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
            </div>
          </Link>
        ))}
      </div>

      {/* Alerts */}
      {stats?.pendingStudents > 0 && (
        <Link to="/students" className="flex items-center gap-4 bg-amber-50 border border-amber-200 rounded-xl p-4 hover:bg-amber-100 transition-colors group">
          <div className="bg-amber-100 p-2.5 rounded-xl">
            <Clock className="w-4 h-4 text-amber-600" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-semibold text-amber-900">{stats.pendingStudents} student(s) waiting for verification</p>
          </div>
          <ArrowRight className="w-4 h-4 text-amber-400 group-hover:translate-x-0.5 transition-transform" />
        </Link>
      )}

      {stats?.pendingTeachers > 0 && (
        <Link to="/teachers" className="flex items-center gap-4 bg-orange-50 border border-orange-200 rounded-xl p-4 hover:bg-orange-100 transition-colors group">
          <div className="bg-orange-100 p-2.5 rounded-xl">
            <Clock className="w-4 h-4 text-orange-600" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-semibold text-orange-900">{stats.pendingTeachers} teacher(s) waiting for verification</p>
          </div>
          <ArrowRight className="w-4 h-4 text-orange-400 group-hover:translate-x-0.5 transition-transform" />
        </Link>
      )}

      {!stats?.pendingStudents && !stats?.pendingTeachers && (
        <div className="flex items-center gap-4 bg-green-50 border border-green-200 rounded-xl p-4">
          <div className="bg-green-100 p-2.5 rounded-xl">
            <CheckCircle className="w-4 h-4 text-green-600" />
          </div>
          <p className="text-sm font-medium text-green-800">All caught up — no pending registrations</p>
        </div>
      )}
    </div>
  );
}
