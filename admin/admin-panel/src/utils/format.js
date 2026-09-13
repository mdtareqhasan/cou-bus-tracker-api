// Shared formatting helpers used by both the Schedules page and the PDF exporter.

export const bengaliNumber = new Intl.NumberFormat('bn-BD');

export const formatTime = (time) => {
  if (!time) return 'নির্ধারিত নয়';
  const [hour, minute] = time.split(':').map(Number);
  const period = hour < 12 ? 'সকাল' : hour < 16 ? 'দুপুর' : hour < 19 ? 'বিকাল' : 'রাত';
  const hour12 = hour % 12 || 12;
  return `${period} ${bengaliNumber.format(hour12)}:${bengaliNumber.format(minute).padStart(2, '০')}`;
};

export const routeParts = (route = '') => {
  const points = route.replaceAll('â†’', '→').split(/\s*(?:→|>|->|–|-)\s*/).filter(Boolean);
  return { startPoint: points[0] || '', endPoint: points.slice(1).join(' → ') || '' };
};

export const isTeacherBus = (schedule) =>
  ['TEACHER', 'OFFICER', 'STAFF'].includes(schedule.category?.toUpperCase());

// Friendly Bengali label for a filter audience value.
export const audienceLabel = (audience) =>
  audience === 'TEACHER' ? 'শিক্ষক/কর্মকর্তা বাস' : 'শিক্ষার্থী বাস';

export const dayGroupLabel = (dayGroup) =>
  dayGroup === 'WEEKEND' ? 'শুক্রবার ও শনিবার' : 'কর্মদিবস';

const DAY_LABELS = {
  'SAT-THU': 'শনিবার–বৃহস্পতিবার',
  'SUN-THU': 'রবিবার–বৃহস্পতিবার',
  'FRI-SAT': 'শুক্রবার–শনিবার',
  EVERYDAY: 'প্রতিদিন',
};

export const daysLabel = (days) => DAY_LABELS[days] || days || '—';

export const directionLabel = (direction) =>
  direction === 'UP' ? 'ক্যাম্পাস অভিমুখে' : direction === 'DOWN' ? 'ক্যাম্পাস থেকে' : '—';

export const statusLabel = (schedule) =>
  schedule.isActive === false ? 'নিষ্ক্রিয়' : 'সক্রিয়';

// Bengali long-form date+time stamp used in PDF headers.
export const bnDateTime = (date = new Date()) => {
  const fmt = new Intl.DateTimeFormat('bn-BD', {
    dateStyle: 'long',
    timeStyle: 'short',
  });
  // Normalize any Arabic digits returned by Intl to Bengali digits for consistency.
  return fmt.format(date).replace(/[0-9]/g, (d) => '০১২৩৪৫৬৭৮৯'[Number(d)]);
};

export const formatTotalCount = (n) => bengaliNumber.format(n);
