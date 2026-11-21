import { useEffect, useState } from 'react';
import { api, auth } from '../api/client.js';
import { INTEREST_MAX } from '../constants.js';
import ErrorNotice from '../components/ErrorNotice.jsx';
import Icon from '../components/Icon.jsx';
import Notice from '../components/Notice.jsx';

/** Dang nhap de lay JWT cho cac API noi bo. */
function LoginForm({ onLoggedIn }) {
  const [username, setUsername] = useState('product01');
  const [password, setPassword] = useState('');
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
      <h2>Đăng nhập nội bộ</h2>
      <ErrorNotice error={error} />
      <div className="field">
        <label htmlFor="u">Tài khoản</label>
        <input id="u" type="text" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor="p">Mật khẩu</label>
        <input id="p" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
      </div>
      <button className="btn" type="submit" disabled={busy}>
        {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
      </button>
      <details className="login-help">
        <summary>Tài khoản demo</summary>
        <p>
          Dùng mật khẩu demo đã cấu hình cho môi trường cục bộ. Tài khoản <code>product01</code> quản lý
          sở thích, <code>manager01</code> chỉ xem phiên bản.
        </p>
      </details>
    </form>
  );
}

/** API 6 + 7: xem va gan lai so thich cho mot phien ban noi dung Tour. */
function VersionTagEditor({ interests, canEdit }) {
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
      const res = await api.replaceVersionInterests(data.tourVersionId, selected);
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

  // Historical inactive mappings remain visible until the user explicitly removes them.
  const options = [...new Map([...(data?.interests ?? []), ...interests]
    .map((interest) => [interest.code, interest])).values()];
  const hasInactiveSelection = options.some((interest) => !interest.active && selected.includes(interest.code));

  return (
    <section className="card card-pad admin-panel">
      <h2>Gắn sở thích cho phiên bản Tour</h2>
      <p className="hint">
        Vai trò PRODUCT chỉ chỉnh sửa được sở thích của phiên bản nháp (DRAFT).
      </p>

      <div className="field">
        <label htmlFor="vid">Mã phiên bản (tourVersionId)</label>
        <div className="btn-row">
          <input id="vid" type="text" inputMode="numeric" value={versionId} disabled={busy} onChange={(e) => {
            setVersionId(e.target.value);
            setData(null);
            setSelected([]);
            setError(null);
            setOkMessage(null);
          }} />
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
          <dl className="kv">
            <dt>Phiên bản đã tải</dt>
            <dd>{data.tourVersionId}</dd>
            <dt>Tour</dt>
            <dd>
              {data.title}
            </dd>
            <dt>Trạng thái nội dung</dt>
            <dd>
              <span className={`pill ${data.editable ? 'draft' : 'closed'}`}>
                {data.contentStatus}
              </span>
            </dd>
            <dt>Sửa được</dt>
            <dd>{canEdit && data.editable ? 'Có' : 'Không'}</dd>
          </dl>

          <fieldset className="field interest-field">
            <legend>
              Sở thích ({selected.length}/{INTEREST_MAX})
            </legend>
            <div className="chips">
              {options
                .filter((i) => i.active || data.interests.some((assigned) => assigned.code === i.code))
                .map((i) => (
                  <button
                    key={i.code}
                    type="button"
                    className={`chip ${selected.includes(i.code) ? 'on' : ''}`}
                    aria-pressed={selected.includes(i.code)}
                    onClick={() => toggle(i.code)}
                    disabled={busy || !canEdit || !data.editable || (!i.active && !selected.includes(i.code))}
                  >
                    <Icon name="check" />
                    {i.name}{!i.active ? ' (đã tắt)' : ''}
                  </button>
                ))}
            </div>
          </fieldset>

          {canEdit && hasInactiveSelection && (
            <p className="hint">Thẻ đã tắt được giữ trong lịch sử. Bỏ chọn thẻ đã tắt trước khi thay thế danh sách.</p>
          )}
          {canEdit && (
            <button className="btn" type="button" onClick={save} disabled={busy || !data.editable || hasInactiveSelection}>
              {busy ? 'Đang lưu…' : 'Lưu danh sách sở thích'}
            </button>
          )}
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
    <section className="card card-pad admin-panel">
      <h2>Danh mục sở thích</h2>

      <ErrorNotice error={error} />
      {okMessage && <Notice kind="ok">{okMessage}</Notice>}

      <div className="table-scroll admin-table" tabIndex={0} role="region" aria-label="Bảng danh mục sở thích">
      <table>
        <thead>
          <tr>
            <th>Mã</th>
            <th>Tên</th>
            <th>Thứ tự</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
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
      </div>

      <form className="catalog-form" onSubmit={create}>
        <h3>Thêm sở thích mới</h3>
        <div className="field">
          <label htmlFor="code">Mã</label>
          <input
            id="code"
            type="text"
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
            type="text"
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
    if (user?.role === 'PRODUCT') reload();
    else setInterests([]);
  }, [user]);

  if (!user) {
    return (
      <main className="page login-page">
        <div className="login-layout">
          <div className="login-intro">
            <span className="login-icon"><Icon name="lock" /></span>
            <h1>Quản trị sở thích Tour</h1>
            <p>Quản lý danh mục sở thích và xem cách các tour được phân loại theo từng phiên bản.</p>
          </div>
          <LoginForm onLoggedIn={() => setUser(auth.getUser())} />
        </div>
      </main>
    );
  }

  const canEdit = user.role === 'PRODUCT';
  const canReadVersion = canEdit || user.role === 'MANAGER';

  return (
    <main className="page">
      <div className="page-head admin-head">
        <div>
          <h1>Quản trị sở thích Tour</h1>
          <p>Xem sở thích của từng phiên bản tour và danh mục theo quyền được cấp.</p>
        </div>
        <div className="admin-session">
          <div>
            <strong>{user.fullName}</strong>
            <span className="pill role-pill">{user.role}</span>
          </div>
          <button
            className="btn btn-ghost"
            type="button"
            onClick={() => {
              auth.clear();
              setUser(null);
            }}
          >
            Đăng xuất
          </button>
        </div>
      </div>

      <ErrorNotice error={error} />

      {!canEdit && (
        <div className="admin-access">
          <Notice kind="info">
            {canReadVersion ? 'Bạn chỉ có quyền xem sở thích của phiên bản Tour.' : 'Bạn không có quyền truy cập nội bộ Module A.'}{' '}
            Thao tác gắn sở thích và quản lý danh mục dành cho <strong>PRODUCT</strong>.
          </Notice>
        </div>
      )}

      <div className="admin-grid">
        {canReadVersion && <VersionTagEditor interests={interests} canEdit={canEdit} />}
        {canEdit && <InterestCatalogEditor interests={interests} reload={reload} />}
      </div>
    </main>
  );
}
