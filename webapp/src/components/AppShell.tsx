import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { apiClient } from '../lib/deepluna'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../lib/errors'
import type { SessionResponse } from '../../api'

const managementItems = [
  { to: '/overview', label: '总览' },
  { to: '/models', label: '模型' },
  { to: '/sessions', label: '会话管理' },
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

  const sessionsQuery = useQuery({
    queryKey: ['sessions', userId],
    enabled: typeof userId === 'number',
    queryFn: async () => {
      if (typeof userId !== 'number') return []
      const res = await apiClient.sessionController.getUserSessions(userId)
      if (!res.success) throw new Error(res.errMsg || '加载会话列表失败')
      return res.data || []
    },
  })

  const sessionList = (sessionsQuery.data ?? []) as SessionResponse[]

  return (
    <div className="flex h-screen bg-base-100 text-base-content overflow-hidden">
      {/* Sidebar */}
      <aside className="w-[280px] bg-base-200 flex flex-col border-r border-base-300 transition-all">
        <div className="p-3">
          <button
            onClick={() => navigate('/chat')}
            className="btn btn-ghost hover:bg-base-300 btn-block justify-start gap-4 items-center rounded-xl h-12"
          >
            <div className="bg-base-content text-base-100 rounded-full w-7 h-7 flex items-center justify-center font-bold text-lg leading-none pb-[2px]">+</div>
            <span className="font-semibold text-[15px]">开启新研究</span>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto scrollbar-hide px-3 py-2">
          <p className="text-xs font-semibold text-base-content/50 mb-3 px-3 uppercase tracking-wider">历史记录</p>
          <ul className="menu menu-md w-full gap-1 p-0">
            {sessionList.map((session) => (
              <li key={session.sessionId}>
                <NavLink
                  to={`/chat/${session.sessionId}`}
                  className={({ isActive }) => `block truncate py-3 px-4 rounded-xl ${isActive ? 'bg-base-300 text-base-content font-medium' : 'text-base-content/70 hover:bg-base-300/50'}`}
                >
                  <div className="flex items-center gap-3">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4 opacity-70">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z" />
                    </svg>
                    <span className="truncate">{session.summary || '未命名研究会话'}</span>
                  </div>
                </NavLink>
              </li>
            ))}
            {sessionList.length === 0 && !sessionsQuery.isLoading && (
              <div className="px-4 py-8 text-center text-sm text-base-content/50">
                暂无会话，请先开启一个研究。
              </div>
            )}
          </ul>
        </div>

        <div className="p-3">
          <div className="dropdown dropdown-top w-full">
            <div tabIndex={0} role="button" className="btn btn-ghost hover:bg-base-300 btn-block justify-start gap-3 h-auto py-3 px-4 rounded-xl">
              <div className="avatar placeholder">
                <div className="bg-primary text-primary-content rounded-full w-9">
                  <span className="text-lg font-semibold">{auth.user?.userName?.charAt(0)?.toUpperCase()}</span>
                </div>
              </div>
              <div className="flex flex-col items-start flex-1 truncate">
                <span className="text-sm font-semibold">{auth.user?.userName}</span>
                <span className={`text-[11px] font-medium ${onlineQuery.data ? 'text-success' : 'text-base-content/50'}`}>
                  {onlineQuery.data ? '● 在线' : '● 离线'}
                </span>
              </div>
            </div>
            <ul tabIndex={0} className="dropdown-content z-[1] menu p-2 shadow-xl bg-base-100 rounded-box w-full mb-3 border border-base-300">
              <li className="menu-title text-xs uppercase opacity-70"><span>管理中心</span></li>
              {managementItems.map((item) => (
                <li key={item.to}>
                  <NavLink to={item.to} className={({ isActive }) => `rounded-lg ${isActive ? 'bg-base-200' : ''}`}>
                    {item.label}
                  </NavLink>
                </li>
              ))}
              <div className="divider my-1"></div>
              <li>
                <button
                  type="button"
                  className="text-error rounded-lg hover:bg-error/10 hover:text-error"
                  onClick={() => logoutMutation.mutate()}
                  disabled={logoutMutation.isPending}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                  </svg>
                  退出登录
                </button>
              </li>
            </ul>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 bg-base-100 relative">
        {logoutMutation.error ? (
          <div className="toast toast-top toast-center z-50">
            <div className="alert alert-error shadow-lg">
              <span>{getErrorMessage(logoutMutation.error)}</span>
            </div>
          </div>
        ) : null}
        <Outlet />
      </main>
    </div>
  )
}
