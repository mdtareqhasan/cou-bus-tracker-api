import { useEffect, useMemo, useState } from 'react';
import { adminAPI } from '../api';
import { Search, UserCog, Pencil, Trash2, ShieldCheck, X, PlusCircle } from 'lucide-react';

export default function AdminUsersPage() {
  const [admins, setAdmins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [editingAdmin, setEditingAdmin] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [message, setMessage] = useState('');

  const loadAdmins = async () => {
    try {
      const res = await adminAPI.getAll();
      setAdmins(res.data);
    } catch {
      setMessage('Unable to load admin accounts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdmins();
  }, []);

  const filteredAdmins = useMemo(() => admins.filter((admin) => {
    const term = search.toLowerCase();
    return admin.name?.toLowerCase().includes(term) || admin.email?.toLowerCase().includes(term);
  }), [admins, search]);

  const openEdit = (admin) => {
    setEditingAdmin(admin);
    setForm({ name: admin.name || '', email: admin.email || '', password: '' });
    setShowForm(true);
    setMessage('');
  };

  const openCreate = () => {
    setEditingAdmin(null);
    setForm({ name: '', email: '', password: '' });
    setShowForm(true);
    setMessage('');
  };

  const closeModal = () => {
    setEditingAdmin(null);
    setShowForm(false);
    setForm({ name: '', email: '', password: '' });
    setMessage('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      if (editingAdmin) {
        await adminAPI.update(editingAdmin.id, { name: form.name, password: form.password || undefined });
        setMessage('Admin profile updated successfully.');
      } else {
        await adminAPI.create(form);
        setMessage('New admin account created successfully.');
      }
      closeModal();
      loadAdmins();
    } catch {
      setMessage(editingAdmin ? 'Unable to update this admin profile.' : 'Unable to create this admin account. The email may already be used.');
    }
  };

  const handleDelete = async (adminId) => {
    if (!window.confirm('Delete this admin account?')) return;
    try {
      await adminAPI.delete(adminId);
      setMessage('Admin account deleted.');
      loadAdmins();
    } catch {
      setMessage('Unable to delete this admin account.');
    }
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;
  }

  return (
    <div>
      <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Admin Management</h1>
          <p className="mt-1 text-gray-500">Manage admin profiles, edit details, and remove accounts.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-full bg-teal-50 px-3 py-1 text-sm font-medium text-emerald-700">{admins.length} admins</div>
          <button onClick={openCreate} className="btn-primary"><PlusCircle className="h-4 w-4" /> Add Admin</button>
        </div>
      </div>

      {message && (
        <div className="mb-4 rounded-xl border border-teal-200 bg-teal-50 px-4 py-3 text-sm text-emerald-700">{message}</div>
      )}

      <div className="mb-6 flex items-center justify-between gap-4 rounded-2xl border border-gray-100 bg-white p-4 shadow-sm">
        <div className="relative w-full max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search admins..." className="input-field pl-10" />
        </div>
        <div className="flex items-center gap-2 rounded-lg bg-teal-50 px-3 py-2 text-sm font-medium text-emerald-700">
          <ShieldCheck className="h-4 w-4" />
          Super admin controls
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-gray-100 bg-gray-50/80">
              <tr>
                {['Admin', 'Email', 'Actions'].map((header) => (
                  <th key={header} className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-gray-600">{header}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filteredAdmins.map((admin) => (
                <tr key={admin.id} className="table-row">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 bg-teal-50 rounded-full flex items-center justify-center text-sm font-medium text-emerald-700">
                        {admin.name?.charAt(0) || 'A'}
                      </div>
                      <div>
                        <p className="font-semibold text-gray-900">{admin.name}</p>
                        <p className="text-xs text-gray-400">Admin account</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-600">{admin.email}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1">
                      <button onClick={() => openEdit(admin)} className="btn-icon hover:bg-teal-50 text-emerald-600" title="Edit profile">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button onClick={() => handleDelete(admin.id)} className="btn-icon hover:bg-red-50 text-red-600" title="Delete admin">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filteredAdmins.length === 0 && (
          <div className="py-12 text-center">
            <UserCog className="mx-auto mb-3 h-12 w-12 text-gray-300" />
            <p className="font-medium text-gray-400">No admin accounts found</p>
          </div>
        )}
      </div>

      {showForm && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content max-w-lg" onClick={(event) => event.stopPropagation()}>
            <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4">
              <div>
                <h3 className="text-xl font-bold text-gray-900">{editingAdmin ? 'Edit Admin Profile' : 'Add New Admin'}</h3>
                <p className="text-sm text-gray-500">Update the selected admin's name or password.</p>
              </div>
              <button onClick={closeModal} className="rounded-xl p-2 hover:bg-gray-100"><X className="h-5 w-5 text-gray-500" /></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4 p-6">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">Name</label>
                <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="input-field" required />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">Email</label>
                <input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} disabled={Boolean(editingAdmin)} className="input-field bg-gray-50 disabled:cursor-not-allowed" required />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700">{editingAdmin ? 'New Password' : 'Password'} {editingAdmin && <span className="text-xs text-gray-400">(optional)</span>}</label>
                <input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} className="input-field" required={!editingAdmin} minLength="6" placeholder={editingAdmin ? 'Leave blank to keep current password' : 'At least 6 characters'} />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button type="button" onClick={closeModal} className="btn-secondary">Cancel</button>
                <button type="submit" className="btn-primary">
                  <PlusCircle className="h-4 w-4" /> {editingAdmin ? 'Save Changes' : 'Create Admin'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
