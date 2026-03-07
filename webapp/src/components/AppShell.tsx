import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { apiClient, API_BASE_URL } from '../lib/deepluna'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../lib/errors'

const navItems = [
  { to: '/', label: '总览' },
  { to: '/models', label: '模型' },
  { to: '/sessions', label: '会话' },
  { to: '/chat', label: '聊天' },
  { to: '/system', label: '系统' },
]

export function AppShell() {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const userId = auth.user?.userId

  const onlineQuery = useQuery({
    queryKey: ['user-online', userId],
    enabled: typeof userId === 'number',
    queryFn: async () => {
      if (typeof userId !== 'number') {
        return false
      }
      const result = await apiClient.userController.isOnline(userId)
      return Boolean(result.success && result.data)
    },
  })

  const logoutMutation = useMutation({
    mutationFn: async () => auth.logout(),
    onSuccess: () => {
      queryClient.clear()
      navigate('/login', { replace: true })
    },
  })

  return (
    <main className="min-h-screen p-4 md:p-8" data-theme="corporate">
      <div className="mx-auto max-w-7xl space-y-4">
        <header className="navbar rounded-xl border border-base-300 bg-base-100 shadow-sm">
          <div className="flex-1 px-2">
            <div>
              <h1 className="text-xl font-bold">DeepLuna Console</h1>
              <p className="text-xs text-base-content/60">API Base: {API_BASE_URL}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="badge badge-outline">{auth.user?.userName}</span>
            <span className={`badge ${onlineQuery.data ? 'badge-success' : 'badge-warning'}`}>
              {onlineQuery.data ? '在线' : '离线'}
            </span>
            <button
              type="button"
              className="btn btn-sm btn-ghost"
              onClick={() => logoutMutation.mutate()}
              disabled={logoutMutation.isPending}
            >
              退出
            </button>
          </div>
        </header>

        <nav className="tabs tabs-box w-fit bg-base-300/40 p-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => `tab ${isActive ? 'tab-active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        {logoutMutation.error ? (
          <div className="alert alert-error text-sm">
            <span>{getErrorMessage(logoutMutation.error)}</span>
          </div>
        ) : null}

        <Outlet />
      </div>
    </main>
  )
}
