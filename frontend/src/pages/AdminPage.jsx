import { useEffect, useState } from 'react';
import { api, auth } from '../api/client.js';
import { INTEREST_MAX } from '../constants.js';
import ErrorNotice from '../components/ErrorNotice.jsx';
import Notice from '../components/Notice.jsx';

/** Dang nhap de lay JWT cho cac API noi bo. */
function LoginForm({ onLoggedIn }) {
  const [username, setUsername] = useState('product01');
  const [password, setPassword] = useState('123456');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const data = await api.login(username.trim(), password);
      auth.save(data.accessToken, {
        username: data.username,
        fullName: data.fullName,
        role: data.role,
      });
      onLoggedIn();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card card-pad login-box" onSubmit={submit}>
      <h2 style={{ marginTop: 0, fontSize: 16 }}>Đăng nhập nội bộ</h2>
      <ErrorNotice error={error} />
      <div className="field">
        <label htmlFor="u">Tài khoản</label>
        <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="p">Mật khẩu</label>
        <input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      </div>
      <button className="btn" type="submit" disabled={busy}>
        {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
      </button>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 0 }}>
        Tài khoản demo (mật khẩu <code>123456</code>): <code>product01</code> gắn và quản lý sở
        thích, <code>manager01</code> chỉ xem, <code>khach01</code> không có quyền nội bộ.
      </p>
    </form>
  );
}

/** API 6 + 7: xem va gan lai so thich cho mot phien ban noi dung Tour. */
function VersionTagEditor({ interests }) {
  const [versionId, setVersionId] = useState('215');
  const [data, setData] = useState(null);
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState(null);
  const [okMessage, setOkMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    setBusy(true);
    setError(null);
    setOkMessage(null);
    try {
      const res = await api.getVersionInterests(versionId.trim());
      setData(res);
      setSelected(res.interests.map((i) => i.code));
    } catch (err) {
      setData(null);
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const save = async () => {
    setBusy(true);
    setError(null);
    setOkMessage(null);
    try {
      const res = await api.replaceVersionInterests(versionId.trim(), selected);
      setData(res);
      setSelected(res.interests.map((i) => i.code));
      setOkMessage(`Đã lưu ${res.interests.length} sở thích cho phiên bản ${res.tourVersionId}.`);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const toggle = (code) => {
    setSelected((prev) => {
      if (prev.includes(code)) return prev.filter((c) => c !== code);
      if (prev.length >= INTEREST_MAX) return prev;
      return [...prev, code];
    });
  };

  return (
    <section className="card card-pad">
      <h2 style={{ marginTop: 0, fontSize: 16 }}>Gắn sở thích cho phiên bản Tour</h2>
      <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 0 }}>
        Chỉ phiên bản ở trạng thái <code>DRAFT</code> được sửa. Phiên bản đã công bố trả về
        lỗi <code>VERSION_LOCKED</code> để giữ nguyên phân loại của bản đang bán.
      </p>

      <div className="field">
        <label htmlFor="vid">tourVersionId</label>
        <div className="btn-row">
          <input id="vid" value={versionId} onChange={(e) => setVersionId(e.target.value)} />
          <button className="btn btn-ghost" type="button" onClick={load} disabled={busy}>
            Tải
          </button>
        </div>
        <div className="hint">
          Dữ liệu mẫu: <code>215</code> là bản nháp (sửa được), <code>201</code> đã công bố (bị khóa).
        </div>
      </div>

      <ErrorNotice error={error} />
      {okMessage && <Notice kind="ok">{okMessage}</Notice>}

      {data && (
        <>
          <dl className="kv" style={{ margin: '14px 0' }}>
            <dt>Tour</dt>
            <dd>
              {data.title} (tourId {data.tourId})
            </dd>
            <dt>Trạng thái nội dung</dt>
            <dd>
              <span className={`pill ${data.editable ? 'draft' : 'closed'}`}>
                {data.contentStatus}
              </span>
            </dd>
            <dt>Sửa được</dt>
            <dd>{data.editable ? 'Có' : 'Không'}</dd>
          </dl>

          <div className="field">
            <label>
              Sở thích ({selected.length}/{INTEREST_MAX})
            </label>
            <div className="chips">
              {interests
                .filter((i) => i.active)
                .map((i) => (
                  <button
                    key={i.code}
                    type="button"
                    className={`chip ${selected.includes(i.code) ? 'on' : ''}`}
                    onClick={() => toggle(i.code)}
                    disabled={!data.editable}
                  >
                    {i.name}
                  </button>
                ))}
            </div>
          </div>

          <button className="btn" type="button" onClick={save} disabled={busy || !data.editable}>
            {busy ? 'Đang lưu…' : 'Lưu danh sách sở thích'}
          </button>
        </>
      )}
    </section>
  );
}

/** API 8 + 9: quan ly danh muc so thich. */
function InterestCatalogEditor({ interests, reload }) {
  const [form, setForm] = useState({ code: '', name: '', description: '', displayOrder: '' });
  const [error, setError] = useState(null);
  const [okMessage, setOkMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  const create = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setOkMessage(null);
    try {
      const created = await api.createInterest({
        code: form.code.trim().toUpperCase(),
        name: form.name.trim(),
        description: form.description.trim() || null,
        displayOrder: form.displayOrder === '' ? null : Number(form.displayOrder),
      });
      setOkMessage(`Đã tạo sở thích ${created.code} — ${created.name}.`);
      setForm({ code: '', name: '', description: '', displayOrder: '' });
      await reload();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const toggleActive = async (interest) => {
    setBusy(true);
    setError(null);
    setOkMessage(null);
    try {
      await api.updateInterest(interest.interestId, { active: !interest.active });
      setOkMessage(
        `${interest.code} đã ${interest.active ? 'tắt' : 'bật'}. Sở thích đã tắt không còn ` +
          'xuất hiện trên bộ lọc và không được tính điểm.',
      );
      await reload();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="card card-pad">
      <h2 style={{ marginTop: 0, fontSize: 16 }}>Danh mục sở thích</h2>

      <ErrorNotice error={error} />
      {okMessage && <Notice kind="ok">{okMessage}</Notice>}

      <table style={{ margin: '12px 0 20px' }}>
        <thead>
          <tr>
            <th>Mã</th>
            <th>Tên</th>
            <th>Thứ tự</th>
            <th>Trạng thái</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {interests.map((i) => (
            <tr key={i.interestId}>
              <td>
                <code>{i.code}</code>
              </td>
              <td>{i.name}</td>
              <td>{i.displayOrder}</td>
              <td>
                <span className={`pill ${i.active ? 'open' : 'closed'}`}>
                  {i.active ? 'Đang bật' : 'Đã tắt'}
                </span>
              </td>
              <td>
                <button
                  className="btn btn-ghost btn-sm"
                  type="button"
                  onClick={() => toggleActive(i)}
                  disabled={busy}
                >
                  {i.active ? 'Tắt' : 'Bật'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <form onSubmit={create}>
        <h3 style={{ fontSize: 14, margin: '0 0 10px' }}>Thêm sở thích mới</h3>
        <div className="field">
          <label htmlFor="code">Mã</label>
          <input
            id="code"
            placeholder="ECO_TOURISM"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
            required
          />
          <div className="hint">Chữ, số và dấu gạch dưới; mã nên ổn định vì được dùng để lọc.</div>
        </div>
        <div className="field">
          <label htmlFor="name">Tên hiển thị</label>
          <input
            id="name"
            placeholder="Du lịch sinh thái"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
        </div>
        <div className="field">
          <label htmlFor="desc">Mô tả tiêu chí</label>
          <textarea
            id="desc"
            rows={2}
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
        <div className="field">
          <label htmlFor="order">Thứ tự hiển thị</label>
          <input
            id="order"
            type="number"
            min={0}
            value={form.displayOrder}
            onChange={(e) => setForm({ ...form, displayOrder: e.target.value })}
          />
        </div>
        <button className="btn" type="submit" disabled={busy}>
          Tạo sở thích
        </button>
      </form>
    </section>
  );
}

export default function AdminPage() {
  const [user, setUser] = useState(auth.getUser());
  const [interests, setInterests] = useState([]);
  const [error, setError] = useState(null);

  const reload = async () => {
    try {
      setInterests(await api.listAllInterests());
      setError(null);
    } catch (err) {
      setError(err);
    }
  };

  useEffect(() => {
    if (user) reload();
  }, [user]);

  if (!user) {
    return (
      <main className="page">
        <LoginForm onLoggedIn={() => setUser(auth.getUser())} />
      </main>
    );
  }

  const canEdit = user.role === 'PRODUCT';

  return (
    <main className="page">
      <div className="page-head">
        <h1>Quản trị sở thích Tour</h1>
        <p>
          Đang đăng nhập: <strong>{user.fullName}</strong> · vai trò <strong>{user.role}</strong>.{' '}
          <button
            className="btn btn-ghost btn-sm"
            type="button"
            onClick={() => {
              auth.clear();
              setUser(null);
            }}
          >
            Đăng xuất
          </button>
        </p>
      </div>

      <ErrorNotice error={error} />

      {!canEdit && (
        <div style={{ marginBottom: 16 }}>
          <Notice kind="info">
            Vai trò <strong>{user.role}</strong> chỉ được xem sở thích của phiên bản Tour. Các
            thao tác gắn sở thích và sửa danh mục dành cho vai trò <strong>PRODUCT</strong>, nên
            server sẽ trả về <code>FORBIDDEN</code> nếu bạn thử lưu.
          </Notice>
        </div>
      )}

      <div className="admin-grid">
        <VersionTagEditor interests={interests} />
        <InterestCatalogEditor interests={interests} reload={reload} />
      </div>
    </main>
  );
}
