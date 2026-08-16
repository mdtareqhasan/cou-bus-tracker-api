import { useState, useEffect } from 'react';
import { studentAPI } from '../api';
import { CheckCircle, X, XCircle, ShieldCheck, Eye, Search, GraduationCap, Trash2 } from 'lucide-react';

export default function StudentsPage() {
  const [students, setStudents] = useState([]);
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(true);
  const [viewCard, setViewCard] = useState(null);
  const [search, setSearch] = useState('');

  const load = () => {
    studentAPI.getAll().then(res => setStudents(res.data)).catch(() => {}).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleVerify = async (id) => { await studentAPI.verify(id); load(); };
  const handleToggle = async (id) => { await studentAPI.toggleActive(id); load(); };
  const handleDelete = async (id, name) => {
    if (!window.confirm(`Delete student "${name}"? This will permanently remove their account and ID card image.`)) return;
    await studentAPI.delete(id);
    load();
  };

  const filtered = (filter === 'pending' ? students.filter(s => !s.isVerified)
    : filter === 'edu' ? students.filter(s => s.isEduMail)
    : filter === 'personal' ? students.filter(s => !s.isEduMail)
    : students
  ).filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase()) ||
    s.email.toLowerCase().includes(search.toLowerCase()) ||
    s.studentId.toLowerCase().includes(search.toLowerCase()) ||
    s.department.toLowerCase().includes(search.toLowerCase())
  );

  const filters = [
    { key: 'all', label: 'All', count: students.length },
    { key: 'pending', label: 'Pending', count: students.filter(s => !s.isVerified).length },
    { key: 'edu', label: 'Edu Mail', count: students.filter(s => s.isEduMail).length },
    { key: 'personal', label: 'Personal', count: students.filter(s => !s.isEduMail).length },
  ];

  if (loading) return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Student Management</h1>
        <p className="text-gray-500 mt-1">{students.length} registered students</p>
      </div>

      {/* Filters & Search */}
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
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search students..."
            className="input-field pl-10 max-w-xs" />
        </div>
      </div>

      {/* View ID Card Modal */}
      {viewCard && (
        <div className="modal-overlay" onClick={() => setViewCard(null)}>
          <div className="modal-content max-w-lg" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h3 className="text-xl font-bold text-gray-900">ID Card — {viewCard.name}</h3>
              <button onClick={() => setViewCard(null)} className="p-2 rounded-xl hover:bg-gray-100"><X className="w-5 h-5 text-gray-500" /></button>
            </div>
            <div className="p-6">
              {viewCard.idCardImageUrl ? (
                <img src={viewCard.idCardImageUrl} alt="ID Card" className="w-full rounded-xl border border-gray-100 shadow-sm" />
              ) : (
                <div className="text-center py-12"><GraduationCap className="w-16 h-16 text-gray-200 mx-auto mb-3" /><p className="text-gray-400">No ID card uploaded</p></div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50/80 border-b border-gray-100">
              <tr>
                {['Student', 'ID', 'Department', 'Batch', 'Mail', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.map(student => (
                <tr key={student.id} className="table-row">
                  <td className="px-6 py-4">
                    <div>
                      <p className="font-semibold text-gray-900">{student.name}</p>
                      <p className="text-xs text-gray-400">{student.email}</p>
                    </div>
                  </td>
                  <td className="px-6 py-4 font-mono text-sm text-gray-700">{student.studentId}</td>
                  <td className="px-6 py-4 text-gray-600">{student.department}</td>
                  <td className="px-6 py-4 text-gray-600">{student.varsityBatch}</td>
                  <td className="px-6 py-4">
                    <span className={`badge ${student.isEduMail ? 'badge-green' : 'badge-orange'}`}>
                      {student.isEduMail ? 'Edu Mail' : 'Personal'}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`badge ${student.isVerified ? 'badge-green' : 'badge-yellow'}`}>
                      {student.isVerified ? 'Verified' : 'Pending'}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1">
                      {student.idCardImageUrl && (
                        <button onClick={() => setViewCard(student)} className="btn-icon hover:bg-teal-50 text-emerald-600" title="View ID Card"><Eye className="w-4 h-4" /></button>
                      )}
                      {!student.isVerified && (
                        <button onClick={() => handleVerify(student.id)} className="btn-icon hover:bg-green-50 text-green-600" title="Verify"><ShieldCheck className="w-4 h-4" /></button>
                      )}
                      <button onClick={() => handleToggle(student.id)} className={`btn-icon ${student.isActive ? 'hover:bg-green-50 text-green-600' : 'hover:bg-red-50 text-red-600'}`}>
                        {student.isActive ? <CheckCircle className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
                      </button>
                      <button onClick={() => handleDelete(student.id, student.name)} className="btn-icon hover:bg-red-50 text-red-600" title="Delete student">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filtered.length === 0 && (
          <div className="text-center py-12"><GraduationCap className="w-12 h-12 text-gray-300 mx-auto mb-3" /><p className="text-gray-400 font-medium">No students found</p></div>
        )}
      </div>
    </div>
  );
}
