// Client-side PDF export for the Schedules page.
// Uses jsPDF + jspdf-autotable with an embedded Noto Sans Bengali TTF so that
// Bengali text renders correctly in the resulting PDF.

import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
// `?url` returns the asset URL at build time but Vite emits the binary into
// the bundle (it is then re-fetched at runtime the same way dynamic assets
// work). To avoid any network failure — including misconfigured SPA rewrites
// on Render Static Sites — we read the TTF as raw text via `?raw` and
// base64-encode it in-place. This guarantees the font is always available
// regardless of how the static site is hosted.
import notoSansBengaliRegularUrl from '../../public/fonts/NotoSansBengali-Regular.ttf?url';
import notoSansBengaliBoldUrl from '../../public/fonts/NotoSansBengali-Bold.ttf?url';
import notoSansBengaliRegularRaw from '../../public/fonts/NotoSansBengali-Regular.ttf?raw';
import notoSansBengaliBoldRaw from '../../public/fonts/NotoSansBengali-Bold.ttf?raw';
import {
  formatTime,
  bengaliNumber,
  directionLabel,
  daysLabel,
  statusLabel,
  audienceLabel,
  bnDateTime,
  isTeacherBus,
} from './format';

const FONT_REGULAR = 'NotoSansBengali';
const FONT_BOLD = 'NotoSansBengaliBold';
const PRIMARY_RGB = [20, 184, 166]; // tailwind teal-500
const TEXT_RGB = [31, 41, 55]; // gray-800
const MUTED_RGB = [107, 114, 128]; // gray-500
const DANGER_RGB = [220, 38, 38]; // red-600
const BORDER_RGB = [229, 231, 235]; // gray-200
const SECTION_BG = [240, 253, 250]; // teal-50

// Vite's `?raw` returns the file as a UTF-8 string. TTF bytes are arbitrary
// (0x00..0xFF) so we round-trip via a binary read of the underlying URL.
// This keeps the TTF bytes intact while only requiring one round-trip on
// first module evaluation.
const loadFontAsBase64 = async (url) => {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Font fetch failed (${response.status}) for ${url}`);
  }
  const buffer = await response.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  const chunkSize = 0x8000;
  let binary = '';
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
};

// Use the raw (base64-encoded) import synchronously so the PDF generation
// itself is sync after first export. `?raw` in Vite is already a UTF-8 string
// representation of the file bytes, but for binary safety we still decode via
// fetch with the resolved asset URL. The inline raw is kept as a fallback so
// this still works in test environments where the asset URL cannot be fetched.
let fontBase64Cache = null;

const getFontBase64 = async () => {
  if (fontBase64Cache) return fontBase64Cache;

  // Prefer the asset URL fetch — preserves binary integrity.
  try {
    const [regular, bold] = await Promise.all([
      loadFontAsBase64(notoSansBengaliRegularUrl),
      loadFontAsBase64(notoSansBengaliBoldUrl),
    ]);
    fontBase64Cache = { regular, bold };
    return fontBase64Cache;
  } catch (urlErr) {
    console.warn('Font fetch by URL failed, falling back to inline ?raw import.', urlErr);
    // Fallback: ?raw returns base64 string already (Vite 5+ detects .ttf as binary).
    fontBase64Cache = {
      regular: notoSansBengaliRegularRaw,
      bold: notoSansBengaliBoldRaw,
    };
    return fontBase64Cache;
  }
};

const groupByTime = (schedules) => {
  const groups = new Map();
  schedules.forEach((schedule) => {
    const key = schedule.departureTime || '99:99';
    groups.set(key, [...(groups.get(key) || []), schedule]);
  });
  return [...groups.entries()].sort(([first], [second]) => first.localeCompare(second));
};

// Split schedules by day-group (weekday vs weekend) and then by audience
// (student vs teacher/officer). Yields a nested structure that mirrors the
// four UI filter combinations.
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

// Top-level sections appear in this order so the PDF reads naturally:
// কর্মদিবস (student → teacher) → শুক্র-শনি (student → teacher).
const SECTION_LAYOUT = [
  { dayKey: 'weekday', dayLabel: 'কর্মদিবস (রবিবার–বৃহস্পতিবার)' },
  { dayKey: 'weekend', dayLabel: 'শুক্রবার ও শনিবার' },
];
const AUDIENCE_LAYOUT = [
  { audKey: 'student', audLabel: 'শিক্ষার্থী বাস' },
  { audKey: 'teacher', audLabel: 'শিক্ষক / কর্মকর্তা / কর্মচারী বাস' },
];

const buildRow = (schedule) => {
  const startPoint = schedule.startPoint || '—';
  const endPoint = schedule.endPoint || '—';
  const route = `${startPoint} → ${endPoint}`;
  return [
    `${schedule.busNumber}${schedule.busName ? ` (${schedule.busName})` : ''}`,
    directionLabel(schedule.direction),
    route,
    formatTime(schedule.arrivalTime) === 'নির্ধারিত নয়' ? '—' : formatTime(schedule.arrivalTime),
    daysLabel(schedule.days),
    statusLabel(schedule),
  ];
};

const buildFileName = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const stamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}`;
  return `bus-schedules-${stamp}.pdf`;
};

const writeHeader = (doc, { scope, filters, totalCount }) => {
  const pageWidth = doc.internal.pageSize.getWidth();

  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(16);
  doc.setTextColor(...TEXT_RGB);
  doc.text('CoU Bus Tracker — বাস শিডিউল', 12, 16);

  doc.setFont(FONT_REGULAR, 'normal');
  doc.setFontSize(10);
  doc.setTextColor(...MUTED_RGB);
  doc.text(`তৈরি: ${bnDateTime()}`, 12, 23);

  const filterLine =
    scope === 'all'
      ? 'সব শিডিউল'
      : `ফিল্টার: ${audienceLabel(filters.busAudience)} · ${
          filters.dayGroup === 'WEEKEND' ? 'শুক্রবার ও শনিবার' : 'কর্মদিবস'
        } · ${
          filters.statusFilter === 'ACTIVE' ? 'সক্রিয়' : filters.statusFilter === 'INACTIVE' ? 'নিষ্ক্রিয়' : 'সব'
        }`;
  doc.text(filterLine, 12, 30);

  doc.setFont(FONT_BOLD, 'bold');
  doc.setTextColor(...TEXT_RGB);
  doc.text(`মোট শিডিউল: ${bengaliNumber.format(totalCount)}`, 12, 37);

  // Accent bar.
  doc.setDrawColor(...PRIMARY_RGB);
  doc.setLineWidth(0.6);
  doc.line(12, 41, pageWidth - 12, 41);
};

const writeFooter = (doc) => {
  const pageCount = doc.internal.getNumberOfPages();
  for (let i = 1; i <= pageCount; i += 1) {
    doc.setPage(i);
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    doc.setFont(FONT_REGULAR, 'normal');
    doc.setFontSize(9);
    doc.setTextColor(...MUTED_RGB);
    doc.text(
      `পৃষ্ঠা ${bengaliNumber.format(i)} / ${bengaliNumber.format(pageCount)}`,
      pageWidth - 12,
      pageHeight - 8,
      { align: 'right' },
    );
  }
};

const truncate = (text, max = 80) => (text.length > max ? `${text.slice(0, max - 1)}…` : text);

// A4 landscape printable height ≈ 175 mm after margins.
const PAGE_BODY_LIMIT = 175;
const TOP_MARGIN = 48;
const BOTTOM_MARGIN = 16;

const ensureRoom = (doc, needed) => {
  const pageHeight = doc.internal.pageSize.getHeight();
  const limit = pageHeight - BOTTOM_MARGIN;
  if (doc.lastAutoTable?.finalY) {
    return doc.lastAutoTable.finalY + needed <= limit;
  }
  return TOP_MARGIN + needed <= limit;
};

const addPageIfNeeded = (doc, needed) => {
  if (!ensureRoom(doc, needed)) {
    doc.addPage();
    return TOP_MARGIN;
  }
  return doc.lastAutoTable ? doc.lastAutoTable.finalY + 6 : TOP_MARGIN;
};

const drawSectionHeading = (doc, { title, count }, cursorY) => {
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(13);
  doc.setTextColor(...PRIMARY_RGB);
  doc.text(title, 12, cursorY);
  doc.setFont(FONT_REGULAR, 'normal');
  doc.setFontSize(9);
  doc.setTextColor(...MUTED_RGB);
  doc.text(`${bengaliNumber.format(count)}টি শিডিউল`, 12, cursorY + 5);
  // Underline.
  const pageWidth = doc.internal.pageSize.getWidth();
  doc.setDrawColor(...PRIMARY_RGB);
  doc.setLineWidth(0.3);
  doc.line(12, cursorY + 7, pageWidth - 12, cursorY + 7);
  return cursorY + 11;
};

const drawAudienceHeading = (doc, label, cursorY) => {
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(11);
  doc.setTextColor(...TEXT_RGB);
  doc.text(label, 12, cursorY);
  return cursorY + 6;
};

const drawTimeHeading = (doc, label, cursorY) => {
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(10);
  doc.setTextColor(...PRIMARY_RGB);
  doc.text(label, 12, cursorY);
  return cursorY + 4;
};

const renderTable = (doc, group, cursorY) => {
  const startY = cursorY + 2;
  autoTable(doc, {
    startY,
    margin: { left: 12, right: 12 },
    styles: {
      font: FONT_REGULAR,
      fontSize: 9,
      cellPadding: 2,
      overflow: 'linebreak',
      lineColor: BORDER_RGB,
      lineWidth: 0.1,
      textColor: TEXT_RGB,
    },
    headStyles: {
      font: FONT_BOLD,
      fontStyle: 'bold',
      fillColor: PRIMARY_RGB,
      textColor: [255, 255, 255],
      fontSize: 9,
    },
    alternateRowStyles: { fillColor: SECTION_BG },
    columnStyles: {
      0: { cellWidth: 38 }, // বাস নম্বর
      1: { cellWidth: 30 }, // দিক
      2: { cellWidth: 'auto' }, // রুট
      3: { cellWidth: 28 }, // পৌঁছানোর সময়
      4: { cellWidth: 36 }, // দিন
      5: { cellWidth: 22 }, // অবস্থা
    },
    head: [['বাস নম্বর', 'দিক', 'রুট', 'পৌঁছানোর সময়', 'দিন', 'অবস্থা']],
    body: group.map((schedule) => buildRow(schedule).map((cell) => truncate(cell))),
    didParseCell: (data) => {
      if (data.section === 'body' && data.column.index === 5) {
        const isInactive = group[data.row.index]?.isActive === false;
        if (isInactive) {
          data.cell.styles.textColor = DANGER_RGB;
          data.cell.styles.fontStyle = 'bold';
        }
      }
    },
    didDrawPage: () => {
      const pageWidth = doc.internal.pageSize.getWidth();
      doc.setDrawColor(...PRIMARY_RGB);
      doc.setLineWidth(0.4);
      doc.line(12, 12, pageWidth - 12, 12);
    },
  });
};

const sortGroup = (group) =>
  [...group].sort((a, b) => String(a.busNumber || '').localeCompare(String(b.busNumber || '')));

export async function exportSchedulesToPDF({ schedules, scope, filters }) {
  if (!Array.isArray(schedules) || schedules.length === 0) {
    alert('নির্বাচিত স্কোপে কোনো শিডিউল নেই');
    return;
  }

  let fontBase64;
  try {
    fontBase64 = await getFontBase64();
  } catch (err) {
    console.error('PDF font load failed', err);
    alert('PDF ফন্ট লোড করা যায়নি');
    return;
  }

  const { regular: regularBase64, bold: boldBase64 } = fontBase64;

  const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
  doc.addFileToVFS('NotoSansBengali-Regular.ttf', regularBase64);
  doc.addFont('NotoSansBengali-Regular.ttf', FONT_REGULAR, 'normal');
  doc.addFileToVFS('NotoSansBengali-Bold.ttf', boldBase64);
  doc.addFont('NotoSansBengali-Bold.ttf', FONT_BOLD, 'bold');

  writeHeader(doc, { scope, filters, totalCount: schedules.length });

  const buckets = partitionByGroup(schedules);
  let cursorY = TOP_MARGIN;

  SECTION_LAYOUT.forEach(({ dayKey, dayLabel }) => {
    const totalInSection =
      buckets[dayKey].student.length + buckets[dayKey].teacher.length;
    if (totalInSection === 0) return;

    cursorY = addPageIfNeeded(doc, 18);
    cursorY = drawSectionHeading(doc, { title: dayLabel, count: totalInSection }, cursorY);

    AUDIENCE_LAYOUT.forEach(({ audKey, audLabel }) => {
      const group = sortGroup(buckets[dayKey][audKey]);
      if (group.length === 0) return;

      cursorY = addPageIfNeeded(doc, 14);
      cursorY = drawAudienceHeading(doc, `${audLabel} (${bengaliNumber.format(group.length)}টি বাস)`, cursorY);

      const timeGroups = groupByTime(group);
      timeGroups.forEach(([time, slot]) => {
        const estimatedRowHeight = 8 + slot.length * 7;
        cursorY = addPageIfNeeded(doc, estimatedRowHeight + 10);
        cursorY = drawTimeHeading(doc, `${formatTime(time)} (${bengaliNumber.format(slot.length)}টি বাস)`, cursorY);
        renderTable(doc, slot, cursorY);
        cursorY = doc.lastAutoTable.finalY + 6;
      });
    });
  });

  writeFooter(doc);
  doc.save(buildFileName());
}
