import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import type { SessionResponse } from '../../api'
import { useAuth } from '../context/AuthContext'
import { streamChatResponse } from '../lib/chatStream'
import { apiClient, unwrapResult } from '../lib/deepluna'
import { getErrorMessage } from '../lib/errors'

type ChatMessage = { id: number; role: 'user' | 'assistant'; content: string }

export function ChatPage() {
  const auth = useAuth()
  const userId = auth.user?.userId
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatForm, setChatForm] = useState({
    sessionId: '',
    modelId: '',
    message: '',
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
  })

  useEffect(() => {
    if (chatForm.sessionId) {
      return
    }
    const firstSessionId = sessionsQuery.data?.[0]?.sessionId ?? ''
    if (firstSessionId) {
      setChatForm((prev) => ({ ...prev, sessionId: firstSessionId }))
    }
  }, [chatForm.sessionId, sessionsQuery.data])

  const chatMutation = useMutation({
    mutationFn: async (payload: { sessionId: string; modelId?: string; message: string }) =>
      streamChatResponse(payload),
    onMutate: ({ message }) => {
      setChatMessages((prev) => [
        ...prev,
        {
          id: Date.now(),
          role: 'user',
          content: message,
        },
      ])
    },
    onSuccess: (content) => {
      setChatMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          role: 'assistant',
          content: content || '（接口返回为空）',
        },
      ])
      setChatForm((prev) => ({ ...prev, message: '' }))
    },
    onError: (error) => {
      setChatMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 2,
          role: 'assistant',
          content: `请求失败: ${getErrorMessage(error)}`,
        },
      ])
    },
  })

  const sessionList = (sessionsQuery.data ?? []) as SessionResponse[]

  return (
    <section className="card border border-base-300 bg-base-100 shadow-sm">
      <div className="card-body">
        <h2 className="card-title">流式聊天</h2>
        <form
          className="grid gap-3 md:grid-cols-12"
          onSubmit={(event) => {
            event.preventDefault()
            if (!chatForm.sessionId || !chatForm.message.trim()) {
              return
            }
            chatMutation.mutate({
              sessionId: chatForm.sessionId,
              modelId: chatForm.modelId.trim() || undefined,
              message: chatForm.message.trim(),
            })
          }}
        >
          <select
            className="select w-full md:col-span-3"
            value={chatForm.sessionId}
            onChange={(event) => setChatForm((prev) => ({ ...prev, sessionId: event.target.value }))}
          >
            <option value="">选择 Session</option>
            {sessionList.map((session) => (
              <option key={session.sessionId || session.summary} value={session.sessionId || ''}>
                {session.summary || session.sessionId}
              </option>
            ))}
          </select>
          <input
            className="input w-full md:col-span-3"
            placeholder="modelId（可选）"
            value={chatForm.modelId}
            onChange={(event) => setChatForm((prev) => ({ ...prev, modelId: event.target.value }))}
          />
          <input
            className="input w-full md:col-span-4"
            placeholder="请输入消息"
            value={chatForm.message}
            onChange={(event) => setChatForm((prev) => ({ ...prev, message: event.target.value }))}
            required
          />
          <button className="btn btn-primary md:col-span-2" disabled={chatMutation.isPending}>
            发送
          </button>
        </form>

        <div className="mt-2 max-h-[420px] space-y-2 overflow-auto rounded-lg border border-base-300 bg-base-200/30 p-3">
          {chatMessages.length === 0 ? (
            <p className="text-sm text-base-content/60">暂无聊天消息。</p>
          ) : (
            chatMessages.map((message) => (
              <div
                key={message.id}
                className={`chat ${message.role === 'user' ? 'chat-end' : 'chat-start'}`}
              >
                <div className="chat-bubble whitespace-pre-wrap">{message.content}</div>
              </div>
            ))
          )}
        </div>
      </div>
    </section>
  )
}
