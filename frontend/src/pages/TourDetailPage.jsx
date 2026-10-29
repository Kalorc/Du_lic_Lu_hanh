import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, formatDate, formatVnd } from '../api/client.js';
import ErrorNotice from '../components/ErrorNotice.jsx';

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

export default function TourDetailPage() {
  const { tourId } = useParams();
  const [tour, setTour] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getTour(tourId)
      .then(setTour)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [tourId]);

  if (loading) {
    return (
      <main className="page">
        <div className="skeleton" style={{ height: 260 }} />
      </main>
    );
  }

  if (error) {
    return (
      <main className="page">
        <ErrorNotice error={error} />
        <p style={{ marginTop: 14 }}>
          <Link to="/tours">← Về trang tra cứu</Link>
        </p>
      </main>
    );
  }

  const itinerary = asArray(tour.itinerary);
  const included = asArray(tour.includedServices);
  const excluded = asArray(tour.excludedServices);

  return (
    <main className="page">
      <p style={{ marginTop: 0 }}>
        <Link to="/tours">← Về trang tra cứu</Link>
      </p>

      <div className="page-head">
        <h1>{tour.title}</h1>
        <p>
          {tour.tourCode} · {tour.durationDays} ngày {tour.nights} đêm ·{' '}
          {tour.destinations.map((d) => d.name).join(' · ')}
        </p>
      </div>

      <div className="detail-hero">
        {tour.coverImageUrl && <img src={tour.coverImageUrl} alt={tour.title} />}
      </div>

      <div className="detail-grid">
        <div>
          {tour.summary && (
            <section className="section">
              <h2>Giới thiệu</h2>
              <p style={{ margin: 0 }}>{tour.summary}</p>
              {tour.description && <p>{tour.description}</p>}
            </section>
          )}

          {itinerary.length > 0 && (
            <section className="section">
              <h2>Lịch trình</h2>
              <ul className="itinerary">
                {itinerary.map((d, idx) => (
                  <li key={idx}>
                    <div className="day">Ngày {d.day ?? idx + 1}</div>
                    <div>{d.title ?? JSON.stringify(d)}</div>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {(included.length > 0 || excluded.length > 0) && (
            <section className="section">
              <h2>Dịch vụ</h2>
              {included.length > 0 && (
                <>
                  <strong>Bao gồm</strong>
                  <ul className="bullets">
                    {included.map((s, i) => (
                      <li key={i}>{String(s)}</li>
                    ))}
                  </ul>
                </>
              )}
              {excluded.length > 0 && (
                <>
                  <strong>Không bao gồm</strong>
                  <ul className="bullets">
                    {excluded.map((s, i) => (
                      <li key={i}>{String(s)}</li>
                    ))}
                  </ul>
                </>
              )}
            </section>
          )}

          <section className="section">
            <h2>Chuyến khởi hành</h2>
            <div className="card">
              <table>
                <thead>
                  <tr>
                    <th>Mã chuyến</th>
                    <th>Khởi hành</th>
                    <th>Giá người lớn</th>
                    <th>Còn chỗ</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {tour.departures.map((d) => (
                    <tr key={d.departureId}>
                      <td>{d.departureCode}</td>
                      <td>{formatDate(d.startsAt)}</td>
                      <td>{formatVnd(d.adultPrice)}</td>
                      <td>{d.availableSeats}</td>
                      <td>
                        <span className={`pill ${d.bookable ? 'open' : 'closed'}`}>
                          {d.bookable ? 'Đang bán' : 'Không bán được'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="hint" style={{ fontSize: 12, color: 'var(--muted)' }}>
              Chuyến chỉ bán được khi đang mở bán, nằm trong cửa sổ đặt, có bản giá đã duyệt
              và còn chỗ. Giá của bản giá chưa duyệt không được dùng.
            </p>
          </section>
        </div>

        <aside>
          <div className="card card-pad" style={{ marginBottom: 16 }}>
            <div className="price-from">Giá từ</div>
            <div className="price" style={{ fontSize: 24 }}>
              {formatVnd(tour.priceFrom)}
            </div>
            <div className="price-basis">
              {tour.currency} · người lớn / khách · chuyến rẻ nhất còn chỗ
            </div>
          </div>

          <div className="card card-pad" style={{ marginBottom: 16 }}>
            <h2 style={{ fontSize: 15, marginTop: 0 }}>Sở thích của tour</h2>
            <div className="tag-row">
              {tour.interests.map((i) => (
                <span key={i.code} className="tag">
                  {i.name}
                </span>
              ))}
              {tour.interests.length === 0 && (
                <span style={{ color: 'var(--muted)', fontSize: 13 }}>Chưa gắn sở thích</span>
              )}
            </div>
          </div>

          <div className="card card-pad">
            <h2 style={{ fontSize: 15, marginTop: 0 }}>Thông tin kỹ thuật</h2>
            <dl className="kv">
              <dt>tourId</dt>
              <dd>{tour.tourId}</dd>
              <dt>tourVersionId</dt>
              <dd>{tour.tourVersionId}</dd>
              <dt>slug</dt>
              <dd>{tour.slug}</dd>
              <dt>Điểm khởi hành</dt>
              <dd>{tour.originDestinationId ?? '—'}</dd>
              <dt>priceBasis</dt>
              <dd>{tour.priceBasis}</dd>
            </dl>
          </div>
        </aside>
      </div>
    </main>
  );
}
