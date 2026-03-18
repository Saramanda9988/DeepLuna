import { useEffect, useState, useRef } from 'react'
import { apiClient, API_BASE_URL, TOKEN_STORAGE_KEY } from '../lib/deepluna'
import type { SessionProgressSnapshotResponse } from '../../api/models/SessionProgressSnapshotResponse'

export function useSessionProgress(sessionId?: string) {
  const [progress, setProgress] = useState<SessionProgressSnapshotResponse | null>(null)
  const [error, setError] = useState<Error | null>(null)
  const abortControllerRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (!sessionId) {
      setProgress(null)
      return
    }

    const abortController = new AbortController()
    abortControllerRef.current = abortController

    const fetchInitialAndStream = async () => {
      try {
        // Fetch initial snapshot
        const res = await apiClient.sessionProgressController.getSessionProgress(sessionId)
        if (res.success && res.data) {
          setProgress(res.data)
        }

        // Start SSE stream
        const token = localStorage.getItem(TOKEN_STORAGE_KEY) || ''
        const headers: Record<string, string> = {
          Accept: 'text/event-stream',
        }
        if (token) {
          headers.Authorization = `Bearer ${token}`
        }

        const response = await fetch(`${API_BASE_URL}/v1/session/progress/${sessionId}/stream`, {
          headers,
          signal: abortController.signal
        })

        if (!response.ok) {
          throw new Error(`SSE连接失败: ${response.statusText}`)
        }

        if (!response.body) return

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { value, done } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          buffer = buffer.replace(/\r\n/g, '\n')

          let separatorIndex = buffer.indexOf('\n\n')
          while (separatorIndex >= 0) {
            const block = buffer.slice(0, separatorIndex)
            buffer = buffer.slice(separatorIndex + 2)
            
            // Parse Event
            const lines = block.split(/\n/)
            let eventType = 'message'
            let eventData = ''
            
            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventType = line.slice(6).trim()
              } else if (line.startsWith('data:')) {
                eventData += line.slice(5).trim()
              }
            }

            if (eventType === 'progress' && eventData) {
              try {
                const parsedEvent = JSON.parse(eventData)
                const snapshot = parsedEvent.snapshot as SessionProgressSnapshotResponse
                if (snapshot) {
                  setProgress(snapshot)
                }
              } catch (e) {
                console.error('解析进度事件失败', e)
              }
            }

            separatorIndex = buffer.indexOf('\n\n')
          }
        }
      } catch (err: any) {
        if (err.name === 'AbortError') return
        setError(err)
        console.error('Session progress stream error:', err)
      }
    }

    fetchInitialAndStream()

    return () => {
      abortController.abort()
    }
  }, [sessionId])

  return { progress, error }
}
