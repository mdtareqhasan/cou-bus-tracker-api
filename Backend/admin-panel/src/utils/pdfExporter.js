// Client-side PDF export for the Schedules page.
// Uses jsPDF + jspdf-autotable with an embedded Noto Sans Bengali TTF so that
// Bengali text renders correctly in the resulting PDF.

import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import {
  formatTime,
  bengaliNumber,
  directionLabel,
  daysLabel,
  statusLabel,
  audienceLabel,
  dayGroupLabel,
  bnDateTime,
  isTeacherBus,
} from './format';

const FONT_REGULAR = 'NotoSansBengali';
const FONT_BOLD = 'NotoSansBengaliBold';
const FONT_REGULAR_URL = '/fonts/NotoSansBengali-Regular.ttf';
const FONT_BOLD_URL = '/fonts/NotoSansBengali-Bold.ttf';
const PRIMARY_RGB = [20, 184, 166]; // tailwind teal-500-ish to match the hero
const TEXT_RGB = [31, 41, 55]; // gray-800
const MUTED_RGB = [107, 114, 128]; // gray-500
const DANGER_RGB = [220, 38, 38]; // red-600
const BORDER_RGB = [229, 231, 235]; // gray-200

// Module-level cache so the TTF is fetched at most once per page load.
let fontLoadPromise = null;

const arrayBufferToBase64 = (buffer) => {
  const bytes = new Uint8Array(buffer);
  // Build in chunks to avoid "Maximum call stack" errors for ~175 KB buffers.
  const chunkSize = 0x8000;
  let binary = '';
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
};

const fetchAsBase64 = async (url) => {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Font fetch failed (${response.status}) for ${url}`);
  }
  const buffer = await response.arrayBuffer();
  return arrayBufferToBase64(buffer);
};

const ensureBengaliFont = () => {
  if (!fontLoadPromise) {
    fontLoadPromise = Promise.all([
      fetchAsBase64(FONT_REGULAR_URL),
      fetchAsBase64(FONT_BOLD_URL),
    ]);
  }
  return fontLoadPromise;
};

const groupByTime = (schedules) => {
  const groups = new Map();
  schedules.forEach((schedule) => {
    const key = schedule.departureTime || '99:99';
    groups.set(key, [...(groups.get(key) || []), schedule]);
  });
  return [...groups.entries()].sort(([first], [second]) => first.localeCompare(second));
};

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
      : `ফিল্টার: ${audienceLabel(filters.busAudience)} · ${dayGroupLabel(filters.dayGroup)} · ${
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

export async function exportSchedulesToPDF({ schedules, scope, filters }) {
  if (!Array.isArray(schedules) || schedules.length === 0) {
    alert('নির্বাচিত স্কোপে কোনো শিডিউল নেই');
    return;
  }

  let regularBase64;
  let boldBase64;
  try {
    [regularBase64, boldBase64] = await ensureBengaliFont();
  } catch (err) {
    console.error('PDF font load failed', err);
    alert('PDF ফন্ট লোড করা যায়নি, ইন্টারনেট সংযোগ পরীক্ষা করুন');
    return;
  }

  const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
  doc.addFileToVFS('NotoSansBengali-Regular.ttf', regularBase64);
  doc.addFont('NotoSansBengali-Regular.ttf', FONT_REGULAR, 'normal');
  doc.addFileToVFS('NotoSansBengali-Bold.ttf', boldBase64);
  doc.addFont('NotoSansBengali-Bold.ttf', FONT_BOLD, 'bold');

  // Sort: teacher buses last within each group for visual stability.
  const ordered = [...schedules].sort((a, b) => {
    const ta = isTeacherBus(a) ? 1 : 0;
    const tb = isTeacherBus(b) ? 1 : 0;
    if (ta !== tb) return ta - tb;
    return String(a.busNumber || '').localeCompare(String(b.busNumber || ''));
  });

  writeHeader(doc, { scope, filters, totalCount: ordered.length });

  const grouped = groupByTime(ordered);
  const startY = 48;
  let cursorY = startY;

  doc.setFont(FONT_BOLD, 'bold');
  doc.setFontSize(12);
  doc.setTextColor(...TEXT_RGB);

  grouped.forEach(([time, group]) => {
    // Make sure each group starts on a fresh page if it can't fit the heading + 1 row.
    const heading = `${formatTime(time)} (${bengaliNumber.format(group.length)}টি বাস)`;
    if (cursorY > 180) {
      doc.addPage();
      cursorY = 16;
    }
    doc.setFont(FONT_BOLD, 'bold');
    doc.text(heading, 12, cursorY);
    cursorY += 4;

    autoTable(doc, {
      startY: cursorY,
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
        // Reset the header accent bar on subsequent pages too.
        const pageWidth = doc.internal.pageSize.getWidth();
        doc.setDrawColor(...PRIMARY_RGB);
        doc.setLineWidth(0.4);
        doc.line(12, 12, pageWidth - 12, 12);
      },
    });

    cursorY = doc.lastAutoTable.finalY + 8;
  });

  writeFooter(doc);
  doc.save(buildFileName());
}
