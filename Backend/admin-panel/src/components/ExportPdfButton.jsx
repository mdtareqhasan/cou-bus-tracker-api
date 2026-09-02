import { useEffect, useRef, useState } from 'react';
import { FileDown, ChevronDown, Loader2 } from 'lucide-react';
import { exportSchedulesToPDF } from '../utils/pdfExporter';
import { scheduleAPI } from '../api';

const SCOPES = [
  { id: 'current', label: 'বর্তমান ভিউ', description: 'প্রযোজ্য ফিল্টার অনুযায়ী' },
  { id: 'all', label: 'সব শিডিউল', description: 'ডাটাবেস থেকে সরাসরি' },
];

export default function ExportPdfButton({ visibleSchedules, filters }) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');
  const wrapperRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const handleClickOutside = (event) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    const handleEscape = (event) => {
      if (event.key === 'Escape') setOpen(false);
    };
    // Use capture: false so the document listener fires AFTER any menuitem
    // click handler — that way clicking a menuitem never races with the
    // outside-click detector.
    document.addEventListener('click', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('click', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [open]);

  const handleSelect = async (scopeId) => {
    // Prevent the document-level outside-click listener from also firing and
    // re-rendering the dropdown while we are mid-export.
    setOpen(false);
    setLoading(true);
    setStatusMsg(scopeId === 'all' ? 'ডাটাবেস থেকে সব শিডিউল নেওয়া হচ্ছে…' : 'বর্তমান ভিউ প্রস্তুত হচ্ছে…');
    try {
      let data;
      if (scopeId === 'all') {
        const res = await scheduleAPI.getAll();
        data = res.data || [];
        console.log('[PDF Export] সব শিডিউল fetched:', data.length, 'records');
      } else {
        data = visibleSchedules;
        console.log('[PDF Export] বর্তমান ভিউ:', data.length, 'records');
      }
      if (!Array.isArray(data) || data.length === 0) {
        alert('নির্বাচিত স্কোপে কোনো শিডিউল নেই');
        return;
      }
      const payload = { scope: scopeId, schedules: data, filters };
      await exportSchedulesToPDF(payload);
    } catch (err) {
      console.error('[PDF Export] failed:', err);
      alert('PDF রিপোর্ট তৈরি করতে সমস্যা হয়েছে');
    } finally {
      setLoading(false);
      setStatusMsg('');
    }
  };

  return (
    <div ref={wrapperRef} className="relative">
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          setOpen((value) => !value);
        }}
        disabled={loading}
        className="btn-secondary schedule-add-button"
        aria-haspopup="menu"
        aria-expanded={open}
        title="PDF হিসেবে ডাউনলোড করুন"
      >
        {loading ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <FileDown className="w-4 h-4" />
        )}
        {loading && statusMsg ? statusMsg : 'PDF Export'}
        <ChevronDown className={`w-4 h-4 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div
          role="menu"
          onClick={(e) => e.stopPropagation()}
          className="absolute right-0 z-20 mt-2 w-56 overflow-hidden rounded-xl border border-gray-100 bg-white shadow-lg"
        >
          {SCOPES.map((scope, index) => (
            <button
              key={scope.id}
              type="button"
              role="menuitem"
              onClick={() => handleSelect(scope.id)}
              className={`flex w-full items-start gap-3 px-4 py-3 text-left text-sm transition-colors hover:bg-teal-50 ${
                index === 0 ? 'border-b border-gray-100' : ''
              }`}
            >
              <FileDown className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
              <div>
                <div className="font-semibold text-gray-800">{scope.label}</div>
                <div className="text-xs text-gray-500">{scope.description}</div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
