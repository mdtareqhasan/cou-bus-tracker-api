import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { profileAPI } from '../api';
import { UserCircle2, PencilLine, Save, ShieldCheck } from 'lucide-react';

export default function AdminProfilePage() {
  const { admin, setAdmin } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const res = await profileAPI.get();
        const profile = res.data;
        setForm({ name: profile.name || '', email: profile.email || '', password: '' });
      } catch {
        setMessage('Unable to load profile right now.');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      const payload = { name: form.name, password: form.password || undefined };
      const res = await profileAPI.update(payload);
      const updatedProfile = res.data;
      localStorage.setItem('admin_name', updatedProfile.name);
      setAdmin((current) => current ? { ...current, name: updatedProfile.name } : current);
      setForm((current) => ({ ...current, name: updatedProfile.name, password: '' }));
      setMessage('Profile updated successfully.');
    } catch {
      setMessage('Unable to update your profile.');
    }
  };

  if (loading) {
    return <div className="flex h-64 items-center justify-center"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;
  }

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Admin Profile</h1>
        <p className="mt-1 text-gray-500">View and update your personal admin details.</p>
      </div>

      {message && (
        <div className="rounded-xl border border-teal-200 bg-teal-50 px-4 py-3 text-sm text-emerald-700">{message}</div>
      )}

      <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-100 pb-5">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-emerald-500 to-cyan-600 text-white shadow-lg shadow-emerald-500/25">
              <UserCircle2 className="h-8 w-8" />
            </div>
            <div>
              <p className="text-lg font-semibold text-gray-900">{form.name || admin?.name}</p>
              <p className="text-sm text-gray-500">{form.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-teal-50 px-3 py-1.5 text-sm font-medium text-emerald-700">
            <ShieldCheck className="h-4 w-4" />
            Signed-in admin
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Name</label>
            <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="input-field" required />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Email</label>
            <input value={form.email} disabled className="input-field bg-gray-50" />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">New Password <span className="text-xs text-gray-400">(optional)</span></label>
            <input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} className="input-field" placeholder="Leave blank to keep current password" />
          </div>
          <div className="flex justify-end">
            <button type="submit" className="btn-primary">
              <Save className="h-4 w-4" /> Save Profile
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
