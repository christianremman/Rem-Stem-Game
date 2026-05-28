import { expect, test } from '@playwright/test'

const lobbyRoom = {
  code: 'E2E123',
  state: 'LOBBY',
  config: { spinIntervalMinutes: 0.5, intensity: 'NORMAL' },
  players: [],
  activeRules: [],
  spinCount: 0
}

test.beforeEach(async ({ page }) => {
  await page.route('**/ws/**', route => route.fulfill({ status: 404, body: '' }))
})

test('host can create a room with the 30-second test interval', async ({ page }) => {
  let configPayload: any = null

  await page.route('**/api/rooms', async route => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ roomCode: 'E2E123', hostToken: 'host-token' })
      })
      return
    }
    await route.continue()
  })

  await page.route('**/api/rooms/E2E123/config', async route => {
    configPayload = route.request().postDataJSON()
    await route.fulfill({ status: 200, body: '' })
  })

  await page.route('**/api/rooms/E2E123', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(lobbyRoom)
    })
  })

  await page.goto('/')
  await page.locator('select').first().selectOption('0.5')
  await page.getByRole('button', { name: 'Create Room' }).click()

  await expect(page).toHaveURL(/\/host\/E2E123$/)
  await expect(page.getByText('E2E123').first()).toBeVisible()
  await expect(page.getByRole('button', { name: 'Start' })).toBeVisible()
  expect(configPayload).toMatchObject({ spinIntervalMinutes: 0.5, intensity: 'NORMAL' })
})

test('player can join an existing room and reaches the player view', async ({ page }) => {
  await page.route('**/api/rooms/ABC123/players', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ playerId: 'p1', playerName: 'Alice' })
    })
  })

  await page.route('**/api/rooms/ABC123', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ...lobbyRoom,
        code: 'ABC123',
        config: { spinIntervalMinutes: 15, intensity: 'NORMAL' },
        players: [{ id: 'p1', name: 'Alice', connected: true, powerUps: ['SAFE'] }]
      })
    })
  })

  await page.goto('/')
  await page.getByPlaceholder('Room code').fill('ABC123')
  await page.getByPlaceholder('Your name').fill('Alice')
  await page.getByRole('button', { name: 'Join Game' }).click()

  await expect(page).toHaveURL(/\/play\/ABC123$/)
  await expect(page.getByText('Hi, Alice!')).toBeVisible()
  await expect(page.getByText('Waiting for host to start')).toBeVisible()
})
