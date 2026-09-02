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
  const wrapperRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const handleClick = (event) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    const handleEscape = (event) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('click', handleClick);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('click', handleClick);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [open]);

  const handleSelect = async (scopeId) => {
    setOpen(false);
    setLoading(true);
    try {
      let data;
      if (scopeId === 'all') {
        const res = await scheduleAPI.getAll();
        data = res.data;
      } else {
        data = visibleSchedules;
      }
      const payload = { scope: scopeId, schedules: data, filters };
      await new Promise((resolve) => setTimeout(resolve, 0));
      await exportSchedulesToPDF(payload);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div ref={wrapperRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
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
        PDF Export
        <ChevronDown className={`w-4 h-4 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div
          role="menu"
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
