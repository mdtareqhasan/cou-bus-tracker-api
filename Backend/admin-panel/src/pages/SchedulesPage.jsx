import { useState, useEffect, useMemo } from 'react';
import { scheduleAPI, busAPI } from '../api';
import {
  formatTime,
  bengaliNumber,
  routeParts,
  isTeacherBus,
} from '../utils/format';
import ExportPdfButton from '../components/ExportPdfButton';
import { Plus, Edit2, Trash2, X, Save, Search, BookOpen, Clock3, CalendarDays, BusFront, UsersRound, Power } from 'lucide-react';

const EMPTY_FORM = {
  busId: '', busName: '', departureTime: '', arrivalTime: '', direction: 'UP',
  startPoint: '', endPoint: '', days: 'SUN-THU'
};

const TIME_OPTIONS = [
  '06:15', '06:30', '06:45', '07:00', '07:30', '08:00', '08:30',
  '09:30', '10:00', '12:00', '13:00', '14:00', '15:00', '16:00', '16:10', '17:30',
  '19:00', '19:30', '20:00', '20:30', '21:00'
];

const DEFAULT_ROUTE_OPTIONS = [
  'কান্দিরপাড় > টমসমব্রিজ > ক্যাম্পাস',
  'কান্দিরপাড় > পুলিশলাইন > ক্যাম্পাস',
  'ধর্মপুর > কোটবাড়ি > ক্যাম্পাস',
  'কান্দিরপাড় > ধর্মপুর > ক্যাম্পাস',
  'কান্দিরপাড় > পদুয়ার বাজার বিশ্বরোড > ক্যাম্পাস',
  'ক্যাম্পাস > পুলিশলাইন > কান্দিরপাড়',
  'ক্যাম্পাস > কোটবাড়ি > ধর্মপুর',
  'ক্যাম্পাস > টমসমব্রিজ > কান্দিরপাড়',
  'ক্যাম্পাস > পদুয়ার বাজার বিশ্বরোড > কান্দিরপাড়',
  'ক্যাম্পাস > ধর্মপুর > কান্দিরপাড়'
];

// formatTime, bengaliNumber, routeParts, isTeacherBus are imported from '../utils/format'.

function ScheduleCard({ schedule, onEdit, onDelete, onToggle }) {
  const goingToCampus = schedule.direction === 'UP';
  const isInactive = schedule.isActive === false;
  return (
    <article className={`rounded-2xl border p-4 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md ${isInactive ? 'border-gray-200 bg-gray-50 opacity-60' : 'border-gray-100 bg-white hover:border-teal-100'}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-lg px-2.5 py-1 text-sm font-bold ${isInactive ? 'bg-gray-200 text-gray-500' : 'bg-teal-50 text-emerald-700'}`}>{schedule.busNumber}</span>
            {schedule.busName && <span className="truncate text-sm text-gray-500">{schedule.busName}</span>}
            {isInactive && <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-600">নিষ্ক্রিয়</span>}
          </div>
          <p className="mt-3 text-sm leading-6 text-gray-600">{schedule.startPoint || '—'} <span className="mx-1 text-cyan-600">→</span> {schedule.endPoint || '—'}</p>
        </div>
        <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${goingToCampus ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
          {goingToCampus ? '↑ ক্যাম্পাস অভিমুখে' : '↓ ক্যাম্পাস থেকে'}
        </span>
      </div>
      <div className="mt-4 flex items-center justify-between gap-3 border-t border-gray-100 pt-3">
        <span className="text-sm font-semibold text-emerald-700">{formatTime(schedule.departureTime)}</span>
        <div className="flex gap-1">
          <button onClick={() => onToggle(schedule.id)} className={`btn-icon ${isInactive ? 'text-red-600 hover:bg-red-50' : 'text-emerald-600 hover:bg-teal-50'}`} title={isInactive ? 'সক্রিয় করুন' : 'নিষ্ক্রিয় করুন'}><Power className="h-4 w-4" /></button>
          <button onClick={() => onEdit(schedule)} className="btn-icon text-emerald-600 hover:bg-teal-50" title="সম্পাদনা"><Edit2 className="h-4 w-4" /></button>
          <button onClick={() => onDelete(schedule.id)} className="btn-icon text-red-600 hover:bg-red-50" title="ম�ছুন"><Trash2 className="h-4 w-4" /></button>
        </div>
      </div>
    </article>
  );
}

export default function SchedulesPage() {
  const [schedules, setSchedules] = useState([]);
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [search, setSearch] = useState('');
  const [busAudience, setBusAudience] = useState('STUDENT');
  const [dayGroup, setDayGroup] = useState('WEEKDAY');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [form, setForm] = useState(EMPTY_FORM);

  const load = () => {
    Promise.all([scheduleAPI.getAll(), busAPI.getAll()])
      .then(([s, b]) => {
        setSchedules(s.data);
        setBuses(b.data);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const routes = [...new Set([...DEFAULT_ROUTE_OPTIONS, ...buses.map(bus => bus.route).filter(Boolean)])];
  const selectedRoute = routes.find(route => {
    const { startPoint, endPoint } = routeParts(route);
    return startPoint === form.startPoint && endPoint === form.endPoint;
  }) || '';
  const busesWithLinks = buses.filter(bus => bus.trackerUrl);
  const selectedBus = buses.find(bus => String(bus.id) === String(form.busId));
  const selectedBusTrackerUrl = selectedBus?.trackerUrl || '';

  const closeForm = () => {
    setShowForm(false);
    setEditingSchedule(null);
    setForm(EMPTY_FORM);
  };

  const openCreateForm = () => {
    setEditingSchedule(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const data = { ...form, busId: Number(form.busId), arrivalTime: form.arrivalTime || null };
    if (editingSchedule) await scheduleAPI.update(editingSchedule.id, data);
    else await scheduleAPI.create(data);
    setDayGroup(data.days === 'FRI-SAT' ? 'WEEKEND' : 'WEEKDAY');
    closeForm();
    load();
  };

  const handleEdit = (schedule) => {
    setEditingSchedule(schedule);
    setForm({
      busId: String(schedule.busId), busName: schedule.busName || '', departureTime: schedule.departureTime || '',
      arrivalTime: schedule.arrivalTime || '', direction: schedule.direction || 'UP',
      startPoint: schedule.startPoint || '', endPoint: schedule.endPoint || '', days: schedule.days || 'SUN-THU'
    });
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (confirm('আপনি কি এই শিডিউলটি মুছে ফেলতে চান?')) {
      await scheduleAPI.delete(id);
      load();
    }
  };

  const handleToggle = async (id) => {
    await scheduleAPI.toggle(id);
    load();
  };

  const filtered = schedules.filter(s =>
    [s.busNumber, s.category, s.startPoint, s.endPoint].filter(Boolean)
      .some(value => value.toLowerCase().includes(search.toLowerCase()))
  ).filter(s => {
    if (statusFilter === 'ACTIVE') return s.isActive !== false;
    if (statusFilter === 'INACTIVE') return s.isActive === false;
    return true;
  });

  const visibleSchedules = useMemo(() => filtered.filter(schedule => {
    const matchAudience = busAudience === 'TEACHER' ? isTeacherBus(schedule) : !isTeacherBus(schedule);
    const matchDays = dayGroup === 'WEEKEND' ? schedule.days === 'FRI-SAT' : schedule.days !== 'FRI-SAT';
    return matchAudience && matchDays;
  }), [filtered, busAudience, dayGroup]);

  const groupedSchedules = useMemo(() => {
    const groups = new Map();
    visibleSchedules.forEach(schedule => {
      const key = schedule.departureTime || '99:99';
      groups.set(key, [...(groups.get(key) || []), schedule]);
    });
    return [...groups.entries()].sort(([first], [second]) => first.localeCompare(second));
  }, [visibleSchedules]);

  if (loading) return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-10 w-10 border-4 border-teal-200 border-t-emerald-600" /></div>;

  return (
    <div className="schedule-page">
      <div className="schedule-hero flex items-center justify-between mb-8 flex-wrap gap-4">
        <div>
          <h1 className="text-3xl font-bold">বাস শিডিউল ব্যবস্থাপনা</h1>
          <p className="mt-1">মোট {bengaliNumber.format(schedules.length)}টি শিডিউল ({bengaliNumber.format(schedules.filter(s => s.isActive !== false).length)}টি সক্রিয়, {bengaliNumber.format(schedules.filter(s => s.isActive === false).length)}টি নিষ্ক্রিয়)</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <ExportPdfButton
            visibleSchedules={visibleSchedules}
            schedules={schedules}
            filters={{ busAudience, dayGroup, statusFilter }}
          />
          <button onClick={openCreateForm} className="btn-primary schedule-add-button">
            <Plus className="w-4 h-4" /> শিডিউল যোগ করুন
          </button>
        </div>
      </div>

      <div className="schedule-search mb-6">
        <div className="relative max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="শিডিউল খুঁজুন..." className="input-field pl-10" />
        </div>
      </div>

      <section className="mb-6 rounded-2xl border border-teal-100 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <button onClick={() => setBusAudience('STUDENT')} className={`filter-btn ${busAudience === 'STUDENT' ? 'filter-btn-active' : 'filter-btn-inactive'}`}><BusFront className="mr-1.5 inline h-4 w-4" />শিক্ষার্থী বাস</button>
          <button onClick={() => setBusAudience('TEACHER')} className={`filter-btn ${busAudience === 'TEACHER' ? 'filter-btn-active' : 'filter-btn-inactive'}`}><UsersRound className="mr-1.5 inline h-4 w-4" />শিক্ষক/কর্মকর্তা বাস</button>
          <span className="mx-1 hidden h-9 border-l border-slate-200 sm:block" />
          <button onClick={() => setDayGroup('WEEKDAY')} className={`filter-btn ${dayGroup === 'WEEKDAY' ? 'filter-btn-active' : 'filter-btn-inactive'}`}><CalendarDays className="mr-1.5 inline h-4 w-4" />কর্মদিবস</button>
          <button onClick={() => setDayGroup('WEEKEND')} className={`filter-btn ${dayGroup === 'WEEKEND' ? 'filter-btn-active' : 'filter-btn-inactive'}`}>শুক্রবার ও শনিবার</button>
          <span className="mx-1 hidden h-9 border-l border-slate-200 sm:block" />
          <button onClick={() => setStatusFilter('ALL')} className={`filter-btn ${statusFilter === 'ALL' ? 'filter-btn-active' : 'filter-btn-inactive'}`}>সব</button>
          <button onClick={() => setStatusFilter('ACTIVE')} className={`filter-btn ${statusFilter === 'ACTIVE' ? 'filter-btn-active' : 'filter-btn-inactive'}`}>সক্রিয়</button>
          <button onClick={() => setStatusFilter('INACTIVE')} className={`filter-btn ${statusFilter === 'INACTIVE' ? 'filter-btn-active' : 'filter-btn-inactive'}`}>নিষ্ক্রিয়</button>
        </div>
      </section>

      {showForm && (
        <div className="modal-overlay" onClick={closeForm}>
          <div className="modal-content max-w-lg" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h3 className="text-xl font-bold text-gray-900">{editingSchedule ? 'শিডিউল আপডেট করুন' : 'নতুন শিডিউল যোগ করুন'}</h3>
              <button onClick={closeForm} className="p-2 rounded-xl hover:bg-gray-100"><X className="w-5 h-5 text-gray-500" /></button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">বাস নম্বর *</label>
                  <select value={form.busId} onChange={e => setForm({ ...form, busId: e.target.value })} required className="select-field">
                    <option value="">বাস নির্বাচন করুন</option>
                    {buses.map(bus => <option key={bus.id} value={bus.id}>{bus.busNumber} ({bus.category})</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">বাসের নাম</label>
                  <input value={form.busName} onChange={e => setForm({ ...form, busName: e.target.value })} className="input-field" placeholder="ঐচ্ছিক" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">বাসের লাইভ লিংক</label>
                {selectedBusTrackerUrl ? (
                  <div className="input-field bg-gray-50 text-gray-700 text-sm truncate">{selectedBusTrackerUrl}</div>
                ) : (
                  <div className="input-field bg-gray-50 text-gray-400 text-sm">নির্বাচিত বাসের কোনো লাইভ লিংক নেই</div>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">রুট *</label>
                <select value={selectedRoute} onChange={e => setForm({ ...form, ...routeParts(e.target.value) })} required className="select-field">
                  <option value="">রুট নির্বাচন করুন</option>
                  {routes.map(route => <option key={route} value={route}>{route}</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">ছাড়ার সময় *</label>
                  <input type="time" list="departure-time-options" value={form.departureTime} onChange={e => setForm({ ...form, departureTime: e.target.value })} required className="input-field" />
                  <datalist id="departure-time-options">
                    <option value="">সময় নির্বাচন করুন</option>
                    {TIME_OPTIONS.map(time => <option key={time} value={time}>{formatTime(time)}</option>)}
                  </datalist>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">পৌঁছানোর সময়</label>
                  <input type="time" list="arrival-time-options" value={form.arrivalTime} onChange={e => setForm({ ...form, arrivalTime: e.target.value })} className="input-field" />
                  <datalist id="arrival-time-options">
                    <option value="">নির্বাচন করুন (ঐচ্ছিক)</option>
                    {TIME_OPTIONS.map(time => <option key={time} value={time}>{formatTime(time)}</option>)}
                  </datalist>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">যাত্রার দিক *</label>
                  <select value={form.direction} onChange={e => setForm({ ...form, direction: e.target.value })} className="select-field">
                    <option value="UP">ক্যাম্পাস অভিমুখে</option>
                    <option value="DOWN">ক্যাম্পাস থেকে</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">চলাচলের দিন</label>
                  <select value={form.days} onChange={e => setForm({ ...form, days: e.target.value })} className="select-field">
                    <option value="SAT-THU">শনিবার–বৃহস্পতিবার</option>
                    <option value="SUN-THU">রবিবার–বৃহস্পতিবার</option>
                    <option value="FRI-SAT">শুক্রবার–শনিবার</option>
                    <option value="EVERYDAY">প্রতিদিন</option>
                  </select>
                </div>
              </div>
              <div className="flex gap-3 justify-end pt-4 border-t border-gray-100">
                <button type="button" onClick={closeForm} className="btn-secondary">বাতিল</button>
                <button type="submit" className="btn-primary"><Save className="w-4 h-4" /> {editingSchedule ? 'আপডেট করুন' : 'সংরক্ষণ করুন'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="mb-4 flex items-center gap-2 text-sm text-gray-500"><Clock3 className="h-4 w-4 text-teal-600" />একই সময়ের প্রতিটি বাস আলাদা কার্ডে দেখানো হচ্ছে।</div>
      <div className="space-y-5">
        {groupedSchedules.map(([time, group]) => (
          <section key={time} className="rounded-2xl border border-teal-100 bg-teal-50/40 p-4 sm:p-5">
            <div className="mb-4 flex items-center gap-3"><div className="rounded-xl bg-teal-100 p-2 text-teal-600"><Clock3 className="h-5 w-5" /></div><div><h2 className="font-bold text-gray-800">{formatTime(time)}</h2><p className="text-sm text-gray-500">{bengaliNumber.format(group.length)}টি বাস</p></div></div>
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">{group.map(schedule => <ScheduleCard key={schedule.id} schedule={schedule} onEdit={handleEdit} onDelete={handleDelete} onToggle={handleToggle} />)}</div>
          </section>
        ))}
        {!groupedSchedules.length && <div className="rounded-2xl border border-dashed border-gray-200 bg-white py-14 text-center"><BookOpen className="mx-auto mb-3 h-12 w-12 text-gray-300" /><p className="font-medium text-gray-400">এই বিভাগে কোনো শিডিউল পাওয়া যায়নি</p></div>}
      </div>

      <div className="schedule-table hidden bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50/80 border-b border-gray-100">
              <tr>
                {['বাস নম্বর', 'দিক', 'ছাড়ার সময়', 'পৌঁছানোর সময়', 'রুট', 'দিন', 'অবস্থা', 'কাজ'].map(h => (
                  <th key={h} className="text-left px-6 py-4 font-semibold text-gray-600 text-xs tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filtered.map(s => (
                <tr key={s.id} className="table-row">
                  <td className="px-6 py-4"><span className="font-semibold text-gray-900">{s.busNumber}</span>{s.busName && <span className="text-xs text-gray-500 ml-1">({s.busName})</span>}<span className="text-xs text-gray-400 ml-1">({s.category})</span></td>
                  <td className="px-6 py-4"><span className={`badge ${s.direction === 'UP' ? 'badge-blue' : 'badge-green'}`}>{s.direction === 'UP' ? 'ক্যাম্পাস অভিমুখে' : 'ক্যাম্পাস থেকে'}</span></td>
                  <td className="px-6 py-4 text-gray-700">{formatTime(s.departureTime)}</td>
                  <td className="px-6 py-4 text-gray-700">{formatTime(s.arrivalTime)}</td>
                  <td className="px-6 py-4 text-gray-600">{s.startPoint || '-'} → {s.endPoint || '-'}</td>
                  <td className="px-6 py-4"><span className="badge bg-gray-100 text-gray-600">{s.days}</span></td>
                  <td className="px-6 py-4"><span className={`badge ${s.isActive !== false ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-600'}`}>{s.isActive !== false ? 'সক্রিয়' : 'নিষ্ক্রিয়'}</span></td>
                  <td className="px-6 py-4"><div className="flex items-center gap-1"><button onClick={() => handleToggle(s.id)} className={`btn-icon ${s.isActive !== false ? 'text-emerald-600 hover:bg-teal-50' : 'text-red-600 hover:bg-red-50'}`} title={s.isActive !== false ? 'নিষ্ক্রিয় করুন' : 'সক্রিয় করুন'}><Power className="w-4 h-4" /></button><button onClick={() => handleEdit(s)} className="btn-icon hover:bg-teal-50 text-emerald-600" title="সম্পাদনা"><Edit2 className="w-4 h-4" /></button><button onClick={() => handleDelete(s.id)} className="btn-icon hover:bg-red-50 text-red-600" title="মুছুন"><Trash2 className="w-4 h-4" /></button></div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filtered.length === 0 && <div className="text-center py-12"><BookOpen className="w-12 h-12 text-gray-300 mx-auto mb-3" /><p className="text-gray-400 font-medium">কোনো শিডিউল পাওয়া যায়নি</p></div>}
      </div>
    </div>
  );
}
