import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../api/client.js';
import { SIZE_DEFAULT } from '../constants.js';
import FilterPanel from '../components/FilterPanel.jsx';
import TourCard from '../components/TourCard.jsx';
import ErrorNotice from '../components/ErrorNotice.jsx';
import Icon from '../components/Icon.jsx';

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
  const [catalogError, setCatalogError] = useState(null);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const latestRequest = useRef(0);
  const latestCatalogRequest = useRef(0);

  const interestNames = useMemo(
    () => Object.fromEntries(interests.map((i) => [i.code, i.name])),
    [interests],
  );
  const destinationNames = useMemo(
    () => Object.fromEntries(destinations.map((d) => [d.destinationId, d.name])),
    [destinations],
  );

  const loadCatalogs = useCallback(async () => {
    const requestId = ++latestCatalogRequest.current;
    setCatalogLoading(true);
    try {
      const [interestList, destinationList] = await Promise.all([
        api.listInterests(), api.listDestinations('CITY'),
      ]);
      if (requestId !== latestCatalogRequest.current) return;
      setInterests(interestList);
      setDestinations(destinationList);
      setCatalogError(null);
    } catch (catalogFailure) {
      if (requestId === latestCatalogRequest.current) setCatalogError(catalogFailure);
    } finally {
      if (requestId === latestCatalogRequest.current) setCatalogLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCatalogs();
    return () => { latestCatalogRequest.current += 1; };
  }, [loadCatalogs]);

  const runSearch = useCallback(async (f, pageNo) => {
    // TC31: only the newest applied filters may update results, errors or loading.
    const requestId = ++latestRequest.current;
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
      if (requestId !== latestRequest.current) return;
      setResult(res.data);
      setMeta(res.meta);
    } catch (e) {
      if (requestId !== latestRequest.current) return;
      setResult(null);
      setMeta(null);
      setError(e);
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    runSearch(applied, page);
    return () => { latestRequest.current += 1; };
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
  const visibleError = error ?? catalogError;

  const retry = () => {
    if (catalogError) loadCatalogs();
    if (error) runSearch(applied, page);
  };

  return (
    <main className="page">
      <section className="discovery-hero" aria-labelledby="discover-title">
        <div className="discovery-hero-copy">
          <h1 id="discover-title">
            Khám phá <span className="nowrap">Việt Nam</span>
            <em>theo cách <span className="nowrap">của bạn.</span></em>
          </h1>
          <p>Tìm tour theo điểm đến, ngân sách và sở thích của bạn.</p>
          <a className="btn hero-link" href="#bo-loc">
            Khám phá các tour <Icon name="down" />
          </a>
        </div>
        <div className="hero-location"><Icon name="pin" /> Mỹ Khê, Đà Nẵng</div>
      </section>

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

        <section className="results" aria-labelledby="result-title" aria-busy={loading}>
          <div className="result-head">
            <div>
              <h2 id="result-title">{meta?.personalized ? 'Tour gợi ý cho bạn' : 'Khám phá tour'}</h2>
              <div className="result-count" aria-live="polite">
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
                loading ? 'Đang tải kết quả…' : error ? 'Không tải được kết quả' : null
              )}
              </div>
            </div>
            {meta && (
              <div className="result-sort">
                <Icon name="sliders" />
                {meta.sort === 'RELEVANCE' ? 'Độ trùng sở thích' : 'Giá tăng dần'}
              </div>
            )}
          </div>

          {visibleError && (
            <div className="result-error">
              <ErrorNotice error={visibleError} />
              <button
                className="btn btn-ghost"
                type="button"
                onClick={retry}
                disabled={loading || catalogLoading}
              >
                {catalogLoading ? 'Đang tải…' : 'Thử lại'}
              </button>
            </div>
          )}

          {loading && (
            <div className="tour-list" aria-hidden="true">
              {[0, 1, 2].map((i) => (
                <div key={i} className="skeleton" />
              ))}
            </div>
          )}

          {!loading && !error && items.length === 0 && (
            <div className="card empty">
              <Icon name="search" />
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
                <TourCard
                  key={item.tourId}
                  item={item}
                  interestNames={interestNames}
                  destinationNames={destinationNames}
                />
              ))}
            </div>
          )}

          {pagination && pagination.totalPages > 1 && (
            <nav className="pager" aria-label="Trang kết quả">
              <button
                className="btn btn-ghost btn-sm"
                disabled={page <= 1 || loading}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
              >
                <Icon name="back" /> Trang trước
              </button>
              <span>
                {pagination.page} / {pagination.totalPages}
              </span>
              <button
                className="btn btn-ghost btn-sm"
                disabled={page >= pagination.totalPages || loading}
                onClick={() => setPage((p) => p + 1)}
              >
                Trang sau <Icon name="forward" />
              </button>
            </nav>
          )}
        </section>
      </div>
    </main>
  );
}
