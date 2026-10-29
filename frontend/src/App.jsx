import { Navigate, Route, Routes } from 'react-router-dom';
import TopBar from './components/TopBar.jsx';
import SearchPage from './pages/SearchPage.jsx';
import TourDetailPage from './pages/TourDetailPage.jsx';
import AdminPage from './pages/AdminPage.jsx';

export default function App() {
  return (
    <>
      <TopBar />
      <Routes>
        <Route path="/" element={<Navigate to="/tours" replace />} />
        <Route path="/tours" element={<SearchPage />} />
        <Route path="/tours/:tourId" element={<TourDetailPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<Navigate to="/tours" replace />} />
      </Routes>
    </>
  );
}
