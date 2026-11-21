import { useState } from 'react';
import Icon from './Icon.jsx';

function ImageContent({ src, alt, loading, thumbnailSrc, sizes }) {
  const [attempt, setAttempt] = useState('responsive');

  if (!src || attempt === 'failed') {
    return (
      <span className="image-fallback" role="img" aria-label={`Chưa có ảnh đại diện: ${alt}`}>
        <Icon name="landscape" />
        Chưa có ảnh đại diện
      </span>
    );
  }

  const useResponsiveSource = thumbnailSrc && attempt === 'responsive';

  return (
    <img
      src={src}
      alt={alt}
      loading={loading}
      decoding="async"
      srcSet={useResponsiveSource ? `${thumbnailSrc} 640w, ${src} 1280w` : undefined}
      sizes={useResponsiveSource ? sizes : undefined}
      onError={(event) => {
        const image = event.currentTarget;
        const originalFailed = image.currentSrc === image.src;
        setAttempt(useResponsiveSource && !originalFailed ? 'original' : 'failed');
      }}
    />
  );
}

export default function TourImage({ src, alt, className, loading = 'lazy', thumbnailSrc, sizes }) {
  return (
    <div className={className}>
      <ImageContent
        key={src || ''}
        src={src}
        alt={alt}
        loading={loading}
        thumbnailSrc={thumbnailSrc}
        sizes={sizes}
      />
    </div>
  );
}
