import { API_BASE_URL, TOKEN_STORAGE_KEY } from './deepluna'

export async function streamChatResponse(payload: {
  sessionId: string
  message: string
  modelId?: string
}): Promise<string> {
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
    return ''
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let output = ''

  const appendEventData = (eventBlock: string) => {
    const lines = eventBlock.split(/\r?\n/)
    for (const line of lines) {
      if (!line.startsWith('data:')) {
        continue
      }

      const payloadLine = line.slice(5).trim()
      if (!payloadLine || payloadLine === '[DONE]') {
        continue
      }

      try {
        const parsed = JSON.parse(payloadLine) as Record<string, unknown>
        const text =
          (typeof parsed.delta === 'string' && parsed.delta) ||
          (typeof parsed.content === 'string' && parsed.content) ||
          (typeof parsed.message === 'string' && parsed.message) ||
          (typeof parsed.text === 'string' && parsed.text) ||
          (typeof parsed.data === 'string' && parsed.data) ||
          ''
        output += text || payloadLine
      } catch {
        output += payloadLine
      }
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''

    for (const block of blocks) {
      appendEventData(block)
    }
  }

  if (buffer) {
    appendEventData(buffer)
  }

  return output.trim()
}
