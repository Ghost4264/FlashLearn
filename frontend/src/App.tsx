import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { useAuthStore } from './store/authStore'
import { DecksPage } from './pages/DecksPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'

function App() {
  const accessToken = useAuthStore((state) => state.accessToken)

  return (
    <Routes>
      <Route path="/login" element={accessToken ? <Navigate to="/decks" replace /> : <LoginPage />} />
      <Route path="/register" element={accessToken ? <Navigate to="/decks" replace /> : <RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/decks" element={<DecksPage />} />
      </Route>
      <Route path="*" element={<Navigate to={accessToken ? '/decks' : '/login'} replace />} />
    </Routes>
  )
}

export default App
