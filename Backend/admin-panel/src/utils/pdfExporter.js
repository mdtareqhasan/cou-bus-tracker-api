// Client-side PDF export for the Schedules page.
//
// Approach: render the report inside a hidden HTML element using the same
// font stack the app already loads (Noto Sans Bengali via Google Fonts), then
// capture it with html2canvas and embed each canvas page as an image in a
// jsPDF document. This avoids the jsPDF limitations on OpenType glyph
// substitution that caused Bengali conjuncts to render as broken letter pairs.

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

const REPORT_BG = '#ffffff';
const TEAL_50 = '#f0fdfa';
const TEAL_100 = '#ccfbf1';
const TEAL_500 = '#14b8a6';
const TEAL_600 = '#0d9488';
const TEAL_700 = '#0f766e';
const GRAY_50 = '#f9fafb';
const GRAY_200 = '#e5e7eb';
const GRAY_300 = '#d1d5db';
const GRAY_500 = '#6b7280';
const GRAY_600 = '#4b5563';
const GRAY_700 = '#374151';
const GRAY_800 = '#1f2937';
const GRAY_900 = '#111827';
const RED_600 = '#dc2626';
const GREEN_600 = '#059669';

const partitionByGroup = (schedules) => {
  const isWeekend = (s) => s.days === 'FRI-SAT';
  const buckets = {
    weekday: { student: [], teacher: [] },
    weekend: { student: [], teacher: [] },
  };
  schedules.forEach((s) => {
    const dayKey = isWeekend(s) ? 'weekend' : 'weekday';
    const audKey = isTeacherBus(s) ? 'teacher' : 'student';
    buckets[dayKey][audKey].push(s);
  });
  return buckets;
};

const groupByTime = (schedules) => {
  const groups = new Map();
  schedules.forEach((schedule) => {
    const key = schedule.departureTime || '99:99';
    groups.set(key, [...(groups.get(key) || []), schedule]);
  });
  return [...groups.entries()].sort(([first], [second]) => first.localeCompare(second));
};

const dash = '—';

const buildFileName = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const stamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}`;
  return `bus-schedules-${stamp}.pdf`;
};

const SECTION_LAYOUT = [
  { dayKey: 'weekday', dayLabel: 'কর্মদিবস (রবিবার–বৃহস্পতিবার)' },
  { dayKey: 'weekend', dayLabel: 'শুক্রবার ও শনিবার' },
];
const AUDIENCE_LAYOUT = [
  { audKey: 'student', audLabel: 'শিক্ষার্থী বাস', icon: '🚌' },
  { audKey: 'teacher', audLabel: 'শিক্ষক / কর্মকর্তা / কর্মচারী বাস', icon: '👨‍🏫' },
];

const sortGroup = (group) =>
  [...group].sort((a, b) => {
    const ta = isTeacherBus(a) ? 1 : 0;
    const tb = isTeacherBus(b) ? 1 : 0;
    if (ta !== tb) return ta - tb;
    return String(a.busNumber || '').localeCompare(String(b.busNumber || ''), 'en', { numeric: true });
  });

// Render a single time-slot group as HTML. We keep the DOM dense and use
// minimal CSS so html2canvas can paint it quickly.
const renderGroupHTML = (group) => {
  const rows = sortGroup(group)
    .map((s) => {
      const busNumber = (s.busNumber ?? '').toString().trim() || dash;
      const busName = (s.busName ?? '').toString().trim();
      const busCell = busName ? `${busNumber} (${busName})` : busNumber;
      const startPoint = (s.startPoint ?? '').toString().trim() || dash;
      const endPoint = (s.endPoint ?? '').toString().trim() || dash;
      const arrival = s.arrivalTime ? formatTime(s.arrivalTime) : dash;
      const isInactive = s.isActive === false;
      const statusColor = isInactive ? RED_600 : GREEN_600;
      const statusWeight = isInactive ? 700 : 600;
      return `
        <tr>
          <td style="font-weight:700;padding:6px 10px;border:1px solid ${GRAY_200};">${busCell}</td>
          <td style="padding:6px 10px;border:1px solid ${GRAY_200};">${directionLabel(s.direction)}</td>
          <td style="padding:6px 10px;border:1px solid ${GRAY_200};">${startPoint} → ${endPoint}</td>
          <td style="padding:6px 10px;border:1px solid ${GRAY_200};text-align:center;">${arrival}</td>
          <td style="padding:6px 10px;border:1px solid ${GRAY_200};text-align:center;">${daysLabel(s.days)}</td>
          <td style="padding:6px 10px;border:1px solid ${GRAY_200};text-align:center;color:${statusColor};font-weight:${statusWeight};">${statusLabel(s)}</td>
        </tr>`;
    })
    .join('');
  return `
    <table style="width:100%;border-collapse:collapse;font-size:13px;margin-bottom:6px;">
      <thead>
        <tr style="background:${TEAL_600};color:white;">
          <th style="padding:8px 10px;border:1px solid ${TEAL_700};text-align:left;">বাস নম্বর</th>
          <th style="padding:8px 10px;border:1px solid ${TEAL_700};text-align:left;">দিক</th>
          <th style="padding:8px 10px;border:1px solid ${TEAL_700};text-align:left;">রুট</th>
          <th style="padding:8px 10px;border:1px solid ${TEAL_700};text-align:center;">পৌঁছানোর সময়</th>
          <th style="padding:8px 10px;border:1px solid ${TEAL_700};text-align:center;">দিন</th>
          <th style="padding:8px 10px;border:1px solid ${TEAL_700};text-align:center;">অবস্থা</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>`;
};

const renderReportHTML = ({ schedules, scope, filters }) => {
  const buckets = partitionByGroup(schedules);
  const sections = [];

  SECTION_LAYOUT.forEach(({ dayKey, dayLabel }) => {
    const totalInSection = buckets[dayKey].student.length + buckets[dayKey].teacher.length;
    if (totalInSection === 0) return;

    const audienceBlocks = AUDIENCE_LAYOUT
      .map(({ audKey, audLabel, icon }) => {
        const group = sortGroup(buckets[dayKey][audKey]);
        if (group.length === 0) return '';
        const timeGroups = groupByTime(group)
          .map(
            ([time, slot]) => `
              <div style="margin-top:14px;">
                <div style="font-weight:700;color:${TEAL_700};font-size:14px;margin-bottom:4px;">
                  ⏰ ${formatTime(time)} — ${bengaliNumber.format(slot.length)}টি বাস
                </div>
                ${renderGroupHTML(slot)}
              </div>`,
          )
          .join('');
        return `
          <div style="margin-top:18px;">
            <div style="display:flex;align-items:center;gap:8px;padding-bottom:6px;border-bottom:2px solid ${TEAL_100};">
              <span style="font-size:16px;">${icon}</span>
              <span style="font-weight:700;color:${GRAY_900};font-size:16px;">${audLabel}</span>
              <span style="color:${GRAY_500};font-size:13px;">(${bengaliNumber.format(group.length)}টি বাস)</span>
            </div>
            ${timeGroups}
          </div>`;
      })
      .join('');

    sections.push(`
      <section style="margin-top:24px;">
        <div style="background:${TEAL_600};color:white;padding:10px 16px;border-radius:8px;display:flex;justify-content:space-between;align-items:center;">
          <span style="font-size:17px;font-weight:700;">${dayLabel}</span>
          <span style="font-size:13px;opacity:0.95;">${bengaliNumber.format(totalInSection)}টি শিডিউল</span>
        </div>
        ${audienceBlocks}
      </section>`);
  });

  const filterLine =
    scope === 'all'
      ? 'স্কোপ: সব শিডিউল'
      : `ফিল্টার: ${filters.busAudience === 'TEACHER' ? 'শিক্ষক/কর্মকর্তা বাস' : 'শিক্ষার্থী বাস'} · ${
          filters.dayGroup === 'WEEKEND' ? 'শুক্রবার ও শনিবার' : 'কর্মদিবস'
        } · ${
          filters.statusFilter === 'ACTIVE' ? 'সক্রিয়' : filters.statusFilter === 'INACTIVE' ? 'নিষ্ক্রিয়' : 'সব'
        }`;

  return `
    <div id="pdf-report-root" style="
      font-family: 'Noto Sans Bengali', 'Hind Siliguri', 'Kalpurush', 'SolaimanLipi', 'Bangla', sans-serif;
      width: 1123px;
      padding: 36px 40px;
      background: ${REPORT_BG};
      color: ${GRAY_900};
      box-sizing: border-box;
    ">
      <header style="
        background: ${TEAL_600};
        color: white;
        padding: 24px 28px;
        border-radius: 12px;
        margin-bottom: 18px;
        display: flex;
        align-items: center;
        justify-content: space-between;
      ">
        <div>
          <div style="font-size:24px;font-weight:700;letter-spacing:0.2px;">CoU Bus Tracker — বাস শিডিউল</div>
          <div style="font-size:13px;opacity:0.85;margin-top:4px;">Comilla University Bus Schedule Report</div>
        </div>
        <div style="text-align:right;font-size:12px;opacity:0.9;line-height:1.5;">
          <div>তৈরি: ${bnDateTime()}</div>
          <div>${filterLine}</div>
          <div style="font-weight:700;font-size:14px;margin-top:2px;">মোট শিডিউল: ${bengaliNumber.format(schedules.length)}</div>
        </div>
      </header>
      ${sections.join('')}
      <footer style="margin-top:30px;border-top:1px solid ${GRAY_200};padding-top:14px;display:flex;justify-content:space-between;color:${GRAY_500};font-size:12px;">
        <span>CoU Bus Tracker — বাস শিডিউল</span>
        <span id="pdf-page-info">পৃষ্ঠা ১ / ১</span>
      </footer>
    </div>`;
};

// Inject the Noto Sans Bengali Google Font into the host document so the
// offscreen renderer can use it during html2canvas capture.
const ensureBengaliFontLink = () => {
  const href =
    'https://fonts.googleapis.com/css2?family=Noto+Sans+Bengali:wght@400;500;600;700&display=swap';
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
      document.fonts.ready.then(() => resolve());
    } else {
      // Best-effort: wait briefly then proceed.
      setTimeout(resolve, 1500);
    }
  });
};

// Build the offscreen render container.
const mountReportNode = (htmlString) => {
  const container = document.createElement('div');
  container.id = 'pdf-report-mount';
  container.style.position = 'fixed';
  container.style.left = '-100000px';
  container.style.top = '0';
  container.style.width = '1123px';
  container.style.background = REPORT_BG;
  container.style.pointerEvents = 'none';
  container.style.zIndex = '-1';
  container.innerHTML = htmlString;
  document.body.appendChild(container);
  return container;
};

// Slice a tall canvas into A4-landscape-friendly page chunks. Each page is
// 297mm × 210mm at 96dpi → ~1123 × 794 px. We slice vertically along the
// canvas height.
const A4_LANDSCAPE_PX = { width: 1123, height: 794 };

const sliceCanvasForPages = (canvas) => {
  const pages = [];
  const pageHeightPx = A4_LANDSCAPE_PX.height;
  let y = 0;
  while (y < canvas.height) {
    const h = Math.min(pageHeightPx, canvas.height - y);
    const pageCanvas = document.createElement('canvas');
    pageCanvas.width = canvas.width;
    pageCanvas.height = h;
    const ctx = pageCanvas.getContext('2d');
    ctx.fillStyle = REPORT_BG;
    ctx.fillRect(0, 0, pageCanvas.width, h);
    ctx.drawImage(canvas, 0, y, canvas.width, h, 0, 0, canvas.width, h);
    pages.push(pageCanvas.toDataURL('image/png'));
    y += h;
  }
  return pages;
};

export async function exportSchedulesToPDF({ schedules, scope, filters }) {
  if (!Array.isArray(schedules) || schedules.length === 0) {
    alert('নির্বাচিত স্কোপে কোনো শিডিউল নেই');
    return;
  }

  // Surface a busy indicator on the button — it will be cleared after PDF save.
  let busyResolver;
  const busyPromise = new Promise((resolve) => {
    busyResolver = resolve;
  });
  // The button uses its own loading state; here we just await font readiness.
  await ensureBengaliFontLink();

  const mount = mountReportNode(renderReportHTML({ schedules, scope, filters }));

  let canvas;
  try {
    canvas = await html2canvas(mount, {
      scale: 1,
      backgroundColor: REPORT_BG,
      useCORS: true,
      logging: false,
      windowWidth: 1123,
      windowHeight: mount.scrollHeight,
    });
  } catch (err) {
    console.error('html2canvas failed', err);
    document.body.removeChild(mount);
    alert('PDF রিপোর্ট রেন্ডার করা যায়নি');
    if (busyResolver) busyResolver();
    return;
  }

  document.body.removeChild(mount);

  const pageImages = sliceCanvasForPages(canvas);
  const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });

  pageImages.forEach((dataUrl, idx) => {
    if (idx > 0) doc.addPage();
    doc.addImage(dataUrl, 'PNG', 0, 0, 297, 210, undefined, 'FAST');
    doc.setFontSize(8);
    doc.setTextColor(GRAY_500);
    doc.text(`পৃষ্ঠা ${bengaliNumber.format(idx + 1)} / ${bengaliNumber.format(pageImages.length)}`, 297 - 12, 210 - 4, { align: 'right' });
  });

  doc.save(buildFileName());
  if (busyResolver) busyResolver();
}
