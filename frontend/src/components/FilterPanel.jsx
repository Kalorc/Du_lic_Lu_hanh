import { SIZE_MAX, KEYWORD_MAX, INTEREST_MAX } from '../constants.js';

/**
 * Bo loc tuong ung dau vao cua thuat toan (muc 5.1 dac ta):
 * keyword, destinationId, durationDays, maxBudget, interestCodes, availableOnly, size.
 */
export default function FilterPanel({
  form,
  onChange,
  interests,
  destinations,
  onSubmit,
  onReset,
  loading,
}) {
  const toggleInterest = (code) => {
    const current = form.interestCodes;
    if (current.includes(code)) {
      onChange({ interestCodes: current.filter((c) => c !== code) });
    } else {
      if (current.length >= INTEREST_MAX) return;
      onChange({ interestCodes: [...current, code] });
    }
  };

  return (
    <form
      className="card card-pad panel"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit();
      }}
    >
      <h2>Điều kiện tìm tour</h2>

      <div className="field">
        <label htmlFor="keyword">Từ khóa</label>
        <input
          id="keyword"
          type="text"
          maxLength={KEYWORD_MAX}
          placeholder="Ví dụ: Đà Nẵng"
          value={form.keyword}
          onChange={(e) => onChange({ keyword: e.target.value })}
        />
        <div className="hint">Tối đa {KEYWORD_MAX} ký tự sau khi bỏ khoảng trắng đầu/cuối.</div>
      </div>

      <div className="field">
        <label htmlFor="destinationId">Điểm đến</label>
        <select
          id="destinationId"
          value={form.destinationId}
          onChange={(e) => onChange({ destinationId: e.target.value })}
        >
          <option value="">Tất cả điểm đến</option>
          {destinations.map((d) => (
            <option key={d.destinationId} value={d.destinationId}>
              {d.name} ({d.destinationId})
            </option>
          ))}
        </select>
        <div className="hint">Lọc bằng mã chuẩn, không so khớp chuỗi tên.</div>
      </div>

      <div className="field">
        <label htmlFor="durationDays">Số ngày</label>
        <input
          id="durationDays"
          type="number"
          min={1}
          placeholder="Ví dụ: 3"
          value={form.durationDays}
          onChange={(e) => onChange({ durationDays: e.target.value })}
        />
      </div>

      <div className="field">
        <label htmlFor="maxBudget">Ngân sách tối đa (VNĐ)</label>
        <input
          id="maxBudget"
          type="number"
          min={1}
          step={100000}
          placeholder="Ví dụ: 5000000"
          value={form.maxBudget}
          onChange={(e) => onChange({ maxBudget: e.target.value })}
        />
        <div className="hint">Áp cho giá người lớn của chuyến khởi hành.</div>
      </div>

      <div className="field">
        <label>
          Sở thích ({form.interestCodes.length}/{INTEREST_MAX})
        </label>
        <div className="chips">
          {interests.map((i) => (
            <button
              key={i.code}
              type="button"
              className={`chip ${form.interestCodes.includes(i.code) ? 'on' : ''}`}
              onClick={() => toggleInterest(i.code)}
              title={i.description ?? i.name}
            >
              {i.name}
            </button>
          ))}
        </div>
        <div className="hint">
          Không chọn sở thích nào thì kết quả chỉ được sắp theo giá, không chấm điểm.
        </div>
      </div>

      <div className="field">
        <label className="switch">
          <input
            type="checkbox"
            checked={form.availableOnly}
            onChange={(e) => onChange({ availableOnly: e.target.checked })}
          />
          Chỉ tour còn chỗ
        </label>
      </div>

      <div className="field">
        <label htmlFor="size">Số tour mỗi trang</label>
        <select id="size" value={form.size} onChange={(e) => onChange({ size: e.target.value })}>
          {Array.from({ length: SIZE_MAX }, (_, idx) => idx + 1).map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </div>

      <div className="btn-row">
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Đang tìm…' : 'Tìm tour'}
        </button>
        <button className="btn btn-ghost" type="button" onClick={onReset}>
          Xóa lọc
        </button>
      </div>
    </form>
  );
}
