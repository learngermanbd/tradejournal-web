import type { Trade } from './domain'

export interface VaultBucket {
  get(key: string): Promise<{ arrayBuffer(): Promise<ArrayBuffer> } | null>
  put(key: string, value: ArrayBuffer | string, options?: { httpMetadata?: { contentType?: string } }): Promise<unknown>
}

export class EncryptedTradeStore {
  constructor(private readonly bucket: VaultBucket, private readonly encryptionKey: string) {}

  async getTrades(userId: string): Promise<Trade[]> {
    const object = await this.bucket.get(keyFor(userId))
    if (!object) return []
    const payload = await object.arrayBuffer()
    return JSON.parse(await decrypt(payload, this.encryptionKey)) as Trade[]
  }

  async saveTrades(userId: string, trades: Trade[]): Promise<void> {
    const encrypted = await encrypt(JSON.stringify(trades), this.encryptionKey)
    await this.bucket.put(keyFor(userId), encrypted, { httpMetadata: { contentType: 'application/octet-stream' } })
  }
}

export function keyFor(userId: string) {
  return `users/${encodeURIComponent(userId)}/vault/trades.json`
}

async function encrypt(plaintext: string, encodedKey: string): Promise<ArrayBuffer> {
  const key = await importKey(encodedKey)
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const encoded = new TextEncoder().encode(plaintext)
  const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encoded)
  const output = new Uint8Array(iv.byteLength + ciphertext.byteLength)
  output.set(iv, 0)
  output.set(new Uint8Array(ciphertext), iv.byteLength)
  return output.buffer
}

async function decrypt(payload: ArrayBuffer, encodedKey: string): Promise<string> {
  const bytes = new Uint8Array(payload)
  if (bytes.byteLength <= 12) throw new Error('Invalid vault payload')
  const key = await importKey(encodedKey)
  const plaintext = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: bytes.slice(0, 12) }, key, bytes.slice(12))
  return new TextDecoder().decode(plaintext)
}

async function importKey(encodedKey: string): Promise<CryptoKey> {
  const raw = decodeBase64(encodedKey)
  if (raw.byteLength !== 32) throw new Error('VAULT_ENCRYPTION_KEY must decode to 32 bytes')
  const keyBytes = new Uint8Array(raw.byteLength)
  keyBytes.set(raw)
  return crypto.subtle.importKey('raw', keyBytes.buffer, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt'])
}

function decodeBase64(value: string): Uint8Array {
  const binary = atob(value)
  return Uint8Array.from(binary, (character) => character.charCodeAt(0))
}
