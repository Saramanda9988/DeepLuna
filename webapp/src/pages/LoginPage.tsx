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
    <main className="flex min-h-screen items-center justify-center bg-base-200 p-6 md:p-10">
      <div className="w-full max-w-md rounded-box border border-base-300 bg-base-100 p-8 shadow-2xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold tracking-tight text-base-content">DeepLuna Console</h1>
          <p className="mt-2 text-sm text-base-content/70">Welcome back. Please login or register to continue.</p>
        </div>

        <div className="tabs tabs-boxed mb-6 p-1 bg-base-200 flex">
          <button
            type="button"
            className={`tab flex-1 h-10 ${mode === 'login' ? 'tab-active font-bold shadow-sm bg-base-100 text-base-content' : 'text-base-content/70'}`}
            onClick={() => setMode('login')}
          >
            登录
          </button>
          <button
            type="button"
            className={`tab flex-1 h-10 ${mode === 'register' ? 'tab-active font-bold shadow-sm bg-base-100 text-base-content' : 'text-base-content/70'}`}
            onClick={() => setMode('register')}
          >
            注册
          </button>
        </div>

        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            if (!canSubmit) return
            authMutation.mutate()
          }}
        >
          <div className="form-control w-full">
            <label className="label">
              <span className="label-text font-medium text-base-content">用户名</span>
            </label>
            <input
              type="text"
              className="input input-bordered w-full focus:input-primary transition-colors"
              placeholder="请输入用户名..."
              value={form.userName}
              onChange={(event) => setForm((prev) => ({ ...prev, userName: event.target.value }))}
              required
            />
          </div>

          <div className="form-control w-full">
            <label className="label">
              <span className="label-text font-medium text-base-content">密码</span>
            </label>
            <input
              type="password"
              className="input input-bordered w-full focus:input-primary transition-colors"
              placeholder="请输入密码..."
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
              required
            />
          </div>

          <div className="pt-2">
            <button 
              className="btn btn-primary w-full h-12 text-base" 
              disabled={authMutation.isPending || !canSubmit}
            >
              {authMutation.isPending ? (
                <>
                  <span className="loading loading-spinner"></span>
                  提交中...
                </>
              ) : mode === 'login' ? '登 录' : '注 册'}
            </button>
          </div>
          
          {!canSubmit && (
            <p className="text-center text-xs text-base-content/50 mt-2">
              请输入用户名和密码后再提交。
            </p>
          )}
        </form>

        {authMutation.error && (
          <div className="alert alert-error mt-4 shadow-sm">
            <svg xmlns="http://www.w3.org/2000/svg" className="stroke-current shrink-0 h-5 w-5" fill="none" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            <span className="text-sm">{getErrorMessage(authMutation.error)}</span>
          </div>
        )}
      </div>
    </main>
  )
}
