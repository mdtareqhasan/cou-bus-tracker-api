import { useState, useEffect } from 'react';
import { busAPI } from '../api';
import { Plus, Edit2, Trash2, Link, X, Save, Search, Bus } from 'lucide-react';

export default function BusesPage() {
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [showTracker, setShowTracker] = useState(null);
  const [editBus, setEditBus] = useState(null);
  const [search, setSearch] = useState('');
  const [form, setForm] = useState({ busNumber: '', busName: '', category: 'BLUE', driverName: '', driverPhone: '', busImageUrl: '', trackerUrl: '' });
  const [trackerForm, setTrackerForm] = useState({ trackerUrl: '', expiresAt: '' });

  const load = () => {
    busAPI.getAll().then(res => setBuses(res.data)).catch(() => {}).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (editBus) {
      const { trackerUrl, ...busData } = form;
      await busAPI.update(editBus.id, busData);
      if (trackerUrl) await busAPI.updateTrackerLink(editBus.id, { trackerUrl, expiresAt: '' });
    } else {
      const { trackerUrl, ...busData } = form;
      const response = await busAPI.create(busData);
      if (trackerUrl) await busAPI.updateTrackerLink(response.data.id, { trackerUrl, expiresAt: '' });
    }
    setShowForm(false);
    setEditBus(null);
    setForm({ busNumber: '', busName: '', category: 'BLUE', driverName: '', driverPhone: '', busImageUrl: '', trackerUrl: '' });
    load();
  };

  const handleEdit = (bus) => {
    setEditBus(bus);
    setForm({ busNumber: bus.busNumber, busName: bus.busName || '', category: bus.category, driverName: bus.driverName || '', driverPhone: bus.driverPhone || '', busImageUrl: bus.busImageUrl || '', trackerUrl: bus.trackerUrl || '' });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('Are you sure you want to delete this bus?')) {
      await busAPI.delete(id);
      load();
    }
  };

  const handleTrackerUpdate = async (busId) => {
    await busAPI.updateTrackerLink(busId, trackerForm);
    setShowTracker(null);
    setTrackerForm({ trackerUrl: '', expiresAt: '' });
    load();
  };

  const filtered = buses.filter(b =>
    b.busNumber.toLowerCase().includes(search.toLowerCase()) ||
    b.category.toLowerCase().includes(search.toLowerCase())
  );

  const categoryColors = {
    BLUE: 'bg-blue-100 text-blue-700 border-blue-200',
    RED: 'bg-red-100 text-red-700 border-red-200',
    TEACHER: 'bg-purple-100 text-purple-700 border-purple-200',
    OFFICER: 'bg-amber-100 text-amber-700 border-amber-200',
    STAFF: 'bg-teal-100 text-teal-700 border-teal-200',
  };

  if (loading) return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-8 flex-wrap gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Bus Management</h1>
          <p className="text-gray-500 mt-1">{buses.length} buses in fleet</p>
        </div>
        <button onClick={() => { setShowForm(true); setEditBus(null); setForm({ busNumber: '', busName: '', category: 'BLUE', driverName: '', driverPhone: '', busImageUrl: '', trackerUrl: '' }); }}
          className="btn-primary">
          <Plus className="w-4 h-4" /> Add Bus
        </button>
      </div>

      {/* Search */}
      <div className="mb-6">
        <div className="relative max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search buses..."
            className="input-field pl-10" />
        </div>
      </div>

      {/* Modal Form */}
      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal-content max-w-lg" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h3 className="text-xl font-bold text-gray-900">{editBus ? 'Edit Bus' : 'Add New Bus'}</h3>
              <button onClick={() => setShowForm(false)} className="p-2 rounded-xl hover:bg-gray-100 transition"><X className="w-5 h-5 text-gray-500" /></button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Bus Number *</label>
                  <input value={form.busNumber} onChange={e => setForm({...form, busNumber: e.target.value})} required className="input-field" placeholder="BUS 03" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Bus Name (Optional)</label>
                  <input value={form.busName} onChange={e => setForm({...form, busName: e.target.value})} className="input-field" placeholder="e.g. Blue Star" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Category *</label>
                  <select value={form.category} onChange={e => setForm({...form, category: e.target.value})} className="select-field">
                    <option value="BLUE">BLUE</option>
                    <option value="RED">RED</option>
                    <option value="TEACHER">TEACHER</option>
                    <option value="OFFICER">OFFICER</option>
                    <option value="STAFF">STAFF</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Driver Name</label>
                  <input value={form.driverName} onChange={e => setForm({...form, driverName: e.target.value})} className="input-field" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Driver Phone</label>
                  <input value={form.driverPhone} onChange={e => setForm({...form, driverPhone: e.target.value})} className="input-field" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Bus Live Link (Optional)</label>
                <input type="url" value={form.trackerUrl} onChange={e => setForm({...form, trackerUrl: e.target.value})} className="input-field" placeholder="https://tracker.example.com/bus" />
              </div>
              <div className="flex gap-3 justify-end pt-4 border-t border-gray-100">
                <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
                <button type="submit" className="btn-primary"><Save className="w-4 h-4" /> {editBus ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Tracker Link Modal */}
      {showTracker && (
        <div className="modal-overlay" onClick={() => setShowTracker(null)}>
          <div className="modal-content max-w-md" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <div>
                <h3 className="text-xl font-bold text-gray-900">Update Tracker Link</h3>
                <p className="text-sm text-gray-500 mt-0.5">{showTracker.busNumber}</p>
              </div>
              <button onClick={() => setShowTracker(null)} className="p-2 rounded-xl hover:bg-gray-100 transition"><X className="w-5 h-5 text-gray-500" /></button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Tracker URL</label>
                <input value={trackerForm.trackerUrl} onChange={e => setTrackerForm({...trackerForm, trackerUrl: e.target.value})} placeholder="https://tracker-platform.com/live/bus03" className="input-field" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Expires At</label>
                <input type="datetime-local" value={trackerForm.expiresAt} onChange={e => setTrackerForm({...trackerForm, expiresAt: e.target.value})} className="input-field" />
              </div>
              <div className="flex gap-3 justify-end pt-4 border-t border-gray-100">
                <button onClick={() => setShowTracker(null)} className="btn-secondary">Cancel</button>
                <button onClick={() => handleTrackerUpdate(showTracker.id)} className="btn-success"><Link className="w-4 h-4" /> Update Link</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Bus Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50/80 border-b border-gray-100">
              <tr>
                <th className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">Bus</th>
                <th className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">Category</th>
                <th className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">Driver</th>
                <th className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">Status</th>
                <th className="text-left px-6 py-4 font-semibold text-gray-600 text-xs uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.map(bus => (
                <tr key={bus.id} className="table-row">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-teal-50 rounded-xl flex items-center justify-center">
                        <Bus className="w-5 h-5 text-emerald-600" />
                      </div>
                      <div>
                        <span className="font-semibold text-gray-900">{bus.busNumber}</span>
                        {bus.busName && <p className="text-xs text-gray-500">{bus.busName}</p>}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`badge border ${categoryColors[bus.category] || 'bg-gray-100 text-gray-700 border-gray-200'}`}>
                      {bus.category}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-gray-600">{bus.driverName || '-'}</td>
                  <td className="px-6 py-4">
                    <span className={`badge ${bus.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                      {bus.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1">
                      <button onClick={() => handleEdit(bus)} className="btn-icon hover:bg-teal-50 text-emerald-600" title="Edit"><Edit2 className="w-4 h-4" /></button>
                      <button onClick={() => { setShowTracker(bus); setTrackerForm({ trackerUrl: bus.trackerUrl || '', expiresAt: '' }); }} className="btn-icon hover:bg-green-50 text-green-600" title="Tracker Link"><Link className="w-4 h-4" /></button>
                      <button onClick={() => handleDelete(bus.id)} className="btn-icon hover:bg-red-50 text-red-600" title="Delete"><Trash2 className="w-4 h-4" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filtered.length === 0 && (
          <div className="text-center py-12">
            <Bus className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-400 font-medium">No buses found</p>
          </div>
        )}
      </div>
    </div>
  );
}
