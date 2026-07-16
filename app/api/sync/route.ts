import { createHash, timingSafeEqual } from "node:crypto"
import { GoogleAuth } from "google-auth-library"
import { NextResponse } from "next/server"

export const runtime = "nodejs"

const MAX_MESSAGES = 500
const MAX_BODY_BYTES = 1_000_000
const SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"

type SyncMessage = {
  timestamp: number
  phoneNumber: string
  senderName: string
  messageText: string
  conversationName: string
  source: string
  direction: string
  uniqueId: string
}

function unauthorized() {
  return NextResponse.json({ error: "Unauthorized" }, { status: 401 })
}

function isString(value: unknown, maxLength: number): value is string {
  return typeof value === "string" && value.length <= maxLength
}

function isMessage(value: unknown): value is SyncMessage {
  if (!value || typeof value !== "object") return false
  const message = value as Record<string, unknown>
  return (
    typeof message.timestamp === "number" &&
    Number.isFinite(message.timestamp) &&
    message.timestamp > 0 &&
    isString(message.phoneNumber, 64) &&
    isString(message.senderName, 256) &&
    isString(message.messageText, 20_000) &&
    isString(message.conversationName, 256) &&
    isString(message.source, 32) &&
    isString(message.direction, 32) &&
    isString(message.uniqueId, 256) &&
    message.uniqueId.length > 0
  )
}

function parseServiceAccount() {
  const raw = process.env.GOOGLE_SERVICE_ACCOUNT_JSON
  if (!raw) throw new Error("Google service account is not configured")

  try {
    const credentials = JSON.parse(raw)
    if (!credentials.client_email || !credentials.private_key) throw new Error()
    return credentials
  } catch {
    throw new Error("Google service account configuration is invalid")
  }
}

function isAuthorized(request: Request): boolean {
  const authorization = request.headers.get("authorization")
  if (!authorization?.startsWith("Bearer ")) return false

  const actualToken = authorization.slice("Bearer ".length)
  const configuredToken = process.env.APP_SYNC_TOKEN
  if (configuredToken) {
    const actual = Buffer.from(actualToken)
    const expected = Buffer.from(configuredToken)
    if (actual.length === expected.length && timingSafeEqual(actual, expected)) return true
  }

  const configuredDigest = process.env.APP_SYNC_TOKEN_SHA256?.trim().toLowerCase()
  if (!configuredDigest || !/^[a-f0-9]{64}$/.test(configuredDigest)) return false

  const actualDigest = createHash("sha256").update(actualToken).digest()
  const expectedDigest = Buffer.from(configuredDigest, "hex")
  return actualDigest.length === expectedDigest.length && timingSafeEqual(actualDigest, expectedDigest)
}

export async function POST(request: Request) {
  if (!isAuthorized(request)) return unauthorized()

  const contentLength = Number(request.headers.get("content-length") ?? "0")
  if (contentLength > MAX_BODY_BYTES) {
    return NextResponse.json({ error: "Request is too large" }, { status: 413 })
  }

  let body: unknown
  try {
    body = await request.json()
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 })
  }

  const messages = (body as { messages?: unknown })?.messages
  if (!Array.isArray(messages) || messages.length < 1 || messages.length > MAX_MESSAGES) {
    return NextResponse.json({ error: `Send between 1 and ${MAX_MESSAGES} messages` }, { status: 400 })
  }
  if (!messages.every(isMessage)) {
    return NextResponse.json({ error: "One or more messages are invalid" }, { status: 400 })
  }

  const spreadsheetId = process.env.GOOGLE_SPREADSHEET_ID
  if (!spreadsheetId) {
    return NextResponse.json({ error: "Spreadsheet is not configured" }, { status: 503 })
  }

  try {
    const auth = new GoogleAuth({ credentials: parseServiceAccount(), scopes: [SHEETS_SCOPE] })
    const client = await auth.getClient()
    const accessToken = await client.getAccessToken()
    if (!accessToken.token) throw new Error("Google access token unavailable")

    const normalizePhone = (value: string) => {
      const trimmed = value.trim()
      const digits = trimmed.replace(/\D/g, "")
      return digits.length >= 7 ? `${trimmed.startsWith("+") ? "+" : ""}${digits}` : ""
    }

    const incomingByPhone = new Map<string, SyncMessage>()
    for (const message of [...messages].sort((a, b) => a.timestamp - b.timestamp)) {
      const phone = normalizePhone(message.phoneNumber)
      if (phone && !incomingByPhone.has(phone)) incomingByPhone.set(phone, { ...message, phoneNumber: phone })
    }
    if (incomingByPhone.size === 0) {
      return NextResponse.json({ error: "Every lead requires a valid mobile number" }, { status: 400 })
    }

    const metadataResponse = await fetch(
      `https://sheets.googleapis.com/v4/spreadsheets/${encodeURIComponent(spreadsheetId)}?fields=sheets.properties(title,index,hidden)`,
      { headers: { Authorization: `Bearer ${accessToken.token}` } },
    )
    if (!metadataResponse.ok) throw new Error("Unable to read spreadsheet metadata")
    const metadata = (await metadataResponse.json()) as {
      sheets?: Array<{ properties?: { title?: string; index?: number; hidden?: boolean } }>
    }
    const sheetProperties = metadata.sheets
      ?.map((sheet) => sheet.properties)
      .filter((properties): properties is { title: string; index?: number; hidden?: boolean } => Boolean(properties?.title)) ?? []
    const firstSheet = sheetProperties
      .filter((properties) => !properties.hidden && properties.title !== "_whatsapp_lead_index")
      .sort((a, b) => (a.index ?? 0) - (b.index ?? 0))[0]
    if (!firstSheet) throw new Error("Spreadsheet has no visible sheet")
    const quotedSheet = `'${firstSheet.title.replace(/'/g, "''")}'`

    if (!sheetProperties.some((properties) => properties.title === "_whatsapp_lead_index")) {
      const createIndexResponse = await fetch(
        `https://sheets.googleapis.com/v4/spreadsheets/${encodeURIComponent(spreadsheetId)}:batchUpdate`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken.token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            requests: [{ addSheet: { properties: { title: "_whatsapp_lead_index", hidden: true } } }],
          }),
        },
      )
      if (!createIndexResponse.ok) throw new Error("Unable to create lead duplicate index")
    }

    const indexRange = encodeURIComponent("'_whatsapp_lead_index'!A:A")
    const existingResponse = await fetch(
      `https://sheets.googleapis.com/v4/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${indexRange}`,
      { headers: { Authorization: `Bearer ${accessToken.token}` } },
    )
    if (!existingResponse.ok) throw new Error("Unable to read lead duplicate index")
    const existingBody = (await existingResponse.json()) as { values?: unknown[][] }
    const existingPhones = new Set(
      (existingBody.values ?? []).map((row) => normalizePhone(String(row[0] ?? ""))).filter(Boolean),
    )

    const leads = [...incomingByPhone.values()].filter((lead) => !existingPhones.has(lead.phoneNumber))
    const skipped = messages.length - leads.length
    if (leads.length === 0) return NextResponse.json({ appended: 0, skipped })

    const values = leads.map((lead) => [
      lead.phoneNumber,
      lead.senderName,
      new Date(lead.timestamp).toISOString(),
      lead.messageText,
    ])

    const range = encodeURIComponent(`${quotedSheet}!A:D`)
    const response = await fetch(
      `https://sheets.googleapis.com/v4/spreadsheets/${encodeURIComponent(spreadsheetId)}/values/${range}:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken.token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ majorDimension: "ROWS", values }),
      },
    )

    if (!response.ok) {
      const details = await response.text()
      console.error("[v0] Google Sheets append failed", response.status, details.slice(0, 500))
      return NextResponse.json({ error: "Google Sheets rejected the update" }, { status: 502 })
    }

    return NextResponse.json({ appended: leads.length, skipped })
  } catch (error) {
    console.error("[v0] Sheets sync failed", error instanceof Error ? error.message : "Unknown error")
    return NextResponse.json({ error: "Sheets sync is temporarily unavailable" }, { status: 503 })
  }
}
