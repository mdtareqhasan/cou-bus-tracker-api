import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';
import {
  formatTime,
  bengaliNumber,
  directionLabel,
  daysLabel,
  statusLabel,
  bnDateTime,
  isTeacherBus,
} from './format';

const BG = '#ffffff';
const TEAL_50 = '#f0fdfa';
const TEAL_200 = '#99f6e4';
const TEAL_600 = '#0d9488';
const TEAL_700 = '#0f766e';
const TEAL_800 = '#115e59';
const GRAY_100 = '#f3f4f6';
const GRAY_200 = '#e5e7eb';
const GRAY_400 = '#9ca3af';
const GRAY_500 = '#6b7280';
const GRAY_700 = '#374151';
const GRAY_900 = '#111827';
const RED_500 = '#ef4444';
const GREEN_600 = '#059669';
const WHITE = '#ffffff';

const DASH = '\u2014';

const partitionByGroup = (schedules) => {
  const buckets = {
    weekday: { student: [], teacher: [] },
    weekend: { student: [], teacher: [] },
  };
  schedules.forEach((s) => {
    const dayKey = s.days === 'FRI-SAT' ? 'weekend' : 'weekday';
    const audKey = isTeacherBus(s) ? 'teacher' : 'student';
    buckets[dayKey][audKey].push(s);
  });
  return buckets;
};

const groupByTime = (schedules) => {
  const groups = new Map();
  schedules.forEach((s) => {
    const key = s.departureTime || '99:99';
    groups.set(key, [...(groups.get(key) || []), s]);
  });
  return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
};

const sortGroup = (group) =>
  [...group].sort((a, b) => {
    const ta = isTeacherBus(a) ? 1 : 0;
    const tb = isTeacherBus(b) ? 1 : 0;
    if (ta !== tb) return ta - tb;
    return String(a.busNumber || '').localeCompare(String(b.busNumber || ''), 'en', { numeric: true });
  });

const buildFileName = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `bus-schedules-${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}.pdf`;
};

const renderTable = (items) => {
  const rows = sortGroup(items)
    .map((s, i) => {
      const bg = i % 2 === 0 ? WHITE : GRAY_100;
      const busName = (s.busName ?? '').toString().trim();
      const busCell = busName ? `${s.busNumber} (${busName})` : s.busNumber || DASH;
      const route = `${s.startPoint || DASH} \u2192 ${s.endPoint || DASH}`;
      const arrival = s.arrivalTime ? formatTime(s.arrivalTime) : DASH;
      const isOff = s.isActive === false;
      return `
        <tr style="background:${bg};">
          <td style="padding:5px 8px;border-bottom:1px solid ${GRAY_200};font-weight:600;font-size:11px;color:${GRAY_900};">${busCell}</td>
          <td style="padding:5px 8px;border-bottom:1px solid ${GRAY_200};font-size:11px;color:${GRAY_700};">${directionLabel(s.direction)}</td>
          <td style="padding:5px 8px;border-bottom:1px solid ${GRAY_200};font-size:11px;color:${GRAY_700};">${route}</td>
          <td style="padding:5px 8px;border-bottom:1px solid ${GRAY_200};font-size:11px;text-align:center;color:${GRAY_700};">${arrival}</td>
          <td style="padding:5px 8px;border-bottom:1px solid ${GRAY_200};font-size:10px;text-align:center;color:${GRAY_500};">${daysLabel(s.days)}</td>
          <td style="padding:5px 8px;border-bottom:1px solid ${GRAY_200};text-align:center;">
            <span style="display:inline-block;padding:1px 6px;border-radius:3px;font-size:10px;font-weight:600;background:${isOff ? '#fef2f2' : '#ecfdf5'};color:${isOff ? RED_500 : GREEN_600};">${statusLabel(s)}</span>
          </td>
        </tr>`;
    })
    .join('');

  return `
    <table style="width:100%;border-collapse:collapse;font-size:11px;margin-top:4px;">
      <thead>
        <tr style="background:${TEAL_700};color:${WHITE};">
          <th style="padding:6px 8px;text-align:left;font-weight:600;font-size:10px;">বাস নম্বর</th>
          <th style="padding:6px 8px;text-align:left;font-weight:600;font-size:10px;">দিক</th>
          <th style="padding:6px 8px;text-align:left;font-weight:600;font-size:10px;">রুট</th>
          <th style="padding:6px 8px;text-align:center;font-weight:600;font-size:10px;">পৌঁছানোর সময়</th>
          <th style="padding:6px 8px;text-align:center;font-weight:600;font-size:10px;">দিন</th>
          <th style="padding:6px 8px;text-align:center;font-weight:600;font-size:10px;">অবস্থা</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>`;
};

const renderTimeGroup = (time, slot) => `
  <div style="margin-top:10px;">
    <div style="display:flex;align-items:center;gap:6px;margin-bottom:2px;">
      <div style="width:6px;height:6px;border-radius:50%;background:${TEAL_600};"></div>
      <span style="font-weight:700;color:${TEAL_700};font-size:12px;">${formatTime(time)}</span>
      <span style="color:${GRAY_400};font-size:10px;margin-left:2px;">(${bengaliNumber.format(slot.length)}টি বাস)</span>
    </div>
    ${renderTable(slot)}
  </div>`;

const renderAudienceSection = (audLabel, icon, group) => {
  if (group.length === 0) return '';
  const timeGroups = groupByTime(group)
    .map(([time, slot]) => renderTimeGroup(time, slot))
    .join('');

  return `
    <div style="margin-top:14px;">
      <div style="display:flex;align-items:center;gap:6px;padding-bottom:5px;border-bottom:2px solid ${TEAL_200};">
        <span style="font-size:13px;">${icon}</span>
        <span style="font-weight:700;color:${GRAY_900};font-size:13px;">${audLabel}</span>
        <span style="color:${GRAY_400};font-size:10px;margin-left:2px;">\u2022 ${bengaliNumber.format(group.length)}টি</span>
      </div>
      ${timeGroups}
    </div>`;
};

const renderSection = (dayKey, dayLabel, buckets) => {
  const total = buckets[dayKey].student.length + buckets[dayKey].teacher.length;
  if (total === 0) return '';

  const audienceBlocks = [
    renderAudienceSection('শিক্ষার্থী বাস', '\uD83D\uDE8C', buckets[dayKey].student),
    renderAudienceSection('শিক্ষক / কর্মকর্তা / কর্মচারী বাস', '\uD83D\uDC68\u200D\uD83C\uDFEB', buckets[dayKey].teacher),
  ].join('');

  return `
    <div style="margin-top:18px;page-break-inside:avoid;">
      <div style="background:${TEAL_600};color:${WHITE};padding:8px 14px;border-radius:6px;display:flex;justify-content:space-between;align-items:center;">
        <span style="font-size:13px;font-weight:700;">${dayLabel}</span>
        <span style="font-size:10px;opacity:0.9;">${bengaliNumber.format(total)}টি শিডিউল</span>
      </div>
      ${audienceBlocks}
    </div>`;
};

const renderReportHTML = ({ schedules, scope, filters }) => {
  const buckets = partitionByGroup(schedules);

  const weekdayCount = buckets.weekday.student.length + buckets.weekday.teacher.length;
  const weekendCount = buckets.weekend.student.length + buckets.weekend.teacher.length;
  const activeCount = schedules.filter((s) => s.isActive !== false).length;
  const inactiveCount = schedules.length - activeCount;

  const filterLine =
    scope === 'all'
      ? 'সব শিডিউল (ডাটাবেস থেকে)'
      : `${filters.busAudience === 'TEACHER' ? 'শিক্ষক/কর্মকর্তা' : 'শিক্ষার্থী'} \u00B7 ${filters.dayGroup === 'WEEKEND' ? 'শুক্র-শনি' : 'কর্মদিবস'} \u00B7 ${filters.statusFilter === 'ACTIVE' ? 'সক্রিয়' : filters.statusFilter === 'INACTIVE' ? 'নিষ্ক্রিয়' : 'সব'}`;

  return `
    <div id="pdf-report-root" style="
      font-family: 'Noto Sans Bengali', 'Hind Siliguri', 'SolaimanLipi', sans-serif;
      width: 1000px;
      padding: 24px 28px;
      background: ${BG};
      color: ${GRAY_900};
      box-sizing: border-box;
    ">
      <header style="
        background: linear-gradient(135deg, ${TEAL_600}, ${TEAL_800});
        color: ${WHITE};
        padding: 16px 22px;
        border-radius: 10px;
        display: flex;
        justify-content: space-between;
        align-items: center;
      ">
        <div>
          <div style="font-size:18px;font-weight:700;">CoU Bus Tracker</div>
          <div style="font-size:11px;opacity:0.85;margin-top:2px;">বাস শিডিউল রিপোর্ট</div>
        </div>
        <div style="text-align:right;font-size:10px;opacity:0.9;line-height:1.5;">
          <div>তৈরি: ${bnDateTime()}</div>
          <div>${filterLine}</div>
        </div>
      </header>

      <div style="display:flex;gap:8px;margin-top:12px;">
        <div style="flex:1;background:${TEAL_50};border:1px solid ${TEAL_200};border-radius:6px;padding:8px 10px;text-align:center;">
          <div style="font-size:18px;font-weight:700;color:${TEAL_700};">${bengaliNumber.format(schedules.length)}</div>
          <div style="font-size:9px;color:${GRAY_500};margin-top:1px;">মোট</div>
        </div>
        <div style="flex:1;background:#ecfdf5;border:1px solid #a7f3d0;border-radius:6px;padding:8px 10px;text-align:center;">
          <div style="font-size:18px;font-weight:700;color:${GREEN_600};">${bengaliNumber.format(activeCount)}</div>
          <div style="font-size:9px;color:${GRAY_500};margin-top:1px;">সক্রিয়</div>
        </div>
        <div style="flex:1;background:#fef2f2;border:1px solid #fecaca;border-radius:6px;padding:8px 10px;text-align:center;">
          <div style="font-size:18px;font-weight:700;color:${RED_500};">${bengaliNumber.format(inactiveCount)}</div>
          <div style="font-size:9px;color:${GRAY_500};margin-top:1px;">নিষ্ক্রিয়</div>
        </div>
        <div style="flex:1;background:#eff6ff;border:1px solid #bfdbfe;border-radius:6px;padding:8px 10px;text-align:center;">
          <div style="font-size:18px;font-weight:700;color:#2563eb;">${bengaliNumber.format(weekdayCount)}</div>
          <div style="font-size:9px;color:${GRAY_500};margin-top:1px;">কর্মদিবস</div>
        </div>
        <div style="flex:1;background:#fefce8;border:1px solid #fde68a;border-radius:6px;padding:8px 10px;text-align:center;">
          <div style="font-size:18px;font-weight:700;color:#d97706;">${bengaliNumber.format(weekendCount)}</div>
          <div style="font-size:9px;color:${GRAY_500};margin-top:1px;">শুক্র-শনি</div>
        </div>
      </div>

      ${renderSection('weekday', 'কর্মদিবস (রবিবার \u2013 বৃহস্পতিবার)', buckets)}
      ${renderSection('weekend', 'শুক্রবার ও শনিবার', buckets)}

      <footer style="margin-top:20px;border-top:1px solid ${GRAY_200};padding-top:8px;display:flex;justify-content:space-between;color:${GRAY_400};font-size:9px;">
        <span>CoU Bus Tracker \u2014 বাস শিডিউল</span>
        <span id="pdf-page-info"></span>
      </footer>
    </div>`;
};

const ensureBengaliFont = () => {
  const href = 'https://fonts.googleapis.com/css2?family=Noto+Sans+Bengali:wght@400;500;600;700&display=swap';
  let link = document.querySelector(`link[data-pdf-font="${href}"]`);
  if (!link) {
    link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = href;
    link.setAttribute('data-pdf-font', href);
    document.head.appendChild(link);
  }
  return new Promise((resolve) => {
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(resolve);
    } else {
      setTimeout(resolve, 1500);
    }
  });
};

const mountReportNode = (htmlString) => {
  const container = document.createElement('div');
  container.id = 'pdf-report-mount';
  container.style.cssText = `position:fixed;left:-100000px;top:0;width:1000px;background:${BG};pointer-events:none;z-index:-1;`;
  container.innerHTML = htmlString;
  document.body.appendChild(container);
  return container;
};

const A4_PX = { width: 1000, height: 707 };

const sliceCanvas = (canvas) => {
  const pages = [];
  const h = A4_PX.height;
  let y = 0;
  while (y < canvas.height) {
    const sliceH = Math.min(h, canvas.height - y);
    const c = document.createElement('canvas');
    c.width = canvas.width;
    c.height = sliceH;
    const ctx = c.getContext('2d');
    ctx.fillStyle = BG;
    ctx.fillRect(0, 0, c.width, sliceH);
    ctx.drawImage(canvas, 0, y, canvas.width, sliceH, 0, 0, c.width, sliceH);
    pages.push(c.toDataURL('image/png'));
    y += sliceH;
  }
  return pages;
};

export async function exportSchedulesToPDF({ schedules, scope, filters }) {
  if (!Array.isArray(schedules) || schedules.length === 0) {
    alert('নির্বাচিত স্কোপে কোনো শিডিউল নেই');
    return;
  }

  await ensureBengaliFont();

  const mount = mountReportNode(renderReportHTML({ schedules, scope, filters }));

  let canvas;
  try {
    const fullHeight = Math.max(
      mount.scrollHeight,
      mount.getBoundingClientRect().height,
      mount.firstElementChild?.scrollHeight ?? 0,
    );
    canvas = await html2canvas(mount, {
      scale: 1.5,
      backgroundColor: BG,
      useCORS: true,
      logging: false,
      width: 1000,
      height: fullHeight,
      windowWidth: 1000,
      windowHeight: fullHeight,
    });
  } catch (err) {
    console.error('html2canvas failed', err);
    document.body.removeChild(mount);
    alert('PDF রিপোর্ট রেন্ডার করা যায়নি');
    return;
  }

  document.body.removeChild(mount);

  const pages = sliceCanvas(canvas);
  const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });

  pages.forEach((dataUrl, idx) => {
    if (idx > 0) doc.addPage();
    doc.addImage(dataUrl, 'PNG', 0, 0, 297, 210, undefined, 'FAST');
    doc.setFontSize(8);
    doc.setTextColor(GRAY_500);
    doc.text(`পৃষ্ঠা ${bengaliNumber.format(idx + 1)} / ${bengaliNumber.format(pages.length)}`, 297 - 12, 210 - 4, { align: 'right' });
  });

  doc.save(buildFileName());
}
