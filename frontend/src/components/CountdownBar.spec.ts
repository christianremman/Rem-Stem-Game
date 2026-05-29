import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import CountdownBar from './CountdownBar.vue'

describe('CountdownBar', () => {
  it('formats remaining seconds as mm:ss', () => {
    const wrapper = mount(CountdownBar, {
      props: { secondsRemaining: 65, totalSeconds: 120 }
    })

    expect(wrapper.text()).toContain('01:05')
  })

  it('shows placeholder when no countdown is active', () => {
    const wrapper = mount(CountdownBar, {
      props: { secondsRemaining: null, totalSeconds: 120 }
    })

    expect(wrapper.text()).toContain('--:--')
    expect(wrapper.find('.h-full.transition-all').attributes('style')).toContain('width: 100%')
  })

  it('clamps negative remaining time to zero', () => {
    const wrapper = mount(CountdownBar, {
      props: { secondsRemaining: -4, totalSeconds: 120 }
    })

    expect(wrapper.text()).toContain('00:00')
    expect(wrapper.find('.h-full.transition-all').attributes('style')).toContain('width: 0%')
  })
})
