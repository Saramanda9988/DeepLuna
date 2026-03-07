import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import type { UserResponse } from '../../api'
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY, apiClient, ensureSuccess, unwrapResult } from '../lib/deepluna'

type AuthContextValue = {
  user: UserResponse | null
  isLoggedIn: boolean
  login: (payload: { userName: string; password: string }) => Promise<UserResponse>
  register: (payload: { userName: string; password: string }) => Promise<UserResponse>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function loadStoredUser(): UserResponse | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as UserResponse
    if (typeof parsed.userId === 'number' && parsed.userName) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}

function persistUser(user: UserResponse) {
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
  if (user.token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, user.token)
  }
}

function clearUserStorage() {
  localStorage.removeItem(USER_STORAGE_KEY)
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(() => loadStoredUser())

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoggedIn: typeof user?.userId === 'number',
      login: async ({ userName, password }) => {
        const result = unwrapResult(
          await apiClient.userController.login({
            userName,
            password,
          }),
          '登录失败',
        )
        persistUser(result)
        setUser(result)
        return result
      },
      register: async ({ userName, password }) => {
        const result = unwrapResult(
          await apiClient.userController.register({
            userName,
            password,
          }),
          '注册失败',
        )
        persistUser(result)
        setUser(result)
        return result
      },
      logout: async () => {
        if (typeof user?.userId === 'number') {
          ensureSuccess(await apiClient.userController.logout(user.userId), '退出登录失败')
        }
        clearUserStorage()
        setUser(null)
      },
    }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth 必须在 AuthProvider 内部使用')
  }
  return context
}
