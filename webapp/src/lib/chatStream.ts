import { API_BASE_URL, TOKEN_STORAGE_KEY } from './deepluna'

export type ChatStreamEvent = 'response' | 'end' | 'error' | 'message'

export type ChatStreamPayload = {
  sessionId?: string
  message?: string
  sessionStatus?: string
  code?: number
  [key: string]: unknown
}

type StreamHandlers = {
  onResponse?: (payload: ChatStreamPayload) => void
  onEnd?: (payload: ChatStreamPayload) => void
  onError?: (payload: ChatStreamPayload) => void
}

type ParsedSseEvent = {
  event: ChatStreamEvent
  data: string
}

function parseEventBlock(block: string): ParsedSseEvent | null {
  const lines = block.split(/\r?\n/)
  let event: ChatStreamEvent = 'message'
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      const name = line.slice(6).trim()
      if (name === 'response' || name === 'end' || name === 'error') {
        event = name
      } else {
        event = 'message'
      }
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }

  if (dataLines.length === 0) {
    return null
  }

  return {
    event,
    data: dataLines.join('\n'),
  }
}

function tryParsePayload(raw: string): ChatStreamPayload {
  try {
    const parsed = JSON.parse(raw) as ChatStreamPayload
    return parsed
  } catch {
    return { message: raw }
  }
}

export async function streamChatResponse(
  payload: { sessionId: string; message: string; modelId?: string },
  handlers?: StreamHandlers,
): Promise<{ finalMessage: string; endPayload?: ChatStreamPayload }> {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY) || ''
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json',
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}/v1/chat/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(`聊天请求失败: ${response.status} ${body}`)
  }

  if (!response.body) {
    return { finalMessage: '' }
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let mergedMessage = ''
  let endPayload: ChatStreamPayload | undefined

  const handleEventBlock = (block: string) => {
    const parsedEvent = parseEventBlock(block)
    if (!parsedEvent) {
      return
    }

    const eventPayload = tryParsePayload(parsedEvent.data)
    const messageChunk =
      (typeof eventPayload.message === 'string' && eventPayload.message) || ''

    if (parsedEvent.event === 'response') {
      mergedMessage += messageChunk
      handlers?.onResponse?.(eventPayload)
      return
    }

    if (parsedEvent.event === 'end') {
      endPayload = eventPayload
      mergedMessage = messageChunk || mergedMessage
      handlers?.onEnd?.(eventPayload)
      return
    }

    if (parsedEvent.event === 'error') {
      handlers?.onError?.(eventPayload)
      throw new Error(
        (typeof eventPayload.message === 'string' && eventPayload.message) || 'SSE 错误',
      )
    }

    mergedMessage += messageChunk
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r\n/g, '\n')

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const block = buffer.slice(0, separatorIndex)
      buffer = buffer.slice(separatorIndex + 2)
      handleEventBlock(block)
      separatorIndex = buffer.indexOf('\n\n')
    }
  }

  if (buffer.trim()) {
    handleEventBlock(buffer)
  }

  return {
    finalMessage: mergedMessage.trim(),
    endPayload,
  }
}
