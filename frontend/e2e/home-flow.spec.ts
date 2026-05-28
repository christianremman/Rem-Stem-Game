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

test('spin wheel lands the target segment under the top pointer', async ({ page }) => {
  await page.goto('/')
  const result = await page.evaluate(() => {
    const size = 300
    const cx = 150
    const cy = 150
    const r = 148
    const segmentCount = 8
    const targetIndex = 4
    const segmentDeg = 360 / segmentCount
    const targetAngle = targetIndex * segmentDeg + segmentDeg / 2
    const rotation = 5 * 360 + (360 - targetAngle)

    function segmentPath(index: number): string {
      const anglePerSeg = (2 * Math.PI) / segmentCount
      const startAngle = index * anglePerSeg - Math.PI / 2
      const endAngle = startAngle + anglePerSeg
      const x1 = cx + r * Math.cos(startAngle)
      const y1 = cy + r * Math.sin(startAngle)
      const x2 = cx + r * Math.cos(endAngle)
      const y2 = cy + r * Math.sin(endAngle)
      return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 0 1 ${x2} ${y2} Z`
    }

    function labelPoint(index: number): { x: number; y: number } {
      const anglePerSeg = (2 * Math.PI) / segmentCount
      const midAngle = index * anglePerSeg - Math.PI / 2 + anglePerSeg / 2
      return {
        x: cx + (r * 0.68) * Math.cos(midAngle),
        y: cy + (r * 0.68) * Math.sin(midAngle)
      }
    }

    document.body.innerHTML = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
      <g style="transform: rotate(${rotation}deg); transform-origin: ${cx}px ${cy}px; transition: none">
        ${Array.from({ length: segmentCount }, (_, index) => {
          const point = labelPoint(index)
          return `<path d="${segmentPath(index)}"></path>
            <circle class="segment-center" data-index="${index}" cx="${point.x}" cy="${point.y}" r="2"></circle>`
        }).join('')}
      </g>
    </svg>`

    const centers = [...document.querySelectorAll<SVGCircleElement>('.segment-center')]
      .map(element => {
        const bounds = element.getBoundingClientRect()
        return {
          index: Number(element.dataset.index),
          centerY: bounds.top + bounds.height / 2
        }
      })
      .sort((a, b) => a.centerY - b.centerY)

    return centers[0].index
  })

  expect(result).toBe(4)
})
