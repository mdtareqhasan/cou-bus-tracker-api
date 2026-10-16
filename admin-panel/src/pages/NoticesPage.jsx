import { useState, useEffect } from 'react';
import { noticeAPI } from '../api';
import { Plus, Trash2, X, Save, Bell, AlertCircle } from 'lucide-react';

export default function NoticesPage() {
  const [notices, setNotices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ title: '', body: '', expiryHours: 24 });

  const load = () => {
    noticeAPI.getAll().then(res => setNotices(res.data)).catch(() => {}).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    await noticeAPI.create(form);
    setShowForm(false);
    setForm({ title: '', body: '', expiryHours: 24 });
    load();
  };

  const handleDelete = async (id) => {
    if (confirm('Delete this notice?')) { await noticeAPI.delete(id); load(); }
  };

  if (loading) return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-8 flex-wrap gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Notice Management</h1>
          <p className="text-gray-500 mt-1">{notices.length} notices posted</p>
        </div>
        <button onClick={() => setShowForm(true)} className="btn-primary">
          <Plus className="w-4 h-4" /> Post Notice
        </button>
      </div>

      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal-content max-w-lg" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h3 className="text-xl font-bold text-gray-900">Post New Notice</h3>
              <button onClick={() => setShowForm(false)} className="p-2 rounded-xl hover:bg-gray-100"><X className="w-5 h-5 text-gray-500" /></button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Title *</label>
                <input value={form.title} onChange={e => setForm({...form, title: e.target.value})} required placeholder="e.g. Blue Bus 4 Service Suspended" className="input-field" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Body *</label>
                <textarea value={form.body} onChange={e => setForm({...form, body: e.target.value})} required rows={5} placeholder="Write your notice here..." className="input-field resize-none" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Expires After (hours)</label>
                <input type="number" value={form.expiryHours} onChange={e => setForm({...form, expiryHours: parseInt(e.target.value)})} min={1} className="input-field" />
              </div>
              <div className="flex gap-3 justify-end pt-4 border-t border-gray-100">
                <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
                <button type="submit" className="btn-primary"><Save className="w-4 h-4" /> Post Notice</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="space-y-4">
        {notices.map(notice => {
          const isExpired = notice.expiresAt && new Date(notice.expiresAt) < new Date();
          return (
            <div key={notice.id} className={`bg-white rounded-2xl shadow-sm border border-gray-100 p-6 transition-all hover:shadow-md ${isExpired ? 'opacity-50' : ''}`}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-3">
                    <div className="p-2 bg-teal-50 rounded-xl"><Bell className="w-4 h-4 text-emerald-600" /></div>
                    <h3 className="font-bold text-gray-900 text-lg">{notice.title}</h3>
                    <span className={`badge ${isExpired ? 'badge-red' : notice.isActive ? 'badge-green' : 'badge-red'}`}>
                      {isExpired ? 'Expired' : notice.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <p className="text-gray-600 whitespace-pre-wrap leading-relaxed">{notice.body}</p>
                  <div className="flex gap-6 mt-4 text-xs text-gray-400">
                    <span>Created: {new Date(notice.createdAt).toLocaleString()}</span>
                    {notice.expiresAt && <span>Expires: {new Date(notice.expiresAt).toLocaleString()}</span>}
                  </div>
                </div>
                <button onClick={() => handleDelete(notice.id)} className="btn-icon hover:bg-red-50 text-red-600 shrink-0" title="Delete"><Trash2 className="w-4 h-4" /></button>
              </div>
            </div>
          );
        })}
        {notices.length === 0 && (
          <div className="text-center py-12 bg-white rounded-2xl border border-gray-100">
            <Bell className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-400 font-medium">No notices posted yet</p>
          </div>
        )}
      </div>
    </div>
  );
}
