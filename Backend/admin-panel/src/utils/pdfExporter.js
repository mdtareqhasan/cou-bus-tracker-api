// Client-side PDF export for the Schedules page.
// Uses jsPDF + jspdf-autotable with an embedded Noto Sans Bengali TTF so that
// Bengali text renders correctly in the resulting PDF.

import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
// Vite `?url` emits the TTF as a hashed asset under dist/assets/ and gives us
// a stable URL at runtime — more reliable than a hard-coded /fonts/... path
// because Render Static Sites can rewrite unmatched paths to index.html.
import notoSansBengaliRegularUrl from '../../public/fonts/NotoSansBengali-Regular.ttf?url';
import notoSansBengaliBoldUrl from '../../public/fonts/NotoSansBengali-Bold.ttf?url';
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

// Color palette.
const PRIMARY_RGB = [13, 148, 136]; // teal-600
const PRIMARY_LIGHT_RGB = [240, 253, 250]; // teal-50
const PRIMARY_FAINT_RGB = [204, 251, 241]; // teal-100
const TEXT_RGB = [17, 24, 39]; // gray-900
const MUTED_RGB = [75, 85, 99]; // gray-600
const DANGER_RGB = [220, 38, 38]; // red-600
const SUCCESS_RGB = [5, 150, 105]; // emerald-600
const BORDER_RGB = [209, 213, 219]; // gray-300

// Module-level cache so the TTF is fetched at most once per page load.
let fontBase64Cache = null;

const arrayBufferToBase64 = (buffer) => {
  const bytes = new Uint8Array(buffer);
  const chunkSize = 0x8000;
  let binary = '';
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
};

const getFontBase64 = async () => {
  if (fontBase64Cache) return fontBase64Cache;
  const [regularResp, boldResp] = await Promise.all([
    fetch(notoSansBengaliRegularUrl),
    fetch(notoSansBengaliBoldUrl),
  ]);
  if (!regularResp.ok || !boldResp.ok) {
    throw new Error(
      `Font fetch failed (regular: ${regularResp.status}, bold: ${boldResp.status})`,
    );
  }
  const [regularBuf, boldBuf] = await Promise.all([
    regularResp.arrayBuffer(),
    boldResp.arrayBuffer(),
  ]);
  fontBase64Cache = {
    regular: arrayBufferToBase64(regularBuf),
    bold: arrayBufferToBase64(boldBuf),
  };
  return fontBase64Cache;
};

const groupByTime = (schedules) => {
  const groups = new Map();
  schedules.forEach((schedule) => {
    const key = schedule.departureTime || '99:99';
    groups.set(key, [...(groups.get(key) || []), schedule]);
  });
  return [...groups.entries()].sort(([first], [second]) => first.localeCompare(second));
};

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

// Reading order: কর্মদিবস → শুক্র-শনি, within each: student → teacher.
const SECTION_LAYOUT = [
  { dayKey: 'weekday', dayLabel: 'কর্মদিবস (রবিবার–বৃহস্পতিবার)' },
  { dayKey: 'weekend', dayLabel: 'শুক্রবার ও শনিবার' },
];
const AUDIENCE_LAYOUT = [
  { audKey: 'student', audLabel: '🚌  শিক্ষার্থী বাস' },
  { audKey: 'teacher', audLabel: '👨‍🏫  শিক্ষক / কর্মকর্তা / কর্মচারী বাস' },
];

// Build a single table row from a schedule. Every field is normalised to a
// non-empty string so empty cells never appear — PDF readers (Chrome, Acrobat)
// collapse empty cells and shift columns visually.
const dash = '—';

const buildRow = (schedule) => {
  const busNumber = (schedule.busNumber ?? '').toString().trim() || dash;
  const busName = (schedule.busName ?? '').toString().trim();
  const busCell = busName ? `${busNumber} (${busName})` : busNumber;
  const startPoint = (schedule.startPoint ?? '').toString().trim() || dash;
  const endPoint = (schedule.endPoint ?? '').toString().trim() || dash;
  const route = `${startPoint} → ${endPoint}`;
  const arrival =
    schedule.arrivalTime && schedule.arrivalTime.length > 0
      ? formatTime(schedule.arrivalTime)
      : dash;
  return [
    busCell,
    directionLabel(schedule.direction),
    route,
    arrival,
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

  // Title bar background.
  doc.setFillColor(...PRIMARY_RGB);
  doc.rect(0, 0, pageWidth, 26, 'F');
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(18);
  doc.setTextColor(255, 255, 255);
  doc.text('CoU Bus Tracker — বাস শিডিউল', 12, 12);
  doc.setFont(FONT_REGULAR, 'normal');
  doc.setFontSize(9);
  doc.setTextColor(204, 251, 241);
  doc.text('Comilla University Bus Schedule Export', pageWidth - 12, 12, { align: 'right' });

  doc.setFont(FONT_REGULAR, 'normal');
  doc.setFontSize(9);
  doc.setTextColor(...MUTED_RGB);
  doc.text(`তৈরি: ${bnDateTime()}`, 12, 32);

  const filterLine =
    scope === 'all'
      ? 'স্কোপ: সব শিডিউল'
      : `ফিল্টার: ${audienceLabel(filters.busAudience)} · ${
          filters.dayGroup === 'WEEKEND' ? 'শুক্রবার ও শনিবার' : 'কর্মদিবস'
        } · ${
          filters.statusFilter === 'ACTIVE' ? 'সক্রিয়' : filters.statusFilter === 'INACTIVE' ? 'নিষ্ক্রিয়' : 'সব'
        }`;
  doc.text(filterLine, 12, 38);

  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(11);
  doc.setTextColor(...TEXT_RGB);
  doc.text(`মোট শিডিউল: ${bengaliNumber.format(totalCount)}`, 12, 45);

  // Accent bar.
  doc.setDrawColor(...PRIMARY_RGB);
  doc.setLineWidth(0.5);
  doc.line(12, 49, pageWidth - 12, 49);
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
      pageHeight - 6,
      { align: 'right' },
    );
    doc.text('CoU Bus Tracker — বাস শিডিউল', 12, pageHeight - 6);
  }
};

// Layout constants — A4 landscape printable area is 297×210 mm.
const TOP_MARGIN = 56;
const BOTTOM_MARGIN = 14;
const PAGE_LIMIT = () => 210 - BOTTOM_MARGIN;

const ensureRoom = (doc, needed) => {
  const limit = PAGE_LIMIT();
  if (doc.lastAutoTable?.finalY) {
    return doc.lastAutoTable.finalY + needed <= limit;
  }
  return TOP_MARGIN + needed <= limit;
};

const newPageY = (doc) => {
  doc.addPage();
  return TOP_MARGIN;
};

const addPageIfNeeded = (doc, needed) => {
  if (!ensureRoom(doc, needed)) return newPageY(doc);
  return doc.lastAutoTable ? doc.lastAutoTable.finalY + 6 : TOP_MARGIN;
};

const drawSectionHeading = (doc, { title, count }, cursorY) => {
  const pageWidth = doc.internal.pageSize.getWidth();
  doc.setFillColor(...PRIMARY_RGB);
  doc.rect(0, cursorY - 6, pageWidth, 12, 'F');
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(13);
  doc.setTextColor(255, 255, 255);
  doc.text(`${title}  •  ${bengaliNumber.format(count)}টি শিডিউল`, 12, cursorY + 1.5);
  return cursorY + 14;
};

const drawAudienceHeading = (doc, label, count, cursorY) => {
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(11);
  doc.setTextColor(...PRIMARY_RGB);
  doc.text(`${label}  (${bengaliNumber.format(count)}টি বাস)`, 12, cursorY);
  // thin underline
  const pageWidth = doc.internal.pageSize.getWidth();
  doc.setDrawColor(...PRIMARY_FAINT_RGB);
  doc.setLineWidth(0.5);
  doc.line(12, cursorY + 2, pageWidth - 12, cursorY + 2);
  return cursorY + 7;
};

const drawTimeHeading = (doc, label, cursorY) => {
  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(10);
  doc.setTextColor(...MUTED_RGB);
  doc.text(`⏰  ${label}`, 14, cursorY);
  return cursorY + 4;
};

const renderTable = (doc, group, cursorY) => {
  autoTable(doc, {
    startY: cursorY + 2,
    margin: { left: 12, right: 12 },
    styles: {
      font: FONT_REGULAR,
      fontSize: 9,
      cellPadding: { top: 2.5, right: 3, bottom: 2.5, left: 3 },
      overflow: 'linebreak',
      lineColor: BORDER_RGB,
      lineWidth: 0.1,
      textColor: TEXT_RGB,
      valign: 'middle',
    },
    headStyles: {
      font: FONT_BOLD,
      fontStyle: 'bold',
      fillColor: PRIMARY_RGB,
      textColor: [255, 255, 255],
      fontSize: 9,
      halign: 'center',
      cellPadding: { top: 3, right: 3, bottom: 3, left: 3 },
    },
    alternateRowStyles: { fillColor: PRIMARY_LIGHT_RGB },
    columnStyles: {
      0: { cellWidth: 42, halign: 'left' }, // বাস নম্বর
      1: { cellWidth: 32, halign: 'left' }, // দিক
      2: { cellWidth: 'auto', halign: 'left' }, // রুট
      3: { cellWidth: 30, halign: 'center' }, // পৌঁছানোর সময়
      4: { cellWidth: 38, halign: 'center' }, // দিন
      5: { cellWidth: 26, halign: 'center' }, // অবস্থা
    },
    head: [['বাস নম্বর', 'দিক', 'রুট', 'পৌঁছানোর সময়', 'দিন', 'অবস্থা']],
    body: group.map((schedule) => buildRow(schedule)),
    didParseCell: (data) => {
      if (data.section === 'body' && data.column.index === 5) {
        const isInactive = group[data.row.index]?.isActive === false;
        if (isInactive) {
          data.cell.styles.textColor = DANGER_RGB;
          data.cell.styles.fontStyle = 'bold';
        } else {
          data.cell.styles.textColor = SUCCESS_RGB;
        }
      }
      // Bus number column — bold for prominence.
      if (data.section === 'body' && data.column.index === 0) {
        data.cell.styles.fontStyle = 'bold';
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
  [...group].sort((a, b) => {
    const ta = isTeacherBus(a) ? 1 : 0;
    const tb = isTeacherBus(b) ? 1 : 0;
    if (ta !== tb) return ta - tb;
    return String(a.busNumber || '').localeCompare(String(b.busNumber || ''), 'en', { numeric: true });
  });

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

  // Track which sections had data so we can write a summary table-of-contents.
  const sectionsRendered = [];

  SECTION_LAYOUT.forEach(({ dayKey, dayLabel }) => {
    const totalInSection =
      buckets[dayKey].student.length + buckets[dayKey].teacher.length;
    if (totalInSection === 0) return;

    sectionsRendered.push({ dayLabel, count: totalInSection });

    cursorY = addPageIfNeeded(doc, 22);
    cursorY = drawSectionHeading(doc, { title: dayLabel, count: totalInSection }, cursorY);

    AUDIENCE_LAYOUT.forEach(({ audKey, audLabel }) => {
      const group = sortGroup(buckets[dayKey][audKey]);
      if (group.length === 0) return;

      cursorY = addPageIfNeeded(doc, 16);
      cursorY = drawAudienceHeading(doc, audLabel, group.length, cursorY);

      const timeGroups = groupByTime(group);
      timeGroups.forEach(([time, slot]) => {
        const estimatedRowHeight = 10 + slot.length * 9;
        cursorY = addPageIfNeeded(doc, estimatedRowHeight + 12);
        cursorY = drawTimeHeading(doc, `${formatTime(time)} — ${bengaliNumber.format(slot.length)}টি বাস`, cursorY);
        renderTable(doc, slot, cursorY);
        cursorY = doc.lastAutoTable.finalY + 6;
      });
    });
  });

  writeFooter(doc);
  doc.save(buildFileName());
}
