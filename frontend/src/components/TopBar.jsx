import { Link, NavLink } from 'react-router-dom';
import Icon from './Icon.jsx';

export default function TopBar() {
  return (
    <header className="topbar">
      <div className="topbar-inner">
        <Link to="/tours" className="brand">
          <span className="brand-icon"><Icon name="compass" /></span>
          <span>
            Du lịch &amp; Lữ hành
            <small>Thông minh</small>
          </span>
        </Link>
        <nav className="nav" aria-label="Điều hướng chính">
          <NavLink to="/tours" className={({ isActive }) => (isActive ? 'active' : '')}>
            <Icon name="compass" />
            Tra cứu &amp; Gợi ý tour
          </NavLink>
          <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
            <Icon name="lock" />
            Quản trị sở thích
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
