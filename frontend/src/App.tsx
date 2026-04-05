import { Navigate, Route, Routes } from 'react-router-dom'
import { IdleSessionWatcher } from './components/IdleSessionWatcher'
import { ProtectedRoute } from './components/ProtectedRoute'
import { useAuthStore } from './store/authStore'
import { DecksPage } from './pages/DecksPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { StudyPage } from './pages/StudyPage'
import { DeckDetailPage } from './pages/DeckDetailPage'
import { ProfilePage } from './pages/ProfilePage'
import { AdminPage } from './pages/AdminPage'
import { HomePage } from './pages/HomePage'
import { FaqPage } from './pages/FaqPage'

function App() {
  const accessToken = useAuthStore((state) => state.accessToken)

  return (
    <>
      <IdleSessionWatcher />
      <Routes>
        <Route path="/" element={accessToken ? <Navigate to="/decks" replace /> : <HomePage />} />
        <Route path="/login" element={accessToken ? <Navigate to="/decks" replace /> : <LoginPage />} />
        <Route path="/register" element={accessToken ? <Navigate to="/decks" replace /> : <RegisterPage />} />
        <Route path="/faq" element={<FaqPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/decks" element={<DecksPage />} />
          <Route path="/decks/:id" element={<DeckDetailPage />} />
          <Route path="/study" element={<StudyPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Route>
        <Route path="*" element={<Navigate to={accessToken ? '/decks' : '/'} replace />} />
      </Routes>
    </>
  )
}

export default App
