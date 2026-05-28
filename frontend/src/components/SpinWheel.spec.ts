import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import SpinWheel from './SpinWheel.vue'

describe('SpinWheel', () => {
  const segments = [
    { label: 'Alice', color: '#f00' },
    { label: 'Bob', color: '#0f0' },
    { label: 'Charlie', color: '#00f' }
  ]

  it('renders one wheel segment per provided segment', () => {
    const wrapper = mount(SpinWheel, {
      props: { segments, size: 300 }
    })

    expect(wrapper.findAll('path')).toHaveLength(3)
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('Bob')
    expect(wrapper.text()).toContain('Charlie')
  })

  it('rotates to the target angle when spinning starts', async () => {
    vi.useFakeTimers()
    const wrapper = mount(SpinWheel, {
      props: { segments, size: 300, spinning: false, targetAngle: 90 }
    })

    await wrapper.setProps({ spinning: true })
    await nextTick()

    const wheelGroup = wrapper.find('svg g')
    expect(wheelGroup.attributes('style')).toContain('rotate(2070deg)')
    expect(wheelGroup.attributes('style')).toContain('transition: transform 4s')

    vi.advanceTimersByTime(4200)
    await nextTick()

    expect(wheelGroup.attributes('style')).toContain('transition: none')
    vi.useRealTimers()
  })
})
