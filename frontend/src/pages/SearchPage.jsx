import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { SIZE_DEFAULT } from '../constants.js';
import FilterPanel from '../components/FilterPanel.jsx';
import TourCard from '../components/TourCard.jsx';
import ErrorNotice from '../components/ErrorNotice.jsx';
import Notice from '../components/Notice.jsx';

const EMPTY_FORM = {
  keyword: '',
  destinationId: '',
  durationDays: '',
  maxBudget: '',
  interestCodes: [],
  availableOnly: true,
  size: String(SIZE_DEFAULT),
};

export default function SearchPage() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [applied, setApplied] = useState(EMPTY_FORM);
  const [page, setPage] = useState(1);

  const [interests, setInterests] = useState([]);
  const [destinations, setDestinations] = useState([]);
  const [result, setResult] = useState(null);
  const [meta, setMeta] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const interestNames = useMemo(
    () => Object.fromEntries(interests.map((i) => [i.code, i.name])),
    [interests],
  );

  // Nap danh muc cho bo loc (API 1 va API 2)
  useEffect(() => {
    Promise.all([api.listInterests(), api.listDestinations('CITY')])
      .then(([is, ds]) => {
        setInterests(is);
        setDestinations(ds);
      })
      .catch(setError);
  }, []);

  const runSearch = useCallback(async (f, pageNo) => {
    setLoading(true);
    setError(null);
    const params = {
      keyword: f.keyword.trim() || undefined,
      destinationId: f.destinationId || undefined,
      durationDays: f.durationDays || undefined,
      maxBudget: f.maxBudget || undefined,
      interestCodes: f.interestCodes.length ? f.interestCodes : undefined,
      availableOnly: f.availableOnly,
      page: pageNo,
      size: f.size,
    };

    try {
      // Co chon so thich -> API goi y (xep hang theo diem); khong chon -> API tra cuu
      const res = f.interestCodes.length
        ? await api.recommendTours(params)
        : await api.searchTours(params);
      setResult(res.data);
      setMeta(res.meta);
    } catch (e) {
      setResult(null);
      setMeta(null);
      setError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    runSearch(applied, page);
  }, [applied, page, runSearch]);

  const submit = () => {
    setApplied(form);
    setPage(1);
  };

  const reset = () => {
    setForm(EMPTY_FORM);
    setApplied(EMPTY_FORM);
    setPage(1);
  };

  const items = result?.items ?? [];
  const pagination = result?.pagination;

  return (
    <main className="page">
      <div className="page-head">
        <h1>Tra cứu tour và gợi ý tour theo sở thích</h1>
        <p>
          Hệ thống lọc bắt buộc theo điều kiện bạn chọn, sau đó xếp tour trùng nhiều sở thích
          hơn lên đầu. Điểm trùng là tỷ lệ sở thích khớp, không phải dự báo mức độ hài lòng.
        </p>
      </div>

      <div className="layout">
        <FilterPanel
          form={form}
          onChange={(patch) => setForm((prev) => ({ ...prev, ...patch }))}
          interests={interests}
          destinations={destinations}
          onSubmit={submit}
          onReset={reset}
          loading={loading}
        />

        <section>
          <div className="result-head">
            <div className="result-count">
              {pagination ? (
                <>
                  Tìm thấy <strong>{pagination.totalItems}</strong> tour
                  {pagination.totalPages > 1 && (
                    <>
                      {' '}
                      · trang {pagination.page}/{pagination.totalPages}
                    </>
                  )}
                </>
              ) : (
                'Đang tải kết quả…'
              )}
            </div>
            {meta && (
              <div className="result-count">
                Sắp xếp:{' '}
                <strong>
                  {meta.sort === 'RELEVANCE' ? 'Độ trùng sở thích' : 'Giá tăng dần'}
                </strong>
              </div>
            )}
          </div>

          <ErrorNotice error={error} />

          {meta && !meta.personalized && !error && (
            <div style={{ marginBottom: 12 }}>
              <Notice kind="info">
                Bạn chưa chọn sở thích nên kết quả được sắp theo giá tăng dần và chưa chấm
                điểm phù hợp.
              </Notice>
            </div>
          )}

          {loading && (
            <div className="tour-list">
              {[0, 1, 2].map((i) => (
                <div key={i} className="skeleton" />
              ))}
            </div>
          )}

          {!loading && !error && items.length === 0 && (
            <div className="card empty">
              <h3>Không có tour nào đạt điều kiện</h3>
              <p>
                Hãy thử nới ngân sách, bỏ bớt điều kiện số ngày, hoặc tắt tùy chọn
                &ldquo;Chỉ tour còn chỗ&rdquo;.
              </p>
            </div>
          )}

          {!loading && items.length > 0 && (
            <div className="tour-list">
              {items.map((item) => (
                <TourCard key={item.tourId} item={item} interestNames={interestNames} />
              ))}
            </div>
          )}

          {pagination && pagination.totalPages > 1 && (
            <div className="pager">
              <button
                className="btn btn-ghost btn-sm"
                disabled={page <= 1 || loading}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
              >
                ← Trang trước
              </button>
              <span>
                {pagination.page} / {pagination.totalPages}
              </span>
              <button
                className="btn btn-ghost btn-sm"
                disabled={page >= pagination.totalPages || loading}
                onClick={() => setPage((p) => p + 1)}
              >
                Trang sau →
              </button>
            </div>
          )}

          {meta && (
            <div className="card card-pad" style={{ marginTop: 18 }}>
              <div className="meta-box">
                asOf={meta.asOf} · priceBasis={meta.priceBasis} · availableOnly=
                {String(meta.availableOnly)} · appliedInterestCodes=[
                {(meta.appliedInterestCodes ?? []).join(', ')}]
              </div>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
