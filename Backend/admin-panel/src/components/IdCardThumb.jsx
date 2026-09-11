import { useState } from 'react';

// Cloudinary URL → small thumbnail transform (40×40 display, auto-format/quality).
// Modal keeps the original URL; only the row thumbnail is downsized.
function thumbUrl(url) {
  return url && url.includes('/image/upload/')
    ? url.replace('/image/upload/', '/image/upload/w_80,h_80,c_fill,q_auto,f_auto/')
    : url;
}

export default function IdCardThumb({ url, name, onOpen }) {
  const [failed, setFailed] = useState(false);
  const showImage = Boolean(url) && !failed;
  const initial = (name || '?').trim().charAt(0).toUpperCase();
  const interactive = Boolean(onOpen);
  const Tag = interactive ? 'button' : 'div';
  const handleClick = interactive && showImage ? onOpen : undefined;
  const ariaLabel = showImage ? `View ${name}'s ID card` : undefined;

  const className =
    'relative w-10 h-10 rounded-xl overflow-hidden flex-shrink-0 ring-1 ring-gray-200 ' +
    'bg-gradient-to-br from-teal-400 to-emerald-500 text-white font-semibold text-sm ' +
    'flex items-center justify-center transition-all ' +
    (interactive && showImage ? 'hover:ring-emerald-400 hover:scale-105 cursor-pointer' : 'cursor-default');

  return (
    <Tag
      type={interactive ? 'button' : undefined}
      onClick={handleClick}
      aria-label={ariaLabel}
      aria-hidden={interactive ? undefined : true}
      className={className}
    >
      {showImage ? (
        <img
          src={thumbUrl(url)}
          alt={`${name} ID card`}
          className="w-full h-full object-cover"
          onError={() => setFailed(true)}
          loading="lazy"
        />
      ) : (
        <span>{initial}</span>
      )}
    </Tag>
  );
}
