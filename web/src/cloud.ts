export type AppArea = 'user' | 'admin'
export type UserRole = 'user' | 'admin' | 'support' | 'analyst' | 'billing' | 'content'

export interface CloudSession {
  userId: string
  displayName: string
  role: UserRole
  accessToken: string
}

export interface CloudJournalRecord {
  id: string
  month: string
  encryptedObjectKey: string
  version: number
  updatedAt: string
}

export interface CloudControlPlane {
  getSession(): Promise<CloudSession | null>
  getJournalManifest(userId: string): Promise<CloudJournalRecord[]>
  saveSettings(userId: string, settings: Record<string, string | boolean>): Promise<void>
}

/** Production implementation will call Cloudflare Workers and Supabase. */
export const cloudBoundary: CloudControlPlane = {
  async getSession() {
    return null
  },
  async getJournalManifest() {
    return []
  },
  async saveSettings() {
    return undefined
  },
}
