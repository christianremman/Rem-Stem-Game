import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PowerUpBar from './PowerUpBar.vue'

describe('PowerUpBar', () => {
  it('renders only available power-ups and emits the selected type', async () => {
    const wrapper = mount(PowerUpBar, {
      props: { powerUps: ['SAFE', 'REVENGE'] }
    })

    expect(wrapper.text()).toContain('Safe Card')
    expect(wrapper.text()).toContain('Revenge')
    expect(wrapper.text()).not.toContain('Double Down')

    await wrapper.findAll('button')[0].trigger('click')

    expect(wrapper.emitted('use')?.[0]).toEqual(['REVENGE'])
  })

  it('shows an empty state when no power-ups are available', () => {
    const wrapper = mount(PowerUpBar, {
      props: { powerUps: [] }
    })

    expect(wrapper.text()).toContain('No power-ups left')
    expect(wrapper.findAll('button')).toHaveLength(0)
  })
})
