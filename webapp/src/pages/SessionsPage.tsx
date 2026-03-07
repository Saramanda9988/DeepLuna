import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import type { SessionDetailResponse, SessionResponse } from '../../api'
import { useAuth } from '../context/AuthContext'
import { apiClient, ensureSuccess, unwrapResult } from '../lib/deepluna'
import { getErrorMessage } from '../lib/errors'

export function SessionsPage() {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const [selectedSessionId, setSelectedSessionId] = useState('')
  const [sessionModel, setSessionModel] = useState('')
  const [sessionEditForm, setSessionEditForm] = useState({
    summary: '',
    researchBrief: '',
  })

  const userId = auth.user?.userId

  const sessionsQuery = useQuery({
    queryKey: ['sessions', userId],
    enabled: typeof userId === 'number',
    queryFn: async () => {
      if (typeof userId !== 'number') {
        return []
      }
      return unwrapResult(
        await apiClient.sessionController.getUserSessions(userId),
        '加载会话列表失败',
      )
    },
  })

  const modelsQuery = useQuery({
    queryKey: ['models'],
    queryFn: async () =>
      unwrapResult(await apiClient.modelController.getAllModels(), '加载模型列表失败'),
  })

  const sessionDetailQuery = useQuery({
    queryKey: ['session-detail', selectedSessionId],
    enabled: Boolean(selectedSessionId),
    queryFn: async () =>
      unwrapResult(
        await apiClient.sessionController.getSessionDetail(selectedSessionId),
        '加载会话详情失败',
      ),
  })

  useEffect(() => {
    const firstSessionId = sessionsQuery.data?.[0]?.sessionId ?? ''
    if (!selectedSessionId && firstSessionId) {
      setSelectedSessionId(firstSessionId)
    }
  }, [sessionsQuery.data, selectedSessionId])

  useEffect(() => {
    if (sessionModel) {
      return
    }

    const firstModel = modelsQuery.data?.[0]
    const nextValue = firstModel?.modelId || firstModel?.name || ''
    if (nextValue) {
      setSessionModel(nextValue)
    }
  }, [modelsQuery.data, sessionModel])

  useEffect(() => {
    if (!sessionDetailQuery.data) {
      return
    }

    setSessionEditForm({
      summary: sessionDetailQuery.data.summary ?? '',
      researchBrief: sessionDetailQuery.data.researchBrief ?? '',
    })
  }, [
    sessionDetailQuery.data?.summary,
    sessionDetailQuery.data?.researchBrief,
    sessionDetailQuery.data?.sessionId,
  ])

  const createSessionMutation = useMutation({
    mutationFn: async (model: string) => {
      if (typeof userId !== 'number') {
        throw new Error('用户未登录')
      }
      return unwrapResult(
        await apiClient.sessionController.createSession({
          userId,
          model,
        }),
        '创建会话失败',
      )
    },
    onSuccess: (sessionId) => {
      setSelectedSessionId(sessionId)
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
    },
  })

  const deleteSessionMutation = useMutation({
    mutationFn: async (sessionId: string) => {
      ensureSuccess(await apiClient.sessionController.deleteSession(sessionId), '删除会话失败')
    },
    onSuccess: (_, deletedSessionId) => {
      if (selectedSessionId === deletedSessionId) {
        setSelectedSessionId('')
      }
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
      queryClient.invalidateQueries({ queryKey: ['session-detail'] })
    },
  })

  const updateSessionMutation = useMutation({
    mutationFn: async (payload: {
      sessionId: string
      summary: string
      researchBrief: string
    }) => {
      ensureSuccess(
        await apiClient.sessionController.updateSession(payload.sessionId, {
          summary: payload.summary,
          researchBrief: payload.researchBrief,
        }),
        '更新会话失败',
      )
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
      queryClient.invalidateQueries({ queryKey: ['session-detail', selectedSessionId] })
    },
  })

  const modelOptions = useMemo(() => {
    return (modelsQuery.data ?? []).map((model) => ({
      id: model.modelId || model.name || '',
      label: model.name || model.modelId || '未命名模型',
    }))
  }, [modelsQuery.data])

  const sessionList = (sessionsQuery.data ?? []) as SessionResponse[]
  const currentSession = sessionDetailQuery.data as SessionDetailResponse | undefined

  return (
    <section className="grid gap-4 lg:grid-cols-3">
      <div className="card border border-base-300 bg-base-100 shadow-sm lg:col-span-1">
        <div className="card-body">
          <h2 className="card-title">会话列表</h2>
          <form
            className="mb-3 flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault()
              if (!sessionModel) {
                return
              }
              createSessionMutation.mutate(sessionModel)
            }}
          >
            <select
              className="select w-full"
              value={sessionModel}
              onChange={(event) => setSessionModel(event.target.value)}
            >
              {modelOptions.map((item) => (
                <option key={item.id || item.label} value={item.id}>
                  {item.label}
                </option>
              ))}
            </select>
            <button className="btn btn-primary" disabled={createSessionMutation.isPending}>
              新建
            </button>
          </form>
          <div className="space-y-2">
            {sessionList.map((session) => (
              <div
                key={session.sessionId || session.summary}
                className={`rounded-lg border p-3 ${
                  selectedSessionId === session.sessionId
                    ? 'border-primary bg-primary/5'
                    : 'border-base-300'
                }`}
              >
                <button
                  type="button"
                  className="w-full text-left"
                  onClick={() => setSelectedSessionId(session.sessionId || '')}
                >
                  <p className="font-medium">{session.summary || '未命名会话'}</p>
                  <p className="text-xs text-base-content/60">{session.sessionId}</p>
                </button>
                <div className="mt-2 text-right">
                  <button
                    type="button"
                    className="btn btn-xs btn-error"
                    onClick={() => {
                      if (session.sessionId) {
                        deleteSessionMutation.mutate(session.sessionId)
                      }
                    }}
                    disabled={!session.sessionId || deleteSessionMutation.isPending}
                  >
                    删除
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card border border-base-300 bg-base-100 shadow-sm lg:col-span-2">
        <div className="card-body">
          <h2 className="card-title">会话详情</h2>
          {!currentSession ? (
            <p className="text-sm text-base-content/70">请选择一个会话查看详情。</p>
          ) : (
            <form
              className="space-y-3"
              onSubmit={(event) => {
                event.preventDefault()
                if (!selectedSessionId) {
                  return
                }
                updateSessionMutation.mutate({
                  sessionId: selectedSessionId,
                  summary: sessionEditForm.summary,
                  researchBrief: sessionEditForm.researchBrief,
                })
              }}
            >
              <div className="grid gap-3 md:grid-cols-2">
                <label className="block space-y-1">
                  <span className="text-sm font-medium">Session ID</span>
                  <input
                    className="input w-full"
                    value={currentSession.sessionId || ''}
                    readOnly
                  />
                </label>
                <label className="block space-y-1">
                  <span className="text-sm font-medium">状态</span>
                  <input className="input w-full" value={currentSession.status || ''} readOnly />
                </label>
              </div>
              <label className="block space-y-1">
                <span className="text-sm font-medium">摘要</span>
                <input
                  className="input w-full"
                  value={sessionEditForm.summary}
                  onChange={(event) =>
                    setSessionEditForm((prev) => ({ ...prev, summary: event.target.value }))
                  }
                />
              </label>
              <label className="block space-y-1">
                <span className="text-sm font-medium">Research Brief</span>
                <textarea
                  className="textarea min-h-32 w-full"
                  value={sessionEditForm.researchBrief}
                  onChange={(event) =>
                    setSessionEditForm((prev) => ({
                      ...prev,
                      researchBrief: event.target.value,
                    }))
                  }
                />
              </label>
              <button className="btn btn-primary" disabled={updateSessionMutation.isPending}>
                保存会话
              </button>
            </form>
          )}
        </div>
      </div>

      {sessionsQuery.error || sessionDetailQuery.error ? (
        <div className="alert alert-error lg:col-span-3 text-sm">
          <span>{getErrorMessage(sessionsQuery.error || sessionDetailQuery.error)}</span>
        </div>
      ) : null}
    </section>
  )
}
