import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { SessionDetailResponse, SessionResponse } from '../../api'
import { useAuth } from '../context/AuthContext'
import { streamChatResponse } from '../lib/chatStream'
import { apiClient, unwrapResult } from '../lib/deepluna'
import { getErrorMessage } from '../lib/errors'

type ChatRole = 'user' | 'assistant' | 'system'

type ChatMessage = {
  id: string
  role: ChatRole
  content: string
  pending?: boolean
  createdAt: number
}

const STATUS_LABEL: Record<string, string> = {
  IDLE: '空闲',
  CLARIFYING: '澄清中',
  RUNNING: '研究中',
  REPORTING: '生成报告',
  COMPLETED: '已完成',
  FAILED: '失败',
}

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function ChatPage() {
  const auth = useAuth()
  const queryClient = useQueryClient()
  const dialogRef = useRef<HTMLDialogElement>(null)

  const userId = auth.user?.userId
  const [activeSessionId, setActiveSessionId] = useState('')
  const [composerText, setComposerText] = useState('')
  const [messagesBySession, setMessagesBySession] = useState<Record<string, ChatMessage[]>>({})
  const [startForm, setStartForm] = useState({
    modelId: '',
    question: '',
  })

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
    refetchInterval: 10000,
  })

  const modelsQuery = useQuery({
    queryKey: ['models'],
    queryFn: async () =>
      unwrapResult(await apiClient.modelController.getAllModels(), '加载模型列表失败'),
  })

  const sessionDetailQuery = useQuery({
    queryKey: ['session-detail', activeSessionId],
    enabled: Boolean(activeSessionId),
    queryFn: async () =>
      unwrapResult(
        await apiClient.sessionController.getSessionDetail(activeSessionId),
        '加载会话详情失败',
      ),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      if (status === 'CLARIFYING' || status === 'RUNNING' || status === 'REPORTING') {
        return 2000
      }
      return false
    },
  })

  useEffect(() => {
    const firstSessionId = sessionsQuery.data?.[0]?.sessionId ?? ''
    if (!activeSessionId && firstSessionId) {
      setActiveSessionId(firstSessionId)
    }
  }, [activeSessionId, sessionsQuery.data])

  useEffect(() => {
    if (startForm.modelId) {
      return
    }
    const firstModel = modelsQuery.data?.[0]
    const modelId = firstModel?.modelId || firstModel?.name || ''
    if (modelId) {
      setStartForm((prev) => ({ ...prev, modelId }))
    }
  }, [modelsQuery.data, startForm.modelId])

  const appendMessage = (sessionId: string, message: ChatMessage) => {
    setMessagesBySession((prev) => ({
      ...prev,
      [sessionId]: [...(prev[sessionId] ?? []), message],
    }))
  }

  const updateMessage = (
    sessionId: string,
    messageId: string,
    updater: (current: ChatMessage) => ChatMessage,
  ) => {
    setMessagesBySession((prev) => {
      const messages = prev[sessionId] ?? []
      return {
        ...prev,
        [sessionId]: messages.map((message) =>
          message.id === messageId ? updater(message) : message,
        ),
      }
    })
  }

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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
    },
  })

  const sendMessageMutation = useMutation({
    mutationFn: async (payload: {
      sessionId: string
      message: string
      modelId?: string
    }) => {
      const assistantMessageId = createMessageId()
      appendMessage(payload.sessionId, {
        id: createMessageId(),
        role: 'user',
        content: payload.message,
        createdAt: Date.now(),
      })
      appendMessage(payload.sessionId, {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        pending: true,
        createdAt: Date.now(),
      })

      try {
        const { finalMessage, endPayload } = await streamChatResponse(
          {
            sessionId: payload.sessionId,
            modelId: payload.modelId,
            message: payload.message,
          },
          {
            onResponse: (eventPayload) => {
              const chunk =
                (typeof eventPayload.message === 'string' && eventPayload.message) || ''
              if (!chunk) {
                return
              }
              updateMessage(payload.sessionId, assistantMessageId, (current) => ({
                ...current,
                content: `${current.content}${chunk}`,
              }))
            },
          },
        )

        return {
          sessionId: payload.sessionId,
          assistantMessageId,
          finalMessage,
          endPayload,
        }
      } catch (error) {
        updateMessage(payload.sessionId, assistantMessageId, (current) => ({
          ...current,
          pending: false,
          content: current.content || `请求失败：${getErrorMessage(error)}`,
        }))
        throw error
      }
    },
    onSuccess: (result) => {
      updateMessage(result.sessionId, result.assistantMessageId, (current) => ({
        ...current,
        pending: false,
        content: result.finalMessage || current.content || '（无响应内容）',
      }))

      queryClient.invalidateQueries({ queryKey: ['session-detail', result.sessionId] })
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
      setComposerText('')
    },
  })

  const activeMessages = messagesBySession[activeSessionId] ?? []
  const sessionList = (sessionsQuery.data ?? []) as SessionResponse[]
  const activeSessionDetail = sessionDetailQuery.data as SessionDetailResponse | undefined
  const activeStatus = activeSessionDetail?.status || 'IDLE'
  const activeStatusLabel = STATUS_LABEL[activeStatus] || activeStatus

  const canSend = Boolean(activeSessionId && composerText.trim()) && !sendMessageMutation.isPending

  const startButtonLabel = useMemo(() => {
    if (createSessionMutation.isPending || sendMessageMutation.isPending) {
      return '启动中...'
    }
    return '开启研究'
  }, [createSessionMutation.isPending, sendMessageMutation.isPending])

  return (
    <>
      <section className="grid h-[calc(100vh-14rem)] min-h-[680px] grid-cols-12 overflow-hidden rounded-2xl border border-base-300 bg-base-100 shadow-sm">
        <aside className="col-span-12 flex flex-col border-b border-base-300 bg-neutral text-neutral-content lg:col-span-3 lg:border-b-0 lg:border-r">
          <div className="flex items-center justify-between px-5 pb-3 pt-5">
            <h2 className="text-xl font-bold">DeepLuna Research</h2>
            <span className="badge badge-primary badge-outline">Beta</span>
          </div>

          <div className="px-4">
            <button
              type="button"
              className="btn btn-primary btn-block"
              onClick={() => dialogRef.current?.showModal()}
              disabled={modelsQuery.isLoading || modelsQuery.data?.length === 0}
            >
              开启一个研究
            </button>
          </div>

          <div className="mt-4 flex-1 overflow-y-auto px-3 pb-4">
            <p className="px-2 pb-2 text-xs opacity-70">会话列表</p>
            <div className="space-y-2">
              {sessionList.map((session) => (
                <button
                  key={session.sessionId || session.summary}
                  type="button"
                  className={`w-full rounded-xl px-3 py-2 text-left transition ${
                    activeSessionId === session.sessionId
                      ? 'bg-primary text-primary-content'
                      : 'bg-base-100/5 hover:bg-base-100/10'
                  }`}
                  onClick={() => {
                    setActiveSessionId(session.sessionId || '')
                    setComposerText('')
                  }}
                >
                  <p className="line-clamp-2 text-sm font-medium">
                    {session.summary || '未命名研究会话'}
                  </p>
                  <p className="mt-1 text-xs opacity-70">{session.sessionId}</p>
                </button>
              ))}
              {sessionList.length === 0 ? (
                <p className="px-2 py-3 text-sm opacity-70">暂无会话，请先开启一个研究。</p>
              ) : null}
            </div>
          </div>
        </aside>

        <div className="col-span-12 flex min-h-0 flex-col bg-base-200/40 lg:col-span-9">
          <header className="border-b border-base-300 bg-base-100 px-5 py-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="badge badge-outline">Session: {activeSessionId || '-'}</span>
              <span className="badge badge-primary">状态: {activeStatusLabel}</span>
              {(activeStatus === 'RUNNING' || activeStatus === 'REPORTING') && (
                <span className="badge badge-info">研究已开始，正在后台执行</span>
              )}
              {activeStatus === 'CLARIFYING' && (
                <span className="badge badge-warning">可能需要继续回答澄清问题</span>
              )}
            </div>
          </header>

          <div className="flex-1 overflow-y-auto px-6 py-6">
            {activeSessionId ? (
              <div className="mx-auto flex max-w-4xl flex-col gap-4">
                {activeMessages.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-base-300 bg-base-100 p-6 text-sm text-base-content/70">
                    请输入你的研究问题。模型会先进行澄清，问题足够清晰后将自动进入研究流程。
                  </div>
                ) : (
                  activeMessages.map((message) => (
                    <div
                      key={message.id}
                      className={`chat ${message.role === 'user' ? 'chat-end' : 'chat-start'}`}
                    >
                      <div
                        className={`chat-bubble whitespace-pre-wrap ${
                          message.role === 'user'
                            ? 'chat-bubble-primary'
                            : message.role === 'system'
                              ? 'chat-bubble-warning'
                              : ''
                        }`}
                      >
                        {message.content || (message.pending ? '思考中...' : '')}
                        {message.pending ? (
                          <span className="loading loading-dots loading-xs ml-2 inline-block" />
                        ) : null}
                      </div>
                    </div>
                  ))
                )}
              </div>
            ) : (
              <div className="flex h-full items-center justify-center">
                <div className="rounded-2xl border border-base-300 bg-base-100 p-10 text-center">
                  <h3 className="text-2xl font-bold">准备开始一个新研究</h3>
                  <p className="mt-2 text-base-content/70">
                    点击左侧“开启一个研究”，输入问题后进入澄清对话流程。
                  </p>
                </div>
              </div>
            )}
          </div>

          <footer className="border-t border-base-300 bg-base-100 px-5 py-4">
            <form
              className="mx-auto flex max-w-4xl items-end gap-3"
              onSubmit={(event) => {
                event.preventDefault()
                if (!canSend || !activeSessionId) {
                  return
                }
                sendMessageMutation.mutate({
                  sessionId: activeSessionId,
                  message: composerText.trim(),
                  modelId: activeSessionDetail?.model || undefined,
                })
              }}
            >
              <textarea
                className="textarea textarea-lg w-full resize-none"
                placeholder={
                  activeSessionId
                    ? '继续补充需求或回答澄清问题...'
                    : '请先在左侧开启一个研究'
                }
                rows={2}
                value={composerText}
                onChange={(event) => setComposerText(event.target.value)}
                disabled={!activeSessionId || sendMessageMutation.isPending}
              />
              <button type="submit" className="btn btn-primary btn-lg" disabled={!canSend}>
                发送
              </button>
            </form>
            {sendMessageMutation.error ? (
              <div className="mx-auto mt-2 max-w-4xl alert alert-error text-sm">
                <span>{getErrorMessage(sendMessageMutation.error)}</span>
              </div>
            ) : null}
            {sessionsQuery.error ? (
              <div className="mx-auto mt-2 max-w-4xl alert alert-error text-sm">
                <span>{getErrorMessage(sessionsQuery.error)}</span>
              </div>
            ) : null}
          </footer>
        </div>
      </section>

      <dialog ref={dialogRef} className="modal">
        <div className="modal-box">
          <h3 className="text-lg font-bold">开启一个研究</h3>
          <p className="mt-1 text-sm text-base-content/70">
            输入初始问题后，系统会先进行澄清，再自动开始研究。
          </p>
          <form
            className="mt-4 space-y-3"
            onSubmit={async (event) => {
              event.preventDefault()
              const question = startForm.question.trim()
              if (!startForm.modelId || !question) {
                return
              }

              try {
                const sessionId = await createSessionMutation.mutateAsync(startForm.modelId)
                setActiveSessionId(sessionId)
                dialogRef.current?.close()
                await sendMessageMutation.mutateAsync({
                  sessionId,
                  message: question,
                  modelId: startForm.modelId,
                })
                setStartForm((prev) => ({ ...prev, question: '' }))
              } catch {
                // 错误状态由 mutation 的 error UI 展示
              }
            }}
          >
            <label className="block space-y-1">
              <span className="text-sm font-medium">模型</span>
              <select
                className="select w-full"
                value={startForm.modelId}
                onChange={(event) =>
                  setStartForm((prev) => ({ ...prev, modelId: event.target.value }))
                }
                required
              >
                {(modelsQuery.data ?? []).map((model) => (
                  <option key={model.modelId || model.name} value={model.modelId || model.name || ''}>
                    {model.name || model.modelId}
                  </option>
                ))}
              </select>
            </label>
            <label className="block space-y-1">
              <span className="text-sm font-medium">研究问题</span>
              <textarea
                className="textarea w-full"
                rows={4}
                placeholder="例如：请研究 2026 年 A 股半导体行业景气度，并给出核心驱动因素"
                value={startForm.question}
                onChange={(event) =>
                  setStartForm((prev) => ({ ...prev, question: event.target.value }))
                }
                required
              />
            </label>

            <div className="modal-action">
              <button type="submit" className="btn btn-primary" disabled={!startForm.question.trim()}>
                {startButtonLabel}
              </button>
              <button type="button" className="btn" onClick={() => dialogRef.current?.close()}>
                取消
              </button>
            </div>
            {createSessionMutation.error ? (
              <div className="alert alert-error text-sm">
                <span>{getErrorMessage(createSessionMutation.error)}</span>
              </div>
            ) : null}
          </form>
        </div>
        <form method="dialog" className="modal-backdrop">
          <button>close</button>
        </form>
      </dialog>
    </>
  )
}
