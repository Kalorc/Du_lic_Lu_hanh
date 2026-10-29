import { Link } from 'react-router-dom';
import { formatVnd } from '../api/client.js';

function scoreClass(score) {
  if (score === null || score === undefined) return 'zero';
  if (score >= 67) return '';
  if (score > 0) return 'mid';
  return 'zero';
}

export default function TourCard({ item, interestNames }) {
  const matched = new Set(item.matchedInterests ?? []);

  return (
    <article className="card tour-card">
      <div className="tour-thumb">
        {item.coverImageUrl ? (
          <img src={item.coverImageUrl} alt={item.title} loading="lazy" />
        ) : (
          <span>Chưa có ảnh đại diện</span>
        )}
      </div>

      <div className="tour-body">
        <h3 className="tour-title">
          <Link to={`/tours/${item.tourId}`}>{item.title}</Link>
        </h3>

        <div className="tour-meta">
          <span>{item.tourCode}</span>
          <span>
            {item.durationDays} ngày {item.nights} đêm
          </span>
          <span>{(item.destinationIds ?? []).join(' · ')}</span>
          {item.availableSeats !== null && <span>Còn {item.availableSeats} chỗ</span>}
        </div>

        {item.summary && <p className="tour-summary">{item.summary}</p>}

        <div className="tag-row">
          {(item.interestCodes ?? []).map((code) => (
            <span key={code} className={`tag ${matched.has(code) ? 'matched' : ''}`}>
              {interestNames?.[code] ?? code}
            </span>
          ))}
        </div>

        <div className="tour-foot">
          <div>
            <div className="price-from">Giá từ</div>
            <div className="price">{formatVnd(item.priceFrom)}</div>
            <div className="price-basis">Người lớn / khách · chuyến rẻ nhất đủ điều kiện</div>
          </div>

          <div className="score">
            {item.matchScore !== null && item.matchScore !== undefined && (
              <span className={`score-badge ${scoreClass(item.matchScore)}`}>
                Trùng {item.matchScore}%
              </span>
            )}
            <span className="score-reason">{item.reason}</span>
          </div>
        </div>
      </div>
    </article>
  );
}
