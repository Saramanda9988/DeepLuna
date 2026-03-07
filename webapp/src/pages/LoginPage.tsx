import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../lib/errors'

type AuthMode = 'login' | 'register'

export function LoginPage() {
  const auth = useAuth()
  const [mode, setMode] = useState<AuthMode>('login')
  const [form, setForm] = useState({ userName: '', password: '' })
  const canSubmit = form.userName.trim().length > 0 && form.password.length > 0

  const authMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        userName: form.userName.trim(),
        password: form.password,
      }

      if (mode === 'login') {
        await auth.login(payload)
        return
      }

      await auth.register(payload)
    },
    onSuccess: () => {
      setForm({ userName: '', password: '' })
    },
  })

  if (auth.isLoggedIn) {
    return <Navigate to="/" replace />
  }

  return (
    <main className="min-h-screen p-6 md:p-10" data-theme="corporate">
      <div className="mx-auto w-full max-w-md rounded-2xl border border-base-300 bg-base-100 p-6 shadow-xl">
        <h1 className="text-2xl font-bold">DeepLuna Console</h1>
        <p className="mt-1 text-sm text-base-content/70">请先登录或注册。</p>

        <div className="mt-4 join grid grid-cols-2">
          <button
            type="button"
            className={`btn join-item ${mode === 'login' ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setMode('login')}
          >
            登录
          </button>
          <button
            type="button"
            className={`btn join-item ${mode === 'register' ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setMode('register')}
          >
            注册
          </button>
        </div>

        <form
          className="mt-5 space-y-3"
          onSubmit={(event) => {
            event.preventDefault()
            if (!canSubmit) {
              return
            }
            authMutation.mutate()
          }}
        >
          <label className="block space-y-1">
            <span className="text-sm font-medium">用户名</span>
            <input
              className="input w-full"
              value={form.userName}
              onChange={(event) => setForm((prev) => ({ ...prev, userName: event.target.value }))}
              required
            />
          </label>
          <label className="block space-y-1">
            <span className="text-sm font-medium">密码</span>
            <input
              className="input w-full"
              type="password"
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
              required
            />
          </label>
          <button className="btn btn-primary w-full" disabled={authMutation.isPending || !canSubmit}>
            {authMutation.isPending ? '提交中...' : mode === 'login' ? '登录' : '注册'}
          </button>
          {!canSubmit ? (
            <p className="text-xs text-base-content/70">请输入用户名和密码后再提交。</p>
          ) : null}
        </form>

        {authMutation.error ? (
          <div className="alert alert-error mt-3 text-sm">
            <span>{getErrorMessage(authMutation.error)}</span>
          </div>
        ) : null}
      </div>
    </main>
  )
}
