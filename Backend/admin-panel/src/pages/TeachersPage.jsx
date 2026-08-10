import { useState, useEffect } from 'react';
import { teacherAPI } from '../api';
import { CheckCircle, XCircle, ShieldCheck, Search, Users } from 'lucide-react';

export default function TeachersPage() {
  const [teachers, setTeachers] = useState([]);
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  const load = () => {
    teacherAPI.getAll().then(res => setTeachers(res.data)).catch(() => {}).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleVerify = async (id) => { await teacherAPI.verify(id); load(); };
  const handleToggle = async (id) => { await teacherAPI.toggleActive(id); load(); };

  const filtered = (filter === 'pending' ? teachers.filter(t => !t.isVerified)
    : filter === 'edu' ? teachers.filter(t => t.isEduMail)
    : teachers
  ).filter(t =>
    t.name.toLowerCase().includes(search.toLowerCase()) ||
    t.email.toLowerCase().includes(search.toLowerCase()) ||
    (t.department && t.department.toLowerCase().includes(search.toLowerCase()))
  );

  const filters = [
    { key: 'all', label: 'All', count: teachers.length },
    { key: 'pending', label: 'Pending', count: teachers.filter(t => !t.isVerified).length },
    { key: 'edu', label: 'Edu Mail', count: teachers.filter(t => t.isEduMail).length },
  ];

  if (loading) return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Teacher Management</h1>
        <p className="text-gray-500 mt-1">{teachers.length} registered teachers</p>
      </div>

      <div className="flex items-center justify-between mb-6 flex-wrap gap-4">
        <div className="flex gap-2 flex-wrap">
          {filters.map(({ key, label, count }) => (
            <button key={key} onClick={() => setFilter(key)}
              className={`filter-btn flex items-center gap-2 ${filter === key ? 'filter-btn-active' : 'filter-btn-inactive'}`}>
              {label}
              <span className={`px-1.5 py-0.5 rounded-md text-xs ${filter === key ? 'bg-white/20' : 'bg-gray-100'}`}>{count}</span>
            </button>
          ))}
        </div>
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search teachers..."
            className="input-field pl-10 max-w-xs" />
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50/80 border-b border-gray-100">
              <tr>
                {['Teacher', 'Designation', 'Department', 'Phone', 'Mail', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.map(teacher => (
                <tr key={teacher.id} className="table-row">
                  <td className="px-6 py-4">
                    <div>
                      <p className="font-semibold text-gray-900">{teacher.name}</p>
                      <p className="text-xs text-gray-400">{teacher.email}</p>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-600">{teacher.designation || '-'}</td>
                  <td className="px-6 py-4 text-gray-600">{teacher.department || '-'}</td>
                  <td className="px-6 py-4 text-gray-600">{teacher.phone || '-'}</td>
                  <td className="px-6 py-4">
                    <span className={`badge ${teacher.isEduMail ? 'badge-green' : 'badge-orange'}`}>
                      {teacher.isEduMail ? 'Edu Mail' : 'Personal'}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`badge ${teacher.isVerified ? 'badge-green' : 'badge-yellow'}`}>
                      {teacher.isVerified ? 'Verified' : 'Pending'}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1">
                      {!teacher.isVerified && (
                        <button onClick={() => handleVerify(teacher.id)} className="btn-icon hover:bg-green-50 text-green-600" title="Verify"><ShieldCheck className="w-4 h-4" /></button>
                      )}
                      <button onClick={() => handleToggle(teacher.id)} className={`btn-icon ${teacher.isActive ? 'hover:bg-green-50 text-green-600' : 'hover:bg-red-50 text-red-600'}`}>
                        {teacher.isActive ? <CheckCircle className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filtered.length === 0 && (
          <div className="text-center py-12"><Users className="w-12 h-12 text-gray-300 mx-auto mb-3" /><p className="text-gray-400 font-medium">No teachers found</p></div>
        )}
      </div>
    </div>
  );
}
