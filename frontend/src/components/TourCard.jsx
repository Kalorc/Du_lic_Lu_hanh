import { Link } from 'react-router-dom';
import { formatVnd } from '../api/client.js';
import Icon from './Icon.jsx';
import TourImage from './TourImage.jsx';

const CARD_IMAGE_SIZES = '(max-width: 670px) calc(100vw - 32px), (max-width: 920px) 46vw, (max-width: 1320px) 35vw, 452px';

function scoreClass(score) {
  if (score === null || score === undefined) return 'zero';
  if (score >= 67) return '';
  if (score > 0) return 'mid';
  return 'zero';
}

export default function TourCard({ item, interestNames, destinationNames }) {
  const matched = new Set(item.matchedInterests ?? []);
  const destinations = (item.destinationIds ?? []).map((id) => destinationNames[id]).filter(Boolean);
  const hasScore = item.matchScore !== null && item.matchScore !== undefined;
  const thumbnailSrc = /^\/images\/tours\/tour-\d+\.webp$/.test(item.coverImageUrl)
    ? item.coverImageUrl.replace(/\.webp$/, '-card.webp')
    : undefined;

  return (
    <article className="card tour-card">
      <TourImage
        className="tour-thumb"
        src={item.coverImageUrl}
        alt={item.title}
        thumbnailSrc={thumbnailSrc}
        sizes={CARD_IMAGE_SIZES}
      />
      {hasScore && (
        <span className={`score-badge score-overlay ${scoreClass(item.matchScore)}`}>
          Trùng {item.matchScore}%
        </span>
      )}

      <div className="tour-body">
        <div className="tour-meta">
          {destinations.length > 0 && <span><Icon name="pin" /> {destinations.join(' · ')}</span>}
          <span>
            <Icon name="clock" />
            {item.durationDays} ngày {item.nights} đêm
          </span>
        </div>

        <h3 className="tour-title">
          <Link to={`/tours/${item.tourId}`}>
            {item.title}
            <span className="tour-link-icon"><Icon name="forward" /></span>
          </Link>
        </h3>

        {item.summary && <p className="tour-summary">{item.summary}</p>}

        <div className="tag-row">
          {(item.interestCodes ?? []).map((code) => (
            <span key={code} className={`tag ${matched.has(code) ? 'matched' : ''}`}>
              {interestNames?.[code] ?? code}
            </span>
          ))}
        </div>

        {hasScore && item.reason && <p className="score-reason">{item.reason}</p>}

        <div className="tour-foot">
          <div>
            <div className="price-from">Giá từ</div>
            <div className="price">{formatVnd(item.priceFrom)}</div>
            <div className="price-basis">Mỗi khách người lớn</div>
          </div>
          {item.availableSeats !== null && <span className="seat-count">Còn {item.availableSeats} chỗ</span>}
        </div>
      </div>
    </article>
  );
}
