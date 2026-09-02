import { useState } from 'react';
import { FileDown, Loader2 } from 'lucide-react';
import { exportSchedulesToPDF } from '../utils/pdfExporter';
import { scheduleAPI } from '../api';

export default function ExportPdfButton({ visibleSchedules, filters }) {
  const [loading, setLoading] = useState(null); // 'current' | 'all' | null
  const [error, setError] = useState('');

  const handleExport = async (scope) => {
    console.log('[ExportPdf] handleExport scope=', scope);
    setError('');
    setLoading(scope);
    try {
      let data;
      if (scope === 'all') {
        try {
          const res = await scheduleAPI.getAll();
          data = Array.isArray(res.data) ? res.data : [];
          console.log('[ExportPdf] সব শিডিউল API -> records:', data.length);
        } catch (apiErr) {
          console.warn('[ExportPdf] API getAll failed, falling back to current view:', apiErr.message);
          data = Array.isArray(visibleSchedules) ? visibleSchedules : [];
          console.log('[ExportPdf] fallback to visibleSchedules, records:', data.length);
        }
      } else {
        data = Array.isArray(visibleSchedules) ? visibleSchedules : [];
        console.log('[ExportPdf] বর্তমান ভিউ records:', data.length);
      }
      if (!data.length) {
        alert('কোনো শিডিউল পাওয়া যায়নি');
        return;
      }
      await exportSchedulesToPDF({ scope, schedules: data, filters });
    } catch (err) {
      console.error('[ExportPdf] error:', err);
      setError(err.message || 'PDF তৈরি ব্যর্থ');
    } finally {
      setLoading(null);
    }
  };

  return (
    <div className="flex items-center gap-1.5 schedule-add-button">
      <button
        type="button"
        onClick={() => handleExport('current')}
        disabled={loading !== null}
        className="btn-secondary flex items-center gap-1.5"
        title="প্রযোজ্য ফিল্টার অনুযায়ী"
      >
        {loading === 'current' ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <FileDown className="w-4 h-4" />
        )}
        <span className="text-sm">বর্তমান ভিউ</span>
      </button>
      <button
        type="button"
        onClick={() => handleExport('all')}
        disabled={loading !== null}
        className="btn-primary flex items-center gap-1.5"
        title="ডাটাবেস থেকে সব শিডিউল"
      >
        {loading === 'all' ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <FileDown className="w-4 h-4" />
        )}
        <span className="text-sm">সব শিডিউল</span>
      </button>
      {error && <span className="text-xs text-red-600 ml-1">{error}</span>}
    </div>
  );
}
