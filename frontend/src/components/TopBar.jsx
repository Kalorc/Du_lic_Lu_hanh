import { NavLink } from 'react-router-dom';
import { auth } from '../api/client.js';

export default function TopBar() {
  const user = auth.getUser();

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <div className="brand">
          Du lịch &amp; Lữ hành <span>Thông minh</span>
        </div>
        <nav className="nav">
          <NavLink to="/tours" className={({ isActive }) => (isActive ? 'active' : '')}>
            Tra cứu &amp; Gợi ý tour
          </NavLink>
          <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
            Quản trị sở thích
          </NavLink>
          {user && (
            <span className="who">
              {user.fullName} · {user.role}
            </span>
          )}
        </nav>
      </div>
    </header>
  );
}
