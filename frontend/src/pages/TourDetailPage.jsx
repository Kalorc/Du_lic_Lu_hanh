import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, formatDate, formatVnd } from '../api/client.js';
import ErrorNotice from '../components/ErrorNotice.jsx';
import Icon from '../components/Icon.jsx';
import TourImage from '../components/TourImage.jsx';

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
      <main className="page" aria-busy="true">
        <p role="status" className="sr-only">Đang tải thông tin tour…</p>
        <div className="skeleton detail-skeleton" aria-hidden="true" />
      </main>
    );
  }

  if (error) {
    return (
      <main className="page">
        <Link className="back-link" to="/tours"><Icon name="back" /> Về trang tra cứu</Link>
        <ErrorNotice error={error} />
      </main>
    );
  }

  const itinerary = asArray(tour.itinerary);
  const included = asArray(tour.includedServices);
  const excluded = asArray(tour.excludedServices);
  const photos = asArray(tour.media).filter((item) => item.mediaType === 'IMAGE');
  const cover = photos.find((item) => item.mediaUrl === tour.coverImageUrl);
  const gallery = photos.filter((item) => !item.cover && item.mediaUrl !== tour.coverImageUrl);

  return (
    <main className="page detail-page">
      <Link className="back-link" to="/tours"><Icon name="back" /> Về trang tra cứu</Link>

      <div className="page-head">
        <h1>{tour.title}</h1>
        <div className="tour-meta detail-meta">
          {tour.destinations.length > 0 && (
            <span><Icon name="pin" /> {tour.destinations.map((d) => d.name).join(' · ')}</span>
          )}
          <span><Icon name="clock" /> {tour.durationDays} ngày {tour.nights} đêm</span>
          <span className="pill closed">{tour.tourCode}</span>
        </div>
      </div>

      <div className="detail-overview">
        <div>
          <TourImage
            className="detail-hero"
            src={tour.coverImageUrl}
            alt={cover?.altText?.trim() || tour.title}
            loading="eager"
          />
          {cover?.caption && <p className="photo-caption">{cover.caption}</p>}
        </div>

        <aside className="card card-pad detail-facts" aria-label="Giá và sở thích của tour">
          <div className="price-from">Giá từ</div>
          <div className="price">{formatVnd(tour.priceFrom)}</div>
          <div className="price-basis">
            {tour.priceFrom ? 'Mỗi khách người lớn · chuyến rẻ nhất còn chỗ' : 'Chưa có giá cho chuyến còn chỗ'}
          </div>
          <a className="btn" href="#khoi-hanh">Xem lịch khởi hành <Icon name="down" /></a>

          <div className="detail-tags">
            <h2>Sở thích của tour</h2>
            <div className="tag-row">
              {tour.interests.map((i) => <span key={i.code} className="tag">{i.name}</span>)}
              {tour.interests.length === 0 && <span className="hint">Chưa gắn sở thích</span>}
            </div>
          </div>
        </aside>
      </div>

      {gallery.length > 0 && (
        <section className="section" aria-labelledby="gallery-title">
          <h2 id="gallery-title">Hình ảnh hành trình</h2>
          <div className="gallery">
            {gallery.map((item, index) => (
              <figure key={item.mediaId}>
                <TourImage
                  className="gallery-image"
                  src={item.mediaUrl}
                  alt={item.altText?.trim() || `Ảnh ${index + 1} của ${tour.title}`}
                />
                {item.caption && <figcaption className="photo-caption">{item.caption}</figcaption>}
              </figure>
            ))}
          </div>
        </section>
      )}

      {(tour.summary || tour.description) && (
        <section className="section detail-copy">
          <h2>Giới thiệu</h2>
          {tour.summary && <p className="detail-summary">{tour.summary}</p>}
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
          <div className="service-grid">
            {included.length > 0 && (
              <div className="card card-pad">
                <h3>Bao gồm</h3>
                <ul className="bullets">
                  {included.map((s, i) => <li key={i}>{String(s)}</li>)}
                </ul>
              </div>
            )}
            {excluded.length > 0 && (
              <div className="card card-pad">
                <h3>Không bao gồm</h3>
                <ul className="bullets">
                  {excluded.map((s, i) => <li key={i}>{String(s)}</li>)}
                </ul>
              </div>
            )}
          </div>
        </section>
      )}

      <section className="section" id="khoi-hanh" aria-labelledby="departures-title">
        <h2 id="departures-title">Chuyến khởi hành</h2>
        {tour.departures.length > 0 ? (
            <div className="card table-scroll" tabIndex={0} role="region" aria-label="Lịch và giá khởi hành">
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
        ) : <p className="hint">Chưa có chuyến khởi hành.</p>}
      </section>
    </main>
  );
}
