import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { streamChatResponse } from '../lib/chatStream'
import { apiClient, unwrapResult } from '../lib/deepluna'
import { getErrorMessage } from '../lib/errors'
import { useSessionProgress } from '../hooks/useSessionProgress'

type ChatRole = 'user' | 'assistant' | 'system'

type ChatMessage = {
  id: string
  role: ChatRole
  content: string
  pending?: boolean
  createdAt: number
}

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function ChatPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const auth = useAuth()
  const queryClient = useQueryClient()
  const userId = auth.user?.userId;

  const [composerText, setComposerText] = useState('')
  const [messagesBySession, setMessagesBySession] = useState<Record<string, ChatMessage[]>>({})
  const [selectedModelId, setSelectedModelId] = useState<string>('')
  const scrollRef = useRef<HTMLDivElement>(null)

  const modelsQuery = useQuery({
    queryKey: ['models'],
    queryFn: async () => unwrapResult(await apiClient.modelController.getAllModels(), '加载模型列表失败'),
  })

  const sessionDetailQuery = useQuery({
    queryKey: ['session-detail', sessionId],
    enabled: Boolean(sessionId),
    queryFn: async () =>
      unwrapResult(
        await apiClient.sessionController.getSessionDetail(sessionId!),
        '加载会话详情失败',
      ),
  })

  // Hook for realtime progress
  const { progress } = useSessionProgress(sessionId)

  // Derive the effective model id: for existing sessions use session model, for new use selected
  const sessionModelId = sessionDetailQuery.data?.model
  const effectiveModelId = sessionId ? (sessionModelId || selectedModelId) : selectedModelId

  const appendMessage = (id: string, message: ChatMessage) => {
    setMessagesBySession((prev) => ({
      ...prev,
      [id]: [...(prev[id] ?? []), message],
    }))
  }

  const updateMessage = (
    id: string,
    messageId: string,
    updater: (current: ChatMessage) => ChatMessage,
  ) => {
    setMessagesBySession((prev) => {
      const messages = prev[id] ?? []
      return {
        ...prev,
        [id]: messages.map((m) => (m.id === messageId ? updater(m) : m)),
      }
    })
  }

  const createSessionMutation = useMutation({
    mutationFn: async (payload: { model: string, initialMessage: string }) => {
      if (typeof userId !== 'number') throw new Error('用户未登录')
      const sid = unwrapResult(
        await apiClient.sessionController.createSession({ userId, model: payload.model }),
        '创建会话失败'
      )
      return sid
    },
    onSuccess: (sid, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
      navigate(`/chat/${sid}`, { state: { initialMessage: variables.initialMessage }, replace: true })
    }
  })

  const sendMessageMutation = useMutation({
    mutationFn: async (payload: { sid: string; message: string; modelId?: string }) => {
      const assistantMessageId = createMessageId()
      appendMessage(payload.sid, {
        id: createMessageId(),
        role: 'user',
        content: payload.message,
        createdAt: Date.now(),
      })
      appendMessage(payload.sid, {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        pending: true,
        createdAt: Date.now(),
      })

      try {
        const { finalMessage, endPayload } = await streamChatResponse(
          {
            sessionId: payload.sid,
            modelId: payload.modelId,
            message: payload.message,
          },
          {
            onResponse: (eventPayload) => {
              const chunk = (typeof eventPayload.message === 'string' && eventPayload.message) || ''
              if (chunk) {
                updateMessage(payload.sid, assistantMessageId, (current) => ({
                  ...current,
                  content: `${current.content}${chunk}`,
                }))
              }
            },
          },
        )
        return { sid: payload.sid, assistantMessageId, finalMessage, endPayload }
      } catch (error) {
        updateMessage(payload.sid, assistantMessageId, (current) => ({
          ...current,
          pending: false,
          content: current.content || `请求失败：${getErrorMessage(error)}`,
        }))
        throw error
      }
    },
    onSuccess: (result) => {
      updateMessage(result.sid, result.assistantMessageId, (current) => ({
        ...current,
        pending: false,
        content: result.finalMessage || current.content || '（无响应内容）',
      }))
      queryClient.invalidateQueries({ queryKey: ['session-detail', result.sid] })
      queryClient.invalidateQueries({ queryKey: ['sessions'] })
    },
  })

  // Trigger initial message if navigated from new chat
  const sendMessageMutateRef = useRef(sendMessageMutation.mutate)
  sendMessageMutateRef.current = sendMessageMutation.mutate

  useEffect(() => {
    if (sessionId && location.state?.initialMessage) {
      const message = location.state.initialMessage
      const modelId = sessionDetailQuery.data?.model || selectedModelId || undefined
      navigate(`/chat/${sessionId}`, { replace: true, state: {} })
      sendMessageMutateRef.current({ sid: sessionId, message, modelId })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId, location.state])

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messagesBySession[sessionId || '']])

  const handleSend = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!composerText.trim() || createSessionMutation.isPending || sendMessageMutation.isPending) return

    if (!sessionId) {
      // New session — require a selected model
      if (!selectedModelId) return
      createSessionMutation.mutate({ model: selectedModelId, initialMessage: composerText.trim() })
    } else {
      // Existing session
      sendMessageMutation.mutate({
        sid: sessionId,
        message: composerText.trim(),
        modelId: effectiveModelId || undefined
      })
    }
    setComposerText('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  const isWorking = createSessionMutation.isPending || sendMessageMutation.isPending;
  const activeMessages = sessionId ? messagesBySession[sessionId] ?? [] : []
  const canSend = !isWorking && !!composerText.trim() && (!sessionId ? !!selectedModelId : true)

  return (
    <div className="flex flex-col h-full relative bg-base-100">
      {/* Top Header / Progress Area */}
      {sessionId && progress ? (
        <div className="absolute top-0 w-full z-10 p-2 sm:p-4 pointer-events-none">
           <div className="max-w-3xl mx-auto pointer-events-auto border border-base-300 rounded-box bg-base-100/90 backdrop-blur shadow-sm">
             <div className="collapse collapse-arrow">
               <input type="checkbox" />
               <div className="collapse-title text-sm font-medium flex items-center gap-3">
                 {progress.sessionStatus === 'RUNNING' || progress.sessionStatus === 'REPORTING' ? (
                    <span className="loading loading-spinner loading-xs text-primary"></span>
                 ) : (
                    <div className="bg-success w-2 h-2 rounded-full"></div>
                 )}
                 <div>
                   <span>Research Status: {progress.sessionStatus}</span>
                   <span className="text-base-content/60 ml-3 text-xs">{progress.latestMessage}</span>
                 </div>
               </div>
               <div className="collapse-content text-xs">
                 <div className="flex gap-4 mb-2 opacity-75">
                   <span>Sup Status: {progress.supervisorState}</span>
                   <span>Agents Running: {progress.runningSubAgents} / {progress.totalSubAgents}</span>
                 </div>
                 {progress.subAgents && progress.subAgents.length > 0 && (
                   <ul className="space-y-1 mt-2">
                     {progress.subAgents.map(ag => (
                       <li key={ag.subAgentId} className="flex gap-2">
                         <span className="w-20 font-bold opacity-70">{ag.status}</span>
                         <span>{ag.researchTopic}</span>
                       </li>
                     ))}
                   </ul>
                 )}
               </div>
             </div>
           </div>
        </div>
      ) : null}

      {/* Main Chat/Scroll Area */}
      <div className="flex-1 overflow-y-auto scrollbar-hide w-full pb-36" ref={scrollRef}>
        {!sessionId ? (
          // Empty State
          <div className="h-full flex flex-col items-center justify-center p-4">
            <div className="avatar placeholder mb-6">
              <div className="bg-primary text-primary-content rounded-full w-20 shadow-lg">
                <span className="text-3xl font-bold">L</span>
              </div>
            </div>
            <h1 className="text-3xl font-bold mb-2 text-base-content">DeepLuna Research</h1>
            <p className="text-base-content/60 text-lg mb-6">今天我能帮您研究什么？</p>
            {/* Model Selector */}
            <div className="w-full max-w-xs">
              <select
                className="select select-bordered w-full"
                value={selectedModelId}
                onChange={(e) => setSelectedModelId(e.target.value)}
                disabled={modelsQuery.isLoading}
              >
                <option value="" disabled>
                  {modelsQuery.isLoading ? '加载模型中...' : '请选择模型'}
                </option>
                {modelsQuery.data?.map((m) => {
                  const id = m.modelId || m.name || ''
                  return (
                    <option key={id} value={id}>
                      {m.name || id}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
        ) : (
          <div className="max-w-3xl mx-auto pt-16 px-4 py-6 flex flex-col gap-8">
             {activeMessages.map((message) => (
                <div key={message.id} className={`flex gap-4 w-full ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  {message.role === 'assistant' && (
                    <div className="avatar placeholder shrink-0 mt-1">
                      <div className="bg-primary text-primary-content rounded-full w-8 h-8 shadow-sm">
                        <span className="text-xs font-bold">AI</span>
                      </div>
                    </div>
                  )}
                  <div className={`max-w-[85%] rounded-2xl px-5 py-3.5 text-[15px] leading-relaxed whitespace-pre-wrap shadow-sm ${
                    message.role === 'user'
                      ? 'bg-base-200 text-base-content rounded-tr-none'
                      : 'bg-base-100 text-base-content border border-base-200 rounded-tl-none'
                  }`}>
                    {message.content || (message.pending ? '思考中...' : '')}
                    {message.pending && message.content === '' && (
                      <span className="loading loading-dots loading-xs opacity-50 ml-2 inline-block align-middle"></span>
                    )}
                  </div>
                </div>
             ))}
          </div>
        )}
      </div>

      {/* Footer Input Area */}
      <div className="absolute bottom-0 w-full bg-gradient-to-t from-base-100 via-base-100 to-transparent pt-6 pb-6 px-4">
        <div className="max-w-3xl mx-auto relative">
          <form
            onSubmit={handleSend}
            className="flex flex-col bg-base-100 border border-base-300 rounded-2xl shadow-lg focus-within:border-base-content/30 focus-within:ring-1 focus-within:ring-base-content/30 transition-all"
          >
            <textarea
              className="textarea w-full resize-none bg-transparent border-none focus:outline-none scrollbar-hide py-4 px-4 min-h-[56px] max-h-[200px] text-[15px] leading-relaxed"
              rows={1}
              placeholder={!sessionId && !selectedModelId ? '请先选择模型...' : '发送消息给 DeepLuna...'}
              value={composerText}
              onChange={(e) => setComposerText(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isWorking || (!sessionId && !selectedModelId)}
            />
            <div className="flex justify-between items-center px-3 pb-3">
              <div className="flex gap-2">
                {/* Optional bottom left actions can go here */}
              </div>
              <button
                type="submit"
                disabled={!canSend}
                className={`btn btn-circle btn-sm border-none shadow-none transition-colors ${
                  canSend
                    ? 'bg-primary text-primary-content hover:bg-primaryFocus'
                    : 'bg-base-200 text-base-content/30'
                }`}
              >
                {isWorking ? (
                   <span className="loading loading-spinner loading-xs"></span>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M5 12L19 12M19 12L12 5M19 12L12 19" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                )}
              </button>
            </div>
          </form>
          <p className="text-center text-xs text-base-content/40 mt-3">
            DeepLuna 可能会犯错。请核实重要信息。
          </p>
        </div>
      </div>

      {/* Global Error Toast */}
      {(createSessionMutation.error || sendMessageMutation.error) && (
        <div className="toast toast-top toast-end z-50">
          <div className="alert alert-error shadow-lg">
            <span>{getErrorMessage(createSessionMutation.error || sendMessageMutation.error)}</span>
          </div>
        </div>
      )}
    </div>
  )
}
